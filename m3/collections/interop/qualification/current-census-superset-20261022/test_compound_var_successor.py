#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Negative-control tests of exactly one post-census Java21 syntax transition."""
import hashlib
from pathlib import Path
import shutil
import tempfile
import unittest
import verify_superset as census


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
ADMITTED_RECEIPT = HERE / "receipts" / "changed-01.tsv"


class CompoundVarSourceReceiptTest(unittest.TestCase):
    def fixture(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        receipts = root / "receipts"
        receipts.mkdir()
        shutil.copyfile(ADMITTED_RECEIPT, receipts / "changed-01.tsv")
        return receipts

    def test_exact_source_receipt_admitted(self):
        proof = self.fixture()
        result = census.exact_compound_transition(proof)
        self.assertEqual(result, census.COMPOUND_TRANSITION)
        self.assertEqual(result[0], census.COMPOUND_TEST_PATH)
        self.assertEqual(result[5], "PINNED_TEST_SYNTAX_ONLY")

    def test_forged_hash_rejected(self):
        proof = self.fixture()
        receipt = proof / "changed-01.tsv"
        receipt.write_bytes(receipt.read_bytes().replace(b"PINNED_TEST_SYNTAX_ONLY",
                                                         b"PINNED_TEST_SYNTAX_MORE"))
        with self.assertRaisesRegex(ValueError, "tampered"):
            census.exact_compound_transition(proof)

    def test_missing_and_symlinked_receipt_refused(self):
        proof = self.fixture()
        target = proof / "changed-01.tsv"
        target.unlink()
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            census.exact_compound_transition(proof)
        target.symlink_to(ADMITTED_RECEIPT)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            census.exact_compound_transition(proof)

    def test_old_forty_source_receipt_retained_verbatim(self):
        found = []
        for name in census.SHARDS:
            file = HERE / "receipts" / name
            self.assertTrue(file.is_file())
            for line in file.read_text(encoding="utf-8").splitlines():
                if line and not line.startswith("#"):
                    cells = line.split("\t")
                    self.assertEqual(6, len(cells))
                    if cells[0] == census.COMPOUND_TEST_PATH:
                        found.append(cells)
        self.assertEqual(1, len(found))
        self.assertEqual(census.COMPOUND_TRANSITION[1], found[0][1])
        self.assertEqual(census.COMPOUND_TRANSITION[2], found[0][2])

    def test_corrected_test_is_the_only_admitted_product_image(self):
        target = ROOT / census.COMPOUND_TEST_PATH
        self.assertTrue(target.is_file(), str(target))
        data = target.read_bytes()
        self.assertEqual(census.COMPOUND_TRANSITION[3], hashlib.sha256(data).hexdigest())
        self.assertEqual(census.COMPOUND_TRANSITION[4], census.git_blob(data))
        source = data.decode("utf-8")
        self.assertIn("var left=sorted.slice(0,3);", source)
        self.assertIn("var right=sorted.slice(3,5);", source)
        self.assertNotIn("var left=sorted.slice(0,3),right=sorted.slice(3,5);", source)


if __name__ == "__main__":
    unittest.main()
