#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Negative controls for the exact 3-test Java21 source successor."""
from pathlib import Path
import tempfile
import unittest
import verify_compile_successor as owner

ORIGINAL = Path(__file__).parent / "receipts" / "changed-compile-03.tsv"


class ThreeTestSuccessorTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.receipt = self.root / "changed-compile-03.tsv"
        self.receipt.write_bytes(ORIGINAL.read_bytes())

    def check(self):
        return owner.rows(self.receipt)

    def test_sealed_three_exact_source_revisions(self):
        received = self.check()
        self.assertEqual(3, len(received))
        self.assertEqual(sorted(owner.APPROVED), [row[0] for row in received])
        self.assertTrue(all(row[-2] == owner.RECIPE for row in received))
        self.assertTrue(all(row[-1] == "PENDING_FULL_INTEROP_CI" for row in received))

    def test_missing_symlink_and_extra_row_refused(self):
        self.receipt.unlink()
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            self.check()
        self.receipt.symlink_to(ORIGINAL)
        with self.assertRaisesRegex(ValueError, "missing or aliased"):
            self.check()
        self.receipt.unlink()
        self.receipt.write_bytes(ORIGINAL.read_bytes() * 2)
        with self.assertRaisesRegex(ValueError, "exactly three"):
            self.check()

    def test_wrong_sha_blob_and_recipe_refused(self):
        original = ORIGINAL.read_text(encoding="utf-8")
        good = self.check()
        self.receipt.write_text(original.replace(good[0][3], "0" * 64),
                                encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unreviewed"):
            self.check()
        self.receipt.write_text(original.replace(good[1][4], "0" * 40),
                                encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unreviewed"):
            self.check()
        self.receipt.write_text(original.replace(owner.RECIPE, "unapproved-recipe"),
                                encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "unreviewed"):
            self.check()

    def test_duplicate_unsorted_and_unauthorized_source_refused(self):
        lines = ORIGINAL.read_text(encoding="utf-8").splitlines()
        self.receipt.write_text("\n".join(reversed(lines)) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "sorted"):
            self.check()
        self.receipt.write_text("\n".join([lines[0], lines[0], lines[2]]) + "\n",
                                encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "sorted"):
            self.check()
        self.receipt.write_text(ORIGINAL.read_text().replace(
            "M3ApacheCollectionsTest.java", "UnknownTest.java"), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "sorted"):
            self.check()

    def test_single_atom_is_an_exact_before_after_byte_change(self):
        self.assertTrue(owner.one_exact_atom(
            b"before\ncall();\nafter\n", b"before\nfixed();\nafter\n",
            "call();", "fixed();"))
        self.assertFalse(owner.one_exact_atom(
            b"call();\n", b"fixed();\nextra mutation\n",
            "call();", "fixed();"))
        self.assertFalse(owner.one_exact_atom(
            b"call(); call();", b"fixed(); fixed();",
            "call();", "fixed();"))
        self.assertFalse(owner.one_exact_atom(
            b"no match\n", b"fixed();", "call();", "fixed();"))

    def test_byte_sha_and_git_blob_framing(self):
        self.assertEqual(owner.git_blob(b"hello\n"),
                         "ce013625030ba8dba906f756967f9e9ca394464a")
        self.assertEqual(owner.digest(b"abc"),
                         "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")


if __name__ == "__main__":
    unittest.main()
