# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Release naming/documentation regression checks; no product runtime claims."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / "m3" / "release"


class ReleaseMetadataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.contract = json.loads((PACK / "release-contract.json").read_text(encoding="utf-8"))
        cls.notes = (PACK / "RELEASE_NOTES.md").read_text(encoding="utf-8")
        cls.checklist = (PACK / "RELEASE_CHECKLIST.md").read_text(encoding="utf-8")
        cls.readme = (PACK / "README.md").read_text(encoding="utf-8")
        cls.prep = (PACK / "NAMING_DOCUMENTATION_COVERAGE.md").read_text(encoding="utf-8")

    def test_release_identity_is_explicit_and_consistent(self):
        branch = self.contract["release"]["release_branch"]
        self.assertRegex(branch, r"^release/m3jdk-21\.\d+\.\d+-m3\.\d+-preview$")
        tag = branch.removeprefix("release/")
        self.assertIn("Branch: `" + branch + "`", self.notes)
        self.assertIn("| Release branch | `" + branch + "` |", self.checklist)
        self.assertIn("Tag: `" + tag + "`", self.notes)
        self.assertIn("| Tag | `" + tag + "`", self.checklist)

    def test_merged_prs_are_not_presented_as_pending_tasks(self):
        self.assertIn("already-merged PRs", self.checklist)
        self.assertNotIn("Review, approve and merge [#2177]", self.checklist)
        self.assertNotIn("Review, approve and merge [#2184]", self.checklist)

    def test_preparation_is_linked_and_not_admission(self):
        self.assertIn("NAMING_DOCUMENTATION_COVERAGE.md", self.readme)
        self.assertIn("not a release admission receipt", self.prep)
        self.assertIn("PENDING", self.prep)
        self.assertIn("JCov/Jacoco", self.prep)

    def test_unqualified_contracts_are_not_promoted_by_release_notes(self):
        claims = self.contract["current_claims"]
        for name in ("java_lang_string_backed_by_m3_string",
                     "full_regex_precompute", "full_jtreg_compatibility",
                     "arrays_qualified", "collections_qualified",
                     "swt_eclipse_ide_qualified"):
            self.assertEqual("NOT_CLAIMED", claims[name], name)
        self.assertIn("Non-claims", self.notes)

    def test_release_notes_do_not_claim_a_contradictory_recipe_producer(self):
        self.assertNotIn("owner per contract); imported receipt", self.notes)
        self.assertIn("HOLD — owner policy conflict", self.notes)
        self.assertIn("HOLD", self.notes)
        self.assertIn("reconcile policy", self.notes)

    def test_historical_ci_numbers_are_explicitly_dated(self):
        self.assertIn("Historical CI snapshot only", self.notes)
        self.assertIn("not 2026-11-07 current", self.notes)
        self.assertIn("source tree", self.checklist)

    def test_custody_disagreement_is_visible_for_review(self):
        self.assertIn("Recipe custody/owner policy", self.notes)
        self.assertIn("blocks sign-off", self.prep)


if __name__ == "__main__":
    unittest.main()
