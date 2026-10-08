#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compile the source gate and exercise its real Java parser plus Python failure boundary."""
import ast
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "m3/runtime-integration"


def tool(name):
    return str(Path(os.environ["JAVA_HOME"]) / "bin" / name) if os.environ.get("JAVA_HOME") else name


class StringLayoutTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scratch = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.scratch.cleanup)
        cls.classes = cls.scratch.name
        result = subprocess.run([
            tool("javac"), "--release", "21", "-Xlint:all", "-Werror", "-d", cls.classes,
            str(BASE / "src/main/java/M3StringLayout.java"),
            str(BASE / "src/test/java/M3StringLayoutTest.java"),
        ], capture_output=True, text=True, timeout=45)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)
        tree = ast.parse((BASE / "check-m3string-invariants.py").read_text(encoding="utf-8"))
        function = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == "check_m3string_layout")
        def fail(message):
            raise SystemExit(message)
        scope = dict(Path=Path, ROOT=ROOT, os=os, subprocess=subprocess, fail=fail)
        exec(compile(ast.Module(body=[function], type_ignores=[]), "layout-boundary", "exec"), scope)
        cls.boundary = staticmethod(scope["check_m3string_layout"])

    def test_parser_corpus_and_current_target(self):
        fixture = Path(os.environ.get("M3_STRING_FIXTURE", ROOT / "src/java.base/share/classes/java/lang/M3String.java"))
        result = subprocess.run([tool("java"), "-cp", self.classes, "M3StringLayoutTest", str(fixture)],
                                capture_output=True, text=True, timeout=45)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("M3_STRING_LAYOUT_TEST_OK accepted=55 refused=38", result.stdout)
        print(result.stdout.strip())

    def test_real_source_launcher_boundary(self):
        self.boundary()

    def test_subprocess_refusals(self):
        for error in [FileNotFoundError("no Java"), subprocess.TimeoutExpired("java", 45)]:
            with self.subTest(error=error), patch.object(subprocess, "run", side_effect=error):
                with self.assertRaises(SystemExit):
                    self.boundary()
        for code, output in [(1, ""), (0, ""), (0, "M3_STRING_LAYOUT_OK\nextra")]:
            with self.subTest(code=code, output=output), patch.object(subprocess, "run", return_value=
                    subprocess.CompletedProcess([], code, stdout=output, stderr="failure")):
                with self.assertRaises(SystemExit):
                    self.boundary()

    def test_input_io_refusals(self):
        missing = Path(self.classes) / "missing.java"
        invalid = Path(self.classes) / "invalid.java"
        invalid.write_bytes(b"\xff")
        huge = Path(self.classes) / "huge.java"
        huge.write_bytes(b" " * (4 * 1024 * 1024 + 1))
        for path in [missing, invalid, huge]:
            with self.subTest(path=path.name):
                result = subprocess.run([tool("java"), "-cp", self.classes, "M3StringLayout", str(path)],
                                        capture_output=True, text=True, timeout=45)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("M3_STRING_LAYOUT_FAIL", result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
