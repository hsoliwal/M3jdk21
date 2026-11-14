#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
"""Regression checks for the Java 21 precompute wire spelling."""
from __future__ import annotations
import importlib.util
import json
import math
import struct
import sys
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location(
    "m3_precompute_exporter", Path(__file__).resolve().parents[1] / "export-m3-lexicon.py")
EXPORT = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = EXPORT
SPEC.loader.exec_module(EXPORT)


class PrecomputeCanonicalTest(unittest.TestCase):
    def test_java21_numeric_spelling_and_signed_zero(self):
        pairs = [
            ("0.0", "0.0"), ("-0.0", "-0.0"), ("1e-5", "1.0E-5"),
            ("1e20", "1.0E20"), ("1e23", "1.0E23"),
            ("10000000.0", "1.0E7"), ("0.001", "0.001"),
            ("0.0001", "1.0E-4"), ("273.15", "273.15"),
            ("5e-324", "4.9E-324"), ("1e-323", "9.9E-324"),
            ("1.7976931348623157e308", "1.7976931348623157E308"),
        ]
        for source, expected in pairs:
            with self.subTest(source=source):
                value = EXPORT.canonical_precompute_payload('{"x":' + source + '}')
                self.assertEqual('{"x":' + expected + '}', value)
                self.assertEqual(struct.pack(">d", float(source)),
                                 struct.pack(">d", json.loads(value)["x"]))

    def test_del_does_not_rewrite_literal_escape_or_string_numbers(self):
        for text in ("\x7f", r"\u007f", "\\\x7f", "\\\\\x7f", 'a"1e-5\\',
                     "1e20 10000000.0", "\b\f\n\r\t", "\ud800", "\udc00", "\U0001f600"):
            with self.subTest(text=repr(text)):
                result = EXPORT.canonical_precompute_payload(json.dumps({"x": text}))
                self.assertEqual(text, json.loads(result)["x"])
        self.assertEqual('{"x":"\x7f"}',
                         EXPORT.canonical_precompute_payload('{"x":"\\u007f"}'))
        self.assertEqual('{"x":"\\\\u007f"}',
                         EXPORT.canonical_precompute_payload('{"x":"\\\\u007f"}'))

    def test_value_shapes_and_integer_kind_are_preserved(self):
        value = {"z": [None, True, False, -1, 9223372036854775807, {"x": 1e20}],
                 "a": "1e-5", "n": 0}
        result = EXPORT.canonical_precompute_payload(json.dumps(value))
        self.assertEqual(value, json.loads(result))
        self.assertIs(type(json.loads(result)["n"]), int)
        self.assertTrue(result.startswith('{"a":'))

    def test_existing_invalid_inputs_still_fail(self):
        for text in ("[]", '{"x":1,"x":2}', '{"x":NaN}', '{"x":Infinity}',
                     '{"x":1e999}', '{"x":}', "{"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                EXPORT.canonical_precompute_payload(text)

    def test_canonicalization_is_idempotent(self):
        for text in ('{"x":1e-5}', '{"x":5e-324}', '{"x":-0.0}',
                     '{"x":"\\u007f"}', '{"x":"\\\\u007f"}',
                     '{"z":[1e20,{"x":1e-5}],"a":false}'):
            result = EXPORT.canonical_precompute_payload(text)
            self.assertEqual(result, EXPORT.canonical_precompute_payload(result))

    def test_payload_budget_is_enforced_before_and_after_rendering(self):
        with self.assertRaises(ValueError):
            EXPORT.canonical_precompute_payload('{"x":"' + "a" * 1_048_576 + '"}')
        # Input and Python JSON spelling fit; Java's spelling exceeds the limit.
        with self.assertRaisesRegex(ValueError, "canonical precompute payload"):
            EXPORT.canonical_precompute_payload('{"x":[' + ",".join(["1e-5"] * 150_000) + ']}')

    def test_existing_donor_type_acceptance(self):
        self.assertTrue(EXPORT._fits_donor_type(273.15, "double"))
        self.assertTrue(EXPORT._fits_donor_type("m3-revision-7", "String"))
        self.assertFalse(EXPORT._fits_donor_type(float("inf"), "double"))
        self.assertFalse(EXPORT._fits_donor_type(True, "double"))
        self.assertFalse(EXPORT._fits_donor_type(7, "String"))


if __name__ == "__main__":
    unittest.main()
