#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Positive and adversarial source-custody gates for native64 mask batches."""
import tempfile
import unittest
from pathlib import Path
from m3_long_mask_receipt import approved_successor, effective_hash, digest, EXPECTED, RECEIPT


class LongMaskSuccessorTest(unittest.TestCase):
    def test_exact_three_atom_successor_proves_current_source_bytes(self):
        approved = approved_successor()
        self.assertEqual(3, len(approved))
        from m3_long_mask_receipt import REPO
        for path, before, after in EXPECTED:
            self.assertEqual(after, effective_hash(path, before, approved))
            self.assertEqual(after, digest(REPO / path))
        self.assertEqual("a" * 64,
            effective_hash("m3/collections/other.java", "a" * 64, approved))

    def test_drift_or_cross_preimage_is_refused(self):
        approved = approved_successor()
        path, original, _ = EXPECTED[0]
        with self.assertRaises(ValueError):
            effective_hash(path, "0" * 64, approved)
        self.assertNotEqual(original, approved[path][1])

    def test_tampered_reordered_or_duplicated_receipt_is_refused(self):
        raw = RECEIPT.read_text(encoding="utf-8")
        with tempfile.TemporaryDirectory() as temp:
            candidate = Path(temp) / "receipt.tsv"
            for damaged in (
                raw.replace(EXPECTED[0][1], "0" * 64, 1),
                "\n".join(reversed(raw.strip().splitlines())) + "\n",
                raw + raw.splitlines()[0] + "\n",
                raw.replace("\t", " ", 1),
            ):
                candidate.write_text(damaged, encoding="utf-8")
                with self.assertRaises(ValueError):
                    approved_successor(candidate)

    def test_symlink_not_accepted(self):
        with tempfile.TemporaryDirectory() as temp:
            alias = Path(temp) / "receipt.tsv"
            alias.symlink_to(RECEIPT)
            with self.assertRaises(ValueError):
                approved_successor(alias)


if __name__ == "__main__":
    unittest.main()
