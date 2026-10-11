# SPDX-License-Identifier: Apache-2.0
"""Executable contract for the Synexia recipe source-custody packet."""

from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
VERIFY = ROOT / "m3/synexia-import/intakes/donor-convergence-workspace-20261011/verify_source_custody.py"


class SourceCustodyTest(unittest.TestCase):
    def test_exact_source_blobs_and_explicit_hold(self) -> None:
        result = subprocess.run(
            [sys.executable, str(VERIFY)],
            cwd=ROOT,
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("M3_SYNEXIA_RECIPE_CUSTODY_PASS files=3 known_holds=1", result.stdout)


if __name__ == "__main__":
    unittest.main()
