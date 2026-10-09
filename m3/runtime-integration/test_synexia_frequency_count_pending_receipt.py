#!/usr/bin/env python3
"""Replay test for the pending Synexia frequency-count mapping."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

_HERE = Path(__file__).resolve()
sys.path.insert(0, str(_HERE.parent))

from check_synexia_frequency_count_pending_receipt import inspect_source, receipt_line  # noqa: E402


class SynexiaFrequencyCountPendingReceiptTest(unittest.TestCase):
    def test_source_mapping_is_explicitly_blocked(self) -> None:
        checks = inspect_source(_HERE.parents[2])
        self.assertTrue(all(check.passed for check in checks), receipt_line(checks))
        self.assertEqual(
            "M3_FREQUENCY_COUNT_PENDING_SOURCE_PASS checks=44/44",
            receipt_line(checks),
        )


if __name__ == "__main__":
    unittest.main()
