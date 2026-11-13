# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import copy
import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import check_m3_full_family as family


class M3FullFamilyPolicyTest(unittest.TestCase):
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
        baseline = json.loads((family.ROOT / "doc/m3/name-mapping.json").read_text())
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
                reason = "per-atom evidence contract" if key == "atom_evidence" else "receiving sequence contract drift: " + key
                self.assert_refused(candidate, reason)
        for index in range(5):
            for key, value in [("state", "ACCEPTED"), ("predecessor", "REMAINING_FAMILIES"),
                               ("required_gates", []), ("acceptance_receipts", ["self-reported-pass"]),
                               ("scope", ""), ("id", "UNKNOWN")]:
                with self.subTest(phase=index, key=key):
                    candidate = copy.deepcopy(baseline)
                    candidate["porting_invariant"]["receiving_sequence"]["phases"][index][key] = value
                    reasons = {
                        "state": "reviewed successor with target evidence required",
                        "acceptance_receipts": "reviewed successor with target evidence required",
                        "predecessor": "receiving predecessor drift",
                        "required_gates": "missing or changed target gates",
                        "scope": "missing scope",
                        "id": "reordered, duplicate or unknown receiving phase",
                    }
                    self.assert_refused(candidate, reasons[key])
        for change in ("duplicate", "delete", "swap"):
            candidate = copy.deepcopy(baseline)
            phases = candidate["porting_invariant"]["receiving_sequence"]["phases"]
            if change == "duplicate": phases[1] = copy.deepcopy(phases[0])
            elif change == "delete": phases.pop()
            else: phases[1], phases[3] = phases[3], phases[1]
            reason = "missing or extra receiving phase" if change == "delete" else "reordered, duplicate or unknown receiving phase"
            self.assert_refused(candidate, reason)

    def assert_refused(self, candidate: dict, reason: str) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "name-mapping.json"
            path.write_text(json.dumps(candidate), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, reason):
                family.validate_receiving_sequence(path)

    def test_receiving_map_refuses_duplicate_json_keys_and_missing_plan(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "name-mapping.json"
            for value, reason in (
                ('{"porting_invariant":{},"porting_invariant":{}}', "duplicate receiving-map JSON key"),
                ('{}', "missing canonical porting invariant"),
                ('{"porting_invariant":{"id":"M3-JDK-PORT-1"}}', "missing receiving sequence"),
            ):
                with self.subTest(value=value):
                    path.write_text(value, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        family.validate_receiving_sequence(path)

    def test_historical_schema_catalogue_and_evidence_ids_are_preserved(self) -> None:
        pin = family.load_pin()
        self.assertEqual("M3JDK21_SYNEXIA_FULL_FAMILY_PIN_V1", pin["schema"])
        self.assertEqual(
            "synexia-openrewrite-recipes/verification/synexia-m3jdk-full-family-20261007/CATALOGUE_INDEX.json",
            pin["catalogue_path"],
        )
        baseline = json.loads((family.ROOT / "doc/m3/name-mapping.json").read_text())
        self.assertEqual("M3-JDK-PORT-1", baseline["porting_invariant"]["id"])
        self.assertIn("synexia_recipe_revision_and_manifest", family.validate_receiving_sequence()["atom_evidence"])
        candidate = copy.deepcopy(baseline)
        candidate["porting_invariant"]["id"] = "M3JDK-PORT-1"
        self.assert_refused(candidate, "missing canonical porting invariant")
        candidate = copy.deepcopy(baseline)
        atoms = candidate["porting_invariant"]["receiving_sequence"]["atom_evidence"]
        atoms[atoms.index("synexia_recipe_revision_and_manifest")] = "m3_recipe_revision_and_manifest"
        self.assert_refused(candidate, "per-atom evidence contract")

    def test_pin_refuses_product_renaming_of_historical_source_identities(self) -> None:
        checked = family.PIN.read_text(encoding="utf-8")
        changes = (
            ("M3JDK21_SYNEXIA_FULL_FAMILY_PIN_V1", "M3JDK21_M3_FULL_FAMILY_PIN_V1"),
            (
                "synexia-openrewrite-recipes/verification/synexia-m3jdk-full-family-20261007/CATALOGUE_INDEX.json",
                "m3-openrewrite-recipes/verification/m3-m3jdk-full-family-20261007/CATALOGUE_INDEX.json",
            ),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pin.tsv"
            for selected in ((changes[0],), (changes[1],), changes):
                with self.subTest(changes=selected):
                    body = checked
                    for before, after in selected:
                        body = body.replace(before, after)
                    self.assertNotEqual(checked, body)
                    path.write_text(body, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "full-family pin drift"):
                        family.load_pin(path)

    def test_pin_requires_complete_nonempty_unique_fields(self) -> None:
        checked = family.PIN.read_text(encoding="utf-8")
        cases = (
            ("empty", "", "header"),
            ("header", checked.replace("field\tvalue", "key\tvalue", 1), "header"),
            ("short-row", checked + "extra\n", "invalid full-family pin row"),
            ("empty-field", checked + "\tvalue\n", "invalid full-family pin row"),
            ("empty-value", checked + "extra\t\n", "invalid full-family pin row"),
            ("duplicate", checked + "schema\tM3JDK21_SYNEXIA_FULL_FAMILY_PIN_V1\n", "duplicate"),
            ("unknown-field", checked + "extra\tvalue\n", "pin drift"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pin.tsv"
            for label, body, reason in cases:
                with self.subTest(label=label):
                    path.write_text(body, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        family.load_pin(path)

    def test_catalogue_refuses_changed_provenance_counts_and_completion(self) -> None:
        baseline = json.loads(family.CATALOGUE.read_text(encoding="utf-8"))
        pin = dict(row.split("\t") for row in family.PIN.read_text().splitlines()[1:])
        cases = (
            (("invariantId",), "OTHER", "catalogue invariant"),
            (("metadata", "sourceRevision"), "0" * 40, "source revision drift"),
            (("metadata", "counts", "total"), 4771, "catalogue total drift"),
            (("metadata", "sourceTreeSelection", "moduleRoots"), [], "checked-root count drift"),
            (("metadata", "sourceTreeSelection", "repositoryWideComplete"), True, "repository-wide completion"),
            (("metadata", "counts", "byDisposition"), {"ACCEPTED": 4770}, "remain unevaluated"),
            (("acceptance",), "ACCEPTED", "target acceptance"),
            (("repositoryCompletion",), "PROVEN", "repository completion"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "catalogue.json"
            for keys, value, reason in cases:
                with self.subTest(keys=keys):
                    candidate = copy.deepcopy(baseline)
                    owner = candidate
                    for key in keys[:-1]:
                        owner = owner[key]
                    owner[keys[-1]] = value
                    path.write_text(json.dumps(candidate), encoding="utf-8")
                    with mock.patch.object(family, "CATALOGUE", path):
                        with self.assertRaisesRegex(ValueError, reason):
                            family.validate_catalogue(pin)

    def test_family_index_refuses_missing_categories_or_completion_claims(self) -> None:
        checked = family.PRECOMPUTE.read_text(encoding="utf-8")
        lines = checked.splitlines()
        accounting = next(line for line in lines if line.split("\t")[1:2] == ["Full named-file migration accounting"])
        cases = (
            ("empty", "", "empty"),
            ("header", checked.replace("sourceWorld", "source", 1), "header"),
            ("missing-family", "\n".join(line for line in lines if line.split("\t")[1:2] != ["Canonical String identity"]), "missing full-family receiving categories"),
            ("completed-accounting", checked.replace(accounting, accounting.replace("NOT_EVALUATED_IN_THIS_LEDGER", "ACCEPTED")), "explicitly unevaluated"),
            ("duplicate-accounting", checked + "\n" + accounting + "\n", "explicitly unevaluated"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "families.tsv"
            for label, body, reason in cases:
                with self.subTest(label=label):
                    path.write_text(body, encoding="utf-8")
                    with mock.patch.object(family, "PRECOMPUTE", path):
                        with self.assertRaisesRegex(ValueError, reason):
                            family.validate_family_index()

    def test_receiving_plan_requires_phase_shape_and_exact_baseline_boundaries(self) -> None:
        baseline = json.loads((family.ROOT / "doc/m3/name-mapping.json").read_text())
        cases = (
            ("phases", {}, "missing or extra receiving phase"),
            ("target_baseline", "main", "missing exact target baseline"),
            ("advance_rule", "", "missing receiving boundary"),
            ("dependency_rule", " \t", "missing receiving boundary"),
            ("enforcement_boundary", None, "missing receiving boundary"),
        )
        for key, value, reason in cases:
            with self.subTest(key=key):
                candidate = copy.deepcopy(baseline)
                candidate["porting_invariant"]["receiving_sequence"][key] = value
                self.assert_refused(candidate, reason)
        candidate = copy.deepcopy(baseline)
        candidate["porting_invariant"]["receiving_sequence"]["phases"][1] = None
        self.assert_refused(candidate, "reordered, duplicate or unknown receiving phase")
        candidate = copy.deepcopy(baseline)
        del candidate["porting_invariant"]["receiving_sequence"]["phases"][0]["predecessor"]
        self.assert_refused(candidate, "receiving predecessor drift")

    def test_cli_keeps_the_qualification_scope_and_rejects_extra_arguments(self) -> None:
        for argv, status, message, error in (
            (["check_m3_full_family.py"], 0, "active=STRING ordered_phases=5 completion=false", ""),
            (["check_m3_full_family.py", "extra"], 2, "", "usage:"),
        ):
            with self.subTest(argv=argv):
                stdout, stderr = io.StringIO(), io.StringIO()
                with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
                    self.assertEqual(status, family.main(argv))
                self.assertIn(message, stdout.getvalue())
                self.assertIn(error, stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
