# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "compatibility" / "synexia-recipe-home-policy.tsv"
PIN = ROOT / "compatibility" / "synexia-recipe-home-pin.tsv"
RESIDUE = ROOT / "compatibility" / "synexia-canonical-residue-gitblobs.tsv"


class SynexiaRecipeHomePolicyTest(unittest.TestCase):
    def test_retired_atom_legacy_stays_absent_while_synexia_mirror_remains(self) -> None:
        ownership = ROOT / "compatibility" / "synexia-recipe-ownership.tsv"
        with ownership.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        retired = [
            row for row in rows
            if row["state"] == "BORROWED_CANONICAL_LEGACY_RETIRED"
        ]
        self.assertEqual(7, len(retired))
        for row in retired:
            self.assertIn("/com/m3/rewrite/atom/", row["legacy_path"])
            self.assertFalse((ROOT.parent / row["legacy_path"]).exists(), row["legacy_path"])
            self.assertTrue((ROOT.parent / row["canonical_path"]).is_file(), row["canonical_path"])

        frozen = [
            row for row in rows
            if row["state"] == "BORROWED_CANONICAL_WITH_FROZEN_LEGACY"
        ]
        self.assertGreater(len(frozen), 0)
        for row in frozen:
            self.assertTrue((ROOT.parent / row["legacy_path"]).is_file(), row["legacy_path"])
            self.assertTrue((ROOT.parent / row["canonical_path"]).is_file(), row["canonical_path"])

    def test_pending_synexia_scope_migration_is_pinned_but_not_claimed_borrowed(self) -> None:
        ownership = ROOT / "compatibility" / "synexia-recipe-ownership.tsv"
        with ownership.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        pending = [
            row for row in rows
            if row["state"].startswith("SYNEXIA_CANONICAL_PENDING_MERGE")
        ]
        self.assertEqual(8, len(pending))
        self.assertEqual(
            {
                "scope-contract-mode",
                "scope-policy",
                "scope-registry",
                "file-fixed-point",
                "visibility-level",
                "visibility-inventory",
                "verification-plan",
                "multipass-planner",
            },
            {row["family"] for row in pending},
        )
        for row in pending:
            self.assertEqual(
                "700cc1765c959a85b5a05249e2c2a08c571616ca",
                row["synexia_revision"],
            )
            self.assertTrue(
                row["canonical_path"].startswith(
                    "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/"
                )
            )
            self.assertEqual(40, len(row["synexia_git_blob"]))
            self.assertFalse(
                (ROOT.parent / row["canonical_path"]).exists(),
                "pending external canonical path must not masquerade as local borrowed custody",
            )

    def test_policy_is_consumer_only_for_reusable_m3_surfaces(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        # The three A3/JNI receiver rows were admitted after the original 16-row freeze.
        # Keep exact cardinality and reject duplicate surfaces; do not relax ownership rules.
        self.assertEqual(23, len(rows))
        self.assertEqual(23, len({row["local_surface"] for row in rows}))
        for row in rows:
            self.assertEqual("M3JDK21_SYNEXIA_RECIPE_HOME_V1", row["schema"])
            self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
            self.assertEqual("hsoliwal/M3jdk21", row["target_repository"])
            self.assertEqual("Apache-2.0", row["license"])

        by_surface = {row["local_surface"]: row for row in rows}
        for surface in (
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/**",
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/a3/**",
            "m3/indexdb/**",
        ):
            self.assertEqual(
                "MIGRATION_RESIDUE_NOT_CANONICAL",
                by_surface[surface]["disposition"],
                surface,
            )
            self.assertEqual("true", by_surface[surface]["handoff_required"])

        self.assertEqual(
            "com.synexia.rewrite.semantic.M3SemanticHashRecipe",
            by_surface[
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic/**"
            ]["canonical_owner"],
        )

        scope_expectations = {
            "M3EditScope.java":
                ("RECEIVER_ADAPTER_ONLY", "com.synexia.rewrite.M3EditScope", "true"),
            "M3ScopeInference.java":
                ("TARGET_PRODUCT_ADAPTER", "com.synexia.rewrite.M3ScopeInference", "true"),
            "M3ContractMode.java":
                ("RECEIVER_ADAPTER_ONLY", "com.synexia.rewrite.M3ContractMode", "true"),
            "M3RecipeScopePolicy.java":
                ("TARGET_PRODUCT_ADAPTER", "com.synexia.rewrite.M3RecipeScopePolicy", "true"),
            "M3RecipeScopeRegistry.java":
                ("TARGET_PRODUCT_ADAPTER", "com.synexia.rewrite.M3RecipeScopeRegistry", "true"),
        }
        for file_name, expected in scope_expectations.items():
            surface = (
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/"
                + file_name
            )
            row = by_surface[surface]
            self.assertEqual(expected[0], row["disposition"], surface)
            self.assertEqual(expected[1], row["canonical_owner"], surface)
            self.assertEqual(expected[2], row["handoff_required"], surface)

        self.assertEqual(
            "RECEIVER_ADAPTER_ONLY",
            by_surface[
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-a3-regex-memory-lab.yml"
            ]["disposition"],
        )
        self.assertEqual(
            "com.synexia.rewrite.M3A3RegexMemoryLab",
            by_surface[
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-a3-regex-memory-lab.yml"
            ]["canonical_owner"],
        )
        self.assertEqual(
            "TARGET_PRODUCT_ADAPTER",
            by_surface[
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-a3-regex-memory-workflow.yml"
            ]["disposition"],
        )
        self.assertEqual(
            "TARGET_PRODUCT_ADAPTER",
            by_surface[
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jni-newstring-admission.yml"
            ]["disposition"],
        )
        self.assertEqual(
            "com.synexia.rewrite.M3JdkHandoff",
            by_surface[
                "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jni-newstring-admission.yml"
            ]["canonical_owner"],
        )

        self.assertEqual(
            "JDK_TARGET_SPECIFIC",
            by_surface[
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/**"
            ]["disposition"],
        )
        self.assertEqual(
            "false",
            by_surface[
                "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/**"
            ]["handoff_required"],
        )


    def test_top_level_shared_surfaces_are_explicitly_noncanonical(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = {row["local_surface"]: row for row in csv.DictReader(handle, delimiter="\t")}

        expected = {
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java":
                "com.synexia.rewrite.M3CanonicalMultiPassPlan",
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java":
                "com.synexia.rewrite.M3RepositoryJava21ConvergenceRecipe",
        }
        for surface, owner in expected.items():
            self.assertEqual("MIGRATION_RESIDUE_NOT_CANONICAL", rows[surface]["disposition"])
            self.assertEqual(owner, rows[surface]["canonical_owner"])
            self.assertEqual("true", rows[surface]["handoff_required"])

        compatibility = rows[
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/InstallIndexStringCompatibility.java"
        ]
        self.assertEqual("RECEIVER_ADAPTER_ONLY", compatibility["disposition"])
        self.assertEqual(
            "com.synexia:synexia-m3index-jdk-bridge",
            compatibility["canonical_owner"],
        )

    def test_full_reusable_residue_set_is_git_blob_frozen(self) -> None:
        with RESIDUE.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(17, len(rows))
        frozen = {row["path"]: row for row in rows}
        self.assertEqual(17, len(frozen))

        actual: set[str] = set()
        for exact in (
            "tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java",
            "tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java",
        ):
            if (ROOT / exact).is_file():
                actual.add("m3/" + exact)

        for directory in (
            "tooling/migration-recipes/src/main/java/com/m3/rewrite/atom",
            "tooling/migration-recipes/src/main/java/com/m3/rewrite/semantic",
            "tooling/migration-recipes/src/main/java/com/m3/rewrite/a3",
            "indexdb/src/main/java/com/m3",
        ):
            start = ROOT / directory
            if start.is_dir():
                actual.update(
                    "m3/" + path.relative_to(ROOT).as_posix()
                    for path in start.rglob("*.java")
                    if path.is_file()
                )

        self.assertEqual(set(frozen), actual)

        for relative, row in frozen.items():
            self.assertEqual("MIGRATION_RESIDUE_NOT_CANONICAL", row["disposition"])
            self.assertEqual("Apache-2.0", row["license"])
            self.assertTrue(
                row["canonical_synexia_owner"].startswith("com.synexia")
                or row["canonical_synexia_owner"].startswith("synexia-")
            )
            path = ROOT.parent / relative
            data = path.read_bytes()
            actual_blob = hashlib.sha1(
                f"blob {len(data)}\0".encode("ascii") + data
            ).hexdigest()
            self.assertEqual(row["git_blob_sha1"], actual_blob, relative)

    def test_a3_mastery_and_catalogue_resolve_to_synexia_recipe_owners(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = {row["local_surface"]: row for row in csv.DictReader(handle, delimiter="\t")}

        a3 = rows["m3/tooling/a3/src/main/java/com/m3/a3/A3Lab.java"]
        self.assertEqual("TARGET_PROOF_CONSUMER", a3["disposition"])
        self.assertEqual(
            "com.synexia.rewrite.M3PureIntConvergenceRecipe",
            a3["canonical_owner"],
        )
        self.assertEqual("true", a3["handoff_required"])

        lab = (
            ROOT
            / "tooling"
            / "a3"
            / "src"
            / "main"
            / "java"
            / "com"
            / "m3"
            / "a3"
            / "A3Lab.java"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "import com.synexia.rewrite.M3PureIntAtomizeRecipe;", lab
        )
        self.assertIn(
            "import com.synexia.rewrite.M3PureIntPatternizeRecipe;", lab
        )
        self.assertNotIn(
            "import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;", lab
        )
        self.assertNotIn(
            "import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;", lab
        )

        catalogue = (ROOT / "tooling" / "recipe-catalogue.tsv").read_text(
            encoding="utf-8"
        )
        for owner in (
            "M3PureIntAtomizeRecipe",
            "M3PureIntInventoryRecipe",
            "M3PureIntPatternizeRecipe",
            "M3PureIntDocumentationRecipe",
            "M3PureIntConvergenceRecipe",
        ):
            self.assertIn(f"com.synexia.rewrite.{owner}", catalogue)
        self.assertNotIn("com.synexia.rewrite.atom.", catalogue)
        self.assertNotIn("com.m3.rewrite.atom.", catalogue)


    def test_canonical_home_pin_binds_exact_synexia_manifest(self) -> None:
        with PIN.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(1, len(rows))
        row = rows[0]
        self.assertEqual("M3JDK21_SYNEXIA_RECIPE_HOME_PIN_V1", row["schema"])
        self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
        self.assertRegex(row["canonical_revision"], r"^[0-9a-f]{40}$")
        self.assertEqual(
            "synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv",
            row["canonical_manifest_path"],
        )
        self.assertRegex(row["canonical_manifest_git_blob"], r"^[0-9a-f]{40}$")
        self.assertEqual(
            "docs/M3-SCALE/invariants/SYNEXIA-PUBLIC-TARGET-CONVERGENCE-1.json",
            row["convergence_invariant_path"],
        )
        self.assertRegex(row["convergence_invariant_git_blob"], r"^[0-9a-f]{40}$")
        self.assertEqual("Apache-2.0", row["license"])
        self.assertEqual("PINNED_CANONICAL_SOURCE", row["state"])


    def test_opt_in_maven_profile_consumes_canonical_synexia_recipe_artifact(self) -> None:
        pom = ROOT / "tooling" / "migration-recipes" / "pom.xml"
        tree = ET.parse(pom)
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        root = tree.getroot()

        version = root.find("m:properties/m:m3.synexia.version", ns)
        self.assertIsNotNone(version)
        self.assertEqual("1.0.0-SNAPSHOT", version.text)

        profiles = root.findall("m:profiles/m:profile", ns)
        profile = next(
            p
            for p in profiles
            if p.find("m:id", ns) is not None
            and p.find("m:id", ns).text == "m3-synexia-canonical-recipes"
        )
        self.assertIsNone(profile.find("m:activation", ns))

        dependency = profile.find(
            "m:build/m:plugins/m:plugin/m:dependencies/m:dependency", ns
        )
        self.assertIsNotNone(dependency)
        self.assertEqual("com.synexia", dependency.find("m:groupId", ns).text)
        self.assertEqual(
            "synexia-openrewrite-recipes", dependency.find("m:artifactId", ns).text
        )
        self.assertEqual(
            "${m3.synexia.version}", dependency.find("m:version", ns).text
        )

    def test_m3index_db_and_jdk_bridge_are_synexia_owned(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = {row["local_surface"]: row for row in csv.DictReader(handle, delimiter="\t")}

        self.assertEqual(
            "com.synexia:synexia-m3index-db",
            rows["m3/indexdb/**"]["canonical_owner"],
        )
        self.assertEqual(
            "com.synexia:synexia-m3index-jdk-bridge",
            rows["m3/ports/**"]["canonical_owner"],
        )


if __name__ == "__main__":
    unittest.main()
