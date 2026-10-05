# SPDX-License-Identifier: Apache-2.0
"""Closed JCC setup profiles using the actual retained plans and validator images.

These controls invoke the existing packet fixture setup in isolated candidate
roots. They do not replace its six installer behavior tests or qualify products.
"""
from __future__ import annotations

import importlib.util
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch


MIGRATION = "m3/migration/migration.py"
INSTALLER = "m3/migration/recipe.py"
PACKET_TEST = "m3/migration/test/test_jcc_handoff_packet.py"
INTAKE_CRATE = Path("m3/migration/intake-20261005/crate")
INTAKE_PLAN_SHA256 = "47aa763f4678124df42f3181e68e2ff0a74636add9403c1e36e40224094e0cdc"
INSTALLER_SHA256 = "453d976a5f29526432be138b357d3f52f868fcc40a4ee697d969cc0836359628"
PROFILES = (
    {
        "state": "before",
        "code_sha256": "fd54e5fade1fc3050816265f2317967dd4176e7bb5e0bc55e6f620878026e88e",
        "plan": "plan.json",
        "plan_sha256": "648f80ae4073400f1ea1210f8cdc26942019d53eb4452d839961c466f395d05a",
        "recipe_id": "jcc-handoff-20261005/2",
        "seal": "70283e86d30abf8e30a09b10d77be40f99160371a98927dc151d680e3c2e4a87",
    },
    {
        "state": "after",
        "code_sha256": "4114b546749888ec5e6065ea6087d39ea63bafb6ce5881e2239eafaade3b7787",
        "plan": "plan-intake-8131.json",
        "plan_sha256": "8034d77f1124584888dce10760bd2f5dae55eeb93497f7a0db1d09a3a197ec5f",
        "recipe_id": "jcc-handoff-20261005/2-intake-8131",
        "seal": "0704f21408ef263d4d0adc9eb5d48e62c7c30949cb1a197fb9b7f8a21aff4152",
    },
)


POM = "m3/tooling/migration-recipes/pom.xml"
HISTORICAL_POM = Path(
    "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
    "jdk21-hash-pinned-text/jcc-handoff-20261005/pom-before-752.xml"
)
HISTORICAL_POM_SHA256 = "8544be570c2656c5fa641ae34fc01eaea420bbb420f6d80ce5f95253504e9888"
CURRENT_POM_SHA256 = "a53bdeee8afe28b90cb0e348e718b70610a9487edb1b75b46e09bfb76e0f3dc9"
CURRENT_PROFILE = {
    "state": "current",
    "code_sha256": "4114b546749888ec5e6065ea6087d39ea63bafb6ce5881e2239eafaade3b7787",
    "plan": "plan-intake-752-ci.json",
    "plan_sha256": "e4ea964db1a6f9527122ff0dad1c969a43dc50a0f80c771c86b70bcd6da6dcaf",
    "recipe_id": "jcc-handoff-20261005/2-intake-752-ci",
    "seal": "6e8d48067e814566e0c63db14db405c360c8ff3ab4948d352170b6ca20293594",
}


class JccHandoffContextProfileTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        override = os.environ.get("M3_JCC_CANDIDATE_ROOT")
        cls.candidate = Path(override).resolve() if override else Path(__file__).resolve().parents[3]
        previous_cache_flag = sys.dont_write_bytecode
        sys.dont_write_bytecode = True
        cls.addClassCleanup(setattr, sys, "dont_write_bytecode", previous_cache_flag)
        cls.recipe = cls.load("jcc_profile_retained_installer", cls.candidate / INSTALLER)
        cls.packet_module = cls.load("jcc_profile_actual_packet_fixture", cls.candidate / PACKET_TEST)
        cls.base = cls.packet_module.JccHandoffPacketTests
        cls.crate = cls.packet_module.CRATE
        cls.source_bytes = {}
        cls.frozen_inputs = {}

        def retain(relative):
            path = cls.candidate / relative
            content = path.read_bytes()
            cls.frozen_inputs[path] = content
            cls.source_bytes[str(relative)] = content
            return content

        if cls.recipe.digest(retain(INSTALLER)) != INSTALLER_SHA256:
            raise AssertionError("Profile controls require the exact retained installer")
        retain(PACKET_TEST)
        intake_path = cls.candidate / INTAKE_CRATE / "plan.json"
        intake_bytes = retain(INTAKE_CRATE / "plan.json")
        if cls.recipe.digest(intake_bytes) != INTAKE_PLAN_SHA256:
            raise AssertionError("Profile controls require the original immutable intake plan")
        for entry in json.loads(intake_bytes)["outputs"]:
            for state in ("before", "after"):
                if entry[state] is not None:
                    retain(INTAKE_CRATE / entry[state]["resource"])
        intake, _ = cls.recipe.sealed_plan(intake_path)
        migration_outputs = [entry for entry in intake["outputs"] if entry["path"] == MIGRATION]
        if len(migration_outputs) != 1:
            raise AssertionError("Original intake must declare exactly one validator output")
        historical_pom = retain(HISTORICAL_POM)
        if cls.recipe.digest(historical_pom) != HISTORICAL_POM_SHA256:
            raise AssertionError("Historical JCC POM fixture drift")
        current_pom = retain(POM)
        if cls.recipe.digest(current_pom) != CURRENT_POM_SHA256:
            raise AssertionError("Current JCC POM fixture drift")
        cls.pom_images = {"before": historical_pom, "after": historical_pom, "current": current_pom}
        cls.code_images = {}
        cls.plans = {}
        for profile in PROFILES:
            image = migration_outputs[0][profile["state"]]
            content = retain(INTAKE_CRATE / image["resource"])
            if image["sha256"] != profile["code_sha256"] or cls.recipe.digest(content) != profile["code_sha256"]:
                raise AssertionError("Retained validator image differs from its exact profile")
            cls.code_images[profile["state"]] = content
            plan_relative = cls.crate / profile["plan"]
            if cls.recipe.digest(retain(plan_relative)) != profile["plan_sha256"]:
                raise AssertionError("Profile controls require the exact raw JCC plan")
            plan, _ = cls.recipe.sealed_plan(cls.candidate / plan_relative)
            if (plan["recipe_id"], plan["plan_sha256"]) != (profile["recipe_id"], profile["seal"]):
                raise AssertionError("JCC profile identity or seal mismatch")
            cls.plans[profile["state"]] = plan
            for guard in plan["guards"]:
                if guard["path"] == MIGRATION:
                    if guard["sha256"] != profile["code_sha256"]:
                        raise AssertionError("JCC profile does not guard its selected validator")
                elif guard["path"] == POM:
                    if guard["sha256"] != HISTORICAL_POM_SHA256:
                        raise AssertionError("Historical JCC POM guard drift")
                else:
                    if cls.recipe.digest(retain(guard["path"])) != guard["sha256"]:
                        raise AssertionError("Actual shared JCC guard drift")
            for entry in plan["outputs"]:
                for state in ("before", "after"):
                    if entry[state] is not None:
                        retain(cls.crate / entry[state]["resource"])
        retain(cls.crate / "manifest.tsv")
        old, current = cls.plans["before"], cls.plans["after"]
        if old["outputs"] != current["outputs"]:
            raise AssertionError("JCC context plans changed the original four output declarations")
        old_other = [guard for guard in old["guards"] if guard["path"] != MIGRATION]
        new_other = [guard for guard in current["guards"] if guard["path"] != MIGRATION]
        if old_other != new_other or len(old_other) != 6:
            raise AssertionError("JCC context plans changed the other six guards")

        current_relative = cls.crate / CURRENT_PROFILE["plan"]
        if cls.recipe.digest(retain(current_relative)) != CURRENT_PROFILE["plan_sha256"]:
            raise AssertionError("Current JCC profile requires exact raw plan bytes")
        current_plan, _ = cls.recipe.sealed_plan(cls.candidate / current_relative)
        if (current_plan["recipe_id"], current_plan["plan_sha256"]) != (CURRENT_PROFILE["recipe_id"], CURRENT_PROFILE["seal"]):
            raise AssertionError("Current JCC profile identity or seal mismatch")
        expected = dict(current)
        expected["recipe_id"] = CURRENT_PROFILE["recipe_id"]
        expected["plan_sha256"] = CURRENT_PROFILE["seal"]
        expected["guards"] = [dict(guard, sha256=CURRENT_POM_SHA256) if guard["path"] == POM else guard
                              for guard in current["guards"]]
        if current_plan != expected:
            raise AssertionError("Current JCC plan changed more than its identity and POM guard")
        cls.plans["current"] = current_plan
        cls.code_images["current"] = cls.code_images["after"]

    @classmethod
    def tearDownClass(cls):
        for path, content in cls.frozen_inputs.items():
            if path.read_bytes() != content:
                raise AssertionError("Profile controls changed an actual input: " + str(path))

    @staticmethod
    def load(name, path):
        spec = importlib.util.spec_from_file_location(name, path)
        if spec is None or spec.loader is None:
            raise AssertionError("Cannot load profile control dependency: " + str(path))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def context(self, profile):
        temporary = tempfile.TemporaryDirectory(prefix="jcc-profile-candidate-")
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        for relative, content in self.source_bytes.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        (root / MIGRATION).write_bytes(self.code_images[profile["state"]])
        (root / POM).write_bytes(self.pom_images[profile["state"]])
        return root

    def exercise_setup(self, root, profile, refusal=None):
        # A local subclass avoids sharing class resources or exposing six inherited
        # methods to unittest discovery a second time.
        isolated = type("IsolatedJccProfile", (self.base,), {"__module__": __name__})
        before = self.base.snapshot(root)
        previous_cache_flag = sys.dont_write_bytecode
        successful = False
        try:
            with patch.dict(os.environ, {"M3_JCC_CANDIDATE_ROOT": str(root)}):
                if refusal is not None:
                    exception, message = refusal
                    with self.assertRaisesRegex(exception, message):
                        isolated.setUpClass()
                else:
                    isolated.setUpClass()
                    successful = True
                    self.assertEqual(profile["recipe_id"], isolated.plan["recipe_id"])
                    self.assertEqual(profile["seal"], isolated.plan["plan_sha256"])
                    selected = root / self.crate / profile["plan"]
                    self.assertEqual(self.source_bytes[str(self.crate / profile["plan"])],
                                     isolated.frozen_inputs[selected])
                    self.assertEqual(self.code_images[profile["state"]], isolated.guard_images[MIGRATION])
                    self.assertEqual(self.pom_images[profile["state"]], isolated.guard_images[POM])
                    self.assertEqual(4, len(isolated.rows))
                    self.assertEqual(7, len(isolated.guard_images))
        finally:
            try:
                if successful:
                    isolated.tearDownClass()
            finally:
                isolated.doClassCleanups()
                sys.dont_write_bytecode = previous_cache_flag
                self.assertEqual(before, self.base.snapshot(root), "Setup must leave candidate bytes and modes exact")

    def test_both_exact_profiles_select_their_sealed_plan_without_writes(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                self.exercise_setup(self.context(profile), profile)

    def test_unknown_validator_refuses_without_fallback(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                root = self.context(profile)
                path = root / MIGRATION
                path.write_bytes(path.read_bytes() + b"\n# unknown validator context\n")
                self.exercise_setup(root, profile, (AssertionError, "Unknown actual migration guard context"))

    def test_missing_selected_plan_refuses_with_other_profile_still_present(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                root = self.context(profile)
                (root / self.crate / profile["plan"]).unlink()
                alternate = next(other for other in PROFILES if other != profile)
                self.assertTrue((root / self.crate / alternate["plan"]).is_file())
                self.exercise_setup(root, profile, (FileNotFoundError, profile["plan"].replace(".", r"\.")))

    def test_swapped_profile_plan_refuses(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                root = self.context(profile)
                alternate = next(other for other in PROFILES if other != profile)
                (root / self.crate / profile["plan"]).write_bytes(
                    self.source_bytes[str(self.crate / alternate["plan"])])
                self.exercise_setup(root, profile, (AssertionError, "Actual packet context plan bytes drift"))

    def test_whitespace_only_plan_drift_refuses(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                root = self.context(profile)
                path = root / self.crate / profile["plan"]
                path.write_bytes(path.read_bytes() + b"\n")
                # The original semantic seal still validates; raw custody must not.
                self.recipe.sealed_plan(path)
                self.exercise_setup(root, profile, (AssertionError, "Actual packet context plan bytes drift"))

    def test_resealed_validator_guard_substitution_refuses(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                root = self.context(profile)
                path = root / self.crate / profile["plan"]
                plan = json.loads(path.read_bytes())
                alternate = next(other for other in PROFILES if other != profile)
                next(guard for guard in plan["guards"] if guard["path"] == MIGRATION)["sha256"] = alternate["code_sha256"]
                body = {key: value for key, value in plan.items() if key != "plan_sha256"}
                plan["plan_sha256"] = self.recipe.digest(self.recipe.canonical(body))
                path.write_bytes(self.recipe.canonical(plan))
                self.recipe.sealed_plan(path)
                self.exercise_setup(root, profile, (AssertionError, "Actual packet context plan bytes drift"))

    def test_resealed_recipe_identity_substitution_refuses(self):
        for profile in PROFILES:
            with self.subTest(state=profile["state"]):
                root = self.context(profile)
                path = root / self.crate / profile["plan"]
                plan = json.loads(path.read_bytes())
                plan["recipe_id"] = "jcc-handoff-20261005/unqualified"
                body = {key: value for key, value in plan.items() if key != "plan_sha256"}
                plan["plan_sha256"] = self.recipe.digest(self.recipe.canonical(body))
                path.write_bytes(self.recipe.canonical(plan))
                self.recipe.sealed_plan(path)
                self.exercise_setup(root, profile, (AssertionError, "Actual packet context plan bytes drift"))

    def test_each_other_guard_drift_refuses_during_actual_setup(self):
        for profile in PROFILES:
            for guard in self.plans[profile["state"]]["guards"]:
                if guard["path"] == MIGRATION:
                    continue
                with self.subTest(state=profile["state"], guard=guard["path"]):
                    root = self.context(profile)
                    path = root / guard["path"]
                    path.write_bytes(path.read_bytes() + b"\n# foreign dependency edit\n")
                    self.exercise_setup(root, profile, (AssertionError, "Actual candidate guard drift"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
