#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
spec = importlib.util.spec_from_file_location('runtime_recipe', HERE / 'apply.py')
recipe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)
manifest = json.loads((HERE / 'manifest.json').read_text())

class RecipeTests(unittest.TestCase):
    def seed(self, root):
        for relative in [manifest['path'], *manifest['runtime_fences']]:
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, path)
        recipe.apply(root, reverse=True)

    def test_forward_idempotence_reverse(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root)
            self.assertEqual('before', recipe.apply(root, check=True))
            self.assertEqual('after', recipe.apply(root))
            self.assertEqual('after', recipe.apply(root))
            self.assertEqual('after', recipe.apply(root, check=True))
            self.assertEqual('before', recipe.apply(root, reverse=True))
            self.assertEqual('before', recipe.apply(root, reverse=True))

    def test_runtime_drift_refused_without_string_write(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root)
            target = root / manifest['path']; original = target.read_bytes()
            for relative in manifest['runtime_fences']:
                path = root / relative; old = path.read_bytes(); path.write_bytes(old + b'\n')
                with self.assertRaisesRegex(ValueError, 'source pin mismatch'): recipe.apply(root)
                self.assertEqual(original, target.read_bytes()); path.write_bytes(old)

    def test_string_drift_and_symlink_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root)
            target = root / manifest['path']; target.write_bytes(target.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'String source drift'): recipe.apply(root)
            target.unlink(); target.symlink_to(ROOT / manifest['path'])
            with self.assertRaisesRegex(ValueError, 'symlink'): recipe.apply(root)

    def test_p0_recipe_still_replays_on_reversed_runtime(self):
        # P0 intentionally seals the unmodified runtime. Test it on an isolated
        # reversed snapshot instead of weakening its historic source guards.
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root)
            p0 = json.loads((ROOT / 'm3/recipes/manifest.json').read_text())
            for relative in [*p0['new_files'], 'm3/recipes/manifest.json']:
                path = root / relative; path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(ROOT / relative, path)
            subprocess.run(['python3', str(root / 'm3/recipes/apply.py'), '--check'], check=True)
            subprocess.run(['python3', str(root / 'm3/recipes/test_recipe.py')], check=True)

if __name__ == '__main__':
    unittest.main()
