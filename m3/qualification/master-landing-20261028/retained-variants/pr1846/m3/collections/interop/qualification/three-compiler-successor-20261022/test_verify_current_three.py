# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Adversarial source-sealed one-owner M3JDK21 compiler repair receipts."""
import hashlib
from pathlib import Path
import tempfile
import unittest
import verify_current_three as proof


ROOT = Path(__file__).resolve().parent
EXPECTED_RECEIPT = ROOT / 'receipts' / 'changed-current-03.tsv'


class ThreeSourceCompilerProofTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.ledger = self.root / 'changed-current-03.tsv'
        self.original = EXPECTED_RECEIPT.read_bytes()
        self.ledger.write_bytes(self.original)

    def verify(self):
        return proof.exact_receipt(self.root)

    def test_three_exact_compiler_sources_admitted(self):
        rows = self.verify()
        self.assertEqual(3, len(rows))
        self.assertEqual(
            {proof.TEST_PREFIX + row[0] for row in proof.EXPECTED}, set(rows))

    def test_missing_or_symlinked_ledger_rejected(self):
        self.ledger.unlink()
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            self.verify()
        external = self.root / 'external.tsv'
        external.write_bytes(self.original)
        self.ledger.symlink_to(external)
        with self.assertRaisesRegex(ValueError, 'missing or aliased'):
            self.verify()

    def test_extra_or_missing_record_refused(self):
        self.ledger.write_bytes(self.original + self.original.splitlines()[0] + b'\n')
        with self.assertRaisesRegex(ValueError, 'exactly three'):
            self.verify()
        self.ledger.write_bytes(b'\n'.join(self.original.splitlines()[:2]) + b'\n')
        with self.assertRaisesRegex(ValueError, 'exactly three'):
            self.verify()

    def test_out_of_order_duplicate_and_altered_path_refused(self):
        lines = self.original.splitlines()
        self.ledger.write_bytes(b'\n'.join([lines[1], lines[0], lines[2]]) + b'\n')
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()
        self.ledger.write_bytes(b'\n'.join([lines[0], lines[0], lines[2]]) + b'\n')
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()
        self.ledger.write_bytes(self.original.replace(b'M3ApacheCollectionsTest.java',
                                                       b'M3UnreviewedCollectionsTest.java'))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()

    def test_forged_old_and_new_sha256_refused(self):
        row = proof.EXPECTED[0]
        self.ledger.write_bytes(self.original.replace(row[1].encode(), b'f' * 64))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()
        self.ledger.write_bytes(self.original.replace(row[3].encode(), b'a' * 64))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()

    def test_forged_original_and_generated_git_blobs_refused(self):
        row = proof.EXPECTED[1]
        self.ledger.write_bytes(self.original.replace(row[2].encode(), b'0' * 40))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()
        self.ledger.write_bytes(self.original.replace(row[4].encode(), b'0' * 40))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()

    def test_forged_recipe_status_and_source_generation_refused(self):
        self.ledger.write_bytes(self.original.replace(
            proof.SOURCE_REVISION_RECIPE.encode(), b'unreviewed-source-editor'))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()
        self.ledger.write_bytes(self.original.replace(
            b'PENDING_JAVA21_INTEROP_FULL_SUITE', b'QUALIFIED_WITHOUT_CI'))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()
        self.ledger.write_bytes(self.original.replace(b'HISTORICAL_36', b'ADDED_40'))
        with self.assertRaisesRegex(ValueError, 'source lineage'):
            self.verify()

    def test_git_blob_hash_has_known_canonical_git_framing(self):
        self.assertEqual('ce013625030ba8dba906f756967f9e9ca394464a',
                         proof.git_blob(b'hello\n'))
        self.assertEqual(hashlib.sha256(self.original).hexdigest(),
                         hashlib.sha256(EXPECTED_RECEIPT.read_bytes()).hexdigest())


if __name__ == '__main__':
    unittest.main()
