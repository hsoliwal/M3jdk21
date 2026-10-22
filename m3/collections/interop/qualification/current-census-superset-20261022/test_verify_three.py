#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""One historical plus two additive Java21 test-source epochs; exact refusal tests."""
from pathlib import Path
import tempfile
import unittest
import verify_superset as census


HERE = Path(__file__).resolve().parent
CANONICAL = HERE / 'receipts'


class ThreeExactJava21TestRevisions(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        for name in ('changed-historical-01.tsv', 'changed-added-02.tsv'):
            (self.root / name).write_bytes((CANONICAL / name).read_bytes())
        self.old_historical = {
            'm3/collections/interop/src/test/java/com/m3/collections/interop/M3ApacheCollectionsTest.java':
            ('ede37d37ca4373ac7888da6542f7447730be822829f789ed82caaaf99dc2ec53',
             '9edf50007e098846f0d677204a445b6bc52b7f0d')
        }
        self.old_additive = {
            'm3/collections/interop/src/test/java/com/m3/collections/interop/M3FiveLibraryKeyRangesTest.java':
            ('f2f8adcccfee3366007330f84a630ddfb1f093e99cdac060952dddb59dda486d',
             '9698cccbf6ca77c7e6a48eec23cee7781838b19e'),
            'm3/collections/interop/src/test/java/com/m3/collections/interop/M3FiveProviderFlagSelectionTest.java':
            ('511c42a8f8cf2119770931307ef4c29f35e0fdbbc1ba438e170aa151905ca76e',
             '86939063fd00e472850add045086c07521ba29c3')
        }

    def historical(self):
        return census.read_sealed_java21_revisions(
            self.root, 'changed-historical-01.tsv',
            census.HISTORICAL_TEST_UPDATE_RECEIPT_BLOB,
            self.old_historical, census.HISTORICAL_TEST_UPDATES, required=True)

    def additive(self):
        return census.read_sealed_java21_revisions(
            self.root, 'changed-added-02.tsv',
            census.ADDITIVE_TEST_UPDATE_RECEIPT_BLOB,
            self.old_additive, census.ADDITIVE_TEST_UPDATES, required=True)

    def test_exact_one_historic_and_two_additive_method_preservation(self):
        original = self.historical()
        added = self.additive()
        self.assertEqual(1, len(original))
        self.assertEqual(2, len(added))
        self.assertEqual(13, sum(int(row[2]) for row in
                         [*census.HISTORICAL_TEST_UPDATES.values(),
                          *census.ADDITIVE_TEST_UPDATES.values()]))
        self.assertFalse(set(original) & set(added))

    def test_missing_receipt_is_refused(self):
        (self.root / 'changed-historical-01.tsv').unlink()
        with self.assertRaisesRegex(ValueError, 'missing required'):
            self.historical()

    def test_symlinked_receipt_is_refused(self):
        receipt = self.root / 'changed-added-02.tsv'
        receipt.unlink()
        receipt.symlink_to(CANONICAL / 'changed-added-02.tsv')
        with self.assertRaisesRegex(ValueError, 'symlinked'):
            self.additive()

    def test_forged_status_or_sha_is_not_authorized(self):
        receipt = self.root / 'changed-added-02.tsv'
        receipt.write_bytes(receipt.read_bytes().replace(
            b'PENDING_JAVA21_VENDOR_CI', b'QUALIFIED_WITHOUT_TESTS'))
        with self.assertRaisesRegex(ValueError, 'forged'):
            self.additive()

    def test_baseline_sha_drift_is_not_hidden_by_a_revision_receipt(self):
        path = next(iter(self.old_historical))
        self.old_historical[path] = ('f' * 64, self.old_historical[path][1])
        with self.assertRaisesRegex(ValueError, 'unreviewed'):
            self.historical()

    def test_duplicate_or_extra_revision_is_refused(self):
        receipt = self.root / 'changed-historical-01.tsv'
        receipt.write_bytes(receipt.read_bytes() + receipt.read_bytes())
        with self.assertRaisesRegex(ValueError, 'forged'):
            self.historical()


if __name__ == '__main__':
    unittest.main()
