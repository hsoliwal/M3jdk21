#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import importlib.util,json,subprocess,tempfile,unittest,shutil,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('integration_recipe',HERE/'apply.py');recipe=importlib.util.module_from_spec(spec);spec.loader.exec_module(recipe)
m=json.loads((HERE/'manifest.json').read_text())
class RecipeTest(unittest.TestCase):
 def seed(self,target):
  for name in m['files']:
   p=target/name;p.parent.mkdir(parents=True,exist_ok=True)
   if (ROOT/name).exists():shutil.copy2(ROOT/name,p)
  self.assertEqual('before',recipe.apply(target,reverse=True))
 def snapshot(self,target):return {n:(target/n).read_bytes() if (target/n).exists() else None for n in m['files']}
 def test_replay_reverse_idempotence_without_history(self):
  with tempfile.TemporaryDirectory() as d:
   t=Path(d);self.seed(t);before=self.snapshot(t)
   self.assertEqual('after',recipe.apply(t));self.assertEqual('after',recipe.apply(t))
   self.assertEqual('before',recipe.apply(t,reverse=True));self.assertEqual(before,self.snapshot(t))
   self.assertEqual('before',recipe.apply(t,reverse=True));self.assertFalse((t/'.git').exists())
 def test_every_drift_refused_without_writes(self):
  with tempfile.TemporaryDirectory() as d:
   t=Path(d);self.seed(t);original=self.snapshot(t)
   for name,data in original.items():
    p=t/name;p.write_bytes((data or b'')+b'\n');before=self.snapshot(t)
    with self.assertRaisesRegex(ValueError,'source drift'):recipe.apply(t)
    self.assertEqual(before,self.snapshot(t))
    if data is None:p.unlink()
    else:p.write_bytes(data)
 def test_mixed_missing_and_symlink(self):
  with tempfile.TemporaryDirectory() as d:
   t=Path(d);self.seed(t);name=next(n for n,h in m['files'].items() if h['before'] is not None);p=t/name;before=p.read_bytes()
   recipe.apply(t);p.write_bytes(before)
   with self.assertRaisesRegex(ValueError,'mixed'):recipe.apply(t)
   p.unlink()
   with self.assertRaisesRegex(ValueError,'source drift'):recipe.apply(t)
   p.symlink_to(ROOT/name)
   with self.assertRaisesRegex(ValueError,'symlink'):recipe.apply(t)
 def test_all_prior_recipe_gates_on_exact_reversed_runtime(self):
  with tempfile.TemporaryDirectory() as d:
   t=Path(d);self.seed(t)
   segment=json.loads((ROOT/'m3/runtime-segments/recipe/manifest.json').read_text())
   prior=json.loads((ROOT/'m3/runtime-stage1/manifest.json').read_text())
   for name in set(segment['files'])|set(prior['runtime_fences']):
    p=t/name
    if not p.exists():p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/name,p)
   p0=json.loads((ROOT/'m3/recipes/manifest.json').read_text());names=[*p0['new_files'],'m3/recipes/manifest.json']
   for folder in ['m3/runtime-stage1','m3/runtime-segments/recipe']:
    names += [str(p.relative_to(ROOT)) for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
   for name in names:
    p=t/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/name,p)
   subprocess.run([sys.executable,str(t/'m3/runtime-segments/recipe/apply.py'),'--check'],check=True)
   subprocess.run([sys.executable,str(t/'m3/runtime-segments/recipe/test_recipe.py')],check=True)
if __name__=='__main__':unittest.main()
