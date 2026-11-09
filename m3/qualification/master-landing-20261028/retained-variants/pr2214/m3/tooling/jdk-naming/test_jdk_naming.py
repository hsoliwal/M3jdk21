# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git fixtures for jdk_naming.py: packages, types, constants and paths move to JDK naming; copyright lines,
links to the hsoliwal/com.synexia repository and lineage files are untouched; a collision refuses the plan; a second
plan after apply is empty (fixed point)."""
from __future__ import annotations

import importlib.util
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("jdk_naming", Path(__file__).with_name("jdk_naming.py"))
jn = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(jn)

JAVA = """// Copyright 2026 Synexia contributors
package jdk.internal.m3.x;

import jdk.internal.m3.util.M3Helper;

/** See https://github.com/hsoliwal/com.synexia for the donor. */
public final class M3Foo {
    static final String TOKEN = System.getenv("M3_TOKEN");
    String tool = "m3-import";
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
        self.put("m3/src/jdk/internal/m3/x/M3Foo.java", JAVA)
        self.put("m3/m3-import/run.sh", "export M3_HOME=/opt\n")
        self.put("m3/x/provenance.json", '{"source": "jdk.internal.m3.x.M3Foo"}\n')
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
        self.assertIn("jdk.internal.m3.x.M3Foo", (self.root / "m3/x/provenance.json").read_text(encoding="utf-8"))
        again = jn.plan(self.root)
        self.assertEqual((again["moves"], again["rewrites"]), ([], []))

    def test_dependencies_and_group_ids_are_not_renamed(self):
        self.put("m3/x/pom.xml", "<project><groupId>com.synexia.m3</groupId>\n"
                 "<dependency><groupId>com.synexia.m3</groupId></dependency>\n</project>\n")
        self.put(".github/workflows/w.yml", "token: ${{ secrets.SYNEXIA_READ_TOKEN }}\nenv: M3_MODE\n")
        self.put("m3/x/Use.java", "class Use { String p = \"jdk.internal.m3\"; }\n")
        jn.apply(self.root, jn.plan(self.root))
        pom = (self.root / "m3/x/pom.xml").read_text(encoding="utf-8")
        self.assertEqual(pom.count("<groupId>com.synexia.m3</groupId>"), 2)
        workflow = (self.root / ".github/workflows/w.yml").read_text(encoding="utf-8")
        self.assertIn("secrets.SYNEXIA_READ_TOKEN", workflow)
        self.assertIn("env: M3_MODE", workflow)
        self.assertIn('"jdk.internal.m3"', (self.root / "m3/x/Use.java").read_text(encoding="utf-8"))

    def test_kept_spans_leave_the_rest_of_their_line_renamed(self):
        self.put("m3/x/pom.xml", "<dependency><groupId>com.synexia</groupId><artifactId>m3-x</artifactId></dependency>\n")
        self.put("m3/x/Note.java", "// see https://github.com/hsoliwal/com.synexia/blob/x/SynexiaFoo.java; use M3Bar\n")
        jn.apply(self.root, jn.plan(self.root))
        pom = (self.root / "m3/x/pom.xml").read_text(encoding="utf-8")
        self.assertIn("<groupId>com.synexia</groupId><artifactId>m3-x</artifactId>", pom)
        note = (self.root / "m3/x/Note.java").read_text(encoding="utf-8")
        self.assertIn("https://github.com/hsoliwal/com.synexia/blob/x/SynexiaFoo.java", note)
        self.assertIn("use M3Bar", note)

