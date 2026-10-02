# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
import difflib
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('text_port', Path(__file__).with_name('text-port.py'))
port = importlib.util.module_from_spec(spec); spec.loader.exec_module(port)

class TextPortTest(unittest.TestCase):
    def setUp(self):
        self.scratch = tempfile.TemporaryDirectory(prefix='m3-port-test-')
        self.addCleanup(self.scratch.cleanup)
        base = Path(self.scratch.name)
        self.source, self.target, self.output = (base / name for name in ['donor', 'candidate', 'out'])
        self.source.mkdir(); self.target.mkdir()
        self.before, self.after = b'class Leaf {}\n', b'class Leaf { int value; }\n'
        donor = self.source / 'Leaf.java'; donor.write_bytes(self.before)
        def git(*args): return subprocess.check_output(['git', '-C', str(self.source), *args])
        git('init', '--quiet'); git('add', 'Leaf.java')
        git('-c', 'user.name=M3 test', '-c', 'user.email=m3-test@example.invalid', 'commit', '-qm', 'donor')
        revision = git('rev-parse', 'HEAD').decode().strip()
        self.path = 'm3/ports/text/src/Leaf.java'
        self.proof = 'm3/evidence/proof.json'
        def put(path, data):
            target = self.target / path; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(data)
        put(self.path, self.after)
        patch = ''.join(difflib.unified_diff(self.before.decode().splitlines(True), self.after.decode().splitlines(True),
                                            fromfile='a/' + self.path, tofile='b/' + self.path)).encode()
        self.patchpath = 'm3/ports/text/adapter.patch'; put(self.patchpath, patch)
        sha = lambda data: hashlib.sha256(data).hexdigest()
        self.provenance = {'source_commit': revision, 'adapter_patch': self.patchpath,
                           'adapter_patch_sha256': sha(patch), 'files': [{'source_path':'Leaf.java',
                           'target_path':self.path, 'sha256':sha(self.before), 'target_sha256':sha(self.after)}]}
        self.mapping = {'schema':2, 'mapping_schema':'mindex-to-m3/v2', 'mappings':[{'mapping_id':'fixture',
                        'sources':[{'sha256':sha(self.before), 'commit':revision}],
                        'destinations':[{'path':self.path, 'sha256':sha(self.after)}],
                        'tests':{'evidence':[self.proof]}, 'recipe':{'rollback':'reverse'}}]}
        self.write_json('m3/ports/text/provenance.json', self.provenance)
        self.write_json('m3/docs/name-mapping.json', self.mapping)
        self.write_json(self.proof, {'source_hashes':{'src/Leaf.java':sha(self.after)}, 'records':[{'exit':0}]})
    def write_json(self, path, value):
        destination = self.target / path; destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps(value), encoding='utf-8')
    def test_replay_is_raw_git_bound_and_idempotent(self):
        # Deliberately dirty/missing working donor does not replace committed authority.
        (self.source / 'Leaf.java').write_text('uncommitted corruption')
        self.assertEqual(port.replay(self.target, self.source, self.output), 1)
        self.assertEqual((self.output / self.path).read_bytes(), self.after)
        self.assertEqual(port.replay(self.target, self.source, self.output), 1)
    def test_conflict_refuses_before_effects(self):
        target = self.output / self.path; target.parent.mkdir(parents=True); target.write_bytes(b'user change')
        with self.assertRaisesRegex(ValueError, 'modified destination'):
            port.replay(self.target, self.source, self.output)
        self.assertEqual(target.read_bytes(), b'user change')
    def test_patch_drift_refuses(self):
        (self.target / self.patchpath).write_bytes(b'altered')
        with self.assertRaisesRegex(ValueError, 'patch drift'): port.validate(self.target, self.source)
        self.assertFalse(self.output.exists())
    def test_missing_or_forged_evidence_refuses(self):
        self.write_json(self.proof, {'source_hashes':{}, 'records':[{'exit':0}]})
        with self.assertRaisesRegex(ValueError, 'unbound candidate'): port.validate(self.target)
    def test_target_drift_refuses(self):
        (self.target / self.path).write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'target divergence'): port.validate(self.target)
    def test_unmapped_and_duplicate_refuse(self):
        self.mapping['mappings'] *= 2; self.write_json('m3/docs/name-mapping.json', self.mapping)
        with self.assertRaisesRegex(ValueError, 'duplicate'): port.validate(self.target)
        self.mapping['mappings'] = []; self.write_json('m3/docs/name-mapping.json', self.mapping)
        with self.assertRaisesRegex(ValueError, 'unmapped'): port.validate(self.target)
    def test_source_pin_drift_refuses(self):
        self.provenance['files'][0]['sha256'] = '0' * 64
        self.mapping['mappings'][0]['sources'][0]['sha256'] = '0' * 64
        self.write_json('m3/ports/text/provenance.json', self.provenance)
        self.write_json('m3/docs/name-mapping.json', self.mapping)
        with self.assertRaisesRegex(ValueError, 'donor source drift'): port.validate(self.target, self.source)

if __name__ == '__main__': unittest.main()
