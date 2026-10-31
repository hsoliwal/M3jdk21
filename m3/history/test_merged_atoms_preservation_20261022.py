# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Independent negative controls for the merged-atom preservation gate."""
import importlib.util
from pathlib import Path
import tempfile
import unittest


ROOT = Path(__file__).parent
NAME = "verify-merged-atoms-preservation-20261022.py"
spec = importlib.util.spec_from_file_location("m3_preservation", ROOT / NAME)
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)


class PreservationTests(unittest.TestCase):
    def test_git_sha1_matches_known_blob(self):
        self.assertEqual(gate.git_blob_sha1(b"hello\n"),
                         "ce013625030ba8dba906f756967f9e9ca394464a")

    def test_requires_preserved_file_and_contract_marker(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            file = root / "a.java"
            file.write_text("public final class M3 { public void keep() {} }\n",
                            encoding="utf-8")
            manifest = root / "manifest.tsv"
            lines = [f"0\ta.java\tREGEX\tpublic\\s+final\\s+class\\s+M3",
                     "0\tb.txt\tPRESENT\t-"]
            manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "lost merged"):
                gate.verify(root, manifest)
            (root / "b.txt").write_text("present", encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "incomplete"):
                gate.verify(root, manifest)
            file.write_text("public final class Removed {}\n", encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "lost merged capability"):
                gate.verify(root, manifest)

    def test_rejects_parent_traversal_and_forged_recipe_image(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "a.txt").write_text("original", encoding="utf-8")
            manifest = root / "manifest.tsv"
            manifest.write_text("0\t../escape\tPRESENT\t-\n", encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "unsafe"):
                gate.verify(root, manifest)
            manifest.write_text("0\ta.txt\tSHA1\t" + "0" * 40 + "\n", encoding="utf-8")
            with self.assertRaisesRegex(AssertionError, "changed pinned"):
                gate.verify(root, manifest)


if __name__ == "__main__":
    unittest.main()
