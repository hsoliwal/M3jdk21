# SPDX-License-Identifier: Apache-2.0
"""Synthetic regression tests for the M3 core-test wiring auditor."""
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
        self.script = self.root / "m3/build.sh"

    def write_script(self, *, compile_beta=False, execute_beta=False, orphan=False):
        compiled = "core/test/AlphaTest.java"
        if compile_beta:
            compiled += " core/test/BetaTest.java"
        if orphan:
            compiled += " core/test/GhostTest.java"
        commands = [
            '"$M3JDK/bin/javac" -d build/tests ' + compiled,
            '"$M3JDK/bin/java" -cp build/tests AlphaTest',
        ]
        if execute_beta:
            commands.append('"$M3JDK/bin/java" -cp build/tests:build/jdk.m3.text.jar BetaTest')
        self.script.write_text("\n".join(commands) + "\n")

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

    def test_phantom_references_are_not_accepted(self):
        self.write_script(compile_beta=True, execute_beta=True, orphan=True)
        report = wiring.audit(self.root)
        self.assertFalse(report["passed"])
        self.assertEqual(["GhostTest"], report["unknown_compile"])


if __name__ == "__main__":
    unittest.main()
