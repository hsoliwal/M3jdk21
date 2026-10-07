#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Prove current runtime custody, supersession, drift refusal, and patch identity."""
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

 def test_current_tree_is_sealed_superseding_fixed_point(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder);self.seed_current(target);before=self.snapshot(target)
   self.assertEqual('superseded',recipe.apply(target,check=True))
   self.assertEqual('superseded',recipe.apply(target))
   self.assertEqual('superseded',recipe.apply(target,reverse=True))
   self.assertEqual(before,self.snapshot(target))

 def test_every_current_target_is_exact_after_or_explicit_superseded(self):
  seen_superseded=0
  for name,hashes in manifest['files'].items():
   source=ROOT/name
   actual=recipe.digest(source.read_bytes()) if source.exists() else None
   admitted={hashes['after'],*hashes.get('superseded',[])}
   self.assertIn(actual,admitted,name)
   if actual in hashes.get('superseded',[]):seen_superseded+=1
  self.assertEqual(7,seen_superseded)

 def test_current_jni_owner_is_verified_supersession(self):
  name='src/hotspot/share/prims/jni.cpp'
  expected='95adc8ea2a9035e65b613a7fb4131f8629bc90f81b70008895a77c1e396b05f9'
  source=ROOT/name
  actual=recipe.digest(source.read_bytes())
  self.assertEqual(expected,actual)
  self.assertIn(actual,manifest['files'][name]['superseded'])

 def test_patch_bytes_remain_content_addressed(self):
  patch=HERE/'runtime.patch'
  self.assertEqual(manifest['patch_sha256'],recipe.digest(patch.read_bytes()))

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

 def test_symlink_target_is_refused(self):
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder);self.seed_current(target)
   name=next(iter(manifest['files']))
   path=target/name;path.unlink();path.symlink_to(ROOT/name)
   with self.assertRaisesRegex(ValueError,'symlink path'):
    recipe.apply(target)


if __name__=='__main__':
 unittest.main()
