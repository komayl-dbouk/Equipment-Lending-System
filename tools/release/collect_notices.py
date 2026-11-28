"""Inventory exact packaged libraries and preserve their original notice bytes."""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def legal_member(name):
    base = PurePosixPath(name).name.lower()
    return (not name.endswith('/') and not base.endswith('.class') and
            bool(re.search(r'(?:license|notice|copying|copyright)', base)))


def pom_licenses(pom, repository, seen=None):
    seen = set() if seen is None else seen
    if not pom.is_file() or pom in seen:
        return []
    seen.add(pom)
    tree = ET.parse(pom)
    entries = tree.findall('m:licenses/m:license', NS)
    url = 'https://repo.maven.apache.org/maven2/' + pom.relative_to(repository).as_posix()
    if entries:
        return [{**{c.tag.split('}')[-1]: (c.text or '').strip() for c in item},
                 'metadataSource': url, 'metadataSha256': sha(pom.read_bytes())} for item in entries]
    parent = tree.find('m:parent', NS)
    if parent is None:
        return []
    parts = [parent.findtext('m:' + field, namespaces=NS) for field in ('groupId', 'artifactId', 'version')]
    if not all(parts) or any('${' in p or '/' in p or '\\' in p for p in parts):
        return []
    group, artifact, version = parts
    return pom_licenses(repository / group.replace('.', '/') / artifact / version /
                        f'{artifact}-{version}.pom', repository, seen)


def collect(jar_path, repository, output, revision, supplemental=None):
    repository = repository.resolve()
    output = output.resolve()
    if not output.is_relative_to(ROOT) or output == ROOT or output.exists():
        raise ValueError('Use a new output directory inside the repository; existing bundles are never overwritten.')
    commit = subprocess.check_output(['git', '-C', str(ROOT), 'rev-parse', '--verify', '--end-of-options',
                                      revision + '^{commit}']).decode().strip()
    tracked = subprocess.check_output(['git', '-C', str(ROOT), 'ls-tree', '-r', '--name-only', commit]).decode().splitlines()
    build_inputs = {}
    for name in tracked:
        if name == 'pom.xml' or name.startswith('src/'):
            original = subprocess.check_output(['git', '-C', str(ROOT), 'show', f'{commit}:{name}'])
            actual_blob = subprocess.check_output(['git', '-C', str(ROOT), 'hash-object', '--path', name, str(ROOT / name)]).decode().strip()
            expected_blob = subprocess.check_output(['git', '-C', str(ROOT), 'rev-parse', f'{commit}:{name}']).decode().strip()
            if actual_blob != expected_blob:
                raise ValueError('Build input differs from selected revision: ' + name)
            build_inputs[name] = sha(original)
    cache = {}
    for cache_root in [repository] + [p.resolve() for p in (supplemental or [])]:
        for path in cache_root.rglob('*.jar'):
            cache.setdefault(path.name, []).append((path, cache_root))
    output.mkdir(parents=True)
    components = []

    def add(data, filename, scope, origin):
        digest = sha(data)
        matches = [(p, r) for p, r in cache.get(filename, []) if sha(p.read_bytes()) == digest]
        if not matches or len({p.relative_to(r).as_posix() for p, r in matches}) != 1:
            raise ValueError('Expected one matching resolved Maven artifact: ' + filename)
        artifact, artifact_repository = matches[0]
        relative = artifact.relative_to(artifact_repository)
        group = '.'.join(relative.parts[:-3])
        artifact_id, version = relative.parts[-3:-1]
        component_id = f'{group}:{artifact_id}:{version}'
        folder = output / group / artifact_id / version
        materials = []
        with zipfile.ZipFile(io.BytesIO(data)) as nested:
            for member in sorted(nested.namelist()):
                if not legal_member(member):
                    continue
                posix = PurePosixPath(member)
                if posix.is_absolute() or '..' in posix.parts or '\\' in member:
                    raise ValueError('Unsafe upstream member')
                content = nested.read(member)
                target = folder.joinpath(*posix.parts)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(content)
                materials.append({'path': target.relative_to(ROOT).as_posix(), 'sha256': sha(content),
                                  'origin': origin + '!/' + member})
        licenses = pom_licenses(artifact.with_suffix('.pom'), artifact_repository)
        components.append({'id': component_id, 'filename': filename, 'sha256': digest, 'scope': scope,
                           'artifactUrl': 'https://repo.maven.apache.org/maven2/' + relative.as_posix(),
                           'licenses': licenses, 'materials': materials,
                           'embeddedMaterialStatus': 'preserved' if materials else 'no standalone legal text in artifact',
                           'binaryDistributionReview': 'not claimed; runtime binaries are excluded from the source candidate'})

    with zipfile.ZipFile(jar_path) as application:
        for member in sorted(application.namelist()):
            if member.startswith('BOOT-INF/lib/') and member.endswith('.jar'):
                add(application.read(member), PurePosixPath(member).name, 'runtime-reference-only', member)
    wrapper = ROOT / '.mvn/wrapper/maven-wrapper.jar'
    add(wrapper.read_bytes(), 'maven-wrapper-3.3.4.jar', 'redistributed-source-build-tool', '.mvn/wrapper/maven-wrapper.jar')
    result = {'schema': 'els-notices-v1', 'applicationBuildRevision': commit,
              'applicationJarSha256': sha(jar_path.read_bytes()), 'buildInputsSha256': build_inputs,
              'distribution': 'Backend source candidate; runtime JAR, JRE, image and database are not redistributed.',
              'components': components,
              'limits': ['Embedded text and resolved POM declarations are preserved, not a legal approval.',
                         'Some runtime artifacts have no standalone embedded notice text; listed individually.',
                         'Binary/image distribution requires its own complete obligation/source/base-image review.']}
    (output / 'inventory.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf8')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--revision', required=True)
    parser.add_argument('--maven-repository', type=Path, default=Path.home() / '.m2/repository')
    parser.add_argument('--supplemental-repository', type=Path, action='append', default=[])
    parser.add_argument('--output', type=Path, default=ROOT / 'third-party-licenses')
    args = parser.parse_args()
    result = collect(args.jar, args.maven_repository, args.output, args.revision, args.supplemental_repository)
    print(f"Preserved notices for {len(result['components'])} exact artifacts; runtime-reference scope is explicit.")


if __name__ == '__main__':
    main()
