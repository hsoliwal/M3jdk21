# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "compatibility" / "synexia-recipe-home-policy.tsv"


class SynexiaRecipeHomePolicyTest(unittest.TestCase):
    def test_policy_is_consumer_only_for_reusable_m3_surfaces(self) -> None:
        with POLICY.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(8, len(rows))
        for row in rows:
            self.assertEqual("M3JDK21_SYNEXIA_RECIPE_HOME_V1", row["schema"])
            self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
            self.assertEqual("hsoliwal/M3jdk21", row["target_repository"])
            self.assertEqual("Apache-2.0", row["license"])

        by_surface = {row["local_surface"]: row for row in rows}
        for surface in (
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/**",
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/**",
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
