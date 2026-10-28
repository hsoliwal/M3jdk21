# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-shape and real-git regression tests; no JDK API transformation."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
import hashlib
import json
from unittest import mock

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

    def test_versioned_passive_evidence_is_not_public_api(self):
        self.assertFalse(self.issue(
            "m3/qualification/collection-convergence-20261024/REGEX_COMPACT_METADATA_RECEIPT.json"))
        self.assertFalse(self.issue(
            "m3/qualification/indexed-sort-image-20261026/SOURCE_RECEIPT.tsv"))
        self.assertFalse(self.issue(
            "m3/synexia-import/current-full-borrow/string-target-successor-v4.tsv"))

    def test_recipe_and_code_in_evidence_directory_still_refused(self):
        self.assertTrue(self.issue(
            "m3/qualification/collection-convergence-20261024/DangerousRecipe.java"))
        self.assertTrue(self.issue(
            "m3/qualification/collection-convergence-20261024/release.sh"))
        self.assertTrue(self.issue(
            ".github/workflows/m3-collection-20261024.yml"))
        self.assertTrue(self.issue(
            "m3/docs/name-mapping.d/new-20261024.json"))

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


class PinnedPassiveReceiptTest(unittest.TestCase):
    RECEIPTS = (
        ("m3/compatibility/synexia-java-base-collection-core-receipt-20261025.json",
         "M3JDK21_JAVA_BASE_COLLECTION_CORE_RECEIPT_V1"),
        ("m3/compatibility/synexia-sdk-catalogue-maintenance-receipt-20261025.json",
         "M3JDK21_SDK_CATALOGUE_MAINTENANCE_RECEIPT_V1"),
    )

    def receipt(self, path):
        return (Path(__file__).resolve().parents[2] / path).read_text(encoding="utf-8")

    def test_exact_two_actual_pinned_receipts_allowed(self):
        self.assertEqual(set(GATE.PINNED_PASSIVE_RECEIPTS), {p for p, _ in self.RECEIPTS})
        for path, schema in self.RECEIPTS:
            with self.subTest(path=path):
                source = self.receipt(path)
                self.assertEqual(json.loads(source)["schema"], schema)
                self.assertFalse(GATE.problems(GATE.Change("A", path), source))

    def test_receipt_changed_bytes_refused_even_with_same_json(self):
        for path, _ in self.RECEIPTS:
            source = self.receipt(path)
            self.assertEqual(json.loads(source), json.loads(source + "\n"))
            self.assertTrue(GATE.problems(GATE.Change("A", path), source + "\n"))

    def test_receipt_schema_checked_independently_of_hash(self):
        path, schema = self.RECEIPTS[0]
        value = json.loads(self.receipt(path))
        value["schema"] = "FOREIGN_EXECUTABLE_OR_RECEIPT_SCHEMA"
        source = json.dumps(value)
        # Qualify the schema fence independently: a matching byte pin alone
        # cannot admit a foreign schema into the finite passive receipt set.
        with mock.patch.dict(GATE.PINNED_PASSIVE_RECEIPTS, {
                path: (hashlib.sha256(source.encode("utf-8")).hexdigest(), schema)}):
            self.assertTrue(GATE.problems(GATE.Change("A", path), source))

    def test_receipt_top_level_object_required_independently_of_hash(self):
        path, schema = self.RECEIPTS[0]
        source = json.dumps([{"schema": schema}])
        with mock.patch.dict(GATE.PINNED_PASSIVE_RECEIPTS, {
                path: (hashlib.sha256(source.encode("utf-8")).hexdigest(), schema)}):
            self.assertTrue(GATE.problems(GATE.Change("A", path), source))

    def test_receipt_malformed_json_refused_independently_of_hash(self):
        path, schema = self.RECEIPTS[0]
        source = '{"schema":'
        with mock.patch.dict(GATE.PINNED_PASSIVE_RECEIPTS, {
                path: (hashlib.sha256(source.encode("utf-8")).hexdigest(), schema)}):
            self.assertTrue(GATE.problems(GATE.Change("A", path), source))

    def test_receipt_without_content_refused(self):
        for path, _ in self.RECEIPTS:
            self.assertTrue(GATE.problems(GATE.Change("A", path)))

    def test_pinned_content_does_not_admit_other_dated_paths(self):
        source = self.receipt(self.RECEIPTS[0][0])
        digest = hashlib.sha256(source.encode("utf-8")).hexdigest()
        for path in ("m3/compatibility/arbitrary-20261025.json",
                     "m3/compatibility/" + digest + "-20261025.json",
                     "src/java.base/share/classes/java/util/Receipt20261025-20261025.java",
                     "m3/collections/src/main/resources/META-INF/rewrite/recipe-20261025.yml",
                     "synexia-openrewrite-recipes/recipe-20261025.json"):
            with self.subTest(path=path):
                self.assertTrue(GATE.problems(GATE.Change("A", path), source))

    def test_real_git_loads_exact_pinned_receipt_json(self):
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
            for path, _ in self.RECEIPTS:
                destination = root / path
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes((Path(__file__).resolve().parents[2] / path).read_bytes())
            git("add", ".")
            git("commit", "-qm", "add exact passive evidence")
            delta, failures = GATE.check(root, base, git("rev-parse", "HEAD"))
            self.assertEqual({change.path for change in delta}, {p for p, _ in self.RECEIPTS})
            self.assertFalse(failures)


if __name__ == "__main__":
    unittest.main()
