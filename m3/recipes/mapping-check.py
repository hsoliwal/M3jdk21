#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Validate the existing permanent map and report its explicit public coverage scope."""
import argparse
from collections import Counter
import hashlib
import importlib.util
import json
from pathlib import Path
import re

from jsonschema import Draft202012Validator
import apply as recipe

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('selected_text_port', Path(__file__).with_name('text-port.py'))
selected = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selected)


def validate(root=ROOT, source=None):
    root = Path(root)
    mapping = selected.load(root, 'm3/docs/name-mapping.json')
    schema = selected.load(root, 'm3/docs/name-mapping.schema.json')
    Draft202012Validator.check_schema(schema)
    errors = sorted(Draft202012Validator(schema).iter_errors(mapping), key=lambda error: str(error.path))
    if errors:
        raise ValueError('mapping schema: ' + errors[0].message)
    ids = [row['mapping_id'] for row in mapping['mappings']]
    if len(ids) != len(set(ids)):
        raise ValueError('duplicate mapping identity')
    destinations = {}
    source_pin = mapping['source_inventory_ref']['commit']
    for row in mapping['mappings']:
        for ancestor in row.get('upstream_mapping_ids', []):
            if ancestor not in ids or ancestor == row['mapping_id']:
                raise ValueError('unresolved lineage: ' + ancestor)
        for endpoint in row['sources']:
            if endpoint['repository'] == mapping['source_inventory_ref']['repository'] and endpoint['commit'] != source_pin:
                raise ValueError('stale source pin: ' + endpoint['path'])
        for endpoint in row['destinations']:
            path = endpoint['path']
            if path in destinations:
                raise ValueError('duplicate destination: ' + path)
            destinations[path] = row
            actual = recipe.safe_path(root, path).read_bytes()
            if hashlib.sha256(actual).hexdigest() != endpoint['sha256']:
                raise ValueError('target divergence: ' + path)
            if path.endswith('.java'):
                # This checks the declared top-level endpoint only. Javac/differential
                # receipts provide behavioral evidence; a matching name cannot do so.
                package, _, name = endpoint['symbol'].rpartition('.')
                text = actual.decode('utf-8')
                if (not re.search(r'\bpackage\s+' + re.escape(package) + r'\s*;', text) or
                        not re.search(r'\b(?:class|interface|enum|record)\s+' + re.escape(name) + r'\b', text)):
                    raise ValueError('broken declared symbol: ' + endpoint['symbol'])
            if row['status'].startswith('implemented_tested'):
                if not row['tests']['evidence']:
                    raise ValueError('claimed port without evidence: ' + path)
                for evidence in row['tests']['evidence']:
                    proof = selected.load(root, evidence)
                    relative = path.removeprefix('m3/ports/text/')
                    if (proof.get('source_hashes', {}).get(relative) != endpoint['sha256'] or
                            not proof.get('records') or any(run.get('exit') != 0 for run in proof['records'])):
                        raise ValueError('unbound candidate evidence: ' + path)
    java = {path.relative_to(root).as_posix()
            for folder in mapping['coverage_roots']
            for path in recipe.safe_path(root, folder).rglob('*.java')}
    missing = java - destinations.keys()
    if missing:
        raise ValueError('unmapped target addition: ' + ', '.join(sorted(missing)))
    selected.validate(root, source)
    return {'scope': 'selected Route A closure', 'covered_java_files': len(java),
            'mapping_count': len(ids), 'statuses': dict(Counter(row['status'] for row in mapping['mappings'])),
            'whole_family_pending': mapping['source_inventory_ref']['pending_item_count'],
            'source_pin': source_pin, 'whole_family_accepted': False}


def report(result):
    lines = ['# Generated migration coverage', '',
             'Scope: ' + result['scope'] + '. Whole-family acceptance remains open.', '',
             '| Measure | Count |', '| --- | ---: |',
             '| Covered target Java files | ' + str(result['covered_java_files']) + ' |',
             '| Permanent selected mappings | ' + str(result['mapping_count']) + ' |',
             '| Private inventory items still pending | ' + str(result['whole_family_pending']) + ' |', '',
             'Source pin: `' + result['source_pin'] + '`.', '',
             'The private inventory remains authoritative for undispatched families and exclusions. '
             'This validator checks selected endpoints, hashes, schema, lineage references and candidate receipts; '
             'it does not prove ABI parity or runtime/compiler acceptance.', '']
    return '\n'.join(lines)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-root', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    result = validate(source=args.source_root)
    if args.report:
        args.report.write_text(report(result), encoding='utf-8')
    print(json.dumps(result, sort_keys=True))
