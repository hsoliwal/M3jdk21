# SPDX-License-Identifier: Apache-2.0
"""Synthetic refusal tests for historical-build plus additive core-test wiring."""
from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("verify_test_wiring.py")
SPEC = importlib.util.spec_from_file_location("m3_test_wiring", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
wiring = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(wiring)


class CoreTestWiringTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.tests = self.root / "m3/core/test"
        self.tests.mkdir(parents=True)
        (self.tests / "AlphaTest.java").write_text("class AlphaTest {}\n")
        (self.tests / "BetaTest.java").write_text("class BetaTest {}\n")
        self.script = self.root / wiring.PRIMARY
        self.extra = self.root / wiring.SUPPLEMENTAL
        self.extra.parent.mkdir(parents=True, exist_ok=True)
        self.workflow = self.root / wiring.WORKFLOW
        self.workflow.parent.mkdir(parents=True, exist_ok=True)
        self.workflow.write_text(
            "run: bash ./make/m3/build.sh\n"
            "run: bash ./make/m3/release/run_extra_core_contracts.sh\n")

    def write_script(self, *, compile_beta=False, execute_beta=False, orphan=False):
        primary = [
            '"$M3JDK/bin/javac" -d build/tests core/test/AlphaTest.java',
            '"$M3JDK/bin/java" -cp build/tests AlphaTest',
        ]
        supplemental = []
        if compile_beta:
            supplemental.append('"$M3JDK/bin/javac" -d build/tests core/test/BetaTest.java')
        if orphan:
            supplemental.append('"$M3JDK/bin/javac" -d build/tests core/test/GhostTest.java')
        if execute_beta:
            supplemental.append(
                '"$M3JDK/bin/java" -cp build/tests:build/jdk.m3.text.jar BetaTest'
            )
        self.script.write_text("\n".join(primary) + "\n")
        self.extra.write_text("\n".join(supplemental) + "\n")

    def test_missing_class_is_a_blocker(self):
        self.write_script()
        report = wiring.audit(self.root)
        self.assertFalse(report["passed"])
        self.assertEqual(["BetaTest"], report["missing_compile"])
        self.assertEqual(["BetaTest"], report["missing_execution"])

    def test_compiling_without_running_is_a_blocker(self):
        self.write_script(compile_beta=True)
        report = wiring.audit(self.root)
        self.assertEqual([], report["missing_compile"])
        self.assertEqual(["BetaTest"], report["missing_execution"])
        self.assertFalse(report["passed"])

    def test_all_standalone_classes_are_wired(self):
        self.write_script(compile_beta=True, execute_beta=True)
        report = wiring.audit(self.root)
        self.assertTrue(report["passed"])
        self.assertEqual(2, report["discovered"])
        self.assertEqual(2, report["executed"])
        self.assertTrue(report["release_workflow_invokes_both"])

    def test_phantom_references_are_not_accepted(self):
        self.write_script(compile_beta=True, execute_beta=True, orphan=True)
        report = wiring.audit(self.root)
        self.assertFalse(report["passed"])
        self.assertEqual(["GhostTest"], report["unknown_compile"])

    def test_missing_additive_runner_is_a_blocker(self):
        self.write_script(compile_beta=True, execute_beta=True)
        self.extra.unlink()
        report = wiring.audit(self.root)
        self.assertFalse(report["passed"])
        self.assertEqual([wiring.SUPPLEMENTAL], report["missing_scripts"])

    def test_runner_not_invoked_by_workflow_is_a_blocker(self):
        self.write_script(compile_beta=True, execute_beta=True)
        self.workflow.write_text("run: bash ./make/m3/build.sh\n")
        report = wiring.audit(self.root)
        self.assertFalse(report["passed"])
        self.assertFalse(report["release_workflow_invokes_both"])


if __name__ == "__main__":
    unittest.main()
