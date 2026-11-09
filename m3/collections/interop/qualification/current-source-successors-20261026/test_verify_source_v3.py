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
        self.assertEqual(v3.EXPECTED[0][1], "61ae67aeedc4a4a42f81f1beefc23ed43aa0c2288b6301fd7bfc1eea023fdf87")
        self.assertEqual(v3.EXPECTED[0][4], "f2b5c032c52c49a98fc09e431f1b6863eb80aaa0")
        self.assertNotEqual(v3.EXPECTED[0][1], v3.EXPECTED[0][3])

    def file_sets(self):
        old = {v3.JAVA_BASE + "Kept.java": ("100644", "a" * 40)}
        now = dict(old)
        now.update({p: ("100644", b) for p, b in v3.ADMITTED_ADDITIONS.items()})
        return old, now

    def test_exactly_admitted_additions_pass(self):
        v3.check_file_set(*self.file_sets())

    def test_unadmitted_addition_and_deletion_refused(self):
        old, now = self.file_sets()
        now[v3.JAVA_BASE + "Unreviewed.java"] = ("100644", "b" * 40)
        with self.assertRaisesRegex(ValueError, "addition/deletion"):
            v3.check_file_set(old, now)
        old, now = self.file_sets()
        del now[v3.JAVA_BASE + "Kept.java"]
        with self.assertRaisesRegex(ValueError, "addition/deletion"):
            v3.check_file_set(old, now)

    def test_admitted_addition_with_another_blob_refused(self):
        old, now = self.file_sets()
        path = sorted(v3.ADMITTED_ADDITIONS)[0]
        now[path] = ("100644", "c" * 40)
        with self.assertRaisesRegex(ValueError, "blob or mode drift"):
            v3.check_file_set(old, now)


if __name__ == "__main__":
    unittest.main()
