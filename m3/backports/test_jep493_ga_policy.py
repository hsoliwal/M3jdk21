# SPDX-License-Identifier: Apache-2.0
"""Regression proof for the JEP 493 GA-final denominator and thin workflow receiver."""

from __future__ import annotations

import csv
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parent
PACKET = ROOT / "recipes" / "jep-493-runtime-image"
REMOVED = (
    "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/runtimelink/"
    "RuntimeImageLinkException.java"
)


def tsv_dict(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as handle:
        return dict(csv.reader(handle, delimiter="\t"))


class Jep493GaPolicyTest(unittest.TestCase):
    def test_ga_materialization_and_typed_exclusion_preserve_47_path_denominator(self) -> None:
        selected = [
            line.strip()
            for line in (PACKET / "PATHS.txt").read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        self.assertEqual(46, len(selected))
        self.assertEqual(46, len(set(selected)))
        self.assertNotIn(REMOVED, selected)

        with (PACKET / "TYPED_EXCLUSIONS.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            exclusions = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(
            [
                {
                    "path": REMOVED,
                    "reason": "GA_REMOVED_DO_NOT_ADD",
                    "donor_state": "ABSENT_IN_JDK24_GA",
                    "authority": "EVIDENCE_ONLY",
                }
            ],
            exclusions,
        )
        self.assertEqual(47, len(selected) + len(exclusions))

        upstream = tsv_dict(PACKET / "UPSTREAM_COMMIT.tsv")
        # UPSTREAM_COMMIT.tsv is a row table, not field/value; validate explicitly below.
        self.assertTrue(upstream)

    def test_upstream_and_composition_counts_stay_at_47(self) -> None:
        with (PACKET / "UPSTREAM_COMMIT.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(1, len(rows))
        self.assertEqual("47", rows[0]["touched_paths"])
        self.assertEqual(
            "2ec358082f0896480bdbfcb289b4ba2bff0dd828",
            rows[0]["upstream_commit"],
        )

        with (PACKET / "COMPOSITION_PLAN.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        feature = [row for row in rows if row["group"] == "feature-join"]
        self.assertEqual(1, len(feature))
        self.assertEqual("47", feature[0]["members"])

        leaves = [row for row in rows if row["group"] != "feature-join"]
        self.assertEqual(47, sum(int(row["members"]) for row in leaves))
        excluded = [row for row in leaves if row["group"] == "ga-removed-exclusion"]
        self.assertEqual(1, len(excluded))
        self.assertEqual("1", excluded[0]["members"])
        self.assertEqual("false", excluded[0]["mutation_authority"])

    def test_materialization_policy_and_receiver_are_fail_closed(self) -> None:
        policy = tsv_dict(PACKET / "MATERIALIZATION_POLICY.tsv")
        self.assertEqual("47", policy["implementation_denominator_paths"])
        self.assertEqual("46", policy["materialization_paths"])
        self.assertEqual("1", policy["typed_exclusions"])
        self.assertEqual("FILE", policy["file_atom_scope"])
        self.assertEqual("MULTI_MODULE", policy["feature_scope"])
        self.assertEqual("false", policy["branch_product_mutation"])
        self.assertEqual("NOT_AUTHORIZED", policy["promotion"])

        receiver = tsv_dict(PACKET / "WORKFLOW_VALIDATOR_RECEIVER.tsv")
        self.assertEqual("hsoliwal/com.synexia", receiver["canonical_repository"])
        self.assertEqual(
            "0f0f9965092955c15f5c32f5eb102137fd0dbf8e",
            receiver["canonical_commit"],
        )
        self.assertEqual(
            "synexia-code-convergence/tools/WorkflowStructureCheck.java",
            receiver["canonical_path"],
        )
        self.assertEqual(
            "f61be81f5a15bea78f1c5ae7d5555844c9cd2ea7",
            receiver["canonical_git_blob"],
        )
        self.assertEqual("M3JDK21_THIN_RECEIVER", receiver["target_role"])
        self.assertEqual("GITHUB_ACTIONS_JOB_ADMISSION", receiver["startup_oracle"])
        self.assertEqual("false", receiver["source_copy_authority"])
        self.assertEqual("false", receiver["promotion_authority"])


if __name__ == "__main__":
    unittest.main()
