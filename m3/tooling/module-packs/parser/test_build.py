# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real Java 21 module-boundary fixtures for the parser build driver.

Run separately with JAVA_HOME selecting the same explicit development JDK.
These small harness fixtures do not replace actual parser qualification.
"""
import importlib.util
import os
from pathlib import Path
import re
import tempfile
import unittest
import zipfile


SPEC = importlib.util.spec_from_file_location("m3_parser_build", Path(__file__).with_name("build.py"))
BUILD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUILD)


class ParserBuildBoundaryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        configured = os.environ.get("JAVA_HOME")
        if configured is None:
            raise RuntimeError("explicit JAVA_HOME for a Java 21 development JDK is required")
        cls.jdk = Path(configured).resolve(strict=True)
        release = (cls.jdk / "release").read_text(encoding="utf-8")
        if not re.search(r'^JAVA_VERSION="21(?:[.+"-])', release, re.MULTILINE):
            raise RuntimeError("Java 21 development JDK required")

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="m3-parser-build-boundary-")
        self.addCleanup(self.temporary.cleanup)
        root = Path(self.temporary.name)
        self.pack = root / "repository" / BUILD.PACK
        self.output = root / "proof"
        self.output.mkdir()
        self.source = self.pack / "src/main/java"
        self.tests = self.pack / "src/test/java"
        self.write(self.source / "module-info.java",
                   "module jdk.m3.parser { requires jdk.compiler; exports jdk.m3.parser; }\n")
        self.write(self.source / "jdk/m3/parser/FixtureOwner.java",
                   "package jdk.m3.parser; public final class FixtureOwner {"
                   " private FixtureOwner() {} static int hidden() { return 42; }"
                   " public static int value() { return hidden(); } }\n")
        self.write(self.tests / "jdk/m3/parser/InternalContract.java",
                   "package jdk.m3.parser; public final class InternalContract {"
                   " private InternalContract() {}"
                   " public static void main(String[] args) {"
                   " if (FixtureOwner.hidden() != 42) throw new AssertionError();"
                   ' System.out.println("INTERNAL_PRIVATE_ACCESS_PASS"); } }\n')
        self.write(self.tests / "jdk/m3/parser/test/ExternalContract.java",
                   "package jdk.m3.parser.test; public final class ExternalContract {"
                   " private ExternalContract() {}"
                   " public static void main(String[] args) {"
                   " if (jdk.m3.parser.FixtureOwner.value() != 42) throw new AssertionError();"
                   ' System.out.println("EXTERNAL_CONSUMER_PASS"); } }\n')
        self.write(self.tests / "jdk/m3/jackson/Support.java",
                   "package jdk.m3.jackson; public final class Support {"
                   " private Support() {} }\n")
        self.modules = self.output / "modules"
        self.consumers = self.output / "test-classes"
        self.compiler_patch = self.output / "compiler-patch"
        for path in (self.modules, self.consumers, self.compiler_patch,
                     self.output / "empty-sourcepath"):
            path.mkdir()
        self.qualification = BUILD.Qualification.__new__(BUILD.Qualification)
        self.qualification.pack = self.pack
        self.qualification.output = self.output
        self.qualification.jdk = self.jdk
        self.qualification.phase = "compile"
        self.qualification.commands = []
        self.qualification.logs = []
        self.common = [self.qualification.tool("javac"), "-source", "21", "-target", "21",
                       "-proc:none", "-implicit:none", "-Xlint:all", "-Werror"]

    @staticmethod
    def write(path, text):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def compile_module(self):
        self.qualification.run("fixture-module", [
            *self.common, "-Xlint:-module", "-d", self.modules / BUILD.MODULE,
            *sorted(self.source.rglob("*.java"))])

    def compile_tests(self):
        return self.qualification.compile_tests(
            self.common, self.compiler_patch, self.modules, self.consumers, [])

    def test_real_internal_private_access_and_external_consumer_stay_separate(self):
        external, internal = BUILD.test_partitions(self.pack)
        self.assertEqual(set(external + internal), set(self.tests.rglob("*.java")))
        self.assertEqual({path.name for path in internal}, {"InternalContract.java"})
        self.assertEqual({path.name for path in external}, {"ExternalContract.java", "Support.java"})
        self.compile_module()
        self.compile_tests()
        product = self.modules / BUILD.MODULE
        self.assertFalse((product / "jdk/m3/parser/InternalContract.class").exists())
        self.assertFalse((self.consumers / "jdk/m3/parser/InternalContract.class").exists())
        self.assertTrue((self.output / "parser-test-patch/jdk/m3/parser/InternalContract.class").is_file())
        self.assertTrue((self.consumers / "jdk/m3/jackson/Support.class").is_file())

        # Package only the actual product directory, as runtime() does.
        jar = self.output / "product.jar"
        self.qualification.run("fixture-jar", [
            self.qualification.tool("jar"), "--create", "--file", jar, "-C", product, "."])
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
        self.assertIn("jdk/m3/parser/FixtureOwner.class", names)
        self.assertNotIn("jdk/m3/parser/InternalContract.class", names)
        self.assertNotIn("jdk/m3/parser/test/ExternalContract.class", names)
        self.assertNotIn("jdk/m3/jackson/Support.class", names)

        self.qualification.phase = "test"
        self.qualification.commands = []
        self.qualification.logs = []
        self.qualification.consumer("internal", self.qualification.tool("java"),
            ("jdk.m3.parser.InternalContract", "INTERNAL_PRIVATE_ACCESS_PASS"), False, True)
        self.qualification.consumer("external", self.qualification.tool("java"),
            ("jdk.m3.parser.test.ExternalContract", "EXTERNAL_CONSUMER_PASS"), False)

    def test_internal_fixture_cannot_replace_product_class(self):
        self.write(self.tests / "jdk/m3/parser/FixtureOwner.java",
                   "package jdk.m3.parser; public final class FixtureOwner {"
                   " private FixtureOwner() {} }\n")
        self.compile_module()
        with self.assertRaisesRegex(ValueError, "replaces a production class"):
            self.compile_tests()
        self.assertTrue((self.modules / BUILD.MODULE / "jdk/m3/parser/FixtureOwner.class").is_file())

    def test_package_prefix_is_not_module_membership(self):
        self.write(self.tests / "jdk/m3/parser/extensions/Outside.java",
                   "package jdk.m3.parser.extensions; public final class Outside {}\n")
        external, internal = BUILD.test_partitions(self.pack)
        self.assertIn(self.tests / "jdk/m3/parser/extensions/Outside.java", external)
        self.assertEqual(len(internal), 1)

    def test_package_path_mismatch_and_test_descriptor_refused(self):
        misplaced = self.tests / "wrong/Location.java"
        self.write(misplaced, "package jdk.m3.parser; class Location {}\n")
        with self.assertRaisesRegex(ValueError, "package/path mismatch"):
            BUILD.test_partitions(self.pack)
        misplaced.unlink()
        self.write(self.tests / "module-info.java", "module unexpected.fixture {}\n")
        with self.assertRaisesRegex(ValueError, "test module descriptor"):
            BUILD.test_partitions(self.pack)

    def test_test_and_runtime_phases_refuse_build_javac(self):
        for phase in ("test", "runtime"):
            self.qualification.phase = phase
            with self.assertRaisesRegex(ValueError, "compiler invocation after compile"):
                self.qualification.run("forbidden", [self.qualification.tool("javac"), "-version"])
        self.assertEqual(self.qualification.commands, [])


if __name__ == "__main__":
    unittest.main()
