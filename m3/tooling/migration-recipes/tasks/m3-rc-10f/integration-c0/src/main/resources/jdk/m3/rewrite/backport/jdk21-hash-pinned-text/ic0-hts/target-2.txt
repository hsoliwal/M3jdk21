# SPDX-License-Identifier: Apache-2.0
"""Exercise the existing sealed installer on this exact intake task crate."""
import importlib.util
from pathlib import Path
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location('intake_existing_installer', ROOT / 'm3/migration/recipe.py')
RECIPE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RECIPE)
PLAN = ROOT / 'm3/migration/intake-20261005/crate/plan.json'


class IntakeRecipeTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name) / 'workspace'
        self.root.mkdir()
        self.plan, self.rows = RECIPE.sealed_plan(PLAN)
        for row in self.rows:
            if row['before'] is not None:
                path = self.root / row['path']
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(row['before'])
        for guard in self.plan['guards']:
            path = self.root / guard['path']
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes((ROOT / guard['path']).read_bytes())
        (self.root / 'unrelated.txt').write_bytes(b'Unrelated contributor source.\n')

    def snapshot(self):
        return {p.relative_to(self.root).as_posix(): p.read_bytes()
                for p in self.root.rglob('*') if p.is_file()}

    def test_actual_task_replay_fixed_point_and_rollback(self):
        before = self.snapshot()
        self.assertEqual('before', RECIPE.execute(PLAN, self.root)['state'])
        applied = RECIPE.execute(PLAN, self.root, 'apply')
        self.assertEqual(len(self.rows), applied['writes'])
        for row in self.rows:
            self.assertEqual(row['after'], (self.root / row['path']).read_bytes())
        self.assertEqual(0, RECIPE.execute(PLAN, self.root, 'apply')['writes'])
        self.assertEqual('after', RECIPE.execute(PLAN, self.root)['state'])
        RECIPE.execute(PLAN, self.root, 'rollback')
        self.assertEqual(before, self.snapshot())

    def test_destination_drift_refuses_before_any_output(self):
        (self.root / 'm3/migration/migration.py').write_bytes(b'concurrent contributor change\n')
        before = self.snapshot()
        with self.assertRaises(RECIPE.Refusal):
            RECIPE.execute(PLAN, self.root, 'apply')
        self.assertEqual(before, self.snapshot())

    def test_exact_resource_drift_refuses_before_any_output(self):
        copied = self.root.parent / 'crate'
        shutil.copytree(PLAN.parent, copied)
        resource = self.plan['outputs'][0]['after']['resource']
        (copied / resource).write_bytes(b'changed candidate\n')
        before = self.snapshot()
        with self.assertRaises(RECIPE.Refusal):
            RECIPE.execute(copied / PLAN.name, self.root, 'apply')
        self.assertEqual(before, self.snapshot())

    def test_dependency_guard_drift_refuses_before_any_output(self):
        guard = self.plan['guards'][0]
        (self.root / guard['path']).write_bytes(b'changed dependency\n')
        before = self.snapshot()
        with self.assertRaises(RECIPE.Refusal):
            RECIPE.execute(PLAN, self.root, 'apply')
        self.assertEqual(before, self.snapshot())
