#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

MIGRATION = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('replay', MIGRATION / 'recipe_replay.py')
replay = importlib.util.module_from_spec(spec)
spec.loader.exec_module(replay)
BUNDLE = MIGRATION / 'rewrite/src/main/resources/com/m3/migration/safety'

class SafetyRecipeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / 'checkout'; self.root.mkdir()
        self.bundle = Path(self.temp.name) / 'bundle'; shutil.copytree(BUNDLE, self.bundle)
        self.pin, self.doc, self.entries = replay.load_recipe(self.bundle)
        for row in self.entries:
            if row['beforeBytes'] is not None:
                p = self.root / row['path']; p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(row['beforeBytes'])
        (self.root / 'unrelated.txt').write_text('preserve me')
    def tearDown(self): self.temp.cleanup()
    def run_recipe(self, **args):
        return replay.replay(self.root, self.bundle, expected_recipe=self.pin, **args)
    def snapshot(self):
        return {str(p.relative_to(self.root)): p.read_bytes() for p in self.root.rglob('*') if p.is_file()}
    def test_replay_fixedpoint_reverse(self):
        before = self.snapshot(); receipt = self.run_recipe()
        self.assertEqual(receipt['status'], 'applied')
        for e in self.entries: self.assertEqual((self.root/e['path']).read_bytes(), e['afterBytes'])
        self.assertEqual(self.run_recipe()['status'], 'fixed-point')
        self.run_recipe(reverse=True); self.assertEqual(self.snapshot(), before)
        self.assertEqual(self.run_recipe(reverse=True)['status'], 'fixed-point')
    def test_dry_run(self):
        before=self.snapshot(); self.assertEqual(self.run_recipe(check_only=True)['status'], 'checked'); self.assertEqual(before,self.snapshot())
    def test_drift_before_any_write(self):
        path=self.root/self.entries[0]['path']; path.write_bytes(path.read_bytes()+b'\n')
        before=self.snapshot()
        with self.assertRaises(replay.Refusal): self.run_recipe()
        self.assertEqual(before,self.snapshot())
    def test_missing_owner(self):
        (self.root/self.entries[0]['path']).unlink()
        with self.assertRaises(replay.Refusal): self.run_recipe()
    def test_mixed_state_requires_explicit_recovery(self):
        row=self.entries[0]; (self.root/row['path']).write_bytes(row['afterBytes'])
        before=self.snapshot()
        with self.assertRaises(replay.Refusal): self.run_recipe()
        self.assertEqual(before,self.snapshot()); self.run_recipe(recover=True)
        self.assertEqual(self.run_recipe()['status'],'fixed-point')
    def test_rollback_preserves_later_edits(self):
        self.run_recipe(); p=self.root/self.entries[0]['path']; p.write_bytes(p.read_bytes()+b'// user edit\n')
        before=self.snapshot()
        with self.assertRaises(replay.Refusal): self.run_recipe(reverse=True)
        self.assertEqual(before,self.snapshot())
    def test_resource_tamper(self):
        p=self.bundle/self.entries[0]['postimage'];p.write_bytes(p.read_bytes()+b'\n')
        with self.assertRaises(replay.Refusal): self.run_recipe()
    def test_review_root_mismatch(self):
        with self.assertRaises(replay.Refusal): replay.replay(self.root,self.bundle,expected_recipe='0'*64)
    def test_duplicate_keys(self):
        (self.bundle/'recipe.json').write_text('{"schemaVersion":1,"schemaVersion":1}')
        with self.assertRaises(replay.Refusal): self.run_recipe()
    def test_duplicate_paths(self):
        self.doc['entries'].append(self.doc['entries'][0]);(self.bundle/'recipe.json').write_text(json.dumps(self.doc))
        with self.assertRaises(replay.Refusal): self.run_recipe()
    def test_unsafe_paths(self):
        for path in ('../escape','/absolute','m3/../escape','m3//alias','m3/./alias','C:/escape','m3\\alias','m3/\0alias'):
            with self.subTest(path=path), self.assertRaises(replay.Refusal): replay.safe_path(self.root,path)
    def test_symlink_parent(self):
        (self.root/'link').symlink_to(self.root/'m3',target_is_directory=True)
        with self.assertRaises(replay.Refusal): replay.safe_path(self.root,'link/file')
    def test_cooperative_lock(self):
        (self.root/'.m3-recipe-replay.lock').write_text('another operation')
        with self.assertRaises(replay.Refusal): self.run_recipe()
        self.assertTrue((self.root/'.m3-recipe-replay.lock').exists())
    def test_destination_permissions_retained(self):
        p=self.root/self.entries[0]['path'];p.chmod(0o640);self.run_recipe();self.assertEqual(p.stat().st_mode & 0o777,0o640)

if __name__ == '__main__': unittest.main(verbosity=2)
