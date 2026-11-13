# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verifier orchestration regressions; synthetic processes never qualify a JDK."""

import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET


HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_regex_product_verify", HERE / "verify.py")
verifier = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(verifier)


class ProductVerifierTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="regex-verifier-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "source"
        self.home = Path(self.temp.name) / "image"
        self.output = Path(self.temp.name) / "proof"
        self.calls = []
        for name in verifier.SOURCE_INPUTS:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("SYNTHETIC_ORCHESTRATION_FIXTURE\n", encoding="utf-8")
        suffix = ".exe" if os.name == "nt" else ""
        names = ["release", "bin/java" + suffix, "bin/javac" + suffix, "lib/modules"]
        names += (["bin/java.dll", "bin/server/jvm.dll"] if os.name == "nt" else
                  ["lib/libjava.dylib", "lib/server/libjvm.dylib"] if sys.platform == "darwin" else
                  ["lib/libjava.so", "lib/server/libjvm.so"])
        for name in names:
            path = self.home / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("SYNTHETIC_ORCHESTRATION_FIXTURE\n", encoding="utf-8")

    def process(self, command, **kwargs):
        self.calls.append((command, kwargs))
        if "-XshowSettings:properties" in command:
            text = "    java.specification.version = 21\n"
        elif "-version" in command:
            text = "javac 21.0.13\n"
        elif "-d" in command:
            classes = Path(command[command.index("-d") + 1])
            (classes / "M3StringRegexPrecomputeTest.class").write_bytes(b"synthetic-class")
            text = ""
        else:
            text = "M3_REGEX_PRODUCT_TEST_PASS|mode=" + command[-1] + "\n"
        kwargs["stdout"].write(text)
        return subprocess.CompletedProcess(command, 0)

    def run_fixture(self, effect=None):
        with patch.object(verifier.subprocess, "run", side_effect=effect or self.process), \
                contextlib.redirect_stdout(io.StringIO()):
            return verifier.verify(self.root, self.home, self.output)

    def receipt(self):
        return json.loads((self.output / "receipt.json").read_text(encoding="utf-8"))

    def test_preserves_all_product_modes_and_native_checks(self):
        receipt = self.run_fixture()
        self.assertEqual(receipt["status"], "PASSED")
        self.assertEqual(receipt["jtreg_harness"], "NOT_RUN")
        self.assertEqual(receipt["source_image_correspondence"], "REQUIRES_SEPARATE_EXACT_BUILD_RECEIPT")
        commands = [command for command, _ in self.calls][-3:]
        self.assertEqual([command[-1] for command in commands], ["required", "required", "off"])
        self.assertIn("-XX:-CompactStrings", commands[1])
        self.assertIn("-XX:-UseM3StringStorage", commands[2])
        for command in commands:
            self.assertIn("-Xcheck:jni", command)
            self.assertIn("--add-opens=java.base/java.lang=ALL-UNNAMED", command)
            self.assertNotIn("--patch-module", " ".join(command))
        self.assertFalse(receipt["receiving_phase_advanced"])

    def test_unrecognized_vm_flag_is_failure_not_skipped(self):
        def effect(command, **kwargs):
            if "-XX:+UseM3StringStorage" in command:
                kwargs["stdout"].write("Unrecognized VM option 'UseM3StringStorage'\n")
                return subprocess.CompletedProcess(command, 1)
            return self.process(command, **kwargs)
        with self.assertRaisesRegex(RuntimeError, "storage failed with exit 1"):
            self.run_fixture(effect)
        self.assertEqual(self.receipt()["status"], "FAILED")
        self.assertEqual(self.receipt()["modes"], {})

    def test_native_assertion_failure_propagates(self):
        def effect(command, **kwargs):
            if command[-1] == "required":
                kwargs["stdout"].write("AssertionError: required libjava native route missing\n")
                return subprocess.CompletedProcess(command, 1)
            return self.process(command, **kwargs)
        with self.assertRaises(RuntimeError):
            self.run_fixture(effect)
        self.assertEqual(self.receipt()["status"], "FAILED")

    def test_zero_exit_without_exact_marker_fails(self):
        def effect(command, **kwargs):
            if command[-1] == "required":
                kwargs["stdout"].write("prefix M3_REGEX_PRODUCT_TEST_PASS|mode=required suffix\n")
                return subprocess.CompletedProcess(command, 0)
            return self.process(command, **kwargs)
        with self.assertRaisesRegex(RuntimeError, "missing exact product success marker"):
            self.run_fixture(effect)

    def test_missing_source_invalidates_previous_pass(self):
        self.output.mkdir()
        (self.output / "receipt.json").write_text('{"status":"PASSED"}', encoding="utf-8")
        (self.root / verifier.TEST).unlink()
        with self.assertRaisesRegex(ValueError, "missing or linked product source"):
            self.run_fixture()
        self.assertEqual(self.receipt()["status"], "FAILED")
        self.assertEqual(self.receipt()["commands"], [])

    def test_missing_image_is_explicit_failure(self):
        (self.home / "lib/modules").unlink()
        with self.assertRaisesRegex(ValueError, "complete source-built"):
            self.run_fixture()
        self.assertEqual(self.receipt()["status"], "FAILED")

    def test_source_mutation_fails(self):
        def effect(command, **kwargs):
            result = self.process(command, **kwargs)
            if command[-1] == "off":
                (self.root / verifier.TEST).write_text("changed", encoding="utf-8")
            return result
        with self.assertRaisesRegex(RuntimeError, "product inputs changed"):
            self.run_fixture(effect)

    def test_image_mutation_fails(self):
        def effect(command, **kwargs):
            result = self.process(command, **kwargs)
            if command[-1] == "off":
                (self.home / "lib/modules").write_text("changed", encoding="utf-8")
            return result
        with self.assertRaisesRegex(RuntimeError, "product image changed"):
            self.run_fixture(effect)

    def test_compiled_test_mutation_fails(self):
        def effect(command, **kwargs):
            result = self.process(command, **kwargs)
            if command[-1] == "off":
                classes = Path(command[command.index("-cp") + 1])
                (classes / "M3StringRegexPrecomputeTest.class").write_bytes(b"changed")
            return result
        with self.assertRaisesRegex(RuntimeError, "compiled product test classes changed"):
            self.run_fixture(effect)

    def test_no_compiled_classes_fails(self):
        def effect(command, **kwargs):
            if "-d" in command:
                return subprocess.CompletedProcess(command, 0)
            return self.process(command, **kwargs)
        with self.assertRaisesRegex(RuntimeError, "no product test classes"):
            self.run_fixture(effect)

    def test_wrong_runtime_major_fails(self):
        def effect(command, **kwargs):
            kwargs["stdout"].write("    java.specification.version = 25\n")
            return subprocess.CompletedProcess(command, 0)
        with self.assertRaisesRegex(RuntimeError, "Java 21 runtime"):
            self.run_fixture(effect)

    def test_timeout_records_failure(self):
        def effect(command, **kwargs):
            raise subprocess.TimeoutExpired(command, kwargs["timeout"])
        with self.assertRaises(subprocess.TimeoutExpired):
            self.run_fixture(effect)
        self.assertEqual(self.receipt()["commands"][0]["state"], "TIMED_OUT")
        self.assertEqual(self.receipt()["status"], "FAILED")

    def test_launch_failure_records_failure(self):
        with self.assertRaises(OSError):
            self.run_fixture(lambda *args, **kwargs: (_ for _ in ()).throw(OSError("missing executable")))
        self.assertEqual(self.receipt()["commands"][0]["state"], "FAILED_TO_START")

    def test_environment_cannot_inject_patched_classes(self):
        injected = {name: "--patch-module=java.base=unreviewed" for name in (
            "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "JDK_JAVAC_OPTIONS")}
        with patch.dict(os.environ, injected):
            self.run_fixture()
        for _, kwargs in self.calls:
            for name in injected:
                self.assertNotIn(name, kwargs["env"])

    def test_output_inside_image_is_refused(self):
        with self.assertRaisesRegex(ValueError, "outside the JDK image"):
            verifier.verify(self.root, self.home, self.home / "proof")

    def test_maven_uses_only_target_owned_verifier_and_keeps_gate_mandatory(self):
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        pom = ET.parse(HERE / "pom.xml").getroot()
        executions = pom.findall(".//m:execution", ns)
        gate = next(item for item in executions
                    if item.findtext("m:id", namespaces=ns) == "verify-canonical-postimages-and-fixture")
        self.assertEqual(gate.findtext("m:phase", namespaces=ns), "verify")
        arguments = [item.text for item in gate.findall(".//m:argument", ns)]
        self.assertIn("${project.basedir}/verify.py", arguments)
        self.assertIn("${m3.test.jdk}", arguments)
        self.assertNotIn("--quick", arguments)
        self.assertNotIn("m3.checkout", ET.tostring(pom, encoding="unicode"))
        self.assertFalse(gate.findall(".//m:skip", ns))


if __name__ == "__main__":
    unittest.main()
