# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixtures for jdk_naming.py: packages, types, constants and paths move to JDK naming; copyright lines,
links to the hsoliwal/com.synexia repository and lineage files are untouched; a collision refuses the plan; a second
plan after apply is empty (fixed point)."""
from __future__ import annotations

import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("jdk_naming", Path(__file__).with_name("jdk_naming.py"))
jn = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(jn)

JAVA = """// Copyright 2026 Synexia contributors
package com.synexia.m3.x;

import com.synexia.util.SynexiaHelper;

/** See https://github.com/hsoliwal/com.synexia for the donor. */
public final class SynexiaFoo {
    static final String TOKEN = System.getenv("SYNEXIA_TOKEN");
    String tool = "synexia-import";
}
"""


class JdkNamingTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)
        subprocess.run(["git", "-C", str(self.root), "config", "core.autocrlf", "false"], check=True)

    def tearDown(self):
        self.tmp.cleanup()

    def put(self, rel, text):
        path = self.root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        subprocess.run(["git", "-C", str(self.root), "add", rel], check=True)

    def test_rename_moves_and_rewrites_and_reaches_a_fixed_point(self):
        self.put("m3/src/com/synexia/m3/x/SynexiaFoo.java", JAVA)
        self.put("m3/synexia-import/run.sh", "export SYNEXIA_HOME=/opt\n")
        self.put("m3/x/provenance.json", '{"source": "com.synexia.m3.x.SynexiaFoo"}\n')
        result = jn.plan(self.root)
        self.assertFalse(result["refusals"])
        jn.apply(self.root, result)
        moved = self.root / "m3/src/jdk/internal/m3/x/M3Foo.java"
        self.assertTrue(moved.is_file())
        text = moved.read_text(encoding="utf-8")
        self.assertIn("package jdk.internal.m3.x;", text)
        self.assertIn("import jdk.internal.m3.util.M3Helper;", text)
        self.assertIn("public final class M3Foo", text)
        self.assertIn('getenv("M3_TOKEN")', text)
        self.assertIn('"m3-import"', text)
        self.assertIn("// Copyright 2026 Synexia contributors", text)
        self.assertIn("https://github.com/hsoliwal/com.synexia", text)
        self.assertTrue((self.root / "m3/m3-import/run.sh").is_file())
        self.assertIn("M3_HOME", (self.root / "m3/m3-import/run.sh").read_text(encoding="utf-8"))
        self.assertIn("com.synexia.m3.x.SynexiaFoo", (self.root / "m3/x/provenance.json").read_text(encoding="utf-8"))
        again = jn.plan(self.root)
        self.assertEqual((again["moves"], again["rewrites"]), ([], []))

    def test_dependencies_and_group_ids_are_not_renamed(self):
        self.put("m3/x/pom.xml", "<project><groupId>com.synexia.m3</groupId>\n"
                 "<dependency><groupId>com.synexia.m3</groupId></dependency>\n</project>\n")
        self.put(".github/workflows/w.yml", "token: ${{ secrets.SYNEXIA_READ_TOKEN }}\nenv: SYNEXIA_MODE\n")
        self.put("m3/x/Use.java", "class Use { String p = \"com.synexia.m3\"; }\n")
        jn.apply(self.root, jn.plan(self.root))
        pom = (self.root / "m3/x/pom.xml").read_text(encoding="utf-8")
        self.assertEqual(pom.count("<groupId>com.synexia.m3</groupId>"), 2)
        workflow = (self.root / ".github/workflows/w.yml").read_text(encoding="utf-8")
        self.assertIn("secrets.SYNEXIA_READ_TOKEN", workflow)
        self.assertIn("env: M3_MODE", workflow)
        self.assertIn('"jdk.internal.m3"', (self.root / "m3/x/Use.java").read_text(encoding="utf-8"))

    def test_kept_spans_leave_the_rest_of_their_line_renamed(self):
        self.put("m3/x/pom.xml", "<dependency><groupId>com.synexia</groupId><artifactId>synexia-x</artifactId></dependency>\n")
        self.put("m3/x/Note.java", "// see https://github.com/hsoliwal/com.synexia/blob/x/SynexiaFoo.java; use SynexiaBar\n")
        jn.apply(self.root, jn.plan(self.root))
        pom = (self.root / "m3/x/pom.xml").read_text(encoding="utf-8")
        self.assertIn("<groupId>com.synexia</groupId><artifactId>m3-x</artifactId>", pom)
        note = (self.root / "m3/x/Note.java").read_text(encoding="utf-8")
        self.assertIn("https://github.com/hsoliwal/com.synexia/blob/x/SynexiaFoo.java", note)
        self.assertIn("use M3Bar", note)

    def test_a_code_line_naming_a_license_is_still_renamed(self):
        self.put("m3/x/Rules.java", "import com.synexia.m3.x.LicenseRule;\n")
        jn.apply(self.root, jn.plan(self.root))
        self.assertIn("import jdk.internal.m3.x.LicenseRule;", (self.root / "m3/x/Rules.java").read_text(encoding="utf-8"))

    def test_collision_refuses_the_plan(self):
        self.put("m3/x/SynexiaBar.java", "class SynexiaBar {}\n")
        self.put("m3/x/M3Bar.java", "class M3Bar {}\n")
        result = jn.plan(self.root)
        self.assertEqual([r[0] for r in result["refusals"]], ["COLLISION"])
        with self.assertRaises(SystemExit):
            jn.apply(self.root, result)

    def test_only_limits_the_plan_to_a_prefix(self):
        self.put("m3/a/SynexiaA.java", "class SynexiaA {}\n")
        self.put("m3/b/SynexiaB.java", "class SynexiaB {}\n")
        result = jn.plan(self.root, ("m3/a/",))
        self.assertEqual([m[0] for m in result["moves"]], ["m3/a/SynexiaA.java"])


if __name__ == "__main__":
    unittest.main()
