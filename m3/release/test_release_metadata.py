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

    def test_changed_atom_coverage_is_mandatory(self):
        self.assertIn("M3_CHANGED_ATOM_COVERAGE",
                      self.contract["mandatory_release_gates"])
        policy = self.contract["coverage_policy"]
        self.assertEqual(0.90, policy["minimum_line_ratio"])
        self.assertEqual(0.90, policy["minimum_branch_ratio"])
        self.assertTrue(policy["all_changed_atoms_measured"])
        self.assertIn("M3_CHANGED_ATOM_COVERAGE", self.notes)

    def test_unqualified_contracts_are_not_promoted_by_release_notes(self):
        claims = self.contract["current_claims"]
        for name in ("java_lang_string_backed_by_m3_string",
                     "full_regex_precompute", "full_jtreg_compatibility",
                     "arrays_qualified", "collections_qualified",
                     "swt_eclipse_ide_qualified"):
            self.assertEqual("NOT_CLAIMED", claims[name], name)
        self.assertIn("Non-claims", self.notes)

    def test_preview_platform_selection_is_linux_x64_only(self):
        selection = self.contract["platform_selection"]
        self.assertEqual(["linux-x64"], selection["selected"])
        platforms = selection["platforms"]
        self.assertEqual("SELECTED", platforms["linux-x64"])
        workflow = (ROOT / ".github" / "workflows" / "m3-release.yml").read_text(encoding="utf-8")
        # The workflow's supported platform list is the selection vocabulary.
        self.assertIn("linux-x64|macos-x64|macos-aarch64|windows-x64|", workflow)
        for platform in ("linux-x64", "macos-x64", "macos-aarch64", "windows-x64"):
            self.assertIn(platform, platforms)
        for platform, state in platforms.items():
            if platform not in selection["selected"]:
                self.assertEqual("NOT_CLAIMED", state, platform)
                self.assertIn(platform, self.notes)
        self.assertIn("OPENJDK_SANITY_MATRIX", self.contract["mandatory_release_gates"])
        self.assertIn("ships **linux-x64 only**", self.notes)

    def test_release_notes_do_not_claim_a_contradictory_recipe_producer(self):
        # Standalone rule (2026-10-31): the receipt is produced inside M3jdk21.
        owner = self.contract["execution_evidence"]["gate_owners"]["M3_MAVEN_CONTROL_REACTOR"]
        self.assertEqual("hsoliwal/M3jdk21", owner)
        self.assertIn("`" + owner + "`", self.notes)
        self.assertNotIn("owner per contract); imported receipt", self.notes)
        self.assertNotIn("receipt from `hsoliwal/com.synexia`", self.notes)
        self.assertIn("PENDING — receipt produced inside M3jdk21 required", self.notes)
        self.assertIn("HOLD", self.notes)

    def test_release_readme_agrees_on_recipe_execution_owner(self):
        readme = self.readme
        preparation = (PACK / "RELEASE_PREPARATION.md").read_text(encoding="utf-8")
        triage = (PACK / "CI_FAILURE_TRIAGE_20261107.md").read_text(encoding="utf-8")
        self.assertEqual(
            "hsoliwal/M3jdk21",
            self.contract["execution_evidence"]["gate_owners"]["M3_MAVEN_CONTROL_REACTOR"])
        self.assertIn("receipt is produced inside `hsoliwal/M3jdk21`", readme)
        self.assertIn("produced inside M3jdk21", readme)
        self.assertIn("`M3_MAVEN_CONTROL_REACTOR` receipt to `hsoliwal/M3jdk21`", preparation)
        self.assertIn("agree that canonical", triage)
        self.assertNotIn("control-reactor evidence are executed here", readme)
        self.assertNotIn("control-reactor execution and replay receipts produced in this repository", readme)
        self.assertNotIn("Both policies currently exist", preparation)

    def test_naming_fixed_point_evidence_uses_stable_path(self):
        evidence = PACK / "NAMING_FIXED_POINT_EVIDENCE.md"
        self.assertTrue(evidence.is_file())
        self.assertIn("NAMING_FIXED_POINT_EVIDENCE.md", self.readme)
        self.assertIn("moves=35 rewrites=56", evidence.read_text(encoding="utf-8"))
        self.assertFalse((PACK / "NAMING_FIXED_POINT_EVIDENCE_20261107.md").exists())

    def test_historical_ci_numbers_are_explicitly_dated(self):
        self.assertIn("Historical CI snapshot only", self.notes)
        self.assertIn("not 2026-11-07 current", self.notes)
        self.assertIn("source tree", self.checklist)

    def test_custody_disagreement_is_visible_for_review(self):
        # Keep this regression identity: resolved custody cannot promote product gates.
        self.assertIn("Recipe custody/owner policy", self.notes)
        self.assertIn("M3JDK21 owns product build, runtime and target acceptance gates", self.prep)
        self.assertIn("as passive data", self.prep)
        self.assertIn("never executes its recorded command", self.prep)
        self.assertIn("zero executable dependencies", self.prep)
        self.assertIn("does not establish installed-image acceptance", self.prep)
        self.assertIn("whole-tree naming fixed point", self.notes)
        self.assertTrue(self.contract["standalone"]["required_for_release"])


if __name__ == "__main__":
    unittest.main()
