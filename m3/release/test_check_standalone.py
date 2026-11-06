# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixtures for the standalone check (check_standalone.py): planted cross-repository dependencies are caught,
an in-repository com.synexia module is a name and not a dependency, documentation is not counted, the baseline is a
ratchet, and release mode refuses any dependency and any unscanned scoped file."""
from __future__ import annotations

import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("check_standalone", Path(__file__).with_name("check_standalone.py"))
cs = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(cs)

WORKFLOW = """name: planted
jobs:
  build:
    steps:
      - uses: actions/checkout@v4
        with:
          repository: hsoliwal/com.synexia
          token: ${{ secrets.SYNEXIA_READ_TOKEN }}
"""
POM = """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <groupId>{group}</groupId><artifactId>{artifact}</artifactId>
  <dependencies>{deps}</dependencies>
</project>
"""
DEP = "<dependency><groupId>{group}</groupId><artifactId>{artifact}</artifactId></dependency>"


class StandaloneCheckTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)

    def tearDown(self):
        self.tmp.cleanup()

    def put(self, rel, text):
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        subprocess.run(["git", "-C", str(self.root), "add", rel], check=True)

    def counts(self):
        return cs.scan(self.root)["counts"]

    def test_clean_tree_passes_both_modes(self):
        self.put(".github/workflows/ok.yml", "name: ok\njobs: {}\n")
        self.put("m3/x/pom.xml", POM.format(group="com.m3", artifact="x", deps=""))
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        self.assertEqual(sum(result["counts"].values()), 0)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), False)[0], "PASS")
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "PASS")

    def test_planted_checkout_and_secret_are_dependencies(self):
        self.put(".github/workflows/planted.yml", WORKFLOW)
        counts = self.counts()
        self.assertEqual(counts["D_CHECKOUT"], 1)
        self.assertEqual(counts["D_SECRET"], 1)

    def test_maven_resolution_separates_dependency_from_name(self):
        self.put("m3/a/pom.xml", POM.format(group="com.synexia.m3", artifact="a", deps=""))
        self.put("m3/b/pom.xml", POM.format(group="com.m3", artifact="b",
                 deps=DEP.format(group="com.synexia.m3", artifact="a") + DEP.format(group="com.synexia.m3", artifact="ext")))
        found = {(kind, text) for kind, _path, _line, text in cs.scan(self.root)["findings"]}
        self.assertIn(("D_MAVEN", "dependency com.synexia.m3:ext"), found)
        self.assertIn(("N_NAME", "dependency com.synexia.m3:a"), found)
        self.assertIn(("N_NAME", "module groupId com.synexia.m3:a"), found)
        self.assertNotIn(("D_MAVEN", "dependency com.synexia.m3:a"), found)

    def test_documentation_and_lineage_are_not_counted(self):
        self.put("README.md", "Lineage: https://github.com/hsoliwal/com.synexia\n")
        self.put("m3/x/provenance.json", '{"source": "hsoliwal/com.synexia"}\n')
        self.put("m3/docs/notes.yml", "repository: hsoliwal/com.synexia\n")
        self.assertEqual(sum(self.counts().values()), 0)

    def test_baseline_is_a_ratchet_and_release_refuses_dependencies(self):
        self.put(".github/workflows/planted.yml", WORKFLOW)
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), False)[0], "PASS")
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "BLOCK")
        self.put(".github/workflows/second.yml", WORKFLOW)
        status, detail = cs.verdict(cs.scan(self.root), cs.read_baseline(self.root), False)
        self.assertEqual(status, "BLOCK")
        self.assertIn("grew past the baseline", detail)

    def test_missing_baseline_blocks(self):
        self.put(".github/workflows/ok.yml", "name: ok\n")
        self.assertEqual(cs.verdict(cs.scan(self.root), None, False)[0], "BLOCK")

    def test_tree_mode_scans_files_that_are_not_checked_out(self):
        self.put(".github/workflows/planted.yml", WORKFLOW)
        self.put("m3/x/pom.xml", POM.format(group="com.m3", artifact="x",
                 deps=DEP.format(group="com.synexia.m3", artifact="ext")))
        subprocess.run(["git", "-C", str(self.root), "-c", "user.name=t", "-c", "user.email=t@example.com",
                        "commit", "-q", "-m", "fixture"], check=True)
        (self.root / ".github/workflows/planted.yml").unlink()
        (self.root / "m3/x/pom.xml").unlink()
        worktree = cs.scan(self.root)
        self.assertEqual((worktree["unscanned"], worktree["counts"]["D_CHECKOUT"]), (2, 0))
        tree = cs.scan(self.root, "HEAD")
        self.assertEqual(tree["unscanned"], 0)
        self.assertEqual((tree["counts"]["D_CHECKOUT"], tree["counts"]["D_SECRET"], tree["counts"]["D_MAVEN"]), (1, 1, 1))
        self.assertIsNone(cs.read_baseline(self.root, "HEAD"))
        cs.write_baseline(self.root, tree)  # in the working tree only: the revision's own ratchet is what counts
        self.assertIsNotNone(cs.read_baseline(self.root))
        self.assertIsNone(cs.read_baseline(self.root, "HEAD"))
        self.assertEqual(cs.verdict(tree, cs.read_baseline(self.root, "HEAD"), False)[0], "BLOCK")

    def test_the_naming_recipe_crate_is_not_counted(self):
        self.put("m3/tooling/jdk-naming/reseal.py", "PATTERN = 'com.synexia.m3 SynexiaFoo SYNEXIA_X'\n")
        self.put("m3/tooling/jdk-naming/conformance/pom.xml", POM.format(group="com.synexia.m3", artifact="m3-x", deps=""))
        self.assertEqual(sum(self.counts().values()), 0)

    def test_unscanned_scoped_file_holds_release(self):
        self.put("m3/x/run.sh", "echo ok\n")
        result = cs.scan(self.root)
        cs.write_baseline(self.root, result)
        (self.root / "m3/x/run.sh").unlink()
        result = cs.scan(self.root)
        self.assertEqual(result["unscanned"], 1)
        self.assertEqual(cs.verdict(result, cs.read_baseline(self.root), True)[0], "HOLD")


if __name__ == "__main__":
    unittest.main()
