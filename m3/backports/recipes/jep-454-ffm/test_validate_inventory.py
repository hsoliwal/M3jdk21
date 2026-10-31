#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import importlib.util
import shutil
import tempfile
import unittest
from pathlib import Path


HERE = Path(__file__).resolve().parent


def load_validator(path: Path):
    spec = importlib.util.spec_from_file_location("m3_jep454_validator", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Jep454InventoryTest(unittest.TestCase):
    def copy_packet(self, root: Path) -> Path:
        target = root / "jep-454-ffm"
        shutil.copytree(HERE, target)
        return target

    def test_current_packet_is_fail_closed_and_valid(self) -> None:
        validator = load_validator(HERE / "validate_inventory.py")
        self.assertEqual(0, validator.main())

    def test_path_denominator_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            packet = self.copy_packet(Path(tmp))
            lines = (packet / "PATH_PLANE.tsv").read_text(encoding="utf-8").splitlines()
            (packet / "PATH_PLANE.tsv").write_text(
                "\n".join(lines[:-1]) + "\n", encoding="utf-8"
            )
            validator = load_validator(packet / "validate_inventory.py")
            with self.assertRaises(SystemExit):
                validator.main()

    def test_api_delta_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            packet = self.copy_packet(Path(tmp))
            lines = (packet / "API_DELTA.tsv").read_text(encoding="utf-8").splitlines()
            (packet / "API_DELTA.tsv").write_text(
                "\n".join(lines[:-1]) + "\n", encoding="utf-8"
            )
            validator = load_validator(packet / "validate_inventory.py")
            with self.assertRaises(SystemExit):
                validator.main()

    def test_authority_cannot_be_widened_silently(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            packet = self.copy_packet(Path(tmp))
            proof = (packet / "PROOF_REQUEST.tsv").read_text(encoding="utf-8")
            proof = proof.replace("promotion\tNOT_AUTHORIZED", "promotion\tAUTHORIZED")
            (packet / "PROOF_REQUEST.tsv").write_text(proof, encoding="utf-8")
            validator = load_validator(packet / "validate_inventory.py")
            with self.assertRaises(SystemExit):
                validator.main()


if __name__ == "__main__":
    unittest.main()