    def test_second_pass_folders_uppercase_words_and_com_m3(self):
        self.put("m3/vendor/m3/lib/Tool.java", "// the Synexia tool, see vendor/m3/lib and Synexia-NOTICE\n")
        self.put("m3/inv/M3-LLM-FIRST-2.json", '{"id": "M3-LLM-FIRST-2"}\n')
        self.put("Synexia-NOTICE", "notice\n")
        self.put("M3-SYNEXIA-NOTICE.md", "notice\n")
        self.put("m3/collections/src/jdk/m3/collections/A.java", "package jdk.m3.collections;\nimport jdk.m3.arrays.B;\n")
        self.put("m3/collections/pom.xml", "<project><groupId>com.m3.tooling</groupId></project>\n")
        jn.apply(self.root, jn.plan(self.root))
        tool = (self.root / "m3/vendor/m3/lib/Tool.java").read_text(encoding="utf-8")
        self.assertIn("// the Synexia tool, see vendor/m3/lib and Synexia-NOTICE", tool)
        self.assertIn('"M3-LLM-FIRST-2"', (self.root / "m3/inv/M3-LLM-FIRST-2.json").read_text(encoding="utf-8"))
        self.assertTrue((self.root / "Synexia-NOTICE").is_file())
        self.assertTrue((self.root / "M3-SYNEXIA-NOTICE.md").is_file())
        self.assertEqual(jn.target_path("m3/x/m3-openrewrite-recipes/licenses/Synexia-LICENSE"),
                         "m3/x/m3-openrewrite-recipes/licenses/Synexia-LICENSE")
        self.assertEqual(jn.rewrite_line("python3 m3/compatibility/check-m3.py; dependency_on_synexia", {}),
                         "python3 m3/compatibility/check-m3.py; dependency_on_synexia")
        moved = (self.root / "m3/collections/src/jdk/m3/collections/A.java").read_text(encoding="utf-8")
        self.assertIn("package jdk.m3.collections;", moved)
        self.assertIn("import jdk.m3.arrays.B;", moved)
        self.assertIn("<groupId>com.m3.tooling</groupId>", (self.root / "m3/collections/pom.xml").read_text(encoding="utf-8"))
        again = jn.plan(self.root)
        self.assertEqual((again["moves"], again["rewrites"]), ([], []))

    def test_doubled_m3_prefixes_from_pass_one_collapse(self):
        self.put(".github/workflows/m3-family.yml", "run: python check_m3_family.py M3_MODE m3-x\n")
        self.put("m3/x/Note.java", "// the Synexia M3 family, M3Owner\n")
        jn.apply(self.root, jn.plan(self.root))
        workflow = (self.root / ".github/workflows/m3-family.yml").read_text(encoding="utf-8")
        self.assertEqual(workflow, "run: python check_m3_family.py M3_MODE m3-x\n")
        self.assertIn("// the Synexia M3 family, M3Owner", (self.root / "m3/x/Note.java").read_text(encoding="utf-8"))
        again = jn.plan(self.root)
        self.assertEqual((again["moves"], again["rewrites"]), ([], []))

    def test_a_moved_file_and_every_reference_to_it_agree(self):
        self.put("m3/compatibility/check_m3_full_family.py", "print('ok')\n")
        self.put("m3/x/AcmeM3Owner.java", "class AcmeM3Owner {}\n")
        self.put(".github/workflows/w.yml", "run: python m3/compatibility/check_m3_full_family.py "
                 "m3/x/AcmeM3Owner.java\n")
        jn.apply(self.root, jn.plan(self.root))
        workflow = (self.root / ".github/workflows/w.yml").read_text(encoding="utf-8")
        for reference in workflow.split()[2:]:
            self.assertTrue((self.root / reference).is_file(), reference)
        self.assertIn("class AcmeM3Owner", (self.root / "m3/x/AcmeM3Owner.java").read_text(encoding="utf-8"))

