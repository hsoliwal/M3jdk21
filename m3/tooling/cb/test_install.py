# SPDX-License-Identifier: Apache-2.0
"""Exercise retained installer atomic admission, rollback and mapping lineage."""
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest

CRATE = Path(__file__).resolve().parent
ROOT = CRATE.parents[2]
sys.path.insert(0, str(ROOT / 'm3/migration'))
import recipe


class InstallTest(unittest.TestCase):
    def prepare(self, directory, packet):
        root = Path(directory) / 'checkout'
        root.mkdir()
        plan, rows = recipe.sealed_plan(CRATE / packet / 'plan.json')
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

    def test_both_packets_replay_fixed_point_and_rollback(self):
        for packet in ('install', 'integration', 'close', 'jni'):
            with self.subTest(packet=packet), tempfile.TemporaryDirectory() as directory:
                root, rows = self.prepare(directory, packet)
                untouched = root / 'unrelated.txt'
                untouched.write_bytes(b'keep')
                plan = CRATE / packet / 'plan.json'
                self.assertEqual(len(rows), recipe.execute(plan, root, 'apply')['writes'])
                self.assertEqual(0, recipe.execute(plan, root, 'apply')['writes'])
                for row in rows:
                    self.assertEqual(row['after'], (root / row['path']).read_bytes())
                recipe.execute(plan, root, 'rollback')
                for row in rows:
                    self.assertEqual(row['before'], recipe.read_optional(root / row['path']))
                self.assertEqual(b'keep', untouched.read_bytes())

    def test_preimage_drift_is_atomic(self):
        with tempfile.TemporaryDirectory() as directory:
            root, rows = self.prepare(directory, 'integration')
            (root / 'm3/docs/name-mapping.json').write_bytes(b'drift')
            with self.assertRaises(recipe.Refusal):
                recipe.execute(CRATE / 'integration/plan.json', root, 'apply')
            for row in rows:
                if row['before'] is None:
                    self.assertFalse((root / row['path']).exists())

    def test_changed_product_guard_refuses_integration(self):
        with tempfile.TemporaryDirectory() as directory:
            root, rows = self.prepare(directory, 'integration')
            (root / 'src/java.base/share/classes/jdk/internal/mindex/M3CB.java').write_bytes(b'drift')
            with self.assertRaises(recipe.Refusal):
                recipe.execute(CRATE / 'integration/plan.json', root, 'apply')
            for row in rows:
                self.assertEqual(row['before'], recipe.read_optional(root / row['path']))

    def test_unrelated_lineage_preserved(self):
        before = json.loads((CRATE / 'integration/name-mapping.json.before').read_text())
        after = json.loads((CRATE / 'integration/name-mapping.json.after').read_text())
        old = before['migration']['records']
        new = after['migration']['records']
        self.assertEqual(old[:27], new[:27])
        self.assertEqual(old[31:], new[31:34])
        self.assertFalse(after['migration']['coverage']['source_tree_complete'])
        self.assertFalse(after['migration']['coverage']['dependency_closure_complete'])
        self.assertEqual(len(new), len({r['id'] for r in new}))
        self.assertEqual(14, sum(r['recipe']['id'] == 'm3-cb' for r in new))
        for original in old[27:31]:
            current = next(r for r in new if r['id'] == original['id'])
            self.assertEqual(original['sources'], current['lineage']['previous_sources'])
            self.assertEqual(original['targets'], current['lineage']['previous_targets'])

    def test_jtreg_sources_are_recipe_outputs(self):
        for name in ('CBProbe', 'VmFixture'):
            generated = CRATE / ('target/generated/src/test/java/com/m3/cb/' + name + '.java')
            actual = ROOT / ('test/jdk/jdk/internal/mindex/cb/com/m3/cb/' + name + '.java')
            self.assertEqual(generated.read_bytes(), actual.read_bytes())


if __name__ == '__main__':
    unittest.main()
