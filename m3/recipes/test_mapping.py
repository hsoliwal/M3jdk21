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
        for folder in ['m3/docs', 'm3/ports/text', 'm3/evidence',
                       'm3/recipes', 'm3/tooling/compiler-lowering']:
            shutil.copytree(ROOT / folder, self.root / folder,
                            ignore=shutil.ignore_patterns('build', 'target', '__pycache__'))
        # Preserve the exact artifact and evidence closure of the existing authority.
        mapping = json.loads((ROOT / 'm3/docs/name-mapping.json').read_text())
        paths = set()
        for row in mapping['migration']['records']:
            if row['status'].startswith('implemented'):
                paths.update(target['path'] for target in row['targets'])
                if row['recipe']['path']:
                    paths.add(row['recipe']['path'])
            for test in row['tests']:
                paths.add(test['receipt'])
                proof = json.loads((ROOT / test['receipt']).read_text())
                for run in proof['runs']:
                    paths.update(run['inputs'])
                    if run.get('stdout_path'):
                        paths.add(run['stdout_path'])
        for relative in paths:
            destination = self.root / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, destination)


    def change(self, edit):
        path = self.root / 'm3/docs/name-mapping.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        edit(data)
        path.write_text(json.dumps(data), encoding='utf-8')

    def test_candidate_and_scope(self):
        result = checker.validate(self.root)
        self.assertEqual(11, result['covered_java_files'])
        self.assertEqual(checker.SCOPE, result['scope'])
        self.assertGreater(result['all_migration_records'], result['mapping_count'])
        self.assertGreater(result['whole_family_pending'], 0)

    def test_unmapped_addition(self):
        (self.root / 'm3/ports/text/src/Unmapped.java').write_text('class Unmapped {}')
        with self.assertRaisesRegex(ValueError, 'unmapped'):
            checker.validate(self.root)

    def test_duplicate_endpoint(self):
        def duplicate(data):
            record = data['migration']['records'][-1]
            data['migration']['records'].append(dict(record, id='duplicate'))
            data['selected_ports']['record_ids'].append('duplicate')
        self.change(duplicate)
        with self.assertRaisesRegex(ValueError, 'duplicate selected destination'):
            checker.validate(self.root)

    def test_missing_lineage(self):
        self.change(lambda data: data['migration']['records'][-1]['lineage'].update(supersedes=['missing']))
        with self.assertRaisesRegex(ValueError, 'lineage'):
            checker.validate(self.root)

    def test_stale_source_pin(self):
        self.change(lambda data: data['selected_ports']['source_inventory_ref'].update(commit='0' * 40))
        with self.assertRaisesRegex(ValueError, 'source pin'):
            checker.validate(self.root)

    def test_target_or_proof_drift(self):
        path = self.root / 'm3/ports/text/src/com/m3/indexstring/M3String.java'
        path.write_bytes(path.read_bytes() + b'\n// drift\n')
        with self.assertRaisesRegex(ValueError, 'hash drift'):
            checker.validate(self.root)

    def test_schema_rejects_incomplete_row(self):
        self.change(lambda data: data['migration']['records'][0].pop('owner'))
        with self.assertRaisesRegex(ValueError, 'schema'):
            checker.validate(self.root)


if __name__ == '__main__':
    unittest.main()
