# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Source-bound refusal tests for legacy public M3 name migration records.

Tests the existing target mapping authority; no product classes are rewritten.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
MAP = Path('m3/docs/name-mapping.json')
PLAN = Path('m3/tooling/name-contract/name-plan.json')
LEGACY = {
    'm3jdk21.legacy-export.M3ArrayList': (
        'com.m3.collections.M3ArrayList',
        'm3/collections/src/main/java/com/m3/collections/M3ArrayList.java',
        'm3/collections/src/main/java/module-info.java',
        'com.m3.collections',
    ),
    'm3jdk21.legacy-export.M3File': (
        'com.m3.file.M3File',
        'm3/file/src/main/java/com/m3/file/M3File.java',
        'm3/file/src/main/java/module-info.java',
        'com.m3.file',
    ),
}


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


class LegacyExportMappingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.raw_map = (ROOT / MAP).read_bytes()
        cls.mapping = json.loads(cls.raw_map.decode('utf-8'))
        cls.plan = json.loads((ROOT / PLAN).read_text(encoding='utf-8'))
        cls.records = cls.mapping['migration']['records']
        cls.by_id = {row['id']: row for row in cls.records}

    def test_unique_stable_ids_and_no_shadow_mapping_authority(self) -> None:
        self.assertEqual(len(self.by_id), len(self.records))
        self.assertEqual(self.plan['authority'], MAP.as_posix())
        self.assertEqual(self.plan['authority_blob'], git_blob(self.raw_map))

    def test_legacy_classes_remain_non_promoted(self) -> None:
        for stable_id, (symbol, path, _, _) in sorted(LEGACY.items()):
            with self.subTest(stable_id=stable_id):
                record = self.by_id[stable_id]
                self.assertEqual(record['kind'], 'pending')
                self.assertEqual(record['status'], 'pending')
                self.assertEqual(record['sync']['direction'], 'traceability-only')
                self.assertIsNone(record['recipe']['id'])
                self.assertEqual(record['tests'], [])
                self.assertTrue(record['contract']['compatibility_obligations'])
                self.assertEqual(record['source_state'], 'implemented')
                self.assertEqual([a['symbol'] for a in record['targets']], [symbol])
                self.assertEqual([a['path'] for a in record['targets']], [path])
                self.assertEqual([a['symbol'] for a in record['sources']], [symbol])
                self.assertEqual([a['path'] for a in record['sources']], [path])

    def test_exact_source_blob_and_existing_export_custody(self) -> None:
        for stable_id, (symbol, path, module, package) in sorted(LEGACY.items()):
            with self.subTest(stable_id=stable_id):
                source = (ROOT / path).read_bytes()
                pinned = self.by_id[stable_id]['targets'][0]['git_blob_sha1']
                self.assertEqual(pinned, git_blob(source))
                self.assertEqual(self.by_id[stable_id]['sources'][0]['git_blob_sha1'], pinned)
                simple = symbol.rsplit('.', 1)[1]
                text = source.decode('utf-8')
                self.assertRegex(text, rf'(?m)^public\s+final\s+class\s+{re.escape(simple)}\b')
                module_text = (ROOT / module).read_text(encoding='utf-8')
                self.assertRegex(module_text,
                                 rf'(?m)^\s*exports\s+{re.escape(package)}\s*;')

    def test_existing_queue_stays_queued_not_executable(self) -> None:
        self.assertGreaterEqual(len(self.plan['operations']), 4)
        for row in self.plan['operations']:
            self.assertEqual(row['state'], 'QUEUED')
            self.assertNotIn('proofs', row)
            self.assertNotIn('installer_crate', row)
        self.assertNotIn('m3jdk21.legacy-export.M3ArrayList',
                         [row['mapping_id'] for row in self.plan['operations']])
        self.assertNotIn('m3jdk21.legacy-export.M3File',
                         [row['mapping_id'] for row in self.plan['operations']])


if __name__ == '__main__':
    unittest.main()
