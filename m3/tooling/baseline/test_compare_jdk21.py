#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("compare_jdk21.py")
SPEC = importlib.util.spec_from_file_location("compare_jdk21", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class CompareJdk21Test(unittest.TestCase):
    def test_classifies_verbatim_modified_added_and_deleted(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            baseline = root / "baseline"
            current = root / "current"
            baseline.mkdir()
            current.mkdir()

            (baseline / "same.txt").write_bytes(b"same\n")
            (current / "same.txt").write_bytes(b"same\n")
            (baseline / "changed.txt").write_bytes(b"before\n")
            (current / "changed.txt").write_bytes(b"after\n")
            (baseline / "deleted.txt").write_bytes(b"gone\n")
            (current / "added.txt").write_bytes(b"new\n")

            rows = {row[0]: row for row in MODULE.compare(baseline, current)}
            self.assertEqual("VERBATIM_EQUAL", rows["same.txt"][3])
            self.assertEqual("true", rows["same.txt"][4])
            self.assertEqual("MODIFIED", rows["changed.txt"][3])
            self.assertEqual("ADDED", rows["added.txt"][3])
            self.assertEqual("DELETED", rows["deleted.txt"][3])

    def test_m3_and_git_are_excluded_from_baseline_oracle(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for name in ("baseline", "current"):
                tree = root / name
                (tree / "m3").mkdir(parents=True)
                (tree / ".git").mkdir(parents=True)
                (tree / "src").mkdir(parents=True)
                (tree / "m3" / "generated.txt").write_text(name, encoding="utf-8")
                (tree / ".git" / "config").write_text(name, encoding="utf-8")
                (tree / "src" / "A.java").write_text("class A {}\n", encoding="utf-8")

            rows = MODULE.compare(root / "baseline", root / "current")
            self.assertEqual(["src/A.java"], [row[0] for row in rows])
            self.assertEqual("VERBATIM_EQUAL", rows[0][3])


if __name__ == "__main__":
    unittest.main()
