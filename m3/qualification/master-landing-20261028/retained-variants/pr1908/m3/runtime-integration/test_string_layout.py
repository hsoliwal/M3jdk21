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


    def check_owner_scope(self, source, overrides=None):
        # Execute the production parser call and retained-owner scan, not a copied predicate.
        tree = ast.parse((BASE / "check-m3string-invariants.py").read_text(encoding="utf-8"))
        layout = next(n for n in tree.body if isinstance(n, ast.FunctionDef)
                      and n.name == "check_m3string_layout")
        call = next(n for n in tree.body if isinstance(n, ast.Expr)
                    and isinstance(n.value, ast.Call) and isinstance(n.value.func, ast.Name)
                    and n.value.func.id == "check_m3string_layout")
        scan = next(n for n in tree.body if isinstance(n, ast.For)
                    and any(isinstance(v, ast.Constant) and v.value == "M3StringOwner.java"
                            for v in ast.walk(n.iter)))
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            target = root / "src/java.base/share/classes/java/lang/M3String.java"
            target.parent.mkdir(parents=True)
            target.write_text(source, encoding="utf-8")
            checker = root / "m3/runtime-integration/src/main/java/M3StringLayout.java"
            checker.parent.mkdir(parents=True)
            checker.write_bytes((BASE / "src/main/java/M3StringLayout.java").read_bytes())
            def fail(message):
                raise SystemExit(message)
            scope = dict(Path=Path, ROOT=root, os=os, subprocess=subprocess, fail=fail,
                         re=__import__("re"), m3=source)
            for key, name in [("owner", "M3StringOwner"), ("atom", "M3StringAtom"),
                              ("tuple_", "M3StringTuple"), ("facts", "M3StringFacts")]:
                scope[key] = (ROOT / ("src/java.base/share/classes/java/lang/" + name + ".java")).read_text()
            scope.update(overrides or {})
            exec(compile(ast.Module(body=[layout, call, scan], type_ignores=[]),
                         "production-owner-layout", "exec"), scope)

    def test_current_nested_snapshot_is_not_retained_string_payload(self):
        try:
            self.check_owner_scope((ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text())
        except SystemExit as error:
            self.fail(str(error))

    def test_nested_and_code_looking_arrays_are_not_direct_members(self):
        source = ('package java.lang; final class M3String {\n'
                  'private final M3StringOwner owner; private final long value;\n'
                  'static final class TokenSnapshot {\n final byte[] packedAscii;\n }\n'
                  'static final String CODE = """\n final char[] fake;\n """;\n'
                  'void inspect() { class Local {\n final int[] offsets;\n } }\n}')
        try:
            self.check_owner_scope(source)
        except SystemExit as error:
            self.fail(str(error))

    def test_direct_array_and_nonarray_fields_still_refuse(self):
        for field in ["private final byte[] payload;", "private final byte payload[];",
                      "private final Object payload;"]:
            source = ('package java.lang; final class M3String {\n'
                      'private final M3StringOwner owner; private final long value;\n' + field + '\n}')
            with self.subTest(field=field), self.assertRaisesRegex(SystemExit, "M3String layout check failed"):
                self.check_owner_scope(source)

    def test_other_owner_instance_array_refusals_are_preserved(self):
        source = ('package java.lang; final class M3String {\n'
                  'private final M3StringOwner owner; private final long value;\n}')
        for key in ["owner", "atom", "tuple_", "facts"]:
            with self.subTest(owner=key), self.assertRaisesRegex(SystemExit, "retains array field"):
                self.check_owner_scope(source, {key: "final byte[] payload;\n"})

    def test_owner_scan_cannot_bypass_missing_compiler(self):
        with patch.object(subprocess, "run", side_effect=FileNotFoundError("no compiler")):
            with self.assertRaisesRegex(SystemExit, "compiler unavailable"):
                self.check_owner_scope("package java.lang; final class M3String {}")


if __name__ == "__main__":
    unittest.main(verbosity=2)
