"""Export and inspect a backend source candidate from one explicit Git commit."""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args])


def safe_name(name):
    path = PurePosixPath(name)
    forbidden = {'subject', '.git', 'target', 'node_modules', '__pycache__', '.venv'}
    if (path.is_absolute() or '..' in path.parts or '\\' in name or ':' in name or
            any(p.lower() in forbidden for p in path.parts)):
        return False
    base = path.name.lower()
    if base.endswith(('.jar', '.war', '.class', '.exe', '.dll', '.so', '.zip', '.tar', '.gz')) and name != '.mvn/wrapper/maven-wrapper.jar':
        return False
    if base.startswith('.env') and name != '.env.example':
        return False
    return base not in {'id_rsa', 'id_ed25519', 'credentials.json'} and not base.endswith(('.p12', '.pfx', '.key', '.log'))


def inspect_archive(data, expected):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        members = [x for x in archive.infolist() if not x.is_dir()]
        names = [m.filename for m in members]
        if len(names) != len({n.casefold() for n in names}) or set(names) != set(expected):
            raise ValueError('Archive member list differs from the exact Git tree.')
        if any(not safe_name(n) for n in names):
            raise ValueError('Unsafe/private archive member.')
        for member in members:
            if sha(archive.read(member)) != expected[member.filename]['sha256']:
                raise ValueError('Archive bytes differ from selected Git blob: ' + member.filename)


def prepare(repo, revision, output):
    repo, output = repo.resolve(), output.resolve()
    release_root = (repo / 'target/release').resolve()
    if not output.is_relative_to(release_root) or output == release_root or output.exists():
        raise ValueError('Use a new candidate directory below this checkout target/release; no overwrite.')
    commit = git(repo, 'rev-parse', '--verify', '--end-of-options', revision + '^{commit}').decode().strip()
    tree = git(repo, 'rev-parse', commit + '^{tree}').decode().strip()
    entries = git(repo, 'ls-tree', '-rz', commit).split(b'\0')
    files, content = {}, {}
    for entry in entries:
        if not entry:
            continue
        meta, raw_path = entry.split(b'\t', 1)
        mode, kind, oid = meta.decode().split()
        name = raw_path.decode('utf8')
        if kind != 'blob' or mode not in ('100644', '100755') or not safe_name(name):
            raise ValueError('Forbidden tracked path/mode: ' + name)
        data = git(repo, 'cat-file', 'blob', oid)
        content[name] = data
        files[name] = {'sha256': sha(data), 'bytes': len(data), 'gitBlob': oid, 'mode': mode}
    for required in ('README.md', 'pom.xml', 'mvnw.cmd', '.env.example', '.mvn/wrapper/maven-wrapper.jar',
                     'third-party-licenses/inventory.json'):
        if required not in files:
            raise ValueError('Required release input absent from selected revision: ' + required)
    inventory = json.loads(content['third-party-licenses/inventory.json'])
    inventoried_inputs = inventory.get('buildInputsSha256', {})
    selected_inputs = {n: item['sha256'] for n, item in files.items() if n == 'pom.xml' or n.startswith('src/')}
    if inventoried_inputs != selected_inputs:
        raise ValueError('Selected application build inputs differ from the notices audit; regenerate it.')
    wrapper = [c for c in inventory['components'] if c['scope'] == 'redistributed-source-build-tool']
    if len(wrapper) != 1 or wrapper[0]['sha256'] != files['.mvn/wrapper/maven-wrapper.jar']['sha256']:
        raise ValueError('Maven Wrapper identity is not covered by the selected notices inventory.')
    if not wrapper[0]['materials']:
        raise ValueError('Redistributed Maven Wrapper has no license/notice material.')
    for component in inventory['components']:
        for material in component['materials']:
            name = material['path']
            if name not in files or files[name]['sha256'] != material['sha256']:
                raise ValueError('Selected notice material missing or changed: ' + name)
    archive = git(repo, '-c', 'core.autocrlf=false', '-c', 'core.eol=lf', 'archive', '--format=zip', commit)
    inspect_archive(archive, files)
    manifest = {'schema': 'els-source-candidate-v1', 'status': 'candidate, not submitted or declared a final release',
                'scope': 'Backend source, team-authored documentation/slides and Maven Wrapper; no application runtime/JRE/image.',
                'sourceRevision': commit, 'sourceTree': tree, 'archive': 'equipment-lending-source.zip',
                'archiveSha256': sha(archive), 'memberCount': len(files),
                'noticeInventorySha256': files['third-party-licenses/inventory.json']['sha256'],
                'files': files}
    output.mkdir(parents=True, exist_ok=False)
    (output / manifest['archive']).write_bytes(archive)
    manifest_bytes = (json.dumps(manifest, indent=2) + '\n').encode('utf8')
    (output / 'manifest.json').write_bytes(manifest_bytes)
    (output / 'SHA256SUMS').write_text(f"{sha(archive)}  {manifest['archive']}\n{sha(manifest_bytes)}  manifest.json\n", encoding='ascii')
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--revision', required=True, help='Explicit reviewed/candidate commit; never defaults to a moving branch.')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = prepare(ROOT, args.revision, args.output)
    print(f"Inspected {result['memberCount']} exact Git members at {result['sourceRevision']}; candidate only.")
    print('Archive SHA-256: ' + result['archiveSha256'])


if __name__ == '__main__':
    main()
