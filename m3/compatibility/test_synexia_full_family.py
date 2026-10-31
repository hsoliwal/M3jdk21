# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path

import check_synexia_full_family as family


class SynexiaFullFamilyPolicyTest(unittest.TestCase):
    def test_checked_in_pin_and_catalogue_are_non_promoting(self) -> None:
        pin = family.load_pin()
        catalogue = family.validate_catalogue(pin)
        self.assertEqual("Apache-2.0", pin["first_party_license"])
        self.assertEqual(
            "Copyright 2026 Hitesh Soliwal and contributors",
            pin["copyright_notice"],
        )
        self.assertEqual(
            "ABSTRACT_IDEA_NOT_RELABELED_AS_COPYRIGHTED_SOURCE",
            pin["abstract_idea_policy"],
        )
        self.assertEqual("false", pin["automatic_application"])
        self.assertEqual("false", pin["target_relicense_authority"])
        self.assertEqual("false", pin["family_completion"])
        self.assertEqual(4770, catalogue["metadata"]["counts"]["total"])
        self.assertEqual(
            {"NOT_EVALUATED_IN_THIS_LEDGER": 4770},
            catalogue["metadata"]["counts"]["byDisposition"],
        )

    def test_required_string_ast_precompute_collection_and_native_families_remain_visible(self) -> None:
        self.assertGreaterEqual(family.validate_family_index(), 28)
        text = family.PRECOMPUTE.read_text(encoding="utf-8")
        for token in (
            "MIndexString",
            "MIndexAST",
            "MIndexDag",
            "MIndexPrecomputeEngine",
            "MIndexUniversalPrecompute",
            "MIndexStringSet",
            "MIndexStringMap",
            "MIndexNativeBytes",
            "MIndexRegexPlan",
        ):
            self.assertIn(token, text)

    def test_pin_refuses_automatic_application_relicensing_or_completion(self) -> None:
        checked = family.PIN.read_text(encoding="utf-8")
        mutations = {
            "automatic_application\tfalse": "automatic_application\ttrue",
            "target_relicense_authority\tfalse": "target_relicense_authority\ttrue",
            "family_completion\tfalse": "family_completion\ttrue",
        }
        for before, after in mutations.items():
            with self.subTest(after=after), tempfile.TemporaryDirectory() as directory:
                candidate = Path(directory) / "pin.tsv"
                candidate.write_text(checked.replace(before, after), encoding="utf-8")
                with self.assertRaises(ValueError):
                    family.load_pin(candidate)

    def test_pin_refuses_license_or_copyright_relabeling(self) -> None:
        checked = family.PIN.read_text(encoding="utf-8")
        for before, after in (
            ("first_party_license\tApache-2.0", "first_party_license\tMIT"),
            (
                "copyright_notice\tCopyright 2026 Hitesh Soliwal and contributors",
                "copyright_notice\tCopyright somebody else",
            ),
            (
                "abstract_idea_policy\tABSTRACT_IDEA_NOT_RELABELED_AS_COPYRIGHTED_SOURCE",
                "abstract_idea_policy\tIDEA_IS_COPYRIGHTED_SOURCE",
            ),
        ):
            with self.subTest(after=after), tempfile.TemporaryDirectory() as directory:
                candidate = Path(directory) / "pin.tsv"
                candidate.write_text(checked.replace(before, after), encoding="utf-8")
                with self.assertRaises(ValueError):
                    family.load_pin(candidate)


    def test_receiving_plan_starts_at_string_and_covers_all_later_phases(self) -> None:
        plan = family.validate_receiving_sequence()
        self.assertEqual("STRING", plan["active_phase"])
        self.assertEqual(list(family.RECEIVING_ORDER), [p["id"] for p in plan["phases"]])
        self.assertTrue(plan["public_jdk_names_unchanged"])
        self.assertFalse(plan["family_completion"])

    def test_receiving_plan_refuses_skips_claims_rights_and_contract_loss(self) -> None:
        baseline = json.loads((family.ROOT / "m3/docs/name-mapping.json").read_text())
        cases = [
            ("active_phase", "ARRAYS"), ("order", list(reversed(family.RECEIVING_ORDER))),
            ("public_jdk_names_unchanged", False), ("canonical_payload_owner_unchanged", False),
            ("recipe_first", False), ("serial_file_atom_replacement", False),
            ("automatic_application", True), ("family_completion", True),
            ("repository_completion", True), ("family_completion", 0),
            ("first_party_license", "MIT"), ("copyright_notice", "unknown"),
            ("abstract_ideas", "EXCLUSIVE_COPYRIGHT"), ("third_party_and_openjdk", "APACHE_EVERYWHERE"),
            ("source_inventory_disposition", "ACCEPTED"), ("source_inventory_named_paths", 4769),
            ("donor_review_order", ["LICENSED_PINNED_GITHUB"]), ("atom_evidence", []),
        ]
        for key, value in cases:
            with self.subTest(key=key, value=value):
                candidate = copy.deepcopy(baseline)
                candidate["porting_invariant"]["receiving_sequence"][key] = value
                self.assert_refused(candidate)
        for index in range(5):
            for key, value in [("state", "ACCEPTED"), ("predecessor", "REMAINING_FAMILIES"),
                               ("required_gates", []), ("acceptance_receipts", ["self-reported-pass"]),
                               ("scope", ""), ("id", "UNKNOWN")]:
                with self.subTest(phase=index, key=key):
                    candidate = copy.deepcopy(baseline)
                    candidate["porting_invariant"]["receiving_sequence"]["phases"][index][key] = value
                    self.assert_refused(candidate)
        for change in ("duplicate", "delete", "swap"):
            candidate = copy.deepcopy(baseline)
            phases = candidate["porting_invariant"]["receiving_sequence"]["phases"]
            if change == "duplicate": phases[1] = copy.deepcopy(phases[0])
            elif change == "delete": phases.pop()
            else: phases[1], phases[3] = phases[3], phases[1]
            self.assert_refused(candidate)

    def assert_refused(self, candidate: dict) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "name-mapping.json"
            path.write_text(json.dumps(candidate), encoding="utf-8")
            with self.assertRaises(ValueError):
                family.validate_receiving_sequence(path)

    def test_receiving_map_refuses_duplicate_json_keys_and_missing_plan(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "name-mapping.json"
            for value in ('{"porting_invariant":{},"porting_invariant":{}}', '{}'):
                path.write_text(value, encoding="utf-8")
                with self.assertRaises(ValueError):
                    family.validate_receiving_sequence(path)


if __name__ == "__main__":
    unittest.main()
