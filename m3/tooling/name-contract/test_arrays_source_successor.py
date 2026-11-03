# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed receipt and patch checks over real M3JDK21 source."""
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent
FILE = ROOT / "receipts/arrays-source-successor/verify.py"
SPEC = importlib.util.spec_from_file_location("m3_arrays_8309665", FILE)
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)


class ArraysJdk8309665SourceTest(unittest.TestCase):
    def test_historical_and_current_source_reconstruction(self):
        self.assertIn("SUCCESSOR_PASS", VERIFY.verify())

    def test_current_source_mutation_rejected(self):
        current = VERIFY.read(VERIFY.ROOT / VERIFY.SOURCE)
        patch = VERIFY.read(FILE.parent / "jdk-8309665.patch.txt")
        with self.assertRaisesRegex(ValueError, "identity drift"):
            VERIFY.reverse_patch(current + b"\n// mutated\n", patch)

    def test_patch_mutation_rejected(self):
        current = VERIFY.read(VERIFY.ROOT / VERIFY.SOURCE)
        patch = VERIFY.read(FILE.parent / "jdk-8309665.patch.txt")
        with self.assertRaisesRegex(ValueError, "identity drift"):
            VERIFY.reverse_patch(current, patch + b"\n# mutated\n")


if __name__ == "__main__":
    unittest.main()
