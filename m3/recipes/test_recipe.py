#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import importlib.util
import hashlib
import json
import os
from pathlib import Path
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('recipe', ROOT / 'm3/recipes/apply.py')
recipe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)
manifest = json.loads((ROOT / 'm3/recipes/manifest.json').read_text())

class RecipeTests(unittest.TestCase):
    def seed(self, root):
        for relative in manifest['unchanged_runtime_files']:
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, path)
    def test_repeatable(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root)
            count = recipe.apply(ROOT, root)
            self.assertEqual(count, recipe.apply(ROOT, root))
            recipe.verify(root, manifest['new_files'])
    def test_reject_runtime_drift_before_writes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root)
            first = root / next(iter(manifest['unchanged_runtime_files']))
            first.write_bytes(first.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'source pin mismatch'): recipe.apply(ROOT, root)
            self.assertFalse((root / 'm3').exists())
    def test_reject_destination_edits(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.seed(root); recipe.apply(ROOT, root)
            first = root / next(iter(manifest['new_files']))
            first.write_bytes(first.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'refusing modified destination'): recipe.apply(ROOT, root)
    def test_source_tamper(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            for relative in [*manifest['new_files'], 'm3/recipes/manifest.json']:
                path = source / relative;path.parent.mkdir(parents=True, exist_ok=True);shutil.copy2(ROOT / relative, path)
            first = source / next(iter(manifest['new_files']))
            first.write_bytes(first.read_bytes() + b'\n')
            with self.assertRaisesRegex(ValueError, 'source pin mismatch'): recipe.apply(source, ROOT, True)

class ThreeWayRecipeTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        directory = Path(self.temporary.name)
        self.baseline = directory / 'baseline'
        self.source = directory / 'source'
        self.target = directory / 'target'
        self.fence = 'src/java.base/share/classes/java/lang/String.java'
        self.before = {'m3/text.bin': b'old\x00\xff', 'm3/retired.txt': b'retired\n'}
        self.after = {'m3/text.bin': b'new\x00\xfe', 'm3/added.txt': b'added\n'}
        self.seed_source(self.baseline, self.before)
        self.seed_source(self.source, self.after)
        shutil.copytree(self.baseline, self.target)
        (self.target / 'm3/target-only.txt').write_bytes(b'keep local adaptation\n')

    def seed_source(self, root, files):
        for relative, data in {**files, self.fence: b'stock runtime\n'}.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        manifest_path = root / 'm3/recipes/manifest.json'
        manifest_path.parent.mkdir(parents=True, exist_ok=True)
        manifest_path.write_text(json.dumps({
            'schema': 1,
            'unchanged_runtime_files': {
                self.fence: hashlib.sha256(b'stock runtime\n').hexdigest()},
            'new_files': {name: hashlib.sha256(data).hexdigest()
                          for name, data in files.items()},
        }), encoding='utf-8')

    def replay(self, **options):
        return recipe.apply(self.source, self.target, baseline=self.baseline, **options)

    def snapshot(self):
        return {str(path.relative_to(self.target)): path.read_bytes()
                for path in self.target.rglob('*') if path.is_file()}

    def test_threeway_replay_reverse_idempotence_and_target_only_preservation(self):
        original = self.snapshot()
        self.replay()
        self.assertEqual(b'new\x00\xfe', (self.target / 'm3/text.bin').read_bytes())
        self.assertEqual(b'added\n', (self.target / 'm3/added.txt').read_bytes())
        self.assertFalse((self.target / 'm3/retired.txt').exists())
        self.assertEqual(b'keep local adaptation\n',
                         (self.target / 'm3/target-only.txt').read_bytes())
        updated = self.snapshot()
        self.replay()
        self.assertEqual(updated, self.snapshot())
        self.replay(reverse=True)
        self.assertEqual(original, self.snapshot())
        self.replay(reverse=True)
        self.assertEqual(original, self.snapshot())

    def test_target_divergence_refused_before_any_write(self):
        (self.target / 'm3/text.bin').write_bytes(b'target adaptation')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'target divergence'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_missing_previous_file_refused_before_any_write(self):
        (self.target / 'm3/text.bin').unlink()
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'target divergence'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_partial_exact_states_converge(self):
        (self.target / 'm3/text.bin').write_bytes(b'new\x00\xfe')
        (self.target / 'm3/retired.txt').unlink()
        self.replay()
        self.assertEqual(b'added\n', (self.target / 'm3/added.txt').read_bytes())
        self.assertEqual((self.source / 'm3/recipes/manifest.json').read_bytes(),
                         (self.target / 'm3/recipes/manifest.json').read_bytes())

    def test_check_only_preserves_target_tree(self):
        original = self.snapshot()
        self.replay(check_only=True)
        self.assertEqual(original, self.snapshot())

    def test_source_tamper_refused_before_any_write(self):
        (self.source / 'm3/text.bin').write_bytes(b'unpinned source')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'source pin mismatch'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_baseline_tamper_refused_before_any_write(self):
        (self.baseline / 'm3/retired.txt').write_bytes(b'corrupt baseline')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'source pin mismatch'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_reverse_requires_explicit_baseline(self):
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'explicit baseline'):
            recipe.apply(self.source, self.target, reverse=True)
        self.assertEqual(original, self.snapshot())

    def test_unsupported_manifest_schema_refused_before_any_write(self):
        manifest_path = self.source / 'm3/recipes/manifest.json'
        data = json.loads(manifest_path.read_text())
        data['schema'] = 999
        manifest_path.write_text(json.dumps(data), encoding='utf-8')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'unsupported recipe schema'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_runtime_drift_refused_before_any_write(self):
        (self.target / self.fence).write_bytes(b'changed runtime')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'source pin mismatch'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_changed_runtime_fence_requires_separate_recipe(self):
        manifest_path = self.source / 'm3/recipes/manifest.json'
        data = json.loads(manifest_path.read_text())
        data['unchanged_runtime_files'][self.fence] = hashlib.sha256(b'new runtime').hexdigest()
        manifest_path.write_text(json.dumps(data), encoding='utf-8')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'runtime fences differ'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_rollback_refuses_target_edit(self):
        self.replay()
        (self.target / 'm3/added.txt').write_bytes(b'edited after replay')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'target divergence'):
            self.replay(reverse=True)
        self.assertEqual(original, self.snapshot())

    def test_manifest_path_escape_refused_before_any_write(self):
        manifest_path = self.source / 'm3/recipes/manifest.json'
        data = json.loads(manifest_path.read_text())
        data['new_files']['m3/../../outside.txt'] = hashlib.sha256(b'escape').hexdigest()
        manifest_path.write_text(json.dumps(data), encoding='utf-8')
        original = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'unsafe'):
            self.replay()
        self.assertEqual(original, self.snapshot())

    def test_linked_managed_ancestor_refused_before_any_write(self):
        linked = self.target / 'm3/recipes'
        shutil.rmtree(linked)
        if os.name == 'nt':
            import _winapi
            _winapi.CreateJunction(str(self.baseline / 'm3/recipes'), str(linked))
        else:
            linked.symlink_to(self.baseline / 'm3/recipes', target_is_directory=True)
        original = self.snapshot()
        baseline_manifest = (self.baseline / 'm3/recipes/manifest.json').read_bytes()
        with self.assertRaisesRegex(ValueError, 'symlink/reparse'):
            self.replay()
        self.assertEqual(original, self.snapshot())
        self.assertEqual(baseline_manifest,
                         (self.baseline / 'm3/recipes/manifest.json').read_bytes())


if __name__ == '__main__': unittest.main()
