# SPDX-License-Identifier: Apache-2.0
"""Regression proof for JEP 467 implementation denominator vs GA materialization."""

from __future__ import annotations

import csv
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parent
PACKET = ROOT / "recipes" / "jep-467-markdown"
REMOVED = (
    "test/langtools/tools/javac/processing/model/util/elements/"
    "TestGetDocComments.java"
)


def field_value(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as handle:
        return dict(csv.reader(handle, delimiter="\t"))


class Jep467GaPolicyTest(unittest.TestCase):
    def test_implementation_denominator_is_251_and_ga_materialization_is_250_plus_one(self) -> None:
        denominator = [
            line.strip()
            for line in (PACKET / "PATHS.txt").read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        materialization = [
            line.strip()
            for line in (PACKET / "MATERIALIZATION_PATHS.txt")
            .read_text(encoding="utf-8")
            .splitlines()
            if line.strip()
        ]

        self.assertEqual(251, len(denominator))
        self.assertEqual(251, len(set(denominator)))
        self.assertEqual(250, len(materialization))
        self.assertEqual(250, len(set(materialization)))
        self.assertIn(REMOVED, denominator)
        self.assertNotIn(REMOVED, materialization)

        with (PACKET / "TYPED_EXCLUSIONS.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            exclusions = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(
            [
                {
                    "path": REMOVED,
                    "reason": "IMPLEMENTATION_DELETION_ALREADY_ABSENT",
                    "implementation_state": "DELETED_BY_JEP467",
                    "jdk21_state": "ABSENT",
                    "jdk23_ga_state": "ABSENT",
                    "authority": "EVIDENCE_ONLY",
                }
            ],
            exclusions,
        )
        self.assertEqual(set(denominator), set(materialization) | {REMOVED})

    def test_packet_dag_models_deleted_file_as_non_mutating_evidence(self) -> None:
        with (PACKET / "packet.tsv").open(encoding="utf-8", newline="") as handle:
            packet = list(csv.DictReader(handle, delimiter="\t"))

        evidence = [
            row for row in packet if row["atom_id"] == "already-absent-deletion"
        ]
        self.assertEqual(1, len(evidence))
        self.assertEqual("FILE", evidence[0]["scope"])
        self.assertEqual("false", evidence[0]["scope_promotion_approved"])
        self.assertEqual("typed-exclusion:" + REMOVED, evidence[0]["work_ref"])

        self.assertFalse(
            any(row["work_ref"] == "generated-file-atom:" + REMOVED for row in packet)
        )
        feature = next(row for row in packet if row["atom_id"] == "feature-join")
        self.assertIn("already-absent-deletion", feature["depends_on"].split(","))

        with (PACKET / "atom-evidence.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        deleted = next(row for row in rows if row["atom_id"] == "already-absent-deletion")
        self.assertEqual("EvidenceAtom", deleted["pattern"])
        self.assertEqual("MarkdownDoc.AlreadyAbsentDeletion", deleted["iop_role"])
        self.assertEqual("false", deleted["fixed_point_required"])

    def test_composition_counts_and_policy_preserve_full_feature_denominator(self) -> None:
        with (PACKET / "COMPOSITION_PLAN.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        feature = next(row for row in rows if row["group"] == "feature-join")
        self.assertEqual("251", feature["members"])

        leaves = [row for row in rows if row["group"] != "feature-join"]
        self.assertEqual(251, sum(int(row["members"]) for row in leaves))
        deleted = next(row for row in leaves if row["group"] == "already-absent-deletion")
        self.assertEqual("1", deleted["members"])
        self.assertEqual("false", deleted["mutation_authority"])

        policy = field_value(PACKET / "MATERIALIZATION_POLICY.tsv")
        self.assertEqual("251", policy["implementation_denominator_paths"])
        self.assertEqual("250", policy["materialization_paths"])
        self.assertEqual("1", policy["typed_exclusions"])
        self.assertEqual(REMOVED, policy["excluded_path"])
        self.assertEqual("IMPLEMENTATION_DELETION_ALREADY_ABSENT", policy["exclusion_reason"])
        self.assertEqual("FILE", policy["file_atom_scope"])
        self.assertEqual("LIBRARY_API", policy["feature_scope"])
        self.assertEqual("false", policy["branch_product_mutation"])
        self.assertEqual("NOT_AUTHORIZED", policy["promotion"])

    def test_packet_plan_uses_251_denominator_and_250_materialization_semantics(self) -> None:
        with (PACKET / "PACKET_PLAN.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual("inventory-upstream", rows[0]["atom_id"])
        self.assertIn("251 implementation paths", rows[0]["stop_condition"])
        self.assertIn("250 materializable paths", rows[1]["stop_condition"])
        self.assertIn("deletion evidence is non-mutating", rows[2]["stop_condition"])


if __name__ == "__main__":
    unittest.main()
