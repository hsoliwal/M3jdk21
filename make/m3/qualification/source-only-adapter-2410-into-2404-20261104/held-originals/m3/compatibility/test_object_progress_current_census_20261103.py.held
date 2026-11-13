#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Negative and exact-source checks for observed-only M3 adapter census."""
from __future__ import annotations

import contextlib
import hashlib
import io
import sys
from pathlib import Path
import tempfile
import types
import unittest

# The full M3JDK21 checkout supplies the real historical checker. The local
# source-only unit-test harness may lack that file; it never tests release proof.
HERE = Path(__file__).resolve().parent
if not (HERE / "check_object_adapter_progress_receiver_20261017.py").exists():
    sys.modules["check_object_adapter_progress_receiver_20261017"] = types.ModuleType(
        "check_object_adapter_progress_receiver_20261017")
sys.path.insert(0, str(HERE))
import check_object_progress_current_census_20261103 as census  # noqa: E402


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


class CurrentCensusTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = (HERE / "m3-object-progress-current-census-20261103.tsv").read_bytes()
        cls.rows = census.parse_rows(cls.data, census.CENSUS_SHA256)

    def test_exact_current_census_has_closed_sorted_scope(self):
        self.assertEqual(census.ROWS, len(self.rows))
        self.assertEqual(sorted(r.path for r in self.rows), [r.path for r in self.rows])
        self.assertEqual(census.DIVERGENCES,
                         sum(r.historical_sha256 != r.observed_sha256 for r in self.rows))
        self.assertEqual(census.ROWS - census.DIVERGENCES,
                         sum(r.historical_sha256 == r.observed_sha256 for r in self.rows))

    def test_one_byte_mutation_refuses_without_resealing(self):
        bad = self.data.replace(b"# path", b"# PATH", 1)
        with self.assertRaisesRegex(RuntimeError, "SHA-256 drift"):
            census.parse_rows(bad, census.CENSUS_SHA256)

    def test_resealed_out_of_order_duplicate_and_missing_row_refuse(self):
        lines = self.data.decode().splitlines()
        variants = [
            [lines[0], lines[2], lines[1], *lines[3:]],
            [*lines[:3], lines[2], *lines[4:]],
            lines[:-1],
        ]
        for bad in variants:
            with self.subTest(first=bad[:3]), self.assertRaisesRegex(
                    RuntimeError, "ordering|row coverage"):
                changed = ("\n".join(bad) + "\n").encode()
                census.parse_rows(changed, sha(changed))

    def test_resealed_unsafe_path_refuses(self):
        lines = self.data.decode().splitlines()
        lines[1] = lines[1].replace(
            "src/java.base/share/classes/java/lang/Object.java",
            "src/java.base/share/classes/java/lang/../Object.java", 1)
        changed = ("\n".join(lines) + "\n").encode()
        with self.assertRaisesRegex(RuntimeError, "invalid source census"):
            census.parse_rows(changed, sha(changed))

    def synthetic(self):
        bytes_by_path = {}
        expected = {}
        fake_rows = []
        for i, entry in enumerate(self.rows):
            source = ("current-" + str(i)).encode()
            current = sha(source)
            old = current if i < census.ROWS - census.DIVERGENCES else sha(
                ("previous-" + str(i)).encode())
            bytes_by_path[entry.path] = source
            expected[entry.path] = old
            fake_rows.append(census.CensusRow(entry.path, old, current,
                                             entry.observed_git_blob_sha1))
        return tuple(fake_rows), expected, bytes_by_path

    def test_synthetic_current_sources_are_observed_not_admitted(self):
        rows, expected, sources = self.synthetic()
        same, different, paths = census.audit(rows, expected, sources.__getitem__)
        self.assertEqual((6, 8), (same, different))
        self.assertEqual(8, len(paths))

    def test_changed_target_refuses_without_changing_historical_expectation(self):
        rows, expected, sources = self.synthetic()
        sources[rows[4].path] += b"changed"
        with self.assertRaisesRegex(RuntimeError, "observed source drift"):
            census.audit(rows, expected, sources.__getitem__)

    def test_wrong_historical_transition_refuses(self):
        rows, expected, sources = self.synthetic()
        expected[rows[1].path] = "f" * 64
        with self.assertRaisesRegex(RuntimeError, "historical source transition drift"):
            census.audit(rows, expected, sources.__getitem__)

    def test_real_checkout_uses_original_historical_composer(self):
        real = HERE / "check_object_adapter_progress_receiver_20261017.py"
        if not real.exists():
            self.skipTest("full receiver repository not mounted in isolated test")
        root = HERE.parents[1]
        with contextlib.redirect_stdout(io.StringIO()) as stdout:
            self.assertEqual(census.ROWS, census.verify(root))
        self.assertIn("OBSERVED_ONLY 14 exact current sources", stdout.getvalue())
        self.assertNotIn("PASS historical", stdout.getvalue())


if __name__ == "__main__":
    unittest.main()
