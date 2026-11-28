"""Delivery safety regressions using isolated Docker fakes and loopback HTTP."""
import contextlib
import base64
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import threading
import time
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
ROOT = Path(__file__).resolve().parents[2]
SHELL = os.environ.get('ELS_HELPER_TEST_SHELL') or shutil.which('powershell') or shutil.which('pwsh')
PROJECT = 'els-demo-helper-test'
STAFF_USER = 'synthetic-staff'
STAFF_PASSWORD = 'synthetic spaced # literal$ password'
STAFF_AUTHORIZATION = 'Basic '+base64.b64encode(f'{STAFF_USER}:{STAFF_PASSWORD}'.encode()).decode()
def q(value): return "'" + str(value).replace("'", "''") + "'"

class Helpers(unittest.TestCase):
    def setUp(self):
        (ROOT / 'target').mkdir(exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(dir=ROOT / 'target'); self.addCleanup(self.directory.cleanup)
        self.folder = Path(self.directory.name); self.state_file = self.folder / 'state.json'
        self.env_file = self.folder / '.env'; self.env_file.write_text('ELS_DB_PASSWORD="synthetic password"\n')
        self.port = 18099; self.calls = []
    def state(self, **changes):
        state = dict(version=2, project=PROJECT, checkout=str(ROOT), baseUrl=f'http://127.0.0.1:{self.port}', databaseId='equipment_lending/42/987654321', status='complete', studentId=1, cameraId=2, laptopId=3, activeLoanId=4, returnedLoanId=5, today='2030-06-15', activeDueDate='2030-06-22', previousBorrowingDate='2030-06-08', previousDueDate='2030-06-14')
        state.update(changes); self.state_file.write_text(json.dumps(state)); return state
    def invoke(self, body, failure=False, test_config=False, down_failure=False, config_password="synthetic password", staff_password=STAFF_PASSWORD, live_staff_password=STAFF_PASSWORD):
        labels = {'com.docker.compose.project':PROJECT, 'com.docker.compose.project.working_dir':str(ROOT)}
        app = {'Config':{'Labels':dict(labels, **{'com.docker.compose.service':'app'}), 'Env':['ELS_DB_URL=jdbc:postgresql://postgres:5432/equipment_lending','ELS_DB_USERNAME=equipment_app']}, 'NetworkSettings':{'Ports':{'8080/tcp':[{'HostIp':'127.0.0.1','HostPort':str(self.port)}]}}}
        db = {'Config':{'Labels':dict(labels, **{'com.docker.compose.service':'postgres'})}, 'NetworkSettings':{'Ports':{'5432/tcp':None}}}
        config = {'services':{'app':{'ports':[{'host_ip':'127.0.0.1','target':8080,'published':str(self.port)}]}, 'postgres':{'environment':{'POSTGRES_DB':'equipment_lending_test' if test_config else 'equipment_lending','POSTGRES_USER':'equipment_app','POSTGRES_PASSWORD':config_password}}}, 'volumes':{'postgres-data':{'name':PROJECT+'_postgres-data'}}}
        config['services']['app']['environment']={'ELS_STAFF_USERNAME':STAFF_USER,'ELS_STAFF_PASSWORD':staff_password.replace('$','$$')}
        app['Config']['Env'] += ['ELS_STAFF_USERNAME='+STAFF_USER,'ELS_STAFF_PASSWORD='+live_staff_password]
        if test_config: config['services']['postgres']['ports']=[{'host_ip':'127.0.0.1','target':5432,'published':'15499'}]
        prefix='\n'.join(f'$global:{name} = {q(json.dumps(value))}' for name,value in [('MockConfig',config),('MockApp',[app]),('MockDb',[db])])
        prefix += '\n$global:MockDownFailure = '+('$true' if down_failure else '$false')
        prefix += r'''
function global:docker {
 param([Parameter(ValueFromRemainingArguments=$true)][object[]]$DockerArguments)
 $global:LASTEXITCODE=0; $line=$DockerArguments -join ' '
 if($line -match 'config --format json'){return $global:MockConfig}
 if($line -match 'ps -a -q$'){return}
 if($line -match 'ps -q app$'){return 'app-id'}
 if($line -match 'ps -q postgres$'){return 'db-id'}
 if($line -eq 'inspect app-id'){return $global:MockApp}
 if($line -eq 'inspect db-id'){return $global:MockDb}
 if($line -match '^exec '){return 'equipment_lending|equipment_app|42|987654321'}
 if($line -match 'down -v$'){if($global:MockDownFailure){$global:LASTEXITCODE=17};return}
 throw "Unexpected fake Docker call: $line"
}
'''
        script=self.folder/'run.ps1'; script.write_text("$ErrorActionPreference='Stop'\n"+prefix+'\n'+body,encoding='utf-8-sig')
        result=subprocess.run([SHELL,'-NoProfile','-ExecutionPolicy','Bypass','-File',str(script)],cwd=ROOT,capture_output=True,text=True,timeout=12)
        if failure: self.assertNotEqual(result.returncode,0,result.stdout)
        else: self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        return result
    def command(self, script, extra=''):
        return f'& {q(ROOT/"tools/demo"/script)} -ProjectName {PROJECT} -EnvFile {q(self.env_file)} {extra}'
    @contextlib.contextmanager
    def api(self, wrong_name=False):
        calls=self.calls
        class Handler(BaseHTTPRequestHandler):
            def do_GET(inner):
                calls.append(('GET',inner.path))
                if inner.path.startswith('/api/') and inner.headers.get('Authorization') != STAFF_AUTHORIZATION:
                    inner.send_error(401);return
                if inner.path=='/actuator/health': value={'status':'UP'}
                elif inner.path.startswith('/api/students/'): value=dict(id=1,name='Different Borrower' if wrong_name else 'ELS Demo Borrower',email='els45.demo.borrower@example.test')
                elif inner.path=='/api/equipment': value=[dict(id=2,inventoryCode='ELS45-CAM-001',name='ELS Demo Camera',category='Camera',description='Synthetic delivery demonstration item',available=False),dict(id=3,inventoryCode='ELS45-LAP-001',name='ELS Demo Laptop',category='Laptop',description='Synthetic returned-history item',available=True)]
                else: value=[dict(id=4,studentId=1,equipmentId=2,borrowingDate='2030-06-15',dueDate='2030-06-22',returnDate=None),dict(id=5,studentId=1,equipmentId=3,borrowingDate='2030-06-08',dueDate='2030-06-14',returnDate='2030-06-15')]
                data=json.dumps(value).encode();inner.send_response(200);inner.send_header('Content-Type','application/json');inner.send_header('Content-Length',str(len(data)));inner.end_headers();inner.wfile.write(data)
            def do_POST(inner): calls.append(('POST',inner.path));inner.send_error(500)
            def log_message(self,*_): pass
        server=ThreadingHTTPServer(('127.0.0.1',0),Handler);self.port=server.server_port
        thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        try: yield
        finally: server.shutdown();server.server_close();thread.join()
    def test_checkpoint_creation_and_replacement(self):
        self.invoke(f". {q(ROOT/'tools/demo/common.ps1')}\nWrite-ElsDemoState {q(self.state_file)} @{{status='seeding'}}\nWrite-ElsDemoState {q(self.state_file)} @{{status='complete'}}")
        self.assertEqual(json.loads(self.state_file.read_text())['status'],'complete')
        self.assertFalse(list(self.folder.glob('*.tmp')))
    def test_failed_reset_preserves_checkpoint(self):
        self.state();original=self.state_file.read_bytes()
        self.invoke(self.command('reset-demo.ps1',f'-ConfirmReset -StatePath {q(self.state_file)}'),failure=True,down_failure=True)
        self.assertEqual(self.state_file.read_bytes(),original)
    def test_successful_reset_removes_only_its_checkpoint(self):
        self.state();sibling=self.folder/'other.json';sibling.write_text('preserve')
        self.invoke(self.command('reset-demo.ps1',f'-ConfirmReset -StatePath {q(self.state_file)}'))
        self.assertFalse(self.state_file.exists());self.assertEqual(sibling.read_text(),'preserve')
    def test_reset_requires_confirmation(self):
        self.state();self.invoke(self.command('reset-demo.ps1',f'-StatePath {q(self.state_file)}'),failure=True);self.assertTrue(self.state_file.exists())
    def test_seed_rejects_wrong_url_before_requests(self):
        with self.api(): self.invoke(self.command('seed-demo.ps1',f'-BaseUrl http://example.invalid:{self.port} -StatePath {q(self.state_file)}'),failure=True)
        self.assertEqual(self.calls,[]);self.assertFalse(self.state_file.exists())
    def test_seed_rejects_database_fingerprint_mismatch(self):
        with self.api():
            self.state(databaseId='other/database/cluster');self.invoke(self.command('seed-demo.ps1',f'-StatePath {q(self.state_file)}'),failure=True)
        self.assertEqual(self.calls,[])
    def test_seed_rejects_other_borrower_with_same_id(self):
        with self.api(wrong_name=True):
            self.state();self.invoke(self.command('seed-demo.ps1',f'-StatePath {q(self.state_file)}'),failure=True)
        self.assertEqual([verb for verb,_ in self.calls],['GET'])
    def test_seed_valid_rerun_is_read_only(self):
        with self.api():
            self.state();original=self.state_file.read_bytes();self.invoke(self.command('seed-demo.ps1',f'-StatePath {q(self.state_file)}'))
        self.assertEqual(len(self.calls),3);self.assertTrue(all(verb=='GET' for verb,_ in self.calls));self.assertEqual(self.state_file.read_bytes(),original)
    def test_interrupted_seed_does_not_retry_writes(self):
        with self.api():
            self.state(status='seeding');self.invoke(self.command('seed-demo.ps1',f'-StatePath {q(self.state_file)}'),failure=True)
        self.assertEqual(self.calls,[])
    def test_seed_rejects_missing_staff_secret_before_requests(self):
        with self.api():
            self.state();self.invoke(self.command('seed-demo.ps1',f'-StatePath {q(self.state_file)}'),failure=True,staff_password='')
        self.assertEqual(self.calls,[])
    def test_seed_rejects_changed_live_staff_secret_before_requests(self):
        with self.api():
            self.state();self.invoke(self.command('seed-demo.ps1',f'-StatePath {q(self.state_file)}'),failure=True,live_staff_password='other synthetic password')
        self.assertEqual(self.calls,[])
    def test_import_uses_resolved_password_and_port(self):
        self.invoke(self.command('import-test-environment.ps1')+r'''
if($env:ELS_TEST_DB_PASSWORD -cne 'synthetic password'){throw 'Wrong resolved password'}
if($env:ELS_TEST_DB_URL -ne 'jdbc:postgresql://127.0.0.1:15499/equipment_lending_test'){throw 'Wrong database target'}
''',test_config=True)
    def test_import_decodes_literal_dollars_once(self):
        self.invoke(self.command('import-test-environment.ps1')+"\nif($env:ELS_TEST_DB_PASSWORD -cne 'synthetic $$ password'){throw 'Literal dollar round trip changed'}",test_config=True,config_password='synthetic $$$$ password')
    def test_health_success(self):
        with self.api(): self.invoke(f'& {q(ROOT/"tools/demo/wait-for-health.ps1")} -BaseUrl http://127.0.0.1:{self.port} -TimeoutSeconds 2')
    def test_health_stalled_response_obeys_deadline(self):
        listener=socket.socket();listener.bind(('127.0.0.1',0));listener.listen(1);stop=threading.Event();accepted=threading.Event()
        def stall():
            try:
                conn,_=listener.accept();accepted.set()
                with conn: stop.wait(10)
            except OSError: pass
        thread=threading.Thread(target=stall,daemon=True);thread.start();start=time.monotonic()
        try:
            result=self.invoke(f'& {q(ROOT/"tools/demo/wait-for-health.ps1")} -BaseUrl http://127.0.0.1:{listener.getsockname()[1]} -TimeoutSeconds 1',failure=True)
            self.assertTrue(accepted.is_set());self.assertIn('within 1 seconds',result.stderr)
            self.assertLess(time.monotonic()-start,5,'HTTP request exceeded deadline plus startup allowance')
        finally: stop.set();listener.close();thread.join(timeout=2)
if __name__=='__main__':
    if not SHELL: raise SystemExit('PowerShell is required for helper safety checks')
    unittest.main(verbosity=2)
