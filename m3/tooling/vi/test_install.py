# SPDX-License-Identifier: Apache-2.0
"""Offline proof of the retained sealed installer for this exact port packet."""
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

CRATE = Path(__file__).resolve().parent
ROOT = Path(os.environ.get('M3_SOURCE_ROOT', CRATE.parents[2]))
sys.path.insert(0, str(ROOT / 'm3/migration'))
import recipe


class InstallTest(unittest.TestCase):
    def prepare(self, directory):
        root = Path(directory) / 'checkout'
        root.mkdir()
        plan, rows = recipe.sealed_plan(CRATE / 'install/plan.json')
        for row in rows:
            if row['before'] is not None:
                path = root / row['path']
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(row['before'])
        for guard in plan['guards']:
            path = root / guard['path']
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / guard['path'], path)
        return root, rows

    def test_apply_fixed_point_and_rollback(self):
        with tempfile.TemporaryDirectory() as directory:
            root, rows = self.prepare(directory)
            untouched = root / 'unrelated.txt'
            untouched.write_bytes(b'keep')
            plan = CRATE / 'install/plan.json'
            recipe.execute(plan, root, 'apply')
            for row in rows:
                self.assertEqual(row['after'], (root / row['path']).read_bytes())
            recipe.execute(plan, root, 'apply')
            self.assertEqual(b'keep', untouched.read_bytes())
            recipe.execute(plan, root, 'rollback')
            for row in rows:
                self.assertEqual(row['before'], recipe.read_optional(root / row['path']))

    def test_preimage_drift_refuses_entire_packet(self):
        with tempfile.TemporaryDirectory() as directory:
            root, rows = self.prepare(directory)
            (root / 'm3/docs/name-mapping.json').write_bytes(b'changed')
            with self.assertRaises(recipe.Refusal):
                recipe.execute(CRATE / 'install/plan.json', root, 'apply')
            for row in rows:
                if row['before'] is None:
                    self.assertFalse((root / row['path']).exists())

    def test_tampered_plan_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'bad.json'
            plan = json.loads((CRATE / 'install/plan.json').read_text())
            plan['outputs'][0]['path'] = '../outside'
            path.write_text(json.dumps(plan))
            with self.assertRaises(recipe.Refusal):
                recipe.sealed_plan(path)

    def test_existing_mapping_lineage_and_census_preserved(self):
        before = json.loads((CRATE / 'install/name-mapping.before.json').read_text())
        after = json.loads((CRATE / 'install/name-mapping.after.json').read_text())
        records = after['migration']['records']
        self.assertEqual(before['migration']['records'], records[:len(before['migration']['records'])])
        self.assertEqual(len(records), len({row['id'] for row in records}))
        self.assertFalse(after['migration']['coverage']['source_tree_complete'])
        self.assertFalse(after['migration']['coverage']['dependency_closure_complete'])
        self.assertEqual(2, sum(row['status'] == 'implemented-unverified' for row in records if row['id'].startswith('synexia.counterpart.')))


if __name__ == '__main__':
    unittest.main()
