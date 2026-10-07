#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
import importlib.util,json,subprocess,tempfile,unittest,shutil,sys
from pathlib import Path
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('segments_recipe',HERE/'apply.py');recipe=importlib.util.module_from_spec(spec);spec.loader.exec_module(recipe)
m=json.loads((HERE/'manifest.json').read_text())
class RecipeTest(unittest.TestCase):
    def seed(self,target):
        for name in m['files']:
            p=target/name;p.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(ROOT/name,p)
        # Reconstruct and verify exact preimages from the sealed postimage/patch.
        # Works in shallow checkouts and exported trees without Git history.
        self.assertEqual('before',recipe.apply(target,reverse=True))
    def test_replay_reverse_idempotence(self):
        with tempfile.TemporaryDirectory() as tmp:
            target=Path(tmp);self.seed(target)
            self.assertEqual('after',recipe.apply(target));self.assertEqual('after',recipe.apply(target))
            self.assertEqual('before',recipe.apply(target,reverse=True));self.assertEqual('before',recipe.apply(target,reverse=True))
    def test_each_preimage_drift_refused_before_any_writes(self):
        with tempfile.TemporaryDirectory() as tmp:
            target=Path(tmp);self.seed(target);names=list(m['files'])
            originals={name:(target/name).read_bytes() for name in names}
            for name in names:
                p=target/name;p.write_bytes(originals[name]+b'\n')
                with self.assertRaisesRegex(ValueError,'source drift'):recipe.apply(target)
                for other in names:
                    if other!=name:self.assertEqual(originals[other],(target/other).read_bytes())
                p.write_bytes(originals[name])
    def test_prior_recipes_on_exact_prior_runtime(self):
        with tempfile.TemporaryDirectory() as tmp:
            target=Path(tmp);self.seed(target)
            prior=json.loads((ROOT/'m3/runtime-stage1/manifest.json').read_text())
            for name in prior['runtime_fences']:
                p=target/name
                if not p.exists():
                    p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/name,p)
            p0=json.loads((ROOT/'m3/recipes/manifest.json').read_text())
            names=[*p0['new_files'],'m3/recipes/manifest.json']
            names += [str(p.relative_to(ROOT)) for p in (ROOT/'m3/runtime-stage1').rglob('*')
                      if p.is_file() and '__pycache__' not in p.parts]
            for name in names:
                p=target/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/name,p)
            subprocess.run([sys.executable,str(target/'m3/runtime-stage1/test_recipe.py')],check=True)
    def test_mixed_state_and_symlink_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            target=Path(tmp);self.seed(target);name=next(iter(m['files']));before=(target/name).read_bytes()
            recipe.apply(target);(target/name).write_bytes(before)
            with self.assertRaisesRegex(ValueError,'mixed'):recipe.apply(target)
            p=target/name;p.unlink();p.symlink_to(ROOT/name)
            with self.assertRaisesRegex(ValueError,'symlink'):recipe.apply(target)
if __name__=='__main__':unittest.main()
