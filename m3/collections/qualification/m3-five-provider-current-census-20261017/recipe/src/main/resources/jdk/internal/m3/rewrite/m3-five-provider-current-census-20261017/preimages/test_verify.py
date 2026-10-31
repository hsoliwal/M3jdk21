# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Tests for exact source-snapshot custody, including both original and successor."""
import hashlib
import tempfile
import unittest
from pathlib import Path
import verify

class SourceCensusTest(unittest.TestCase):
    def make_fixture(self, root):
        old = []
        now = []
        for i, path in enumerate(verify.ALLOWED_PATHS):
            payload = f'// deterministic fixture source {i}\n'.encode()
            f = root / path
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_bytes(payload)
            after = verify.sha256(payload)
            predecessor = 'ABSENT'
            if path.split('/')[-1] in (
                    'M3LibraryCollections.java', 'M3LibraryCollectors.java',
                    'M3SpringMultiValueMap.java', 'M3LibraryClosureTest.java',
                    'M3LibraryCollectionsTest.java'):
                predecessor = after if not path.endswith('/M3LibraryCollections.java') else 'a'*64
                old.append((path, 'ABSENT', predecessor, f.name + '.txt'))
            now.append((path, predecessor, after, verify.git_blob(payload)))
        old.sort()
        baseline = root / 'original.tsv'
        baseline.write_text('\n'.join('\t'.join(x) for x in old)+'\n')
        census = root / 'current.tsv'
        census.write_text('\n'.join('\t'.join(x) for x in now)+'\n')
        return census, baseline
    def test_five_historical_plus_eight_additive(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            current, old = self.make_fixture(root)
            allowed = verify.verify(root, current, old,
                verify.sha256(current.read_bytes()), verify.git_blob(old.read_bytes()), False)
            self.assertEqual(len(allowed), 13)
    def test_missing_drift_and_alias_refusal(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            current, old = self.make_fixture(root)
            digest = verify.sha256(current.read_bytes())
            oldblob = verify.git_blob(old.read_bytes())
            first = root / verify.ALLOWED_PATHS[0]
            first.write_bytes(first.read_bytes()+b'wrong\n')
            with self.assertRaisesRegex(ValueError, 'source drift'):
                verify.verify(root, current, old, digest, oldblob, False)
            first.unlink()
            first.symlink_to(root / verify.ALLOWED_PATHS[1])
            with self.assertRaisesRegex(ValueError, 'aliased'):
                verify.verify(root, current, old, digest, oldblob, False)
    def test_manifest_tamper_and_legacy_mutation_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            current, old = self.make_fixture(root)
            digest = verify.sha256(current.read_bytes())
            oldblob = verify.git_blob(old.read_bytes())
            current.write_bytes(current.read_bytes()+b'X')
            with self.assertRaisesRegex(ValueError, 'receipt'):
                verify.verify(root, current, old, digest, oldblob, False)
            current.write_bytes(current.read_bytes()[:-1])
            old.write_bytes(old.read_bytes()+b'# tampered\n')
            with self.assertRaisesRegex(ValueError, 'historical'):
                verify.verify(root, current, old, digest, oldblob, False)
    def test_unapproved_historical_owner_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            current, old = self.make_fixture(root)
            actual = current.read_text()
            # Even when receipt checksum is recomputed, an unrelated old owner cannot drift.
            owner = verify.MAIN + 'M3LibraryCollectors.java'
            line = next(x for x in actual.splitlines() if x.startswith(owner+'\t'))
            cells = line.split('\t')
            cells[2] = '0'*64
            current.write_text(actual.replace(line, '\t'.join(cells)))
            with self.assertRaisesRegex(ValueError, 'changed unapproved historical'):
                verify.verify(root, current, old, verify.sha256(current.read_bytes()),
                    verify.git_blob(old.read_bytes()), False)

if __name__ == '__main__':
    unittest.main()
