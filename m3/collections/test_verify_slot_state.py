#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Independent source receipt tests for 64-bit object-state JNI masks."""
import tempfile
import unittest
from pathlib import Path
from m3_slot_state_receipt import (
    EXPECTED, RECEIPT, ROOT, approved_successor, qualified_hash, sha256,
)


class SlotStateReceiptTest(unittest.TestCase):
    def test_exact_chained_source_postimages(self):
        accepted = approved_successor()
        self.assertEqual(4, len(accepted))
        for path, before, after in EXPECTED:
            self.assertEqual(after, qualified_hash(path, before, accepted))
            self.assertEqual(after, sha256(ROOT / path))
            self.assertFalse((ROOT / path).is_symlink())
        self.assertEqual("b" * 64, qualified_hash("unrelated.java", "b" * 64, accepted))

    def test_wrong_predecessor_never_qualifies_a_product(self):
        accepted = approved_successor()
        for path, _, _ in EXPECTED:
            with self.assertRaises(ValueError):
                qualified_hash(path, "a" * 64, accepted)

    def test_malformed_duplicate_reordered_and_drifted_receipts_refuse(self):
        original = RECEIPT.read_text(encoding="utf-8")
        with tempfile.TemporaryDirectory() as folder:
            candidate = Path(folder) / "bad.tsv"
            for changed in (
                original.replace(EXPECTED[0][2], "0" * 64),
                original + original.splitlines()[0] + "\n",
                "\n".join(reversed(original.strip().splitlines())) + "\n",
                original.replace("\t", " ", 1),
            ):
                candidate.write_text(changed, encoding="utf-8")
                with self.assertRaises(ValueError):
                    approved_successor(candidate)
            with self.assertRaises(ValueError):
                approved_successor(Path(folder) / "missing.tsv")

    def test_symlinked_source_receipt_refuses(self):
        with tempfile.TemporaryDirectory() as folder:
            alias = Path(folder) / "alias.tsv"
            alias.symlink_to(RECEIPT)
            with self.assertRaises(ValueError):
                approved_successor(alias)


if __name__ == "__main__":
    unittest.main()