    def test_jni_symbols_headers_and_flattened_paths_follow_the_package(self):
        line = ("JNIEXPORT jint JNICALL Java_jdk_internal_m3_common_collections_SegmentedBits_rank(JNIEnv *env, jclass c);"
                " #include \"jdk_internal_m3_mindex_TextMetadata.h\" Java_jdk_m3_fs_M3FsNative_open "
                "diffs/m3-fast-search__src__main__java__jdk__internal__m3__search__Problem.java.diff "
                "no_runtime_dependency_on_com_m3_repository")
        self.assertEqual(jn.rewrite_line(line, {}),
                         "JNIEXPORT jint JNICALL Java_jdk_internal_m3_common_collections_SegmentedBits_rank(JNIEnv *env,"
                         " jclass c); #include \"jdk_internal_m3_mindex_TextMetadata.h\" Java_jdk_m3_fs_M3FsNative_open "
                         "diffs/m3-fast-search__src__main__java__jdk__internal__m3__search__Problem.java.diff "
                         "no_runtime_dependency_on_com_m3_repository")

    def test_the_standard_documents_every_rule_with_a_true_example(self):
        """Section 5 of NAMING_STANDARD.md is a fixture: each `RULE` | before | after row rewrites as documented, the
        rule named is among those that fired, and QUICK (the prefilter) admits every documented input."""
        standard = Path(__file__).with_name("NAMING_STANDARD.md").read_text(encoding="utf-8")
        section = standard[standard.index("## 5. The rules"):standard.index("## 6.")]
        rows = re.findall(r"^\| `([A-Z0-9_]+)` \| (.+?) \| (.+?) \|$", section, re.M)
        documented = {name for name, _b, _a in rows}
        self.assertEqual(documented, {name for name, _r, _x in jn.TEXT_RULES + jn.PROSE_RULES})
        for name, befores, afters in rows:
            for before, after in zip(befores.split(", "), afters.split(", ")):
                before, after = before.strip("`"), after.strip("`")
                counts: dict = {}
                self.assertTrue(jn.QUICK.search(before), (name, before))
                self.assertEqual(jn.rewrite_line(before, counts), after, name)
                self.assertIn(name, counts, (name, before))

    def test_dotted_segments_types_in_the_root_package_prefix_literals_and_record_keys(self):
        line = ('package jdk.m3.importer; import jdk.internal.m3.M3Planner; p.startsWith("com.synexia.") '
                'row["synexiaState"] legal/synexia.md "schema": "m3.recipe-source-custody/1" https://synexia.com/x')
        self.assertEqual(jn.rewrite_line(line, {}),
                         'package jdk.m3.importer; import jdk.internal.m3.M3Planner; p.startsWith("com.synexia.") '
                         'row["synexiaState"] legal/synexia.md "schema": "m3.recipe-source-custody/1" https://synexia.com/x')
        self.assertEqual(jn.renamed_path("m3/m3-import/src/main/java/jdk/m3/importer/M3ImportPlan.java"),
                         "m3/m3-import/src/main/java/jdk/m3/importer/M3ImportPlan.java")
        glued = ("package jdk.internal.m3.recipe; lab_jdk.internal.m3.rewrite.X recipe\\tjdk.internal.m3.rewrite.Y\\njdk.m3.z.W "
                 "${m3.version} m3-tq/synexia.md.txt.before Java_jdk_internal_m3_recipe_X_f")
        self.assertEqual(jn.rewrite_line(glued, {}),
                         "package jdk.internal.m3.recipe; lab_jdk.internal.m3.rewrite.X recipe\\tjdk.internal.m3.rewrite.Y"
                         "\\njdk.m3.z.W ${m3.version} m3-tq/synexia.md.txt.before Java_jdk_internal_m3_recipe_X_f")
        self.assertEqual(jn.renamed_path("src/java.base/share/legal/synexia.md"), "src/java.base/share/legal/synexia.md")
        self.assertTrue(jn.KEEP_PATH.search("src/java.base/share/legal/synexia.md"))

