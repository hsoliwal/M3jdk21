#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Independent fixtures for the M3JDK21 multi-pass source-custody check."""
import json
from pathlib import Path
import tempfile
import unittest

import verify_multi_pass as m


class MultiPassSealTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.data = self.root / m.PREFIX
        self.data.mkdir(parents=True)
        self.agg = self.root / m.RESOURCE / m.CRATE
        self.agg.mkdir(parents=True)
        self.rows = []
        self.plan = []
        self.sources = []
        for index, (path, stage) in enumerate(zip(m.PASS_PATHS, m.STAGES), 1):
            before = (f'// original owner {index}\n').encode() if index != 3 else None
            after = (f'// generated exact Java source {index}\n').encode()
            name = f'{index:04d}-Original.java.txt'
            pre = m.digest(before) if before is not None else 'ABSENT'
            post = m.digest(after)
            row = '\t'.join([path, pre, post, name])
            self.rows.append(row)
            self.plan.append('\t'.join([
                str(index), stage, path, name, f'behavioral-atom-{index}',
                f'problem-category-{index}']))
            self.sources.append((path, after))
            out = self.root / path
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(after)
            for place in (self.agg, self.root / m.RESOURCE / stage):
                place.mkdir(parents=True, exist_ok=True)
                (place / name).write_bytes(after)
                if before is not None:
                    (place / 'pre').mkdir(exist_ok=True)
                    (place / 'pre' / name).write_bytes(before)
            stage_path = self.root / m.RESOURCE / stage / 'manifest.tsv'
            stage_path.write_text(row + '\n', encoding='utf-8')
        (self.agg / 'manifest.tsv').write_text('\n'.join(self.rows) + '\n', encoding='utf-8')
        (self.data / 'PASSES.tsv').write_text('\n'.join(self.plan) + '\n', encoding='utf-8')
        (self.data / 'MODES.tsv').write_text(
            '\n'.join(p + '\t0644' for p in m.PASS_PATHS) + '\n', encoding='utf-8')
        (self.data / 'DATASET.json').write_text(json.dumps({
            'crate_name': m.CRATE, 'targets': 3, 'java_targets': 3,
        }) + '\n', encoding='utf-8')
        yaml = '''name: com.synexia.m3jdk.AstBuildReverse20261017\nrecipeList:\n'''
        for stage in m.STAGES:
            yaml += '  - com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe:\n'
            yaml += '      crateName: ' + stage + '\n'
        f = self.root / m.DESCRIPTOR
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(yaml, encoding='utf-8')

    def tearDown(self):
        self.temp.cleanup()

    def test_success_and_true_fixed_point_model(self):
        report = m.verify(self.root)
        self.assertEqual(report['file_local_recipes'], 3)
        self.assertEqual(report['source_changes'], 3)
        self.assertEqual(report['fixed_point_changes'], 0)
        self.assertEqual(report, m.verify(self.root))

    def test_product_drift_refused(self):
        path = self.root / m.PASS_PATHS[0]
        path.write_bytes(path.read_bytes() + b'// drift\n')
        with self.assertRaisesRegex(ValueError, 'product source'):
            m.verify(self.root)

    def test_stage_preimage_drift_refused(self):
        path = self.root / m.RESOURCE / m.STAGES[1] / 'pre' / '0002-Original.java.txt'
        path.write_bytes(b'// wrong original\n')
        with self.assertRaisesRegex(ValueError, 'preimage drift'):
            m.verify(self.root)

    def test_recipe_order_refused(self):
        path = self.root / m.DESCRIPTOR
        text = path.read_text(encoding='utf-8')
        text = text.replace(m.STAGES[0], 'DUMMY').replace(m.STAGES[1], m.STAGES[0]).replace('DUMMY', m.STAGES[1])
        path.write_text(text, encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'composition drift'):
            m.verify(self.root)

    def test_extra_or_overlapping_stage_refused(self):
        place = self.root / m.RESOURCE / m.STAGES[1] / 'manifest.tsv'
        place.write_text('\n'.join([self.rows[0], self.rows[1]]) + '\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'exactly one'):
            m.verify(self.root)

    def test_source_namespace_escape_refused(self):
        invalid = self.rows[0].replace(m.PASS_PATHS[0], '../escape.java')
        (self.agg / 'manifest.tsv').write_text('\n'.join([invalid, *self.rows[1:]]) + '\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'aggregate target'):
            m.verify(self.root)

    def test_missing_phase_category_refused(self):
        invalid = self.plan[2].replace('problem-category-3', '')
        (self.data / 'PASSES.tsv').write_text('\n'.join([*self.plan[:2], invalid]) + '\n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'unclassified'):
            m.verify(self.root)


if __name__ == '__main__':
    unittest.main()
