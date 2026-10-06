# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import shutil
import tempfile
import unittest
from pathlib import Path

import check_synexia_recipe_mirrors as mirrors


class SynexiaRecipeMirrorPolicyTest(unittest.TestCase):
    def test_checked_in_mirrors_are_exact(self) -> None:
        root = Path(__file__).resolve().parents[2]
        self.assertEqual(7, mirrors.validate(root))

    def test_target_drift_is_rejected(self) -> None:
        root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as directory:
            copy = Path(directory) / "repo"
            copy.mkdir()
            with mirrors.MANIFEST.open("r", encoding="utf-8", newline="") as stream:
                rows = list(csv.DictReader(stream, delimiter="\t"))
            for row in rows:
                target = copy / row["target_path"]
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(root / row["target_path"], target)
            drift = copy / rows[0]["target_path"]
            drift.write_text(drift.read_text(encoding="utf-8") + "\n// drift\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "recipe mirror drift"):
                mirrors.validate(copy)


if __name__ == "__main__":
    unittest.main()
