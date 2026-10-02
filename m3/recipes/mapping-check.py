#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Wrap the established migration authority with selected public endpoint coverage."""
import argparse
from collections import Counter
import importlib.util
import json
from pathlib import Path
import re

import apply as recipe

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('selected_text_port', Path(__file__).with_name('text-port.py'))
selected = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selected)
owner = selected.owner
SCOPE = 'selected Route A closure and bounded opt-in Route B'


def validate(root=ROOT, source=None):
    root = Path(root)
    mapping = selected.load(root, 'm3/docs/name-mapping.json')
    errors = owner.validate(mapping, root)
    if errors:
        raise ValueError('migration authority: ' + '; '.join(errors))
    extension = mapping.get('selected_ports', {})
    if extension.get('version') != 1:
        raise ValueError('selected scope extension version drift')
    all_records = {row['id']: row for row in mapping['migration']['records']}
    selected_ids = extension.get('record_ids', [])
    if not selected_ids or len(selected_ids) != len(set(selected_ids)):
        raise ValueError('duplicate or missing selected mapping identity')
    if any(rid not in all_records for rid in selected_ids):
        raise ValueError('unresolved selected mapping identity')
    records = [all_records[rid] for rid in selected_ids]
    destinations = {}
    inventory = extension['source_inventory_ref']
    source_pin = inventory['commit']
    for row in records:
        for ancestor in row['lineage']['supersedes']:
            if ancestor not in all_records or ancestor == row['id']:
                raise ValueError('unresolved lineage: ' + ancestor)
        for endpoint in row['sources']:
            if endpoint['repo'] == inventory['repository'] and endpoint['commit'] != source_pin:
                raise ValueError('stale selected source pin: ' + endpoint['path'])
        for endpoint in row['targets']:
            path = endpoint['path']
            if path in destinations:
                raise ValueError('duplicate selected destination: ' + path)
            destinations[path] = row
            if path.endswith('.java'):
                # The existing authority already verifies exact artifact hashes and
                # executed receipt input closures. This is only a declaration check.
                package, _, name = endpoint['symbol'].rpartition('.')
                text = recipe.safe_path(root, path).read_text(encoding='utf-8')
                if (not re.search(r'\bpackage\s+' + re.escape(package) + r'\s*;', text) or
                        not re.search(r'\b(?:class|interface|enum|record)\s+' + re.escape(name) + r'\b', text)):
                    raise ValueError('broken declared symbol: ' + endpoint['symbol'])
    java = {path.relative_to(root).as_posix()
            for folder in extension['coverage_roots']
            for path in recipe.safe_path(root, folder).rglob('*.java')}
    missing = java - destinations.keys()
    if missing:
        raise ValueError('unmapped target addition: ' + ', '.join(sorted(missing)))
    selected.validate(root, source)
    return {'scope': SCOPE, 'covered_java_files': len(java),
            'mapping_count': len(records), 'all_migration_records': len(all_records),
            'statuses': dict(Counter(row['status'] for row in records)),
            'whole_family_pending': inventory['pending_item_count'],
            'source_pin': source_pin, 'whole_family_accepted': False}


def report(result):
    lines = ['# Generated migration coverage', '',
             'Scope: ' + result['scope'] + '. Whole-family acceptance remains open.', '',
             '| Measure | Count |', '| --- | ---: |',
             '| Covered selected target Java files | ' + str(result['covered_java_files']) + ' |',
             '| Permanent selected migration records | ' + str(result['mapping_count']) + ' |',
             '| All records in the naming authority | ' + str(result['all_migration_records']) + ' |',
             '| Private inventory items still pending | ' + str(result['whole_family_pending']) + ' |', '',
             'Historical selected source pin: `' + result['source_pin'] + '`.', '',
             'The existing `m3/migration/migration.py` remains the schema, artifact and receipt '
             'validator for all migration records. This wrapper adds selected endpoint declarations, '
             'coverage and historical source pin checks; it does not prove complete ABI parity '
             'or runtime/compiler acceptance. The private inventory remains authoritative for '
             'undispatched families and exclusions.', '']
    return '\n'.join(lines)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    result = validate(source=args.source_root)
    if args.report:
        args.report.write_text(report(result), encoding='utf-8', newline='\n')
    print(json.dumps(result, sort_keys=True))
