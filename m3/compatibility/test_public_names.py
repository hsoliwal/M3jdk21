# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-shape and real-git regression tests; no JDK API transformation."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("check_public_names.py")
SPEC = importlib.util.spec_from_file_location("public_names", SCRIPT)
GATE = importlib.util.module_from_spec(SPEC)
import sys
sys.modules[SPEC.name] = GATE
SPEC.loader.exec_module(GATE)


class PublicNamesTest(unittest.TestCase):
    def issue(self, path, source=None, status="A"):
        return GATE.problems(GATE.Change(status, path), source)

    def test_original_jdk_type_is_allowed(self):
        self.assertFalse(self.issue("src/java.base/share/classes/java/util/CollectionViews.java",
                                   "package java.util; public final class CollectionViews {}"))

    def test_new_m3_public_type_refused(self):
        self.assertTrue(self.issue("src/java.base/share/classes/java/util/M3HashMap.java",
                                   "package java.util; public class M3HashMap {}"))

    def test_internal_m3_owner_allowed(self):
        self.assertFalse(self.issue("src/java.base/share/classes/jdk/internal/mindex/M3BitLane.java",
                                   "package jdk.internal.mindex; final class M3BitLane {}"))

    def test_new_date_stamped_workflow_refused(self):
        self.assertTrue(self.issue(".github/workflows/m3-collection-20261022.yml"))

    def test_new_experiment_version_refused(self):
        self.assertTrue(self.issue("m3/runtime-integration/collection-views-v7/README-v2.md"))

    def test_nested_date_stamped_experiment_directory_refused(self):
        self.assertTrue(self.issue("m3/collections/qualification/sort-20261022/README.md"))

    def test_other_jdk_public_module_m3_type_refused(self):
        self.assertTrue(self.issue("src/java.desktop/share/classes/java/awt/M3Window.java",
                                   "package java.awt; public class M3Window {}"))

    def test_stable_workflow_allowed(self):
        self.assertFalse(self.issue(".github/workflows/public-api-naming.yml"))

    def test_new_mapping_fragment_refused(self):
        self.assertTrue(self.issue("m3/docs/name-mapping.d/experimental-collections.json"))

    def test_grandfathered_existing_path_may_be_modified(self):
        self.assertFalse(self.issue("m3/docs/name-mapping.d/experimental-20261022.json", status="M"))

    def test_java_package_disagreement_refused(self):
        self.assertTrue(self.issue("src/java.base/share/classes/java/util/CollectionViews.java",
                                   "package wrong; public class CollectionViews {}"))

    def test_jtreg_type_mismatch_refused(self):
        self.assertTrue(self.issue("test/jdk/java/util/CollectionViews/CollectionViewsTest.java",
                                   "public class OldTest {}"))

    def test_stable_jtreg_test_allowed(self):
        self.assertFalse(self.issue("test/jdk/java/util/CollectionViews/CollectionViewsTest.java",
                                    "public class CollectionViewsTest {}"))

    def test_public_nested_m3_jdk_type_refused(self):
        self.assertTrue(self.issue("src/java.base/share/classes/java/util/CollectionViews.java",
                                   "package java.util; public class CollectionViews {public class M3Foo {}}"))

    def test_existing_public_jdk_types_not_renamed(self):
        self.assertFalse(self.issue("src/java.base/share/classes/java/util/M3Existing.java", status="M"))

    def test_new_synexia_recipe_crate_refused(self):
        self.assertTrue(self.issue("synexia-openrewrite-recipes/crates/catalogue/pom.xml"))

    def test_historical_product_recipe_source_mutation_refused(self):
        self.assertTrue(self.issue(
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/OldRecipe.java", status="M"))

    def test_new_openrewrite_java_recipe_refused(self):
        self.assertTrue(self.issue(
            "m3/collections/src/main/java/com/m3/rewrite/OptimizeRecipe.java",
            "class OptimizeRecipe {}"))

    def test_new_openrewrite_yaml_descriptor_refused(self):
        self.assertTrue(self.issue(
            "m3/collections/src/main/resources/META-INF/rewrite/optimize.yml"))

    def test_native_and_jdk_implementation_results_allowed(self):
        self.assertFalse(self.issue(
            "src/java.base/share/classes/jdk/internal/mindex/M3IndexLane.java",
            "package jdk.internal.mindex; class M3IndexLane {}"))
        self.assertFalse(self.issue("src/java.base/share/native/libjava/M3IndexLane.c"))

    def test_product_test_and_qualification_results_allowed(self):
        self.assertFalse(self.issue(
            "test/jdk/java/util/CollectionViews/FilteredOwnerMembershipTest.java",
            "class FilteredOwnerMembershipTest {}"))
        self.assertFalse(self.issue("m3/compatibility/collection-views/README.md"))

    def test_real_git_diff_detects_additions_and_renames(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root).decode().strip()
            git("init", "-q")
            git("config", "user.name", "M3 Tests")
            git("config", "user.email", "tests@example.invalid")
            (root / "README.md").write_text("baseline\n", encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", "baseline")
            base = git("rev-parse", "HEAD")
            (root / "README.md").write_text("update\n", encoding="utf-8")
            path = root / "test/jdk/java/util/CollectionViews"
            path.mkdir(parents=True)
            (path / "CollectionViewsTest.java").write_text(
                "class CollectionViewsTest {}\n", encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", "add stable test")
            head = git("rev-parse", "HEAD")
            delta, failures = GATE.check(root, base, head)
            self.assertEqual(len(delta), 2)
            self.assertFalse(failures)
            self.assertEqual(GATE.main(["--base", base, "--head", head, "--repo", str(root)]), 0)
            self.assertEqual(GATE.main(["--base", "HEAD", "--head", head, "--repo", str(root)]), 2)


    def test_new_synexia_recipe_crate_refused(self):
        self.assertTrue(self.issue("synexia-openrewrite-recipes/crates/catalogue/pom.xml"))

    def test_historical_product_recipe_source_mutation_refused(self):
        self.assertTrue(self.issue(
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/OldRecipe.java", status="M"))

    def test_new_openrewrite_java_recipe_refused(self):
        self.assertTrue(self.issue(
            "m3/collections/src/main/java/com/m3/rewrite/OptimizeRecipe.java",
            "class OptimizeRecipe {}"))

    def test_new_openrewrite_yaml_descriptor_refused(self):
        self.assertTrue(self.issue(
            "m3/collections/src/main/resources/META-INF/rewrite/optimize.yml"))

    def test_native_and_jdk_implementation_results_allowed(self):
        self.assertFalse(self.issue(
            "src/java.base/share/classes/jdk/internal/mindex/M3IndexLane.java",
            "package jdk.internal.mindex; class M3IndexLane {}"))
        self.assertFalse(self.issue("src/java.base/share/native/libjava/M3IndexLane.c"))

    def test_product_test_and_qualification_results_allowed(self):
        self.assertFalse(self.issue(
            "test/jdk/java/util/CollectionViews/FilteredOwnerMembershipTest.java",
            "class FilteredOwnerMembershipTest {}"))
        self.assertFalse(self.issue("m3/compatibility/collection-views/README.md"))

    def test_new_donor_imports_in_jdk_owner_refused(self):
        path = "src/java.base/share/classes/java/util/CollectionViews.java"
        self.assertTrue(self.issue(
            path, "package java.util;\nimport com.synexia.indexstring.MIndexString;\n"
                  "class CollectionViews {}"))
        self.assertTrue(self.issue(
            path, "package java.util;\nimport org.eclipse.collections.api.list.MutableList;\n"
                  "class CollectionViews {}"))

    def test_fully_qualified_donor_reference_refused(self):
        path = "src/java.base/share/classes/jdk/internal/mindex/M3Bits.java"
        self.assertTrue(self.issue(
            path, "package jdk.internal.mindex; class M3Bits {"
                  " Object o = com.google.common.collect.ImmutableList.of(); }"))
        self.assertTrue(self.issue(
            path, "import org.apache.commons.collections4.Bag; class M3Bits {}"))

    def test_lexical_guard_ignores_comments_and_text_blocks(self):
        path = "src/java.base/share/classes/java/util/CollectionViews.java"
        program = ('package java.util;\n// import com.synexia.X;\n'
                   '/* org.eclipse.collections.Y */\n'
                   'class CollectionViews { String s = """\n'
                   'com.google.common.collect.ImmutableList\n"""; }\n')
        self.assertFalse(self.issue(path, program))

    def test_old_only_reference_grandfathered_and_new_reference_rejected(self):
        path = "src/java.base/share/classes/java/util/CollectionViews.java"
        old = "package java.util;\nimport com.m3.text.M3String;\nclass CollectionViews {}\n"
        self.assertFalse(GATE.problems(GATE.Change("M", path), old, old))
        self.assertTrue(GATE.problems(
            GATE.Change("M", path), old + "\nimport org.springframework.context.X;", old))

    def test_ordinary_jdk_names_and_m3_proving_ground_unaffected(self):
        self.assertFalse(self.issue(
            "src/java.xml/share/classes/com/sun/org/apache/xalan/internal/X.java",
            "package com.sun.org.apache.xalan.internal; class X {}"))
        self.assertFalse(self.issue(
            "m3/core/src/com/m3/text/M3String.java",
            "package com.m3.text; class M3String {}"))

    def test_real_git_modified_jdk_source_rejects_donor_name(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root).decode().strip()
            git("init", "-q")
            git("config", "user.name", "M3 Tests")
            git("config", "user.email", "tests@example.invalid")
            path = root / "src/java.base/share/classes/java/util/Foo.java"
            path.parent.mkdir(parents=True)
            path.write_text("package java.util; class Foo {}\n", encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", "baseline")
            base = git("rev-parse", "HEAD")
            path.write_text(
                "package java.util;\nimport org.apache.commons.collections4.Bag;\n"
                "class Foo {}\n", encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", "import external donor")
            head = git("rev-parse", "HEAD")
            changed, failures = GATE.check(root, base, head)
            self.assertEqual(len(changed), 1)
            self.assertEqual(len(failures), 1)
            self.assertIn("org.apache.commons.collections4.Bag", failures[0][1])


if __name__ == "__main__":
    unittest.main()
