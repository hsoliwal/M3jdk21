# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "compatibility" / "m3-recipe-home-policy.tsv"
PIN = ROOT / "compatibility" / "m3-recipe-home-pin.tsv"


class M3RecipeHomePolicyTest(unittest.TestCase):
    def test_policy_is_consumer_only_for_reusable_m3_surfaces(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(8, len(rows))
        for row in rows:
            self.assertEqual("M3JDK21_M3_RECIPE_HOME_V1", row["schema"])
            self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
            self.assertEqual("hsoliwal/M3jdk21", row["target_repository"])
            self.assertEqual("Apache-2.0", row["license"])

        by_surface = {row["local_surface"]: row for row in rows}
        for surface in (
            "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/scope/**",
            "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/atom/**",
            "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/semantic/**",
            "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/a3/**",
            "m3/indexdb/**",
        ):
            self.assertEqual(
                "MIGRATION_RESIDUE_NOT_CANONICAL",
                by_surface[surface]["disposition"],
                surface,
            )
            self.assertEqual("true", by_surface[surface]["handoff_required"])

        self.assertEqual(
            "JDK_TARGET_SPECIFIC",
            by_surface[
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/backport/**"
            ]["disposition"],
        )
        self.assertEqual(
            "false",
            by_surface[
                "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/backport/**"
            ]["handoff_required"],
        )


    def test_canonical_home_pin_binds_exact_m3_manifest(self) -> None:
        with PIN.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(1, len(rows))
        row = rows[0]
        self.assertEqual("M3JDK21_M3_RECIPE_HOME_PIN_V1", row["schema"])
        self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
        self.assertRegex(row["canonical_revision"], r"^[0-9a-f]{40}$")
        self.assertEqual(
            "m3-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv",
            row["canonical_manifest_path"],
        )
        self.assertRegex(row["canonical_manifest_git_blob"], r"^[0-9a-f]{40}$")
        self.assertEqual(
            "docs/M3-SCALE/invariants/M3-PUBLIC-TARGET-CONVERGENCE-1.json",
            row["convergence_invariant_path"],
        )
        self.assertRegex(row["convergence_invariant_git_blob"], r"^[0-9a-f]{40}$")
        self.assertEqual("Apache-2.0", row["license"])
        self.assertEqual("PINNED_CANONICAL_SOURCE", row["state"])


    def test_opt_in_maven_profile_consumes_canonical_m3_recipe_artifact(self) -> None:
        pom = ROOT / "tooling" / "migration-recipes" / "pom.xml"
        tree = ET.parse(pom)
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        root = tree.getroot()

        version = root.find("m:properties/m:m3.version", ns)
        self.assertIsNotNone(version)
        self.assertEqual("1.0.0-SNAPSHOT", version.text)

        profiles = root.findall("m:profiles/m:profile", ns)
        profile = next(
            p
            for p in profiles
            if p.find("m:id", ns) is not None
            and p.find("m:id", ns).text == "m3-canonical-recipes"
        )
        self.assertIsNone(profile.find("m:activation", ns))

        dependency = profile.find(
            "m:build/m:plugins/m:plugin/m:dependencies/m:dependency", ns
        )
        self.assertIsNotNone(dependency)
        self.assertEqual("com.synexia", dependency.find("m:groupId", ns).text)
        self.assertEqual(
            "m3-openrewrite-recipes", dependency.find("m:artifactId", ns).text
        )
        self.assertEqual(
            "${m3.version}", dependency.find("m:version", ns).text
        )

    def test_m3index_db_and_jdk_bridge_are_m3_owned(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = {row["local_surface"]: row for row in csv.DictReader(handle, delimiter="\t")}

        self.assertEqual(
            "com.synexia:m3-m3index-db",
            rows["m3/indexdb/**"]["canonical_owner"],
        )
        self.assertEqual(
            "com.synexia:m3-m3index-jdk-bridge",
            rows["m3/ports/**"]["canonical_owner"],
        )


if __name__ == "__main__":
    unittest.main()
