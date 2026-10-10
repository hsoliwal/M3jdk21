#!/usr/bin/env python3
"""Replay test for the Synexia phrase receiver boundary."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

_HERE = Path(__file__).resolve()
sys.path.insert(0, str(_HERE.parent))

from check_synexia_phrase_proof_receipt import inspect_source, receipt_line  # noqa: E402


class SynexiaPhraseProofReceiptTest(unittest.TestCase):
    def test_source_boundary_is_complete(self) -> None:
        checks = inspect_source(_HERE.parents[2])
        self.assertTrue(all(check.passed for check in checks), receipt_line(checks))
        self.assertTrue(
            receipt_line(checks).startswith(
                "M3_PHRASE_PROOF_RECEIPT_SOURCE_PASS checks="
            )
        )


if __name__ == "__main__":
    unittest.main()
