#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Discriminators for selected-scope coverage, lineage, and candidate proof drift."""
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('mapping_check', Path(__file__).with_name('mapping-check.py'))
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class MappingTests(unittest.TestCase):
    def setUp(self):
        self.scratch = tempfile.TemporaryDirectory()
        self.addCleanup(self.scratch.cleanup)
        self.root = Path(self.scratch.name)
        for folder in ['m3/docs', 'm3/ports/text', 'm3/evidence']:
            shutil.copytree(ROOT / folder, self.root / folder,
                            ignore=shutil.ignore_patterns('build', '__pycache__'))

    def change(self, edit):
        path = self.root / 'm3/docs/name-mapping.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        edit(data)
        path.write_text(json.dumps(data), encoding='utf-8')

    def test_candidate_and_scope(self):
        result = checker.validate(self.root)
        self.assertEqual(10, result['covered_java_files'])
        self.assertEqual('selected Route A closure', result['scope'])
        self.assertGreater(result['whole_family_pending'], 0)

    def test_unmapped_addition(self):
        (self.root / 'm3/ports/text/src/Unmapped.java').write_text('class Unmapped {}')
        with self.assertRaisesRegex(ValueError, 'unmapped'):
            checker.validate(self.root)

    def test_duplicate_endpoint(self):
        self.change(lambda data: data['mappings'].append(dict(data['mappings'][0], mapping_id='duplicate')))
        with self.assertRaisesRegex(ValueError, 'duplicate destination'):
            checker.validate(self.root)

    def test_missing_lineage(self):
        self.change(lambda data: data['mappings'][-1].update(upstream_mapping_ids=['missing']))
        with self.assertRaisesRegex(ValueError, 'lineage'):
            checker.validate(self.root)

    def test_stale_source_pin(self):
        self.change(lambda data: data['source_inventory_ref'].update(commit='0' * 40))
        with self.assertRaisesRegex(ValueError, 'source pin'):
            checker.validate(self.root)

    def test_target_or_proof_drift(self):
        path = self.root / 'm3/ports/text/src/com/m3/indexstring/M3String.java'
        path.write_bytes(path.read_bytes() + b'\n// drift\n')
        with self.assertRaisesRegex(ValueError, 'target divergence'):
            checker.validate(self.root)

    def test_schema_rejects_incomplete_row(self):
        self.change(lambda data: data['mappings'][0].pop('canonical_owner'))
        with self.assertRaisesRegex(ValueError, 'schema'):
            checker.validate(self.root)


if __name__ == '__main__':
    unittest.main()
