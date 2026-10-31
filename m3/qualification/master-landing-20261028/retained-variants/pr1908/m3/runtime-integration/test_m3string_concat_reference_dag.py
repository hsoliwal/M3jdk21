#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Source-gate refusal regressions; actual concat behavior is tested by jtreg."""
import contextlib
import io
from pathlib import Path
import runpy
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
GATE = Path(__file__).with_name("check-m3string-concat-reference-dag.py")
FACTORY = ROOT / "src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java"
HELPER = ROOT / "src/java.base/share/classes/java/lang/StringConcatHelper.java"

class ConcatReferenceGateTest(unittest.TestCase):
    def run_gate(self, source=None, old=None, new=None):
        read = Path.read_text
        def contents(path, *args, **kwargs):
            value = read(path, *args, **kwargs)
            if path == source:
                self.assertIn(old, value)
                value = value.replace(old, new, 1)
            return value
        with patch.object(Path, "read_text", contents), contextlib.redirect_stdout(io.StringIO()) as out:
            runpy.run_path(str(GATE), run_name="__main__")
        return out.getvalue()

    def refuse(self, source, old, new, reason):
        with self.assertRaisesRegex(SystemExit, reason):
            self.run_gate(source, old, new)

    def test_current_source_passes(self):
        self.assertIn("M3_STRING_CONCAT_REFERENCE_DAG_PASS", self.run_gate())

    def test_original_nullable_string_bypass_refused(self):
        self.refuse(FACTORY, "private static MethodHandle m3Stringifier(Class<?> type) {",
                    "private static MethodHandle m3Stringifier(Class<?> type) {\n        if (type == String.class) return null;",
                    "nullable_reference_bypass")

    def test_missing_reference_gateway_refused_in_own_method(self):
        self.refuse(FACTORY, "return objectStringifier();\n    }",
                    "return null;\n    }", "nullable_reference_bypass|reference_gateway")

    def test_primitive_gateway_refused(self):
        self.refuse(FACTORY, "return stringValueOf(int.class).asType(methodType(String.class, type));",
                    "return objectStringifier();", "stringifier")

    def test_null_constant_marker_preserved(self):
        self.refuse(HELPER, "if (piece == null || piece.isEmpty()) return result;",
                    "if (piece.isEmpty()) return result;", "m3Append")

    def test_null_returning_to_string_preserved(self):
        self.refuse(HELPER, '(value == null || (s = value.toString()) == null) ? "null" : s',
                    '(value == null) ? "null" : value.toString()', "stringOf null/toString")

    def test_argument_append_order_preserved(self):
        self.refuse(HELPER, "result = m3Append(result, args[index]);",
                    "result = m3Append(result, constants[index]);", "missing_or_out_of_order")

if __name__ == "__main__":
    unittest.main()
