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


    def test_versioned_qualification_json_is_validated_passive_data(self):
        import json
        filename = "m3/qualification/collection-convergence-20261024/REGEX_COMPACT_METADATA_RECEIPT.json"
        content = json.dumps({"schema": "M3_REGEX_COMPACT_METADATA_RECEIVER_V1",
                              "canonical_repository": "hsoliwal/com.synexia",
                              "status": "RESOURCE_HOLD"})
        self.assertFalse(self.issue(filename, content))
        self.assertFalse(self.issue("m3/qualification/collection-convergence-20261024/RECEIPT.json", content))
        self.assertTrue(self.issue(filename))
        self.assertTrue(self.issue(filename, "{not-json"))
        self.assertTrue(self.issue(filename, content.replace("hsoliwal/com.synexia", "wrong/repo")))
        self.assertTrue(self.issue(filename, content.replace("M3_REGEX", "INVALID_REGEX")))

    def test_qualification_tsv_requires_origin_and_spdx(self):
        filename = "m3/qualification/indexed-sort-image-20261026/SOURCE_RECEIPT.tsv"
        content = ("# SPDX-License-Identifier: Apache-2.0\n"
                   "path\tidentity_kind\tbefore_identity\tafter_identity\tcanonical_origin\tqualification\n"
                   "test/jdk/jdk/internal/m3/M3IndexedArrayTest.java\tSHA-256\tA\tB\t"
                   "hsoliwal/com.synexia:m3-recipe\tPENDING\n")
        self.assertFalse(self.issue(filename, content))
        self.assertTrue(self.issue(filename, content.replace("hsoliwal/com.synexia", "wrong/repo")))
        self.assertTrue(self.issue(filename, content.replace("# SPDX-License-Identifier: Apache-2.0\n", "")))
        self.assertTrue(self.issue(filename, content.replace("\tPENDING\n", "")))

    def test_exact_source_borrow_tsv_version_is_checked(self):
        filename = "m3/synexia-import/current-full-borrow/string-source-observation-v3.tsv"
        data = ("schema\ttarget_path\tpredecessor_git_blob\n"
                "M3JDK21_STRING_SOURCE_OBSERVATION_V3\t"
                "src/java.base/share/classes/java/lang/M3String.java\tbefore\n")
        self.assertFalse(self.issue(filename, data))
        self.assertTrue(self.issue(filename, data.replace("_V3", "_V4")))
        self.assertTrue(self.issue(filename, data.replace("src/java.base/share/classes/java/lang/", "../")))
        successor = "m3/synexia-import/current-full-borrow/string-target-successor-v4.tsv"
        self.assertFalse(self.issue(successor, data.replace(
            "SOURCE_OBSERVATION_V3", "TARGET_SUCCESSOR_V4")))

    def test_dated_executable_and_nested_artifact_still_refused(self):
        self.assertTrue(self.issue(".github/workflows/m3-names-20261026.yml"))
        self.assertTrue(self.issue("m3/qualification/task-20261026/README.md"))
        self.assertTrue(self.issue("m3/qualification/task-20261026/build.py"))
        self.assertTrue(self.issue("m3/qualification/task-20261026/run.sh"))
        self.assertTrue(self.issue("m3/qualification/task-20261026/dir/RECEIPT.json", "{}"))
        self.assertTrue(self.issue("m3/qualification/task-20261026/RECEIPT.json", "{}"))
        self.assertTrue(self.issue("src/java.base/share/classes/java/util/M3NewType.java",
                                   "package java.util; public class M3NewType {}"))
        self.assertTrue(self.issue("m3/tooling/migration-recipes/README.md", status="M"))

    def test_real_git_diff_reads_and_validates_passive_receipts(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", *args], cwd=root).decode().strip()
            git("init", "-q")
            git("config", "user.name", "M3 Tests")
            git("config", "user.email", "tests@example.invalid")
            (root / "README.md").write_text("original\n", encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", "original")
            baseline = git("rev-parse", "HEAD")
            receipt = root / "m3/qualification/collection-convergence-20261024/RECEIPT.json"
            receipt.parent.mkdir(parents=True)
            receipt.write_text(
                '{"schema":"M3JDK21_EXECUTED_COLLECTION_CONVERGENCE_V1",'
                '"canonical_repository":"hsoliwal/com.synexia"}\n',
                encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", "canonical passive receipt")
            first = git("rev-parse", "HEAD")
            delta, failed = GATE.check(root, baseline, first)
            self.assertEqual(1, len(delta))
            self.assertFalse(failed)
            receipt.write_text(
                '{"schema":"M3JDK21_EXECUTED_COLLECTION_CONVERGENCE_V1",'
                '"canonical_repository":"attacker/forgery"}\n',
                encoding="utf-8")
            git("commit", "-qam", "forged receipt source")
            later = git("rev-parse", "HEAD")
            delta, failed = GATE.check(root, baseline, later)
            self.assertEqual(1, len(delta))
            self.assertEqual(1, len(failed))
            self.assertIn("unverified or malformed passive provenance receipt", failed[0][1])

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


if __name__ == "__main__":
    unittest.main()
