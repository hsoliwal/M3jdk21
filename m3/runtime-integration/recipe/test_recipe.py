#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Prove current runtime custody: exact after-image fixed point, reversibility, drift refusal, patch identity."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('integration_recipe',HERE/'apply.py')
recipe=importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)
manifest=json.loads((HERE/'manifest.json').read_text())


class RecipeTest(unittest.TestCase):
 def seed_current(self,target):
  for name in manifest['files']:
   source=ROOT/name
   destination=target/name
   destination.parent.mkdir(parents=True,exist_ok=True)
   if source.exists():destination.write_bytes(source.read_bytes())

 def snapshot(self,target):
  return {
   name:(target/name).read_bytes() if (target/name).exists() else None
   for name in manifest['files']
  }

 def test_manifest_is_upstream_to_master_v2(self):
  self.assertEqual('M3_RUNTIME_INTEGRATION_RECIPE_V2',manifest['schema'])
  self.assertEqual(manifest['base_commit'],manifest['upstream_commit'])
  self.assertEqual(64,len(manifest['files']))
  for name,hashes in manifest['files'].items():
   self.assertIsNotNone(hashes['after'],name)
   self.assertNotIn('superseded',hashes,name)

 def test_current_tree_is_exact_after_image_fixed_point(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder);self.seed_current(target);before=self.snapshot(target)
   self.assertEqual('after',recipe.apply(target,check=True))
   self.assertEqual('after',recipe.apply(target))
   self.assertEqual(before,self.snapshot(target))

 def test_every_current_target_is_exact_after(self):
  for name,hashes in manifest['files'].items():
   source=ROOT/name
   actual=recipe.digest(source.read_bytes()) if source.exists() else None
   self.assertEqual(hashes['after'],actual,name)

 def test_reverse_restores_upstream_preimages_and_reapply_is_fixed_point(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder);self.seed_current(target);current=self.snapshot(target)
   self.assertEqual('before',recipe.apply(target,reverse=True))
   for name,hashes in manifest['files'].items():
    path=target/name
    actual=recipe.digest(path.read_bytes()) if path.exists() else None
    self.assertEqual(hashes['before'],actual,name)
   self.assertEqual('before',recipe.apply(target,check=True))
   self.assertEqual('after',recipe.apply(target))
   self.assertEqual(current,self.snapshot(target))

 def test_patch_bytes_remain_content_addressed(self):
  patch=HERE/'runtime.patch'
  self.assertEqual(manifest['patch_sha256'],recipe.digest(patch.read_bytes()))
  self.assertNotIn(b'\r',patch.read_bytes())

 def test_each_unknown_drift_is_refused_without_partial_write(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder);self.seed_current(target)
   for name in manifest['files']:
    path=target/name
    original=path.read_bytes() if path.exists() else None
    if original is None:
     path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'drift')
    else:path.write_bytes(original+b'\n')
    before=self.snapshot(target)
    with self.assertRaisesRegex(ValueError,'source drift'):
     recipe.apply(target)
    self.assertEqual(before,self.snapshot(target))
    if original is None:path.unlink()
    else:path.write_bytes(original)

 def test_mixed_before_and_after_state_is_refused(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder)/'target';target.mkdir();self.seed_current(target)
   upstream=Path(folder)/'upstream';upstream.mkdir();self.seed_current(upstream)
   self.assertEqual('before',recipe.apply(upstream,reverse=True))
   name=next(n for n,h in manifest['files'].items() if h['before'] is not None)
   (target/name).write_bytes((upstream/name).read_bytes())
   before=self.snapshot(target)
   with self.assertRaisesRegex(ValueError,'mixed runtime patch state'):
    recipe.apply(target)
   self.assertEqual(before,self.snapshot(target))

 def test_symlink_target_is_refused(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder);self.seed_current(target)
   name=next(iter(manifest['files']))
   path=target/name;path.unlink();path.symlink_to(ROOT/name)
   with self.assertRaisesRegex(ValueError,'symlink path'):
    recipe.apply(target)


if __name__=='__main__':
 unittest.main()