    def test_the_product_name_follows_the_per_kind_jdk_style(self):
        """User decision 2026-10-31: prose M3JDK (comment lines only), types M3Jdk*, constants M3JDK_*, kebab m3jdk-*;
        the repository name hsoliwal/M3jdk21, PR references and string literals stay."""
        cases = {
            "// Adapted for the M3JDK21 M3FS runtime": "// Adapted for the M3JDK 21 M3FS runtime",
            "# built in M3jdk21; see hsoliwal/M3jdk21 and M3jdk21 #2216": "# built in M3JDK 21; see hsoliwal/M3jdk21 and M3jdk21 #2216",
            " * M3jdk is standalone; the M3 JDK image": " * M3JDK is standalone; the M3JDK image",
            'throw new X("M3JDK21 libjava")': 'throw new X("M3JDK21 libjava")',
            "env: M3JDK21_REFUSE M3JDK": "env: M3JDK21_REFUSE M3JDK",
            "run: tar xf m3jdk21.tar.gz m3jdk-proof.yml": "run: tar xf m3jdk21.tar.gz m3jdk-proof.yml",
            "id: M3JDK-PORT-1": "id: M3JDK-PORT-1",
            "class M3Jdk21Handoff": "class M3Jdk21Handoff",
            "// https://github.com/hsoliwal/M3jdk21/pull/5": "// https://github.com/hsoliwal/M3jdk21/pull/5",
        }
        for before, after in cases.items():
            self.assertEqual(jn.rewrite_line(before, {}), after, before)
        self.assertEqual(jn.renamed_path(".github/workflows/m3jdk21-backports.yml"), ".github/workflows/m3jdk21-backports.yml")

