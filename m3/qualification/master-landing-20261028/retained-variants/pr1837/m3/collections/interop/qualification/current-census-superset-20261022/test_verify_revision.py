# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Reject forged or missing source authorization for the Java21 arrayification repair."""
from pathlib import Path
import tempfile
import unittest
import verify_superset as census


ORIGINAL_SHA = '43d670c2278b4db3cb39e8c6d63eff56827c7ef8359e7c85aa8f72b892bba51d'
ORIGINAL_BLOB = '42d78b4fae7525467f0a5d4f34baf45b511e3b55'


class OneAdditiveRevisionContract(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.row = [
            census.APPROVED_ADDITIVE_TEST, ORIGINAL_SHA, ORIGINAL_BLOB,
            'five-provider-index', 'TEST', 'Apache-2.0']
        self.ledger = self.root / 'changed-added-01.tsv'
        self.columns = [
            census.APPROVED_ADDITIVE_TEST, ORIGINAL_SHA, ORIGINAL_BLOB,
            census.APPROVED_REPAIR_SHA256, census.APPROVED_REPAIR_GIT_BLOB,
            census.APPROVED_REPAIR_RECIPE, 'PENDING_JAVA21_VENDOR_CI']
        self.write()

    def write(self):
        self.ledger.write_text('\t'.join(self.columns) + '\n', encoding='utf-8')

    def verify(self):
        return census.read_admitted_additive_revision(self.root, [self.row], required=True)

    def test_one_exact_source_repair_is_admitted(self):
        self.assertEqual(
            self.verify(),
            {census.APPROVED_ADDITIVE_TEST:
                (census.APPROVED_REPAIR_SHA256, census.APPROVED_REPAIR_GIT_BLOB)})

    def test_required_receipt_and_symlink_are_refused(self):
        self.ledger.unlink()
        with self.assertRaisesRegex(ValueError, 'missing required'):
            self.verify()
        target = self.root / 'unrelated'
        target.write_text('\t'.join(self.columns), encoding='utf-8')
        self.ledger.symlink_to(target)
        with self.assertRaisesRegex(ValueError, 'aliased'):
            self.verify()

    def test_mutated_baseline_source_receipt_is_refused(self):
        self.row[1] = 'f' * 64
        with self.assertRaisesRegex(ValueError, 'unadmitted'):
            self.verify()

    def test_new_source_sha_or_blob_is_not_a_wildcard(self):
        self.columns[3] = '0' * 64
        self.write()
        with self.assertRaisesRegex(ValueError, 'unadmitted'):
            self.verify()
        self.columns[3] = census.APPROVED_REPAIR_SHA256
        self.columns[4] = '0' * 40
        self.write()
        with self.assertRaisesRegex(ValueError, 'unadmitted'):
            self.verify()

    def test_unknown_recipe_status_and_extra_rows_are_refused(self):
        self.columns[5] = 'not-canonical'
        self.write()
        with self.assertRaisesRegex(ValueError, 'unadmitted'):
            self.verify()
        self.columns[5] = census.APPROVED_REPAIR_RECIPE
        self.columns[6] = 'QUALIFIED_WITHOUT_CI'
        self.write()
        with self.assertRaisesRegex(ValueError, 'unadmitted'):
            self.verify()
        self.columns[6] = 'PENDING_JAVA21_VENDOR_CI'
        self.write()
        self.ledger.write_text(self.ledger.read_text() * 2)
        with self.assertRaisesRegex(ValueError, 'exactly one'):
            self.verify()


if __name__ == '__main__':
    unittest.main()
