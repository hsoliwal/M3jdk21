#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


recipe = load_module("recipe", ROOT / "m3/recipes/apply.py")
refresh = load_module("refresh_manifest", ROOT / "m3/recipes/refresh-manifest.py")
manifest = json.loads((ROOT / "m3/recipes/manifest.json").read_text())


class RecipeTests(unittest.TestCase):
    def seed(self, root):
        for relative in manifest["unchanged_runtime_files"]:
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, path)

    def test_manifest_covers_and_hashes_the_recipe_source_surface(self):
        expected = set(refresh.collect_new_files(ROOT))
        self.assertEqual(expected, set(manifest["new_files"]))
        for relative, expected_hash in manifest["new_files"].items():
            actual_hash = hashlib.sha256((ROOT / relative).read_bytes()).hexdigest()
            self.assertEqual(expected_hash, actual_hash, relative)

    def test_repeatable(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.seed(root)
            count = recipe.apply(ROOT, root)
            self.assertEqual(count, recipe.apply(ROOT, root))
            recipe.verify(root, manifest["new_files"])

    def test_reject_runtime_drift_before_writes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.seed(root)
            first = root / next(iter(manifest["unchanged_runtime_files"]))
            first.write_bytes(first.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "source pin mismatch"):
                recipe.apply(ROOT, root)
            self.assertFalse((root / "m3").exists())

    def test_reject_destination_edits(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.seed(root)
            recipe.apply(ROOT, root)
            first = root / next(iter(manifest["new_files"]))
            first.write_bytes(first.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "refusing modified destination"):
                recipe.apply(ROOT, root)

    def test_source_tamper(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            for relative in [*manifest["new_files"], "m3/recipes/manifest.json"]:
                path = source / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(ROOT / relative, path)
            first = source / next(iter(manifest["new_files"]))
            first.write_bytes(first.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "source pin mismatch"):
                recipe.apply(source, ROOT, True)


if __name__ == "__main__":
    unittest.main()
