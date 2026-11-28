"""Release guard regressions using isolated synthetic Git repositories."""
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import warnings
import zipfile

SPEC = importlib.util.spec_from_file_location('prepare_submission', Path(__file__).with_name('prepare_submission.py'))
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)


class CandidateTests(unittest.TestCase):
    def setUp(self):
        parent = release.ROOT / 'target/release-tests'
        parent.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix='synthetic-', dir=parent)
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.git('init', '-q')
        self.git('config', 'user.name', 'Synthetic Release Test')
        self.git('config', 'user.email', 'synthetic@example.test')
        self.git('config', 'core.autocrlf', 'false')
        self.write('README.md', b'Synthetic source fixture\n')
        for name in ['pom.xml', 'mvnw.cmd', '.env.example']:
            self.write(name, b'Synthetic test input\n')
        self.write('.mvn/wrapper/maven-wrapper.jar', b'SYNTHETIC TEST WRAPPER')
        self.write('third-party-licenses/wrapper/LICENSE', b'SYNTHETIC TEST NOTICE')
        self.inventory = {'components': [{'scope': 'redistributed-source-build-tool',
            'sha256': release.sha(b'SYNTHETIC TEST WRAPPER'),
            'materials': [{'path': 'third-party-licenses/wrapper/LICENSE',
                           'sha256': release.sha(b'SYNTHETIC TEST NOTICE')}]}]}
        self.save_inventory()
        self.commit()
        self.revision = self.git('rev-parse', 'HEAD').decode().strip()
        self.output = self.repo / 'target/release/candidate'

    def git(self, *args, data=None):
        return subprocess.check_output(['git', '-C', str(self.repo), *args], input=data, stderr=subprocess.DEVNULL)

    def write(self, name, data):
        path = self.repo / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def save_inventory(self):
        self.inventory['buildInputsSha256'] = {'pom.xml': release.sha(b'Synthetic test input\n')}
        self.write('third-party-licenses/inventory.json', json.dumps(self.inventory).encode())

    def commit(self):
        self.git('add', '.')
        self.git('commit', '-qm', 'Synthetic release fixture')

    def test_explicit_revision_ignores_dirty_and_untracked_files(self):
        self.write('README.md', b'Dirty working copy must not be exported')
        self.write('untracked.txt', b'Must not be exported')
        result = release.prepare(self.repo, self.revision, self.output)
        with zipfile.ZipFile(self.output / result['archive']) as z:
            self.assertEqual(z.read('README.md'), b'Synthetic source fixture\n')
            self.assertNotIn('untracked.txt', z.namelist())
        self.assertEqual(result['sourceRevision'], self.revision)
        self.assertEqual(result['archiveSha256'], hashlib.sha256((self.output / result['archive']).read_bytes()).hexdigest())
        again = release.prepare(self.repo, self.revision, self.repo / 'target/release/again')
        self.assertEqual(result, again)

    def test_private_and_binary_paths_rejected(self):
        for name in ['Subject/assignment.docx', 'docs/Subject/input.docx', '.env', '.env.private',
                     'target/app.jar', '.git/config', 'x/../secret', 'C:/secret', 'x\\secret',
                     'runtime.jar', 'keys/id_rsa', 'private.pfx', 'node_modules/file.js']:
            with self.subTest(name=name):
                self.assertFalse(release.safe_name(name))
        self.assertTrue(release.safe_name('.env.example'))
        self.assertTrue(release.safe_name('.mvn/wrapper/maven-wrapper.jar'))

    def test_windows_checkout_conversion_does_not_change_archive_bytes(self):
        self.git('config', 'core.autocrlf', 'true')
        self.git('config', 'core.eol', 'crlf')
        result = release.prepare(self.repo, self.revision, self.output)
        with zipfile.ZipFile(self.output / result['archive']) as z:
            self.assertEqual(z.read('README.md'), b'Synthetic source fixture\n')

    def test_tracked_runtime_binary_prevents_export(self):
        self.write('runtime.jar', b'SYNTHETIC RUNTIME')
        self.commit()
        with self.assertRaisesRegex(ValueError, 'Forbidden tracked'):
            release.prepare(self.repo, 'HEAD', self.output)
        self.assertFalse(self.output.exists())

    def test_missing_or_changed_notice_prevents_export(self):
        self.inventory['components'][0]['materials'][0]['sha256'] = '0' * 64
        self.save_inventory()
        self.commit()
        with self.assertRaisesRegex(ValueError, 'notice material missing or changed'):
            release.prepare(self.repo, 'HEAD', self.output)
        self.assertFalse(self.output.exists())

    def test_wrong_wrapper_identity_prevents_export(self):
        self.inventory['components'][0]['sha256'] = '0' * 64
        self.save_inventory()
        self.commit()
        with self.assertRaisesRegex(ValueError, 'Wrapper identity'):
            release.prepare(self.repo, 'HEAD', self.output)

    def test_changed_application_input_requires_fresh_inventory(self):
        self.write('pom.xml', b'Changed synthetic dependency definition\n')
        self.commit()
        with self.assertRaisesRegex(ValueError, 'build inputs differ'):
            release.prepare(self.repo, 'HEAD', self.output)

    def test_outside_destination_and_overwrite_prevented(self):
        with self.assertRaisesRegex(ValueError, 'new candidate directory'):
            release.prepare(self.repo, self.revision, self.repo / 'outside')
        release.prepare(self.repo, self.revision, self.output)
        with self.assertRaisesRegex(ValueError, 'new candidate directory'):
            release.prepare(self.repo, self.revision, self.output)

    def test_symlink_mode_prevents_export(self):
        oid = self.git('hash-object', '-w', '--stdin', data=b'../outside').decode().strip()
        self.git('update-index', '--add', '--cacheinfo', f'120000,{oid},unsafe-link')
        self.git('commit', '-qm', 'Synthetic symlink entry')
        with self.assertRaisesRegex(ValueError, 'Forbidden tracked path/mode'):
            release.prepare(self.repo, 'HEAD', self.output)

    def test_tampered_duplicate_and_missing_archive_members_rejected(self):
        expected = {'README.md': {'sha256': release.sha(b'expected')}}
        for members in [[('README.md', b'tampered')], [('README.md', b'expected'), ('extra', b'x')],
                        [('README.md', b'expected'), ('README.md', b'expected')], []]:
            buffer = io.BytesIO()
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                with zipfile.ZipFile(buffer, 'w') as z:
                    for name, value in members:
                        z.writestr(name, value)
            with self.assertRaises(ValueError):
                release.inspect_archive(buffer.getvalue(), expected)


if __name__ == '__main__':
    unittest.main(verbosity=2)
