#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Unit tests of M3_TO_JDK_LAYOUT (m3_integrate.py): rule classification, path model, reference forms."""
from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_integrate", HERE / "m3_integrate.py")
mi = importlib.util.module_from_spec(SPEC)
sys.modules["m3_integrate"] = mi
SPEC.loader.exec_module(mi)

OLD = [
    "m3/pom.xml", "m3/README.md", "m3/docs/a.md", "m3/docs/data/x.json",
    "m3/tooling/t/pom.xml", "m3/tooling/t/run.py", "m3/tooling/t/src/main/java/X.java",
    "m3/fs/pom.xml", "m3/fs/src/main/java/jdk/m3/fs/F.java", "m3/fs/verify.py",
    "m3/vendor/third_party/lib/A.java", "m3/release/check.py", "src/java.base/share/classes/X.java",
    ".github/workflows/w.yml",
]
RULES = [
    ["R4", "prefix", "m3/vendor/third_party/", "RETIRE", "P4", "RETIRE", "donor"],
    ["R2-DOCS", "prefix", "m3/docs/", "doc/m3/{rest}", "P2", "MOVE", "docs"],
    ["R2", "prefix", "m3/", "make/m3/{rest}", "P2", "MOVE", "tooling"],
]
AREAS = mi.area_rules([["fs", "jdk.m3.io", "HOLD_P1", "io"]])


def mapping():
    rows = []
    for p in OLD:
        if p.startswith("m3/"):
            target, rid, phase, status, reason = mi.classify(p, RULES, AREAS)
            rows.append([p, target, rid, phase, status, "100644", "0" * 40, reason])
    return rows


class MapTest(unittest.TestCase):
    def test_rules(self):
        got = {r[0]: (r[1], r[3], r[4]) for r in mapping()}
        self.assertEqual(got["m3/vendor/third_party/lib/A.java"], ("RETIRE", "P4", "RETIRE"))
        self.assertEqual(got["m3/docs/a.md"], ("doc/m3/a.md", "P2", "MOVE"))
        self.assertEqual(got["m3/tooling/t/run.py"], ("make/m3/tooling/t/run.py", "P2", "MOVE"))
        self.assertEqual(got["m3/fs/src/main/java/jdk/m3/fs/F.java"][0],
                         "src/jdk.m3.io/share/classes/jdk/m3/fs/F.java")
        self.assertEqual(got["m3/fs/verify.py"][1:], ("P1", "HOLD_P1"))  # the whole area stays together

    def test_lib_name(self):
        self.assertEqual(mi.lib_name("commons-io-2.18.0"), "commonsio")
        self.assertEqual(mi.lib_name("HdrHistogram"), "hdrhistogram")


