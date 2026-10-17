#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify source-sealed generation-safe object-state projection and denial cases."""
import tempfile
import unittest
from pathlib import Path
from m3_object_state_offset_receipt import (
    EXPECTED, ROOT, RECEIPT, approved_successor, include_successor, sha256,
)


class ObjectStateOffsetReceivingTest(unittest.TestCase):
    def test_exact_two_java_postimages_and_unmodified_historical_rows(self):
        approved = approved_successor()
        self.assertEqual(2, len(approved))
        older = [("m3/collections/old.java", "a" * 64)]
        result = include_successor(older)
        self.assertEqual(older, result[:1])
        self.assertEqual({row[0] for row in EXPECTED}, {p for p, _ in result[1:]})
        for path, before, after in EXPECTED:
            self.assertEqual(after, sha256(ROOT / path))
            self.assertEqual(after, dict(result)[path])
            self.assertFalse((ROOT / path).is_symlink())

    def test_preimage_must_match_if_historical_row_already_exists(self):
        approved = approved_successor()
        target, before, after = EXPECTED[0]
        self.assertEqual([(target, after)], include_successor([(target, before)], approved))
        with self.assertRaises(ValueError):
            include_successor([(target, "0" * 64)], approved)
        with self.assertRaises(ValueError):
            include_successor([(target, before), (target, before)], approved)

    def test_tampering_missing_reordering_duplicate_refuses(self):
        contents = RECEIPT.read_text(encoding="utf-8")
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / "receipt.tsv"
            for changed in (
                contents.replace(EXPECTED[0][1], "0" * 64, 1),
                contents + contents.splitlines()[0] + "\n",
                "\n".join(reversed(contents.strip().splitlines())) + "\n",
                contents.replace("\t", " ", 1),
            ):
                file.write_text(changed, encoding="utf-8")
                with self.assertRaises(ValueError):
                    approved_successor(file)
            with self.assertRaises(ValueError):
                approved_successor(Path(directory) / "missing.tsv")

    def test_symlinked_receipt_refuses(self):
        with tempfile.TemporaryDirectory() as directory:
            alias = Path(directory) / "receipt.tsv"
            alias.symlink_to(RECEIPT)
            with self.assertRaises(ValueError):
                approved_successor(alias)


if __name__ == "__main__":
    unittest.main()
