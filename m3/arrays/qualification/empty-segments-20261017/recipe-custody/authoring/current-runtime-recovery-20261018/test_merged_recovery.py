#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Focused refusal and prefix-preservation checks for the restoration authorer."""

import unittest

from prepare_merged_recovery import append_missing_rows, restore_exact, sha, verify_chain


def part(before, after):
    return {"before": "ABSENT" if before is None else sha(before), "after": sha(after),
            "preimage": before, "postimage": after}


class MergedRecoveryPreparationTest(unittest.TestCase):
    def test_exact_serial_body_atoms(self):
        chain = [part(b"original", b"documented"), part(b"documented", b"annotated")]
        self.assertEqual(b"annotated", restore_exact(b"original", chain))

    def test_absent_original_addition(self):
        self.assertEqual(b"addition", restore_exact(None, [part(None, b"addition")]))

    def test_unknown_source_drift_is_refused(self):
        with self.assertRaises(ValueError):
            restore_exact(b"current unrelated edit", [part(b"original", b"refined")])

    def test_noncontiguous_source_atoms_are_refused(self):
        with self.assertRaises(ValueError):
            verify_chain([part(b"original", b"first"), part(b"unrelated", b"second")])

    def test_corrupt_original_payload_is_refused(self):
        broken = part(b"original", b"refined")
        broken["postimage"] = b"tampered"
        with self.assertRaises(ValueError):
            verify_chain([broken])

    def test_current_catalogue_prefix_and_numeric_row_survive(self):
        base = b"id\tcategory\nbase\towner\n"
        current = base + b"ALG-NUMERIC-COPY-RUNS\tcurrent\n"
        original = base + b"runtime-a\tfirst\nruntime-b\tsecond\n"
        actual, count = append_missing_rows(current, base, original)
        self.assertEqual(current + original[len(base):], actual)
        self.assertTrue(actual.startswith(current))
        self.assertEqual(2, count)

    def test_identical_row_is_not_duplicated(self):
        base = b"id\tcategory\n"
        full = base + b"runtime\towner\n"
        self.assertEqual((full, 0), append_missing_rows(full, base, full))

    def test_conflicting_catalogue_owner_is_refused(self):
        base = b"id\tcategory\n"
        with self.assertRaises(ValueError):
            append_missing_rows(base + b"runtime\tnew owner\n", base, base + b"runtime\told owner\n")

    def test_changed_or_duplicate_current_prefix_is_refused(self):
        base = b"id\tcategory\nbase\towner\n"
        for current in (b"changed\n", base + b"base\tduplicate\n"):
            with self.subTest(current=current), self.assertRaises(ValueError):
                append_missing_rows(current, base, base + b"runtime\towner\n")


if __name__ == "__main__":
    unittest.main()
