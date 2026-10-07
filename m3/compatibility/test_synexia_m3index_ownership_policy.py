# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PIN = ROOT / "compatibility" / "synexia-m3index-ownership-pin.tsv"
FAMILIES = ROOT / "compatibility" / "synexia-m3index-family-handoff.tsv"
BORROWING = ROOT / "docs" / "synexia-apache2-borrowing-contract.md"
RECEIVER = ROOT / "compatibility" / "SYNEXIA_M3INDEX_RECEIVER.md"
HEX40 = re.compile(r"^[0-9a-f]{40}$")


class SynexiaM3IndexOwnershipPolicyTest(unittest.TestCase):
    def test_candidate_pin_is_exact_non_promoting_and_consumer_only(self) -> None:
        with PIN.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(1, len(rows))
        row = rows[0]
        self.assertEqual(
            [
                "schema",
                "canonical_repository",
                "canonical_revision",
                "source_pr",
                "source_merge_commit",
                "policy_path",
                "policy_git_blob",
                "ownership_path",
                "ownership_git_blob",
                "donor_plan_path",
                "donor_plan_git_blob",
                "license",
                "generic_implementation_authority",
                "target_runtime_authority",
                "state",
            ],
            list(row),
        )
        self.assertEqual("M3JDK21_SYNEXIA_M3INDEX_OWNERSHIP_PIN_V2", row["schema"])
        self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
        self.assertRegex(row["canonical_revision"], HEX40)
        self.assertEqual(row["canonical_revision"], row["source_merge_commit"])
        self.assertEqual("9644", row["source_pr"])
        for field in ("policy_git_blob", "ownership_git_blob", "donor_plan_git_blob"):
            self.assertRegex(row[field], HEX40)
        self.assertEqual("Apache-2.0", row["license"])
        self.assertEqual("false", row["generic_implementation_authority"])
        self.assertEqual("true", row["target_runtime_authority"])
        self.assertEqual("PINNED_CANONICAL_SOURCE", row["state"])

    def test_every_generic_family_remains_synexia_owned(self) -> None:
        with FAMILIES.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(8, len(rows))
        by_family = {row["family"]: row for row in rows}
        self.assertEqual(8, len(by_family))

        for family in (
            "STRING_CORE",
            "AST",
            "DAG",
            "DATA_STRUCTURE",
            "PRECOMPUTE",
            "GENERIC_NATIVE_JNI",
            "MIGRATION_RECIPES",
        ):
            row = by_family[family]
            self.assertNotEqual("PRODUCT_OWNER", row["m3jdk21_role"], family)
            self.assertEqual("PINNED_CANDIDATE", row["state"], family)
            self.assertIn("synexia", row["canonical_synexia_owner"].lower(), family)

        target = by_family["JDK_PRODUCT_INTEGRATION"]
        self.assertEqual("hsoliwal/M3jdk21", target["canonical_synexia_owner"])
        self.assertEqual("PRODUCT_OWNER", target["m3jdk21_role"])
        self.assertEqual("TARGET_CANONICAL", target["state"])
        self.assertIn("OpenJDK", target["license_boundary"])

    def test_string_ast_dag_and_precompute_handoff_names_are_explicit(self) -> None:
        text = RECEIVER.read_text(encoding="utf-8")
        for token in (
            "MIndexString",
            "M3String",
            "MIndexAST",
            "M3AST",
            "MIndexASTAtom",
            "M3ASTAtom",
            "MIndexDag",
            "M3Dag",
            "HotSpot/JIT/GC/CDS/JVMTI/JNI",
        ):
            self.assertIn(token, text)

    def test_borrowing_contract_never_relicenses_jdk_or_third_party(self) -> None:
        text = BORROWING.read_text(encoding="utf-8")
        self.assertIn("PINNED_CANONICAL_SOURCE", text)
        self.assertIn("grants no product mutation", text)
        self.assertIn("OpenJDK", text)
        self.assertIn("relicenses OpenJDK", text)


if __name__ == "__main__":
    unittest.main()
