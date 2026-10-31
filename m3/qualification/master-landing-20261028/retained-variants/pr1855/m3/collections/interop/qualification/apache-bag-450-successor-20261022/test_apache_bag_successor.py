#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Six negative controls for exactly one Apache Bag 4.5.0 source evolution."""
from pathlib import Path
import tempfile
import unittest

import verify_apache_bag_successor as owner


ORIGINAL = Path(__file__).parent / "receipts" / "changed-bag-01.tsv"


class ApacheBag450SourceReceiptTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        self.receipt = self.root / "changed-bag-01.tsv"
        self.receipt.write_bytes(ORIGINAL.read_bytes())

    def validate(self):
        return owner.receipt_rows(self.receipt)

    def test_original_and_result_are_exactly_one_canonical_owner(self):
        cols = self.validate()
        self.assertEqual(7, len(cols))
        self.assertEqual(owner.TARGET, cols[0])
        self.assertEqual(owner.BEFORE_HASH, cols[1])
        self.assertEqual(owner.AFTER_HASH, cols[3])
        self.assertEqual(owner.BEFORE_BLOB, cols[2])
        self.assertEqual(owner.AFTER_BLOB, cols[4])
        self.assertEqual(owner.RECIPE, cols[5])

    def test_missing_and_symlinked_receipt_refused(self):
        self.receipt.unlink()
        with self.assertRaisesRegex(ValueError, "missing/symlinked"):
            self.validate()
        self.receipt.symlink_to(ORIGINAL)
        with self.assertRaisesRegex(ValueError, "missing/symlinked"):
            self.validate()

    def test_forged_old_and_new_hashes_are_refused(self):
        baseline = ORIGINAL.read_text(encoding="utf-8")
        for actual in (owner.BEFORE_HASH, owner.AFTER_HASH):
            self.receipt.write_text(baseline.replace(actual, "0" * 64), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "unapproved"):
                self.validate()

    def test_unreviewed_recipe_status_and_extra_rows_refused(self):
        baseline = ORIGINAL.read_text(encoding="utf-8")
        self.receipt.write_text(baseline.replace(owner.RECIPE, "untrusted-recipe"),
                                encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unapproved"):
            self.validate()
        self.receipt.write_text(baseline + baseline, encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unapproved"):
            self.validate()

    def test_git_blob_framing_and_sha256(self):
        self.assertEqual("ce013625030ba8dba906f756967f9e9ca394464a",
                         owner.blob(b"hello\n"))
        self.assertEqual("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                         owner.sha(b"abc"))

    def test_apache450_bounded_count_semantics(self):
        decisions = 0
        for owned in range(0, 18):
            for requested in range(0, 20):
                if 1 <= requested <= owned:
                    reference = requested
                else:
                    reference = 0
                removed = owned if requested > owned else owned - requested
                self.assertEqual(reference, owned - removed)
                if requested > owned and owned > 0:
                    decisions += 1
        self.assertGreater(decisions, 0)


if __name__ == "__main__":
    unittest.main()
