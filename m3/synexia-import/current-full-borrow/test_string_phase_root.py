#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Regression proof: the Phase-1 String custody checker resolves the checkout root."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().with_name("check_string_phase.py")
REPO = SCRIPT.parents[3]
TARGET = "src/java.base/share/classes/java/lang/M3String.java"


class StringPhaseRootTest(unittest.TestCase):
    def test_repository_root_is_correct(self) -> None:
        spec = importlib.util.spec_from_file_location("m3_phase_check", SCRIPT)
        self.assertIsNotNone(spec)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        self.assertEqual(REPO, module.ROOT)
        self.assertTrue((module.ROOT / TARGET).is_file())

    def test_m3_directory_cannot_masquerade_as_repository(self) -> None:
        self.assertFalse((SCRIPT.parents[2] / TARGET).is_file())
        self.assertTrue((SCRIPT.parents[3] / TARGET).is_file())

    def test_git_blob_integrity_remains_authoritative(self) -> None:
        spec = importlib.util.spec_from_file_location("m3_phase_check", SCRIPT)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        blob = module.git_blob(REPO / TARGET)
        self.assertRegex(blob, r"^[a-f0-9]{40}$")
        with tempfile.TemporaryDirectory() as temporary:
            changed = Path(temporary) / "tampered.java"
            changed.write_bytes((REPO / TARGET).read_bytes() + b"\n")
            self.assertNotEqual(blob, module.git_blob(changed))


if __name__ == "__main__":
    unittest.main()
