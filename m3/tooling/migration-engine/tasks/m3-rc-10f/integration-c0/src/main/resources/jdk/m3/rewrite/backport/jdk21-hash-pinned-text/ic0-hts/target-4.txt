# SPDX-License-Identifier: Apache-2.0
"""Exact third validator/POM profile; historical plans and behavior stay retained."""
from __future__ import annotations

import importlib.util
import io
import json
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch


class JccHandoffCurrentContextTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        override = os.environ.get("M3_JCC_CANDIDATE_ROOT")
        cls.candidate = Path(override).resolve() if override else Path(__file__).resolve().parents[3]
        previous = sys.dont_write_bytecode
        sys.dont_write_bytecode = True
        cls.addClassCleanup(setattr, sys, "dont_write_bytecode", previous)
        path = cls.candidate / "m3/migration/test/test_jcc_handoff_context_profiles.py"
        spec = importlib.util.spec_from_file_location("jcc_current_retained_profiles", path)
        if spec is None or spec.loader is None:
            raise AssertionError("Cannot load exact JCC context fixture")
        cls.profiles = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.profiles)
        cls.controls = type("IsolatedCurrentJccProfiles", (cls.profiles.JccHandoffContextProfileTests,), {})
        cls.addClassCleanup(cls.controls.doClassCleanups)
        cls.controls.setUpClass()
        cls.addClassCleanup(cls.controls.tearDownClass)

    def setUp(self):
        self.control = self.controls("runTest")
        self.addCleanup(self.control.doCleanups)
        self.current = self.profiles.CURRENT_PROFILE

    def test_current_exact_pair_selects_new_plan_without_writes(self):
        root = self.control.context(self.current)
        self.control.exercise_setup(root, self.current)
        self.assertEqual(self.profiles.CURRENT_POM_SHA256,
                         self.control.recipe.digest((root / self.profiles.POM).read_bytes()))
        for historical in self.profiles.PROFILES:
            self.assertEqual(self.control.plans[historical["state"]]["outputs"],
                             self.control.plans["current"]["outputs"])

    def test_known_validator_and_pom_in_unadmitted_pair_refuse(self):
        root = self.control.context(self.current)
        (root / self.profiles.MIGRATION).write_bytes(self.control.code_images["before"])
        self.control.exercise_setup(root, self.current, (AssertionError, "Unknown actual migration guard context"))

    def test_unknown_pom_or_validator_refuses_without_fallback(self):
        for path in (self.profiles.POM, self.profiles.MIGRATION):
            with self.subTest(path=path):
                root = self.control.context(self.current)
                target = root / path
                target.write_bytes(target.read_bytes() + b"\n# unknown context image\n")
                self.control.exercise_setup(root, self.current, (AssertionError, "Unknown actual migration guard context"))

    def test_current_selected_plan_missing_drifted_or_resealed_refuses(self):
        for mutation in ("missing", "whitespace", "guard", "identity"):
            with self.subTest(mutation=mutation):
                root = self.control.context(self.current)
                target = root / self.control.crate / self.current["plan"]
                if mutation == "missing":
                    target.unlink()
                    refusal = (FileNotFoundError, "plan-intake-752-ci")
                else:
                    if mutation == "whitespace":
                        target.write_bytes(target.read_bytes() + b"\n")
                    else:
                        plan = json.loads(target.read_bytes())
                        if mutation == "guard":
                            next(g for g in plan["guards"] if g["path"] == self.profiles.POM)["sha256"] = self.profiles.HISTORICAL_POM_SHA256
                        else:
                            plan["recipe_id"] = "jcc-handoff-20261005/unqualified-current"
                        body = {key: value for key, value in plan.items() if key != "plan_sha256"}
                        plan["plan_sha256"] = self.control.recipe.digest(self.control.recipe.canonical(body))
                        target.write_bytes(self.control.recipe.canonical(plan))
                    self.control.recipe.sealed_plan(target)
                    refusal = (AssertionError, "Actual packet context plan bytes drift")
                self.control.exercise_setup(root, self.current, refusal)

    def test_historical_pom_fixture_drift_refuses_without_writes(self):
        root = self.control.context(self.current)
        target = root / self.profiles.HISTORICAL_POM
        target.write_bytes(target.read_bytes() + b"\n<!-- historical image drift -->\n")
        before = self.control.base.snapshot(root)
        isolated = type("IsolatedHistoricalPomDrift", (self.profiles.JccHandoffContextProfileTests,), {})
        try:
            with patch.dict(os.environ, {"M3_JCC_CANDIDATE_ROOT": str(root)}):
                with self.assertRaisesRegex(AssertionError, "Historical JCC POM fixture drift"):
                    isolated.setUpClass()
        finally:
            isolated.doClassCleanups()
            self.assertEqual(before, self.control.base.snapshot(root))

    def test_current_other_guard_drift_still_refuses_without_writes(self):
        for guard in self.control.plans["current"]["guards"]:
            if guard["path"] in (self.profiles.MIGRATION, self.profiles.POM):
                continue
            with self.subTest(path=guard["path"]):
                root = self.control.context(self.current)
                target = root / guard["path"]
                target.write_bytes(target.read_bytes() + b"\n# current guard drift\n")
                self.control.exercise_setup(root, self.current, (AssertionError, "Actual candidate guard drift"))

    def test_all_three_profiles_execute_six_retained_packet_methods(self):
        expected = {
            "test_actual_packet_check_apply_idempotent_rollback_and_replay",
            "test_each_actual_guard_missing_or_drifted_refuses_atomically",
            "test_actual_required_preimage_missing_or_drifted_refuses_atomically",
            "test_each_actual_absent_output_occupied_refuses_atomically",
            "test_each_single_postimage_mixed_with_preimages_refuses_atomically",
            "test_each_foreign_postimage_edit_refuses_rollback_and_replay_atomically",
        }
        self.assertEqual(expected, set(unittest.defaultTestLoader.getTestCaseNames(self.control.base)))
        for profile in (*self.profiles.PROFILES, self.current):
            with self.subTest(state=profile["state"]):
                root = self.control.context(profile)
                before = self.control.base.snapshot(root)
                isolated = type("IsolatedThreeProfilePacket", (self.control.base,), {})
                suite = unittest.defaultTestLoader.loadTestsFromTestCase(isolated)
                stream = io.StringIO()
                previous = sys.dont_write_bytecode
                try:
                    with patch.dict(os.environ, {"M3_JCC_CANDIDATE_ROOT": str(root)}):
                        result = unittest.TextTestRunner(stream=stream, verbosity=2).run(suite)
                finally:
                    sys.dont_write_bytecode = previous
                self.assertEqual(6, result.testsRun, stream.getvalue())
                self.assertTrue(result.wasSuccessful(), stream.getvalue())
                self.assertFalse(result.skipped, stream.getvalue())
                self.assertEqual(before, self.control.base.snapshot(root))


if __name__ == "__main__":
    unittest.main(verbosity=2)
