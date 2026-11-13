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
            VERIFY.reconstruct_source_chain(current + b"\n// mutated\n", VERIFY.read(VERIFY.CURRENT_PATCH), patch)

    def test_patch_mutation_rejected(self):
        current = VERIFY.read(VERIFY.ROOT / VERIFY.SOURCE)
        patch = VERIFY.read(FILE.parent / "jdk-8309665.patch.txt")
        with self.assertRaisesRegex(ValueError, "identity drift"):
            VERIFY.reconstruct_source_chain(current, VERIFY.read(VERIFY.CURRENT_PATCH), patch + b"\n# mutated\n")

    def test_live_successor_patch_mutation_rejected(self):
        with self.assertRaisesRegex(ValueError, "identity drift"):
            VERIFY.reconstruct_source_chain(
                VERIFY.read(VERIFY.ROOT / VERIFY.SOURCE),
                VERIFY.read(VERIFY.CURRENT_PATCH) + b"\n# mutated\n",
                VERIFY.read(FILE.parent / "jdk-8309665.patch.txt"))

    def test_original_and_intermediate_source_pins_survive_live_successor(self):
        historical, original = VERIFY.reconstruct_source_chain(
            VERIFY.read(VERIFY.ROOT / VERIFY.SOURCE), VERIFY.read(VERIFY.CURRENT_PATCH),
            VERIFY.read(FILE.parent / "jdk-8309665.patch.txt"))
        self.assertEqual(VERIFY.git_blob(historical), VERIFY.NEW_BLOB)
        self.assertEqual(VERIFY.git_blob(original), VERIFY.OLD_BLOB)
        self.assertNotEqual(historical, original)


if __name__ == "__main__":
    unittest.main()
