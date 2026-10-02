#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Tests for exact-postimage deployment logic; full guard replay requires the pinned JDK tree."""
from __future__ import annotations
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
MODULE = ROOT / 'm3/collections'
spec = importlib.util.spec_from_file_location('m3_collections_recipe', MODULE / 'recipe/apply.py')
recipe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)
manifest = json.loads((MODULE / 'recipe/manifest.json').read_text(encoding='utf-8'))

class RecipeUnitTests(unittest.TestCase):
    def test_manifest_binds_every_install_file(self):
        expected = {
            p.relative_to(MODULE).as_posix()
            for p in [MODULE / 'README.md', MODULE / 'build.sh', *sorted((MODULE / 'src').rglob('*.java')), *sorted((MODULE / 'test').rglob('*.java'))]
        }
        self.assertEqual(expected, set(manifest['new_files']))
        recipe.verify(MODULE, manifest['new_files'])

    def test_rejects_modified_source_postimage(self):
        with tempfile.TemporaryDirectory() as directory:
            copy = Path(directory)
            for relative in manifest['new_files']:
                target = copy / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes((MODULE / relative).read_bytes())
            first = copy / next(iter(manifest['new_files']))
            first.write_bytes(first.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'source pin mismatch'):
                recipe.verify(copy, manifest['new_files'])

if __name__ == '__main__':
    unittest.main()
