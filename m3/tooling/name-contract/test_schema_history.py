# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed historic naming vocabulary exception tied to one stable ID."""
from __future__ import annotations

import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]
HISTORICAL_ID = 'm3.collections.evicting-queue.owner-naming'
HISTORICAL = {
    'kind': 'internal-relocation-proposal',
    'status': 'proposed',
    'direction': 'same-repository internal name review',
    'source_state': 'implemented-proving-module-unpromoted',
}


def examine(mapping: dict, schema: dict) -> None:
    record_schema = schema['$defs']['record']
    conditionals = record_schema['allOf']
    if len(conditionals) != 1:
        raise ValueError('historical exception must be exactly one scoped condition')
    gate = conditionals[0]
    if (gate['if']['properties']['id']['const'] != HISTORICAL_ID
            or gate['if']['required'] != ['id']):
        raise ValueError('historical exception is no longer bound to stable ID')
    fields = ('kind', 'status', 'direction', 'source_state')
    rows = mapping['migration']['records']
    if len({row['id'] for row in rows}) != len(rows):
        raise ValueError('duplicate stable migration IDs')
    matched = [row for row in rows if row['id'] == HISTORICAL_ID]
    if len(matched) != 1:
        raise ValueError('missing/ambiguous historical migration record')
    for key in fields:
        expected = HISTORICAL[key]
        current = (matched[0]['sync']['direction'] if key == 'direction'
                   else matched[0][key])
        if key == 'direction':
            allowed = gate['else']['properties']['sync']['properties']['direction']['enum']
            constrained = gate['then']['properties']['sync']['properties']['direction']['const']
        else:
            allowed = gate['else']['properties'][key]['enum']
            constrained = gate['then']['properties'][key]['const']
        if current != expected or constrained != expected or expected in allowed:
            raise ValueError('historical meaning or exception scope changed: ' + key)
        for row in rows:
            if row['id'] == HISTORICAL_ID:
                continue
            actual = row['sync']['direction'] if key == 'direction' else row[key]
            if actual not in allowed:
                raise ValueError('historical-only value leaked to migration: ' + row['id'])


class HistoricalSchemaTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.schema = json.loads((ROOT / 'm3/docs/name-mapping.schema.json').read_text(encoding='utf-8'))
        cls.mapping = json.loads((ROOT / 'm3/docs/name-mapping.json').read_text(encoding='utf-8'))

    def test_historical_exception_is_narrow_and_consistent(self) -> None:
        examine(self.mapping, self.schema)

    def test_unrelated_record_cannot_reuse_historical_status(self) -> None:
        from copy import deepcopy
        modified = deepcopy(self.mapping)
        other = next(r for r in modified['migration']['records'] if r['id'] != HISTORICAL_ID)
        other['status'] = HISTORICAL['status']
        with self.assertRaisesRegex(ValueError, 'historical-only value leaked'):
            examine(modified, self.schema)

    def test_missing_historical_record_is_refused(self) -> None:
        from copy import deepcopy
        modified = deepcopy(self.mapping)
        modified['migration']['records'] = [
            r for r in modified['migration']['records'] if r['id'] != HISTORICAL_ID]
        with self.assertRaisesRegex(ValueError, 'missing/ambiguous'):
            examine(modified, self.schema)


if __name__ == '__main__':
    unittest.main()
