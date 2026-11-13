# SPDX-License-Identifier: Apache-2.0
"""Synthetic checks that release branch identity drift is never silently admitted."""
from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SOURCE = Path(__file__).with_name("audit_release_identity.py")
SPEC = importlib.util.spec_from_file_location("m3_release_identity", SOURCE)
assert SPEC is not None and SPEC.loader is not None
identity = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(identity)


class ReleaseIdentityTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.release = self.root / "m3/release"
        self.release.mkdir(parents=True)
        self.write("release/qualified", "release/qualified")

    def write(self, notes_branch, checklist_branch, product="M3JDK21"):
        info = {"release": {"release_branch": "release/qualified", "product": product}}
        (self.release / "release-contract.json").write_text(
            json.dumps(info), encoding="utf-8")
        tick = chr(96)
        (self.release / "RELEASE_NOTES.md").write_text(
            "Tag: preview | Branch: " + tick + notes_branch + tick + "\n",
            encoding="utf-8")
        (self.release / "RELEASE_CHECKLIST.md").write_text(
            "| Release branch | " + tick + checklist_branch + tick + " |\n",
            encoding="utf-8")

    def test_matching_identity_passes(self):
        report = identity.audit(self.root)
        self.assertTrue(report["identity_consistent"])
        self.assertTrue(report["release_documentation_admissible"])

    def test_stale_notes_block(self):
        self.write("release/old", "release/qualified")
        report = identity.audit(self.root)
        self.assertFalse(report["release_documentation_admissible"])
        self.assertEqual("release/old", report["branches"]["release_notes"])

    def test_stale_checklist_blocks(self):
        self.write("release/qualified", "release/old")
        self.assertFalse(identity.audit(self.root)["identity_consistent"])

    def test_nonproduct_metadata_blocks(self):
        self.write("release/qualified", "release/qualified", product="Other")
        report = identity.audit(self.root)
        self.assertTrue(report["identity_consistent"])
        self.assertFalse(report["release_documentation_admissible"])

    def test_missing_human_branch_blocks(self):
        self.write("release/qualified", "release/qualified")
        (self.release / "RELEASE_NOTES.md").write_text("No branch here\n")
        self.assertFalse(identity.audit(self.root)["identity_consistent"])


if __name__ == "__main__":
    unittest.main()
