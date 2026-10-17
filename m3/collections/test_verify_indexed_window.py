#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed source readback for independent row/mask offset JNI selection."""
import tempfile
import unittest
from pathlib import Path

from m3_window_mask_receipt import (
    EXPECTED, RECEIPT, ROOT, approved_successor, qualified_hash, sha256,
)


class IndexedWindowReceiptTest(unittest.TestCase):
    def test_exact_three_source_files_and_immutable_predecessors(self):
        approved = approved_successor()
        self.assertEqual(len(approved), 3)
        for path, before, after in EXPECTED:
            self.assertEqual(after, qualified_hash(path, before, approved))
            self.assertEqual(after, sha256(ROOT / path))
            self.assertFalse((ROOT / path).is_symlink())
        self.assertEqual("a" * 64, qualified_hash("src/other.java", "a" * 64, approved))

    def test_wrong_historical_predecessor_never_promotes(self):
        approved = approved_successor()
        with self.assertRaises(ValueError):
            qualified_hash(EXPECTED[0][0], "0" * 64, approved)

    def test_tampered_missing_duplicate_or_reordered_receipt_refuses(self):
        raw = RECEIPT.read_text(encoding="utf-8")
        with tempfile.TemporaryDirectory() as tmp:
            candidate = Path(tmp) / "source-receipt.tsv"
            for text in (
                raw.replace(EXPECTED[0][1], "0" * 64, 1),
                raw + raw.splitlines()[0] + "\n",
                "\n".join(reversed(raw.strip().splitlines())) + "\n",
                raw.replace("\t", " ", 1),
            ):
                candidate.write_text(text, encoding="utf-8")
                with self.assertRaises(ValueError):
                    approved_successor(candidate)
            with self.assertRaises(ValueError):
                approved_successor(Path(tmp) / "missing.tsv")

    def test_symlinked_receipt_refuses(self):
        with tempfile.TemporaryDirectory() as tmp:
            alias = Path(tmp) / "receipt.tsv"
            alias.symlink_to(RECEIPT)
            with self.assertRaises(ValueError):
                approved_successor(alias)


if __name__ == "__main__":
    unittest.main()
