# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fixture tests for fold_package.py (FOLD_PACKAGE v1): files and resources of the folded package move, every
reference form of its types follows, a package sharing the prefix and the evidence stay, refusals write nothing, and
a second run is a fixed point."""
from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).with_name("fold_package.py")
TREE = {
    "a/src/main/java/jdk/m3/p/Ngrams.java": "package jdk.m3.p;\npublic final class Ngrams { static native int f(); }\n",
    "a/src/main/java/jdk/m3/p/Ppmi.java": "package jdk.m3.p;\nfinal class Ppmi { Ngrams n; }\n",
    "a/src/main/java/module-info.java": "module org.x.p { exports jdk.m3.p; }\n",
    "a/src/test/java/jdk/m3/p/NgramsTest.java": "package jdk.m3.p;\nfinal class NgramsTest { Ngrams n; }\n",
    "a/src/main/resources/jdk/m3/p/table.tsv": "k\tv\n",
    "a/native/p.c": "JNIEXPORT jint JNICALL Java_jdk_m3_p_Ngrams_f(void) { return 0; }\n#include \"jdk_m3_p_Ngrams.h\"\n",
    "b/src/main/java/jdk/m3/q/Search.java": "package jdk.m3.q;\nimport jdk.m3.p.Ngrams;\nfinal class Search { Ngrams n; }\n",
    "c/src/main/java/jdk/m3/r/User.java": "package jdk.m3.r;\nimport jdk.m3.p.*;\nimport jdk.m3.p.sub.Other;\n"
                                           "final class User { Ngrams n; String s = \"jdk/m3/p/table.tsv\"; }\n",
    "d/src/main/java/jdk/m3/p/sub/Other.java": "package jdk.m3.p.sub;\npublic final class Other { }\n",
    ".github/workflows/w.yml": "run: java -cp x jdk.m3.p.NgramsTest && cat a/src/main/java/jdk/m3/p/Ngrams.java\n",
    "a/recipe/relocate.tsv": "jdk.m3.p.Ngrams\tjdk/m3/p/Ngrams.java\n",
}
ARGS = ["--from-root", "a", "--from-package", "jdk.m3.p", "--to-root", "b", "--to-package", "jdk.m3.q"]


def run(root: Path, *extra: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, "-I", str(TOOL), "--root", str(root), *ARGS, *extra],
                          capture_output=True, text=True)


class FoldPackageTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        for rel, text in TREE.items():
            (self.root / rel).parent.mkdir(parents=True, exist_ok=True)
            (self.root / rel).write_bytes(text.encode("utf-8"))

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def read(self, rel: str) -> str:
        return (self.root / rel).read_text(encoding="utf-8")

    def fold(self) -> None:
        out = run(self.root)
        self.assertEqual(out.returncode, 0, out.stdout + out.stderr)

    def test_files_and_resources_move_with_the_package_declaration(self) -> None:
        self.fold()
        self.assertEqual(self.read("b/src/main/java/jdk/m3/q/Ppmi.java"), "package jdk.m3.q;\nfinal class Ppmi { Ngrams n; }\n")
        self.assertTrue((self.root / "b/src/test/java/jdk/m3/q/NgramsTest.java").exists())
        self.assertTrue((self.root / "b/src/main/resources/jdk/m3/q/table.tsv").exists())
        self.assertFalse((self.root / "a/src/main/java/jdk/m3/p").exists())
        self.assertTrue((self.root / "a/src/main/java/module-info.java").exists())

    def test_references_follow_in_every_form(self) -> None:
        self.fold()
        self.assertEqual(self.read("b/src/main/java/jdk/m3/q/Search.java"), "package jdk.m3.q;\nfinal class Search { Ngrams n; }\n")
        self.assertEqual(self.read("c/src/main/java/jdk/m3/r/User.java"),
                         "package jdk.m3.r;\nimport jdk.m3.q.*;\nimport jdk.m3.p.sub.Other;\n"
                         "final class User { Ngrams n; String s = \"jdk/m3/q/table.tsv\"; }\n")
        self.assertEqual(self.read("a/native/p.c"),
                         "JNIEXPORT jint JNICALL Java_jdk_m3_q_Ngrams_f(void) { return 0; }\n#include \"jdk_m3_q_Ngrams.h\"\n")
        self.assertEqual(self.read(".github/workflows/w.yml"),
                         "run: java -cp x jdk.m3.q.NgramsTest && cat b/src/main/java/jdk/m3/q/Ngrams.java\n")

    def test_prefix_sharing_package_and_evidence_stay(self) -> None:
        self.fold()
        self.assertEqual(self.read("d/src/main/java/jdk/m3/p/sub/Other.java"), TREE["d/src/main/java/jdk/m3/p/sub/Other.java"])
        self.assertEqual(self.read("a/recipe/relocate.tsv"), TREE["a/recipe/relocate.tsv"])

    def test_a_name_taken_in_the_target_package_is_refused(self) -> None:
        (self.root / "b/src/main/java/jdk/m3/q/Ppmi.java").write_text("package jdk.m3.q;\nclass Ppmi { }\n", encoding="utf-8")
        out = run(self.root)
        self.assertNotEqual(out.returncode, 0)
        self.assertIn("REFUSE", out.stderr)
        self.assertTrue((self.root / "a/src/main/java/jdk/m3/p/Ngrams.java").exists())

    def test_a_split_package_is_refused(self) -> None:
        (self.root / "e/src/main/java/jdk/m3/p").mkdir(parents=True)
        (self.root / "e/src/main/java/jdk/m3/p/Extra.java").write_text("package jdk.m3.p;\nclass Extra { }\n", encoding="utf-8")
        out = run(self.root)
        self.assertNotEqual(out.returncode, 0)
        self.assertIn("split package", out.stderr)

    def test_second_run_is_a_fixed_point(self) -> None:
        self.fold()
        self.assertEqual(run(self.root, "--check").returncode, 0)

    def test_other_resources_move_and_service_files_merge(self) -> None:
        """v1.1: META-INF/services and test fixtures of A are product: they move to B, a service file by line union."""
        svc = "src/main/resources/META-INF/services/com.sun.source.util.Plugin"
        (self.root / "a" / svc).parent.mkdir(parents=True)
        (self.root / "a" / svc).write_text("jdk.m3.p.Ngrams\n", encoding="utf-8")
        (self.root / "b" / svc).parent.mkdir(parents=True)
        (self.root / "b" / svc).write_text("jdk.m3.q.Search\n", encoding="utf-8")
        (self.root / "a/src/test/resources/fixtures").mkdir(parents=True)
        (self.root / "a/src/test/resources/fixtures/Sample.java").write_text("class Sample { }\n", encoding="utf-8")
        self.fold()
        self.assertEqual(self.read("b/" + svc), "jdk.m3.q.Search\njdk.m3.q.Ngrams\n")
        self.assertFalse((self.root / "a" / svc).exists())
        self.assertEqual(self.read("b/src/test/resources/fixtures/Sample.java"), "class Sample { }\n")
        self.assertEqual(run(self.root, "--check").returncode, 0)

    def test_retire_moves_the_module_rest_to_evidence_and_leaves_no_root(self) -> None:
        (self.root / "pom.xml").write_text("<modules>\n    <module>a</module>\n    <module>b</module>\n</modules>\n",
                                           encoding="utf-8")
        out = run(self.root, "--retire-to", "ev/a", "--aggregator", "pom.xml")
        self.assertNotEqual(out.returncode, 0)  # a/native/p.c is product code: refused, nothing moved
        self.assertIn("native sources", out.stderr)
        self.assertTrue((self.root / "a/src/main/java/jdk/m3/p/Ngrams.java").exists())
        (self.root / "a/native/p.c").unlink()
        out = run(self.root, "--retire-to", "ev/a", "--aggregator", "pom.xml")
        self.assertEqual(out.returncode, 0, out.stdout + out.stderr)
        self.assertFalse((self.root / "a").exists())
        self.assertEqual(self.read("ev/a/module-info.java"), TREE["a/src/main/java/module-info.java"])
        self.assertEqual(self.read("ev/a/recipe/relocate.tsv"), TREE["a/recipe/relocate.tsv"])
        self.assertEqual(self.read("pom.xml"), "<modules>\n    <module>b</module>\n</modules>\n")
        self.assertEqual(run(self.root, "--check", "--retire-to", "ev/a", "--aggregator", "pom.xml").returncode, 0)

    def test_aggregator_line_is_matched_by_the_path_relative_to_the_reactor(self) -> None:
        """A reactor below the root names its modules by relative path (m3/pom.xml: <module>compiler/x</module>)."""
        (self.root / "a/native/p.c").unlink()
        mod = self.root / "m3/sub/a"
        mod.parent.mkdir(parents=True)
        (self.root / "a").rename(mod)
        (self.root / "m3/pom.xml").write_text("<modules>\n    <module>sub/a</module>\n    <module>b</module>\n</modules>\n",
                                              encoding="utf-8")
        out = subprocess.run([sys.executable, "-I", str(TOOL), "--root", str(self.root), "--from-root", "m3/sub/a",
                              "--from-package", "jdk.m3.p", "--to-root", "b", "--to-package", "jdk.m3.q",
                              "--retire-to", "ev/a", "--aggregator", "m3/pom.xml"], capture_output=True, text=True)
        self.assertEqual(out.returncode, 0, out.stdout + out.stderr)
        self.assertEqual(self.read("m3/pom.xml"), "<modules>\n    <module>b</module>\n</modules>\n")
        self.assertFalse((self.root / "m3/sub/a").exists())


if __name__ == "__main__":
    unittest.main()
