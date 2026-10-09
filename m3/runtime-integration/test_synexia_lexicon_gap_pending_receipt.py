#!/usr/bin/env python3
"""Replay test for unresolved Synexia lexicon families staged in M3JDK."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

_HERE = Path(__file__).resolve()
sys.path.insert(0, str(_HERE.parent))

from check_synexia_lexicon_gap_pending_receipt import inspect_source, receipt_line  # noqa: E402


class SynexiaLexiconGapPendingReceiptTest(unittest.TestCase):
    def test_gaps_remain_fail_closed(self) -> None:
        checks = inspect_source(_HERE.parents[2])
        self.assertTrue(all(check.passed for check in checks), receipt_line(checks))
        self.assertEqual(
            "M3_LEXICON_GAP_PENDING_SOURCE_PASS checks=28/28",
            receipt_line(checks),
        )


if __name__ == "__main__":
    unittest.main()
