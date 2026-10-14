#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import unittest
from pathlib import Path


class Jep404InventoryPolicyTest(unittest.TestCase):
    ROOT = Path(__file__).with_name("recipes") / "jep-404-generational-shenandoah"

    def test_exact_sorted_unique_228_path_denominator(self) -> None:
        paths = [
            line.strip()
            for line in (self.ROOT / "INTEGRATION_PATHS.txt")
            .read_text(encoding="utf-8")
            .splitlines()
            if line.strip()
        ]
        self.assertEqual(228, len(paths))
        self.assertEqual(paths, sorted(paths))
        self.assertEqual(len(paths), len(set(paths)))
        self.assertTrue(all(not path.startswith("/") for path in paths))
        self.assertTrue(all("\\" not in path for path in paths))

    def test_policy_is_inventory_only_and_fail_closed(self) -> None:
        with (self.ROOT / "MATERIALIZATION_POLICY.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            policy = dict(csv.reader(handle, delimiter="\t"))

        expected = {
            "jep": "404",
            "classification": "COMPATIBLE_RUNTIME",
            "default_java21": "CANDIDATE",
            "integration_commit": "28ae281b42cd00f471e275db544a5d23a42df59c",
            "integration_jbs": "8337511",
            "donor_release": "24",
            "donor_tag": "jdk-24+36",
            "integration_denominator_paths": "228",
            "baseline_tag": "jdk-21+35",
            "inventory_joiner": "m3/backports/selected_path_inventory.py",
            "generator": "m3/backports/generate_recipe_crates.py",
            "crate_size": "1",
            "include_native": "true",
            "include_text": "true",
            "file_atom_scope": "FILE",
            "feature_scope": "MULTI_MODULE",
            "executor": "m3/backports/materialize_generated_crates.py",
            "materialization": "EPHEMERAL_CANDIDATE_BUILD_PROOF",
            "candidate_checkout_mutation": "EPHEMERAL_ONLY",
            "build_gate": "FASTDEBUG_IMAGES",
            "runtime_gate": "LEGACY_AND_EXPERIMENTAL_GENERATIONAL_SHENANDOAH",
            "jtreg_gate": "GENERATIONAL_JNI_MXBEANS_JFR",
            "source_mutation": "NOT_AUTHORIZED",
            "compatibility": "NOT_PROVEN",
            "promotion": "NOT_AUTHORIZED",
        }
        self.assertEqual(expected, policy)

    def test_packet_plan_does_not_authorize_mutation_before_dependency_review(self) -> None:
        with (self.ROOT / "PACKET_PLAN.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(9, len(rows))
        self.assertEqual(
            [
                "integration-path-inventory",
                "final-ga-path-classification",
                "generate-file-recipe-atoms",
                "dependency-review",
                "materialize-candidate",
                "build-shenandoah",
                "shenandoah-jtreg-runtime",
                "fixed-point",
                "promotion",
            ],
            [row["atom_id"] for row in rows],
        )
        self.assertEqual(
            ["false", "false", "false", "false"],
            [row["mutation"] for row in rows[:4]],
        )
        self.assertEqual("true", rows[4]["mutation"])
        self.assertEqual("MULTI_MODULE", rows[4]["scope"])
        self.assertEqual("LIBRARY_API", rows[-1]["scope"])
        self.assertEqual("false", rows[-1]["mutation"])


if __name__ == "__main__":
    unittest.main()
