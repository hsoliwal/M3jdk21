# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

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


if __name__ == "__main__":
    unittest.main()
