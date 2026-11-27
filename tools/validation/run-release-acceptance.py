"""Run ELS-47 on a clean tracked checkout, isolated PostgreSQL and a packaged JAR."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import sys
import time
import urllib.request
import uuid
import xml.etree.ElementTree as ET
import zipfile

sys.dont_write_bytecode = True
from release_acceptance import ReleaseAcceptance

ROOT = Path(__file__).resolve().parents[2]


def command(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, **kwargs)


def free_port():
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        return listener.getsockname()[1]


def junit(folder):
    suites = [ET.parse(p).getroot() for p in (ROOT / 'target' / folder).glob('TEST-*.xml')]
    if not suites:
        raise AssertionError('JUnit evidence missing')
    result = {k: sum(int(s.get(k, 0)) for s in suites) for k in ['tests', 'failures', 'errors', 'skipped']}
    if any(result[k] for k in ['failures', 'errors', 'skipped']):
        raise AssertionError('Full regression must pass without skips')
    result['methods'] = sorted(s.get('name') + '#' + t.get('name') for s in suites for t in s.findall('testcase'))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--prepared-build', action='store_true', help='Use the full Maven verification already completed in this CI job')
    args = parser.parse_args()
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    if subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=no'], cwd=ROOT, text=True).strip():
        raise SystemExit('A clean tracked checkout is required')
    run_id = uuid.uuid4().hex
    folder = ROOT / 'target/els47-acceptance' / run_id
    folder.mkdir(parents=True)
    output = ROOT / 'target/els47-acceptance/results.json'
    name = 'els47-acceptance-' + run_id[:12]
    db_port, app_port = free_port(), free_port()
    while app_port == db_port:
        app_port = free_port()
    db_password, app_password, staff_password = [secrets.token_urlsafe(24) + ' spaced # literal$' for _ in range(3)]
    env = dict(os.environ, POSTGRES_DB='els47_integration_test', POSTGRES_USER='els47_tests', POSTGRES_PASSWORD=db_password,
               ELS_TEST_DB_URL=f'jdbc:postgresql://127.0.0.1:{db_port}/els47_integration_test',
               ELS_TEST_DB_USERNAME='els47_tests', ELS_TEST_DB_PASSWORD=db_password,
               ELS_DB_URL=f'jdbc:postgresql://127.0.0.1:{db_port}/els47_workflow_test',
               ELS_DB_USERNAME='els47_app', ELS_DB_PASSWORD=app_password,
               ELS_STAFF_USERNAME='els47-staff', ELS_STAFF_PASSWORD=staff_password)
    java = str(Path(env['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java'))
    java_version = subprocess.run([java, '-version'], capture_output=True, text=True, check=True).stderr
    if not re.search(r'version "17\.', java_version):
        raise SystemExit('Java 17 required')
    report = {'card': 'ELS-47', 'sourceCommit': source, 'status': 'failed', 'cleanTrackedCheckout': True,
              'runId': run_id, 'environment': {'java': re.search(r'version "([^"]+)', java_version).group(1),
              'postgresql': '17.5', 'transport': 'loopback HTTP', 'sqlTransport': 'native psql through task-owned docker exec',
              'testClock': '2030-06-15T09:00:00Z', 'database': 'els47_workflow_test', 'user': 'els47_app'},
              'preparedBuild': args.prepared_build, 'commands': [], 'cleanup': {'status': 'pending'}}
    owned = False
    process = None
    app_log = None
    workflow = None
    phase = 'database setup'
    try:
        command(['docker', 'run', '-d', '--name', name, '--label', 'els.card=ELS-47', '--label', 'els.run=' + run_id,
                 '--tmpfs', '/var/lib/postgresql/data', '-p', f'127.0.0.1:{db_port}:5432',
                 '-e', 'POSTGRES_DB', '-e', 'POSTGRES_USER', '-e', 'POSTGRES_PASSWORD', 'postgres:17.5-alpine'],
                env=env, capture_output=True, text=True)
        owned = True
        for _ in range(100):
            if subprocess.run(['docker', 'exec', name, 'pg_isready', '-U', 'els47_tests', '-d', 'els47_integration_test'], capture_output=True).returncode == 0:
                break
            time.sleep(.2)
        else:
            raise AssertionError('Isolated PostgreSQL readiness failed')
        sql = f"CREATE ROLE els47_app LOGIN PASSWORD '{app_password}';\nCREATE DATABASE els47_workflow_test OWNER els47_app;\n"
        command(['docker', 'exec', '-i', name, 'psql', '-U', 'els47_tests', '-d', 'els47_integration_test', '-v', 'ON_ERROR_STOP=1'], input=sql, text=True, capture_output=True)
        phase = 'full regression'
        if not args.prepared_build:
            wrapper = str(ROOT / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))
            for label, options in [('standard', []), ('database', ['-Pdatabase-test'])]:
                invocation = [wrapper, '--batch-mode', '--no-transfer-progress'] + options + ['verify']
                with (folder / (label + '.log')).open('w') as log:
                    command(invocation, env=env, stdout=log, stderr=subprocess.STDOUT)
                report['commands'].append({'command': 'mvnw ' + ' '.join(invocation[1:]), 'exitCode': 0})
        report['unit'] = junit('surefire-reports')
        report['integration'] = junit('failsafe-reports')
        jar = ROOT / 'target/equipment-lending-system-0.0.1-SNAPSHOT.jar'
        report['jarSha256'] = hashlib.sha256(jar.read_bytes()).hexdigest()
        with zipfile.ZipFile(jar) as archive:
            if any('elsvalidation/' in p or p.endswith('IT.class') or 'DatabaseTestSafetyProcessor' in p for p in archive.namelist()):
                raise AssertionError('Test-only classes must not enter delivery JAR')
            packaged_config = archive.read('BOOT-INF/classes/application.yml')
            config_text = packaged_config.decode().replace('\r\n', '\n')
            if 'ddl-auto: validate' not in config_text or 'flyway:\n    enabled: true' not in config_text:
                raise AssertionError('Packaged schema policy must remain Flyway plus JPA validate')
        fixture = folder / 'fixed-clock.jar'
        with zipfile.ZipFile(fixture, 'w') as archive:
            archive.write(ROOT / 'target/test-classes/elsvalidation/FixedClockConfiguration.class', 'elsvalidation/FixedClockConfiguration.class')
        phase = 'packaged HTTP acceptance'
        app_log = (folder / 'application.log').open('w')
        process = subprocess.Popen([java, '-Dloader.path=' + str(fixture), '-cp', str(jar),
                  'org.springframework.boot.loader.launch.PropertiesLauncher', '--spring.main.sources=elsvalidation.FixedClockConfiguration',
                  '--spring.profiles.active=validation-workflow', '--server.address=127.0.0.1', f'--server.port={app_port}'],
                  cwd=ROOT, env=env, stdout=app_log, stderr=subprocess.STDOUT,
                  **({'creationflags': subprocess.CREATE_NO_WINDOW} if os.name == 'nt' else {}))
        base_url = f'http://127.0.0.1:{app_port}'
        for _ in range(240):
            if process.poll() is not None:
                raise AssertionError('Packaged application exited before readiness')
            try:
                with urllib.request.urlopen(base_url + '/actuator/health', timeout=1) as response:
                    if response.status == 200 and json.load(response) == {'status': 'UP'}:
                        break
            except (OSError, ValueError):
                pass
            time.sleep(.25)
        else:
            raise AssertionError('Packaged readiness deadline exceeded')
        saved = dict(os.environ)
        try:
            os.environ.update(env)
            workflow = ReleaseAcceptance(argparse.Namespace(base_url=base_url, psql='docker', container=name, expected_database='els47_workflow_test'))
            workflow.run()
        finally:
            os.environ.clear(); os.environ.update(saved)
        app_log.flush()
        logs = (folder / 'application.log').read_text(errors='replace')
        forbidden = [db_password, app_password, staff_password, workflow.authorization,
                     '@els47.example.test', '@els36.example.test', 'Key (email)=', 'synthetic acceptance failure']
        if any(value in logs for value in forbidden):
            raise AssertionError('Private/student data appeared in production application logs')
        report['productionApplicationLogPrivacy'] = 'passed after real conflicts and forced failures'
        report['staticChecks'] = {'deliveryJarExcludesTestClasses': True,
                                 'packagedConfigurationSha256': hashlib.sha256(packaged_config).hexdigest(),
                                 'schemaOwner': 'Flyway migrations with Hibernate validate'}
        report['status'] = 'passed'
        report['commands'].append({'command': 'packaged JAR + release_acceptance.ReleaseAcceptance', 'exitCode': 0})
    except Exception as error:
        report['failure'] = {'phase': phase, 'type': type(error).__name__, 'message': 'See ignored task-local logs; no private exception payload retained'}
        print('Acceptance failed in ' + phase + ': ' + type(error).__name__, file=sys.stderr)
        raise
    finally:
        if process and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill(); process.wait(timeout=5)
        if app_log:
            app_log.close()
        if owned:
            try:
                info = json.loads(subprocess.check_output(['docker', 'inspect', name], text=True))[0]
                if info['Config']['Labels'].get('els.run') != run_id:
                    raise AssertionError('Cleanup ownership mismatch')
                command(['docker', 'rm', '-f', '-v', name], capture_output=True, text=True)
                report['cleanup'] = {'status': 'passed', 'scope': 'owned Java process and labeled tmpfs PostgreSQL container; no host files or other projects removed'}
            except Exception:
                report['cleanup'] = {'status': 'failed', 'scope': 'owned resource only; manual recovery required'}
                report['status'] = 'failed'
        else:
            report['cleanup'] = {'status': 'no container created'}
        if workflow:
            report.update(requestCount=len(workflow.requests), assertions=workflow.assertions, requests=workflow.requests,
                          sqlChecks=workflow.database_checks, races=workflow.races, events=workflow.events,
                          baseline=workflow.baseline if hasattr(workflow, 'baseline') else None)
        output.write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps({k: report[k] for k in ['status', 'sourceCommit', 'cleanup']}))
    if report['status'] != 'passed':
        raise SystemExit(1)


if __name__ == '__main__':
    main()
