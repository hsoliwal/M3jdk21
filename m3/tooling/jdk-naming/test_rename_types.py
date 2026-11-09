# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fixture tests for rename_types.py (DROP_PRODUCT_PREFIX): every reference form follows a rename (simple name in the
package and in importers, dotted, JNI, binary/resource path, declared docs), evidence and excluded records stay byte for
byte, and a second run is a fixed point (RECIPE_MEMORY 2026-11-08, PATTERN:BINARY_PATH_FORM)."""
from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).with_name("rename_types.py")
TREE = {
    "m/src/main/java/jdk/m3/x/M3Foo.java": "package jdk.m3.x;\npublic final class M3Foo { static native int f(); }\n",
    "m/src/main/java/jdk/m3/x/Use.java": "package jdk.m3.x;\nfinal class Use { M3Foo foo; String s = \"M3Foo\"; }\n",
    "m/src/main/java/jdk/m3/x/Doc.java": "package jdk.m3.x;\n/** Uses {@link M3Foo}; not com.acme.M3Foo. */\n"
                                         "final class Doc { String s = \"// M3Foo\"; M3Foo f; }\n"
                                         "//FLLDCIM: RELOCATE - donor https://h/r/blob/0/src/com/acme/M3Foo.java\n",
    "m/src/main/java/jdk/m3/x/recipe/M3Bar.java": "package jdk.m3.x.recipe;\npublic final class M3Bar { }\n",
    "n/src/main/java/jdk/m3/y/Other.java": "package jdk.m3.y;\nimport jdk.m3.x.M3Foo;\nfinal class Other { M3Foo f; }\n",
    "n/src/main/java/jdk/m3/y/Foreign.java": "package jdk.m3.y;\nfinal class Foreign { com.acme.M3Foo f; }\n",
    "m/native/foo.c": "JNIEXPORT jint JNICALL Java_jdk_m3_x_M3Foo_f(void) { return 0; }\n#include \"jdk_m3_x_M3Foo.h\"\n",
    "m/pom.xml": "<include>jdk/m3/x/M3Foo*</include><mainClass>jdk.m3.x.M3Foo</mainClass>\n",
    "m/verify.sh": "javac src/main/java/jdk/m3/x/M3Foo.java && java -cp out jdk.m3.x.M3Foo\n",
    "m/README.md": "`M3Foo` is the entry; `com.acme.M3Foo` is not ours; see jdk.m3.x.M3Foo.\n"
                   "Donor: https://h/r/blob/0/src/com/acme/M3Foo.java; here: src/main/java/jdk/m3/x/M3Foo.java\n",
    "docs/other.md": "`M3Foo` outside the declared docs keeps its simple name.\n",
    "m/APPLICATION.json": "{\"path\": \"m/src/main/java/jdk/m3/x/M3Foo.java\"}\n",
    "m/evidence/run.tsv": "jdk.m3.x.M3Foo\tjdk/m3/x/M3Foo.java\n",
}


def run(root: Path, *extra: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, "-I", str(TOOL), "--root", str(root), "--package", "jdk.m3.x",
                           "--package", "jdk.m3.x.recipe", "--exclude", "m/APPLICATION.json", "--doc", "m/", *extra],
                          capture_output=True, text=True)


class RenameTypesTest(unittest.TestCase):
    EXTRA: tuple = ()

    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        for rel, text in TREE.items():
            (self.root / rel).parent.mkdir(parents=True, exist_ok=True)
            (self.root / rel).write_bytes(text.encode("utf-8"))
        self.out = run(self.root, *self.EXTRA)
        self.assertEqual(self.out.returncode, 0, self.out.stdout + self.out.stderr)

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def read(self, rel: str) -> str:
        return (self.root / rel).read_text(encoding="utf-8")

    def test_type_and_simple_names_follow(self) -> None:
        self.assertFalse((self.root / "m/src/main/java/jdk/m3/x/M3Foo.java").exists())
        self.assertIn("class Foo ", self.read("m/src/main/java/jdk/m3/x/Foo.java"))
        self.assertEqual(self.read("m/src/main/java/jdk/m3/x/Use.java"),
                         "package jdk.m3.x;\nfinal class Use { Foo foo; String s = \"M3Foo\"; }\n")
        self.assertIn("import jdk.m3.x.Foo;", self.read("n/src/main/java/jdk/m3/y/Other.java"))
        self.assertIn("{ Foo f; }", self.read("n/src/main/java/jdk/m3/y/Other.java"))
        self.assertEqual(self.read("n/src/main/java/jdk/m3/y/Foreign.java"), TREE["n/src/main/java/jdk/m3/y/Foreign.java"])

    def test_jni_dotted_and_binary_path_forms_follow(self) -> None:
        self.assertIn("Java_jdk_m3_x_Foo_f", self.read("m/native/foo.c"))
        self.assertIn("jdk_m3_x_Foo.h", self.read("m/native/foo.c"))
        self.assertEqual(self.read("m/pom.xml"), "<include>jdk/m3/x/Foo*</include><mainClass>jdk.m3.x.Foo</mainClass>\n")
        self.assertEqual(self.read("m/verify.sh"), "javac src/main/java/jdk/m3/x/Foo.java && java -cp out jdk.m3.x.Foo\n")

    def test_declared_docs_follow_and_other_docs_keep_simple_names(self) -> None:
        self.assertEqual(self.read("m/README.md"),
                         "`Foo` is the entry; `com.acme.M3Foo` is not ours; see jdk.m3.x.Foo.\n"
                         "Donor: https://h/r/blob/0/src/com/acme/M3Foo.java; here: src/main/java/jdk/m3/x/Foo.java\n")
        self.assertEqual(self.read("docs/other.md"), TREE["docs/other.md"])

    def test_evidence_and_excluded_records_stay(self) -> None:
        self.assertEqual(self.read("m/APPLICATION.json"), TREE["m/APPLICATION.json"])
        self.assertEqual(self.read("m/evidence/run.tsv"), TREE["m/evidence/run.tsv"])

    def test_a_package_named_like_an_evidence_segment_is_renamed(self) -> None:
        self.assertTrue((self.root / "m/src/main/java/jdk/m3/x/recipe/Bar.java").exists())

    def test_comments_are_kept_without_comment_words(self) -> None:
        self.assertEqual(self.read("m/src/main/java/jdk/m3/x/Doc.java"),
                         "package jdk.m3.x;\n/** Uses {@link M3Foo}; not com.acme.M3Foo. */\n"
                         "final class Doc { String s = \"// M3Foo\"; Foo f; }\n"
                         "//FLLDCIM: RELOCATE - donor https://h/r/blob/0/src/com/acme/M3Foo.java\n")

    def test_second_run_is_a_fixed_point(self) -> None:
        self.assertEqual(run(self.root, "--check", *self.EXTRA).returncode, 0)


class CommentWordsTest(RenameTypesTest):
    EXTRA = ("--comment-words",)

    def test_comments_are_kept_without_comment_words(self) -> None:
        # --comment-words: the Javadoc link follows; another package's token, a string holding // and the donor
        # URL of a //FLLDCIM anchor (history) stay
        self.assertEqual(self.read("m/src/main/java/jdk/m3/x/Doc.java"),
                         "package jdk.m3.x;\n/** Uses {@link Foo}; not com.acme.M3Foo. */\n"
                         "final class Doc { String s = \"// M3Foo\"; Foo f; }\n"
                         "//FLLDCIM: RELOCATE - donor https://h/r/blob/0/src/com/acme/M3Foo.java\n")


if __name__ == "__main__":
    unittest.main()