class RefsTest(unittest.TestCase):
    def setUp(self):
        rows = [r for r in mapping() if r[3] == "P2"]
        retired = [r[0] for r in mapping() if r[1] == "RETIRE"]
        self.pm = mi.PathMap([(p, "100644", "0" * 40) for p in OLD], rows, retired)
        self.rw = mi.Rewriter(self.pm)

    def run_text(self, old, new, text):
        where = []
        return self.rw.text(old, new, text, where), where

    def test_path_model(self):
        self.assertEqual(self.pm.new_of("m3/tooling/t"), "make/m3/tooling/t")
        self.assertEqual(self.pm.new_of("m3/tooling/t/target/x.jar"), "make/m3/tooling/t/target/x.jar")
        self.assertEqual(self.pm.new_of("m3/fs/src"), "m3/fs/src")
        self.assertIsNone(self.pm.new_of("m3"))  # the reactor root is split, not one module
        self.assertEqual(self.pm.new_of("m3/vendor"), "m3/vendor")  # retire rows do not split
        # an untracked class under a source root (fixture or stale) is not dragged along by the move
        self.assertEqual(self.pm.new_of("m3/tooling/t/src/main/java/Missing.java"), "m3/tooling/t/src/main/java/Missing.java")

    def test_root_forms(self):
        text, _ = self.run_text("src/java.base/share/classes/X.java", "src/java.base/share/classes/X.java",
                                "// recipe m3/tooling/t/run.py and $ROOT/m3/release/check.py, m3/fs/pom.xml.")
        self.assertEqual(text, "// recipe make/m3/tooling/t/run.py and $ROOT/make/m3/release/check.py, m3/fs/pom.xml.")
        text, _ = self.run_text("m3/release/check.py", "make/m3/release/check.py",
                                'p = ROOT / "m3" / "tooling" / "t" / "run.py"; q = before/m3/tooling/t/run.py')
        self.assertEqual(text, 'p = ROOT / "make" / "m3" / "tooling" / "t" / "run.py"; q = before/m3/tooling/t/run.py')

    def test_relative_forms(self):
        text, _ = self.run_text("m3/tooling/t/run.py", "make/m3/tooling/t/run.py",
                                'ROOT = Path(__file__).resolve().parents[3]\nsrc = "../../../src/java.base"\nx = "../../fs/pom.xml"')
        self.assertEqual(text, 'ROOT = Path(__file__).resolve().parents[4]\nsrc = "../../../../src/java.base"\n'
                               'x = "../../../../m3/fs/pom.xml"')
        text, _ = self.run_text("m3/docs/a.md", "doc/m3/a.md", "[d](data/x.json) [r](../release/check.py) wait...")
        self.assertEqual(text, "[d](data/x.json) [r](../../make/m3/release/check.py) wait...")

    def test_dot_tokens_are_not_punctuation(self):
        src = 'if ".." in parts or part in {"", ".", ".."}: x = "m3/release/check.py."'
        text, _ = self.run_text("m3/release/check.py", "make/m3/release/check.py", src)
        self.assertEqual(text, src.replace('"m3/release/check.py."', '"make/m3/release/check.py."'))

    def test_python_variable_parents(self):
        src = ("HERE = Path(__file__).resolve().parent\nROOT = HERE.parents[2]\nTOOL = HERE.parent\n"
               "UP = Path(__file__).parents[1]\nTOP = UP.parent.parent\n")
        text, _ = self.run_text("m3/tooling/t/run.py", "make/m3/tooling/t/run.py", src)
        self.assertEqual(text, "HERE = Path(__file__).resolve().parent\nROOT = HERE.parents[3]\nTOOL = HERE.parent\n"
                               "UP = Path(__file__).parents[1]\nTOP = UP.parent.parent.parent\n")

    def test_pom(self):
        pom = "<parent><groupId>g</groupId>\n  <artifactId>a</artifactId>\n</parent>\n<module>../../fs</module>"
        text, _ = self.run_text("m3/tooling/t/pom.xml", "make/m3/tooling/t/pom.xml", pom)
        self.assertIn("<module>../../../../m3/fs</module>", text)
        reactor = "<modules><module>fs</module><module>tooling/t</module></modules>"
        text, _ = self.run_text("m3/pom.xml", "make/m3/pom.xml", reactor)
        self.assertEqual(text, "<modules><module>../../m3/fs</module><module>tooling/t</module></modules>")
        child = "<parent>\n    <artifactId>r</artifactId>\n</parent>"
        text, _ = self.run_text("m3/fs/pom.xml", "m3/fs/pom.xml", child)
        self.assertIn("<relativePath>../../make/m3/pom.xml</relativePath>", text)

    def test_workflow_union(self):
        wf = "on:\n  push:\n    branches:\n      - 'm3/**'\n    paths:\n      - 'm3/**'\n      - 'm3/tooling/t/**'\n"
        text, _ = self.run_text(".github/workflows/w.yml", ".github/workflows/w.yml", wf)
        self.assertIn("    branches:\n      - 'm3/**'\n    paths:\n      - 'm3/**'\n      - 'make/m3/**'\n"
                      "      - 'doc/m3/**'\n      - 'make/m3/tooling/t/**'\n", text)

    def test_fixture_tests_keep_their_synthetic_paths(self):
        src = ('import tempfile\nROOT = Path(__file__).resolve().parents[3]\nput(tmp, "m3/tooling/t/synthetic.py")\n'
               'real = ROOT / "m3/release/check.py"\n')
        self.assertTrue(mi.fixture_test("m3/tooling/jdk-naming/test_jdk_naming.py", src))
        self.assertFalse(mi.fixture_test("m3/release/test_release_admission.py", src))
        self.rw.fixture = True
        text, _ = self.run_text("m3/tooling/t/test_run.py", "make/m3/tooling/t/test_run.py", src)
        self.assertEqual(text, src.replace("parents[3]", "parents[4]").replace('"m3/release', '"make/m3/release'))
        self.rw.fixture = False

    def test_protected_corpus(self):
        self.assertTrue(mi.skip_content("m3/tooling/jdk-naming/NAMING_RULES.tsv"))
        self.assertTrue(mi.skip_content("make/m3/tooling/jdk-naming/RENAME_MAP.tsv"))
        self.assertFalse(mi.skip_content("make/m3/tooling/jdk-naming/PACKAGE_BASELINE.tsv"))


if __name__ == "__main__":
    unittest.main()
