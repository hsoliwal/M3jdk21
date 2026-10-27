#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Real source-receipt refusal controls for the original five-test successor."""
from pathlib import Path
import tempfile
import unittest
import verify_source_v3 as v3


class FiveTestSuccessorGate(unittest.TestCase):
    def fixture(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        receipt = Path(tmp.name) / "CURRENT_TEST_SUCCESSIONS.tsv"
        receipt.write_text("# immutable source successor\n" +
                           "".join("\t".join(row) + "\n" for row in v3.EXPECTED),
                           encoding="utf-8")
        return receipt

    def test_exact_immutable_five_source_manifest(self):
        self.assertEqual(v3.EXPECTED, v3.receipt_rows(self.fixture()))

    def test_modified_postimage_cannot_be_recatalogued(self):
        file = self.fixture()
        data = file.read_text(encoding="utf-8")
        file.write_text(data.replace(v3.EXPECTED[0][4], "f" * 40), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "content drift"):
            v3.receipt_rows(file)

    def test_duplicate_and_reordered_rows_refused(self):
        file = self.fixture()
        lines = file.read_text(encoding="utf-8").splitlines(keepends=True)
        file.write_text("".join(lines[:1] + lines[2:3] + lines[1:2] + lines[3:]),
                        encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "sorting"):
            v3.receipt_rows(file)
        file.write_text("".join(lines + lines[1:2]), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "shape"):
            v3.receipt_rows(file)

    def test_missing_and_symlinked_receipt_refused(self):
        file = self.fixture()
        file.unlink()
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            v3.receipt_rows(file)
        target = self.fixture()
        file.symlink_to(target)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            v3.receipt_rows(file)

    def test_current_review_is_not_a_source_equivalence_shortcut(self):
        self.assertEqual(5, len(v3.EXPECTED))
        self.assertEqual(v3.EXPECTED[0][1], "6d31a53eb94af7c3cfe08ce126ce1be1f8d7da1b09150bd72a12c602d3ab3565")
        self.assertEqual(v3.EXPECTED[0][4], "d163cdc9ebbf7c023df16c423cc094f01987e4bd")
        self.assertNotEqual(v3.EXPECTED[0][1], v3.EXPECTED[0][3])


if __name__ == "__main__":
    unittest.main()
