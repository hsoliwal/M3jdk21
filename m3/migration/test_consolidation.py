# SPDX-License-Identifier: Apache-2.0
"""Replay the actual owner consolidation and installation plans in isolated trees."""
from pathlib import Path
import tempfile
import unittest
from recipe import Refusal, checked_path, execute, sealed_plan

ROOT = Path(__file__).resolve().parents[2]
PORT = ROOT / 'm3/ports/indexstring'


class ConsolidationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='m3-owner-replay-')
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.plan_path = PORT / 'consolidation/recipe.json'
        self.plan, self.rows = sealed_plan(self.plan_path)
        for guard in self.plan['guards']:
            path = checked_path(self.root, guard['path'])
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(checked_path(ROOT, guard['path']).read_bytes())
        for row in self.rows:
            if row['before'] is not None:
                path = checked_path(self.root, row['path'])
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(row['before'])

    def test_actual_union_replays_and_rolls_back_exactly(self):
        self.assertEqual(execute(self.plan_path, self.root, 'check')['state'], 'before')
        self.assertGreater(execute(self.plan_path, self.root, 'apply')['writes'], 0)
        self.assertEqual(execute(self.plan_path, self.root, 'apply')['writes'], 0)
        for row in self.rows:
            path = checked_path(self.root, row['path'])
            self.assertEqual(path.read_bytes() if path.exists() else None, row['after'])
        execute(self.plan_path, self.root, 'rollback')
        for row in self.rows:
            path = checked_path(self.root, row['path'])
            self.assertEqual(path.read_bytes() if path.exists() else None, row['before'])

    def test_guard_drift_refuses_before_any_owner_write(self):
        guard = checked_path(self.root, self.plan['guards'][0]['path'])
        guard.write_bytes(b'user change')
        with self.assertRaisesRegex(Refusal, 'guard drift'):
            execute(self.plan_path, self.root, 'apply')
        for row in self.rows:
            path = checked_path(self.root, row['path'])
            self.assertEqual(path.read_bytes() if path.exists() else None, row['before'])

    def test_active_installer_emits_exactly_one_owner_set(self):
        plan_path = PORT / 'recipe-consolidated.json'
        with tempfile.TemporaryDirectory(prefix='m3-owner-install-') as temporary:
            root = Path(temporary)
            execute(plan_path, root, 'apply')
            java = list(root.rglob('*.java'))
            self.assertEqual(len(java), 8)
            self.assertEqual(len(list(root.rglob('FrozenBytes.java'))), 1)
            self.assertEqual(execute(plan_path, root, 'apply')['writes'], 0)


if __name__ == '__main__': unittest.main()
