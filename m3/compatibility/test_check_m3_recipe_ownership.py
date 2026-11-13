# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutation tests for check_m3_recipe_ownership.py: the checker passes on this repository and refuses a one-byte change
of any record it pins and of any file a record freezes, a retired legacy recipe brought back and an unsealed residue
file. Each case runs on a copy of exactly the files the checker reads."""
from __future__ import annotations

import contextlib
import csv
import importlib.util
import io
import shutil
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("check_m3_recipe_ownership", HERE / "check_m3_recipe_ownership.py")
own = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(own)
REPO = own.ROOT


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def frozen_files() -> list[str]:
    """Every repository file the records freeze, as repository paths."""
    paths = set(own.reusable_residue_paths())
    for row in rows(own.LEDGER):
        for field in ("legacy_path", "canonical_path"):
            if (REPO / row[field]).is_file():
                paths.add(row[field])
    paths.update(row["path"] for row in rows(own.RESIDUE))
    return sorted(paths)


class RecipeOwnershipMutationTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        for path in frozen_files():
            (self.root / path).parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(REPO / path, self.root / path)
        records = self.root / "m3/compatibility"
        records.mkdir(parents=True, exist_ok=True)
        saved = {name: getattr(own, name) for name in ("ROOT", "LEDGER", "PIN", "RESIDUE")}
        self.addCleanup(lambda: [setattr(own, k, v) for k, v in saved.items()])
        own.ROOT = self.root
        for name in ("LEDGER", "PIN", "RESIDUE"):
            source = saved[name]
            shutil.copyfile(source, records / source.name)
            setattr(own, name, records / source.name)

    def run_checker(self):
        with contextlib.redirect_stdout(io.StringIO()) as out:
            code = own.main(["check_m3_recipe_ownership.py"])
        return code, out.getvalue()

    def flip(self, path: Path, offset: int = 0):
        data = bytearray(path.read_bytes())
        data[offset] ^= 0x01
        path.write_bytes(bytes(data))

    def test_this_repository_passes(self):
        own.ROOT, own.LEDGER, own.PIN, own.RESIDUE = REPO, HERE / own.LEDGER.name, HERE / own.PIN.name, HERE / own.RESIDUE.name
        code, out = self.run_checker()
        self.assertEqual(0, code)
        self.assertIn("M3_RECIPE_OWNERSHIP_PASS", out)

    def test_the_copy_passes(self):
        self.assertEqual(0, self.run_checker()[0])

    def test_a_one_byte_change_of_a_record_refuses(self):
        for name in ("LEDGER", "PIN", "RESIDUE"):
            path = getattr(own, name)
            for offset in (0, len(path.read_bytes()) // 2, len(path.read_bytes()) - 2):
                with self.subTest(record=path.name, offset=offset):
                    original = path.read_bytes()
                    self.flip(path, offset)
                    with self.assertRaisesRegex(ValueError, "record drift"):
                        self.run_checker()
                    path.write_bytes(original)

    def test_a_one_byte_change_of_a_frozen_file_refuses(self):
        for path in frozen_files():
            with self.subTest(path=path):
                target = self.root / path
                original = target.read_bytes()
                self.flip(target, len(original) // 2)
                with self.assertRaisesRegex(ValueError, "drift"):
                    self.run_checker()
                target.write_bytes(original)

    def test_a_retired_legacy_recipe_brought_back_refuses(self):
        retired = next(row for row in rows(own.LEDGER) if row["state"].endswith("LEGACY_RETIRED"))
        (self.root / retired["legacy_path"]).parent.mkdir(parents=True, exist_ok=True)
        (self.root / retired["legacy_path"]).write_text("class Back {}\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "retired legacy recipe reintroduced"):
            self.run_checker()

    def test_an_unsealed_residue_file_refuses(self):
        extra = self.root / own.RESIDUE_DIRS[0] / "M3Unsealed.java"
        extra.parent.mkdir(parents=True, exist_ok=True)
        extra.write_text("class M3Unsealed {}\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "reusable residue set drift"):
            self.run_checker()

    def test_every_adapter_row_is_verified_by_its_blob(self):
        adapters = [row for row in rows(own.LEDGER) if row["state"] == own.ADAPTER]
        self.assertEqual(3, len(adapters))
        for row in adapters:
            with self.subTest(family=row["family"]):
                target = self.root / row["legacy_path"]
                original = target.read_bytes()
                target.write_bytes(original + b"\n")
                with self.assertRaisesRegex(ValueError, "target adapter drift"):
                    self.run_checker()
                target.write_bytes(original)


if __name__ == "__main__":
    unittest.main()
