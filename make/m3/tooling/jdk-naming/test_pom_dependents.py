# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixture for pom_dependents.py: the dependents of an artifact come from the HEAD tree (a module declaring the
artifact only as its own artifactId is not a dependent, evidence poms are ignored), and --require-checkout refuses the
run when a dependent module is not in the working tree (RECIPE_MEMORY 2026-11-08, PATTERN:RENAME_COMPLETENESS)."""
from __future__ import annotations

import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("pom_dependents", Path(__file__).with_name("pom_dependents.py"))
pd = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(pd)

POM = """<project><groupId>com.m3</groupId><artifactId>{artifact}</artifactId><version>1</version>
<dependencies>{deps}</dependencies></project>
"""
DEP = "<dependency><groupId>com.m3</groupId><artifactId>{a}</artifactId><version>1</version></dependency>"


def write(root: Path, rel: str, text: str) -> None:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


class PomDependentsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        write(self.root, "m3/a/pom.xml", POM.format(artifact="m3-a", deps=""))
        write(self.root, "m3/b/pom.xml", POM.format(artifact="m3-b", deps=DEP.format(a="m3-a")))
        write(self.root, "m3/c/pom.xml", POM.format(artifact="m3-c", deps=DEP.format(a="m3-other")))
        write(self.root, "m3/b/recipe/old/pom.xml", POM.format(artifact="m3-old", deps=DEP.format(a="m3-a")))
        for args in (["init", "-q"], ["add", "-A"], ["-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "fixture"]):
            subprocess.run(["git", "-C", str(self.root), *args], check=True, capture_output=True)

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def test_dependents_are_found_in_the_tree(self) -> None:
        self.assertEqual([("m3/b", "m3/b/pom.xml")], pd.dependents(self.root, "m3-a"))
        self.assertEqual([], pd.dependents(self.root, "m3-c"))

    def test_a_dependent_outside_the_checkout_refuses_the_run(self) -> None:
        (self.root / "m3/b/pom.xml").unlink()  # the module is in HEAD but not in the (sparse) checkout
        self.assertEqual(["m3/b"], pd.missing_from_checkout(self.root, pd.dependents(self.root, "m3-a")))
        self.assertEqual(2, pd.main(["pom_dependents.py", str(self.root), "m3-a", "--require-checkout"]))
        self.assertEqual(0, pd.main(["pom_dependents.py", str(self.root), "m3-a"]))


if __name__ == "__main__":
    unittest.main()
