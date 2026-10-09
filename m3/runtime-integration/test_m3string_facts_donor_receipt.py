#!/usr/bin/env python3
"""Replay test for the M3JDK fact-owner donor receipt."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

_HERE = Path(__file__).resolve()
sys.path.insert(0, str(_HERE.parent))

from check_m3string_facts_donor_receipt import inspect_source, receipt_line  # noqa: E402


class M3StringFactsDonorReceiptTest(unittest.TestCase):
    def test_receipt_and_target_owner_are_complete(self) -> None:
        root = _HERE.parents[2]
        checks = inspect_source(root)
        self.assertEqual(42, len(checks))
        self.assertTrue(all(check.passed for check in checks), receipt_line(checks))
        self.assertEqual(
            "M3_STRING_FACTS_DONOR_RECEIPT_SOURCE_PASS checks=42/42",
            receipt_line(checks),
        )


if __name__ == "__main__":
    unittest.main()