    def test_recipe_memory_rows_name_a_recipe_and_a_fixture_that_exist(self):
        """RECIPE_MEMORY.tsv: eight non-empty fields, an ISO-8601 UTC time, a recipe file next to the ledger, and
        every guard `file#method` resolving to a test method (the memory gate of the com.synexia stack recipes)."""
        here = Path(__file__).parent
        lines = (here / "RECIPE_MEMORY.tsv").read_text(encoding="utf-8").splitlines()
        self.assertEqual(lines[0], "utc\trecipe\ttrigger\tobserved\tcorrection\tcorrected_by\tguard\tlesson")
        for line in lines[1:]:
            fields = line.split("\t")
            self.assertEqual(len(fields), 8, line[:60])
            self.assertTrue(all(fields), line[:60])
            self.assertRegex(fields[0], r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ$")
            self.assertTrue((here / fields[1]).is_file(), fields[1])
            self.assertTrue(fields[2].startswith("PATTERN:"), fields[2])
            for guard in fields[6].split("; "):
                file, method = guard.split("#")
                sources = {"test_jdk_naming.py": here / file, "test_reseal.py": here / file,
                           "JdkNamingConformanceTest": here / "conformance/src/test/java/jdk/m3/tooling/naming/"
                           "JdkNamingConformanceTest.java"}
                self.assertIn(method, sources[file].read_text(encoding="utf-8"), guard)

    def test_a_code_line_naming_a_license_is_still_renamed(self):
        self.put("m3/x/Rules.java", "import jdk.internal.m3.x.LicenseRule;\n")
        jn.apply(self.root, jn.plan(self.root))
        self.assertIn("import jdk.internal.m3.x.LicenseRule;", (self.root / "m3/x/Rules.java").read_text(encoding="utf-8"))

    def test_collision_refuses_the_plan(self):
        self.put("m3/x/M3Bar.java", "class M3Bar {}\n")
        self.put("m3/x/M3Bar.java", "class M3Bar {}\n")
        result = jn.plan(self.root)
        self.assertEqual([r[0] for r in result["refusals"]], ["COLLISION"])
        with self.assertRaises(SystemExit):
            jn.apply(self.root, result)

    def test_colliding_lineage_files_follow_their_directory_and_keep_their_names(self):
        image = "m3/vendor/m3/snapshots/image/"
        self.put(image + "jdk.m3.x-jar.log.txt", "built jdk.m3.x\n")
        self.put(image + "jdk.m3.x-jar.log.txt", "built jdk.m3.x\n")
        self.put(image + "jdk.m3.x-notes.txt", "notes on jdk.m3.x\n")
        result = jn.plan(self.root)
        self.assertEqual(result["refusals"], [])
        self.assertEqual(sorted(result["moves"]), [
            (image + "jdk.m3.x-jar.log.txt", "m3/vendor/m3/snapshots/image/jdk.m3.x-jar.log.txt"),
            (image + "jdk.m3.x-notes.txt", "m3/vendor/m3/snapshots/image/jdk.m3.x-notes.txt"),
            (image + "jdk.m3.x-jar.log.txt", "m3/vendor/m3/snapshots/image/jdk.m3.x-jar.log.txt")])
        self.assertEqual([p for p, _c in result["rewrites"]], [image + "jdk.m3.x-notes.txt"])
        jn.apply(self.root, result)
        again = jn.plan(self.root)
        self.assertEqual((again["moves"], again["rewrites"], again["refusals"]), ([], [], []))

    def commit(self):
        run = ["git", "-C", str(self.root), "-c", "user.name=t", "-c", "user.email=t@example.com"]
        subprocess.run(run + ["commit", "-q", "-m", "fixture"], check=True)
        return subprocess.run(run + ["rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip()

    def show(self, rev, path):
        return subprocess.run(["git", "-C", str(self.root), "show", f"{rev}:{path}"], check=True,
                              capture_output=True).stdout

    def test_a_file_outside_the_checkout_is_absent_even_without_a_m3_path(self):
        self.put("m3/x/Use.java", "import jdk.internal.m3.util.A;\n")
        self.commit()
        subprocess.run(["git", "-C", str(self.root), "update-index", "--skip-worktree", "m3/x/Use.java"], check=True)
        (self.root / "m3/x/Use.java").unlink()
        self.assertEqual(jn.plan(self.root)["absent"], ["m3/x/Use.java"])

    def test_tree_mode_renames_blobs_outside_the_checkout_and_keeps_bytes_and_modes(self):
        self.put("m3/x/Use.java", "import jdk.internal.m3.util.A;\r\nclass Use {}\r\n")
        self.put("m3/m3-tool/run it.sh", "export M3_HOME=/opt\n")
        subprocess.run(["git", "-C", str(self.root), "update-index", "--chmod=+x", "m3/m3-tool/run it.sh"], check=True)
        self.put("m3/x/Plain.java", "class Plain {}\n")
        base = self.commit()
        (self.root / "m3/x/Use.java").unlink()  # not checked out: tree mode reads the blob
        for key, value in (("user.name", "t"), ("user.email", "t@example.com")):
            subprocess.run(["git", "-C", str(self.root), "config", key, value], check=True)
        commit = jn.apply_tree(self.root, base, "refs/heads/renamed", "rename")
        self.assertEqual(self.show(commit, "m3/x/Use.java"), b"import jdk.internal.m3.util.A;\r\nclass Use {}\r\n")
        self.assertEqual(self.show(commit, "m3/m3-tool/run it.sh"), b"export M3_HOME=/opt\n")
        tree = subprocess.run(["git", "-C", str(self.root), "ls-tree", "-r", commit], check=True, capture_output=True,
                              text=True).stdout
        self.assertIn("100755 blob", next(line for line in tree.splitlines() if line.endswith("run it.sh")))
        self.assertNotIn("m3-tool", tree)
        self.assertIn("m3/x/Plain.java", tree)
        self.assertEqual(self.show(commit, jn.RENAME_MAP),
                         b"path_before\tpath_after\nm3/m3-tool/run it.sh\tm3/m3-tool/run it.sh\n")
        again = jn.plan_tree(self.root, commit)
        self.assertEqual((again["moves"], again["rewrites"]), ([], []))

    def test_only_limits_the_plan_to_a_prefix(self):
        self.put("m3/a/M3A.java", "class M3A {}\n")
        self.put("m3/b/M3B.java", "class M3B {}\n")
        result = jn.plan(self.root, ("m3/a/",))
        self.assertEqual([m[0] for m in result["moves"]], ["m3/a/M3A.java"])


if __name__ == "__main__":
    unittest.main()
