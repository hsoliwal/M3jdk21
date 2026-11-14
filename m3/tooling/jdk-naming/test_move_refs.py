# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fixture for move_refs.py (MOVE_REFS v1): a REPLACE whose new literal contains the old one is applied once (fixed point),
SPARSE adds the module paths after the moved entry with the same indentation, and a remaining live reference to the old
prefix is a RESIDUE finding (exit 2) unless a KEEP literal allows it."""
from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).with_name("move_refs.py")
WORKFLOW = """jobs:
  check:
    steps:
      - uses: actions/checkout@v4
        with:
          sparse-checkout: |
            m3/arrays
            m3/collections
      - run: find m3/arrays/src/main/java -name '*.java' | sort > sources.txt
"""
RULES = "\n".join([
    "FILES\t.github/workflows/*.yml",
    "REPLACE\tfind m3/arrays/src/main/java\tfind m3/arrays/src/main/java src/jdk.m3.collections/share/classes/jdk/m3/arrays",
    "SPARSE\tm3/arrays\tsrc/jdk.m3.collections/share\tmake/m3/arrays",
    "RESIDUE\tm3/arrays/src/main/java",
    "KEEP\tfind m3/arrays/src/main/java src/jdk.m3.collections/share/classes/jdk/m3/arrays",
]) + "\n"


def run(root: Path, *extra: str) -> int:
    return subprocess.run([sys.executable, "-I", str(TOOL), str(root), str(root / "rules.tsv"), *extra],
                          capture_output=True, text=True).returncode


class MoveRefsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root / ".github/workflows").mkdir(parents=True)
        self.workflow = self.root / ".github/workflows/w.yml"
        self.workflow.write_text(WORKFLOW, encoding="utf-8")
        (self.root / "rules.tsv").write_text(RULES, encoding="utf-8")

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def test_replace_and_sparse_reach_a_fixed_point(self) -> None:
        self.assertEqual(0, run(self.root))
        text = self.workflow.read_text(encoding="utf-8")
        self.assertEqual(1, text.count("find m3/arrays/src/main/java src/jdk.m3.collections/share/classes/jdk/m3/arrays -name"))
        self.assertIn("            m3/arrays\n            src/jdk.m3.collections/share\n            make/m3/arrays\n", text)
        self.assertEqual(0, run(self.root, "--check"))
        self.assertEqual(text, self.workflow.read_text(encoding="utf-8"))

    def test_an_unrewritten_reference_is_residue(self) -> None:
        self.workflow.write_text(WORKFLOW + "      - run: ls m3/arrays/src/main/java/jdk\n", encoding="utf-8")
        self.assertEqual(2, run(self.root))


if __name__ == "__main__":
    unittest.main()
