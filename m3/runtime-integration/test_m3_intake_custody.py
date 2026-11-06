# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Product-policy regression: recipe custody is distinct from a runtime dependency."""
from __future__ import annotations

import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).with_name("verify-m3-intake.py")
spec = importlib.util.spec_from_file_location("intake_custody", MODULE)
intake = importlib.util.module_from_spec(spec)
spec.loader.exec_module(intake)


class RecipeCustodyTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        columns = ["mode", "source_license", "target_scope", "source_copy",
                   "runtime_dependency", "required_evidence", "disposition"]
        self.rows = []
        for mode in sorted(intake.REQUIRED_MODES):
            self.rows.append(dict(zip(columns, [mode, "Apache-2.0", "m3/**", "N/A",
                                                "NO", "source and product proof", "ADMITTED"])))
        self.row("TARGET_OWNED_PORT")["source_copy"] = "NO_DIRECT_COPY"
        self.row("EXPLICIT_RELICENSE_PORT")["disposition"] = "REVIEW_REQUIRED"
        self.row("EXTERNAL_RECIPE_TOOL").update(
            target_scope="hsoliwal/com.synexia only",
            source_copy="POSTIMAGES_AND_PASSIVE_RECEIPTS_ONLY")
        self.columns = columns
        self.write(intake.MAPPING, json.dumps({"migration": {"records": []}}))
        self.write(intake.DIRECT, "mapping_id\tm3jdk21_target_paths\treview_status\trequired_resolution\n")
        self.write(intake.FOSS, "repository\tlicense\tclassification\tcapability\n"
                   "https://github.com/hsoliwal/com.synexia\tApache-2.0\tTARGET_OWNED_PORT\tfixture\n")
        self.write(intake.LICENSE_INFO, "programs licensed under the Apache License, Version 2.0\n"
                   "would not permit you to commingle code under an incompatible license\n")

    def row(self, mode):
        return next(row for row in self.rows if row["mode"] == mode)

    def write(self, path, text):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8")

    def verify(self):
        text = "\t".join(self.columns) + "\n"
        text += "".join("\t".join(row[c] for c in self.columns) + "\n" for row in self.rows)
        self.write(intake.POLICY, text)
        with contextlib.redirect_stdout(io.StringIO()):
            intake.verify(self.root)

    def test_m3_only_execution_and_passive_target_receipts_are_accepted(self):
        self.verify()

    def test_legacy_target_tooling_scope_is_rejected_even_without_runtime_dependency(self):
        self.row("EXTERNAL_RECIPE_TOOL")["target_scope"] = "m3/tooling/**"
        with self.assertRaisesRegex(SystemExit, "exclusively in Synexia"):
            self.verify()

    def test_recipe_source_copy_is_rejected_even_with_m3_execution(self):
        self.row("EXTERNAL_RECIPE_TOOL")["source_copy"] = "ALLOW_IN_TOOLING_ONLY"
        with self.assertRaisesRegex(SystemExit, "postimages and passive receipts"):
            self.verify()

    def test_runtime_dependency_remains_forbidden(self):
        self.row("EXTERNAL_RECIPE_TOOL")["runtime_dependency"] = "YES"
        with self.assertRaisesRegex(SystemExit, "runtime dependency"):
            self.verify()

    def test_duplicate_mode_cannot_hide_a_conflicting_policy_row(self):
        self.rows.append(dict(self.row("EXTERNAL_RECIPE_TOOL")))
        with self.assertRaisesRegex(SystemExit, "mode set drift"):
            self.verify()

    def test_relicensing_review_is_not_weakened_by_custody_correction(self):
        self.row("EXPLICIT_RELICENSE_PORT")["disposition"] = "ADMITTED"
        with self.assertRaisesRegex(SystemExit, "explicit relicense review"):
            self.verify()


if __name__ == "__main__":
    unittest.main()
