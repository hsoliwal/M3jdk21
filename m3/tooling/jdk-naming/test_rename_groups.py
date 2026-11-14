# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixtures for rename_groups.py (recipe JDK_NAMING_GROUPS): produced com.synexia groups are renamed in project
files, snippets and coordinates, donor prose and donor artifacts are not, collisions and unproduced references refuse,
and apply reaches a fixed point."""
from __future__ import annotations

import contextlib
import importlib.util
import io
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("rename_groups", Path(__file__).with_name("rename_groups.py"))
rg = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(rg)

POM = """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <groupId>{group}</groupId><artifactId>{artifact}</artifactId>
  <dependencies>{deps}</dependencies>
</project>
"""
DEP = "<dependency><groupId>{group}</groupId><artifactId>{artifact}</artifactId></dependency>"
NOTES = ('"""Relocated from com.synexia m3-fast-search (com.synexia.common.Foo, com.synexia#12)."""\n'
         'DONOR = "com.synexia:m3-donor-only"\n')
GEN = ('PROFILE = """<plugin><artifactId>p</artifactId><dependencies>\n  <dependency>\n'
       '    <groupId>com.synexia</groupId>\n    <artifactId>donor-scanner</artifactId>\n  </dependency>\n'
       '  <dependency><groupId>com.synexia</groupId><artifactId>c</artifactId></dependency>\n</dependencies></plugin>"""\n')


class RenameGroupsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)

    def tearDown(self):
        self.tmp.cleanup()

    def put(self, rel, text):
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(text.encode("utf-8"))
        subprocess.run(["git", "-C", str(self.root), "add", rel], check=True)

    def text(self, rel):
        return (self.root / rel).read_bytes().decode("utf-8")

    def run_recipe(self, mode):
        with contextlib.redirect_stdout(io.StringIO()):
            return rg.main([mode, "--root", str(self.root)])

    def fixture(self):
        self.put("m3/a/pom.xml", POM.format(group="com.synexia.m3", artifact="a", deps=""))
        self.put("m3/b/pom.xml", POM.format(group="com.synexia.verification", artifact="b",
                                            deps=DEP.format(group="com.synexia.m3", artifact="a")
                                            + DEP.format(group="org.example", artifact="x")))
        self.put("m3/c/pom.xml", POM.format(group="com.synexia", artifact="c", deps="").replace("\n", "\r\n"))
        self.put(".github/workflows/w.yml", "run: |\n  mvn -DgroupId=com.synexia.verification -DartifactId=b install\n"
                 "  echo '<groupId>com.synexia.m3</groupId>' > gen.xml\n  mvn dependency:get -Dartifact=com.synexia:c:1\n")
        self.put("m3/a/notes.py", NOTES)
        self.put("m3/a/README.md", "groupId com.synexia.m3\n")
        self.put("m3/a/gen.py", GEN)
        self.put("m3/a/tool-pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>com.synexia.m3</groupId>"
                 "<artifactId>tool</artifactId></project>\n")
        self.put("m3/t/source-recovery/source-results/ROOT_POM.xml", "<project><modelVersion>4.0.0</modelVersion>"
                 "<groupId>com.synexia</groupId><artifactId>donor-root</artifactId></project>\n")
        self.put("m3/qualification/r/retained-variants/pr1/pom.xml", POM.format(group="com.synexia.m3", artifact="old", deps=""))

    def test_produced_groups_are_renamed_where_they_are_coordinates(self):
        self.fixture()
        self.assertEqual(0, self.run_recipe("apply"))
        self.assertIn("<groupId>com.m3</groupId><artifactId>a</artifactId>", self.text("m3/a/pom.xml"))
        b = self.text("m3/b/pom.xml")
        self.assertIn("<groupId>com.m3.verification</groupId><artifactId>b</artifactId>", b)
        self.assertIn(DEP.format(group="com.m3", artifact="a"), b)
        self.assertIn(DEP.format(group="org.example", artifact="x"), b)
        self.assertIn("<groupId>com.m3</groupId><artifactId>c</artifactId>\r\n", self.text("m3/c/pom.xml"))  # CRLF kept
        w = self.text(".github/workflows/w.yml")
        self.assertIn("-DgroupId=com.m3.verification", w)
        self.assertIn("<groupId>com.m3</groupId>", w)
        self.assertIn("-Dartifact=com.m3:c:1", w)
        self.assertIn("<groupId>com.m3</groupId><artifactId>tool</artifactId>", self.text("m3/a/tool-pom.xml"))
        # an embedded dependency is renamed only when its artifact is produced here
        gen = self.text("m3/a/gen.py")
        self.assertIn("<groupId>com.synexia</groupId>\n    <artifactId>donor-scanner</artifactId>", gen)
        self.assertIn("<dependency><groupId>com.m3</groupId><artifactId>c</artifactId></dependency>", gen)
        # donor prose, an unproduced coordinate, documentation, recovered donor files and the archive are left alone
        self.assertEqual(NOTES, self.text("m3/a/notes.py"))
        self.assertEqual("groupId com.synexia.m3\n", self.text("m3/a/README.md"))
        self.assertIn("<groupId>com.synexia</groupId>", self.text("m3/t/source-recovery/source-results/ROOT_POM.xml"))
        self.assertIn("com.synexia.m3", self.text("m3/qualification/r/retained-variants/pr1/pom.xml"))
        mapping, rewrites, refusals, texts = rg.plan(self.root)  # fixed point
        self.assertEqual(({}, [], [], {}), (mapping, rewrites, refusals, texts))

    def test_a_collision_or_an_unproduced_reference_refuses_and_writes_nothing(self):
        self.fixture()
        self.put("m3/d/pom.xml", POM.format(group="com.m3", artifact="a", deps=""))  # com.synexia.m3:a -> com.m3:a
        self.assertEqual(1, self.run_recipe("apply"))
        self.assertIn("<groupId>com.synexia.m3</groupId>", self.text("m3/a/pom.xml"))
        (self.root / "m3/d/pom.xml").unlink()
        subprocess.run(["git", "-C", str(self.root), "rm", "-q", "--cached", "m3/d/pom.xml"], check=True)
        self.put("m3/e/pom.xml", POM.format(group="com.m3.e", artifact="e",
                                            deps=DEP.format(group="com.synexia.m3", artifact="missing")))
        _mapping, _rewrites, refusals, _texts = rg.plan(self.root)
        self.assertEqual(["UNPRODUCED\tm3/e/pom.xml\tdependency com.synexia.m3:missing"], refusals)

    def test_the_mapping_follows_the_repository_maven_naming(self):
        self.assertEqual(["com.m3", "com.m3", "com.m3.recipes", "com.m3.verification", "com.m3.sourcebuilt.openrewrite"],
                         [rg.target(g) for g in ("com.synexia", "com.synexia.m3", "com.synexia.m3.recipes",
                                                 "com.synexia.verification", "com.synexia.sourcebuilt.openrewrite")])


if __name__ == "__main__":
    unittest.main()
