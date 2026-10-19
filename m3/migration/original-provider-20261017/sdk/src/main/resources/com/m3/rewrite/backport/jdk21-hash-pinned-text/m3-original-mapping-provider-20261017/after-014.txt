#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Validate the existing naming authority and reconcile pinned inventory evidence.

Consumes Synexia InventoryWriter's TSV; it is not a replacement inventory scanner,
compiler, equivalence prover, signature verifier, or automatic conflict resolver.
No private source is fetched or published. Python remains tooling-only.
"""
import argparse
import csv
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import sys

from jsonschema import Draft202012Validator

SCHEMA_PATH = Path(__file__).resolve().parents[1] / "docs/name-mapping.schema.json"

SHA = re.compile(r'^[0-9a-f]{64}$')
COMMIT = re.compile(r'^[0-9a-f]{40}$')
KINDS = {'direct-port', 'rename', 'adapter', 'consolidation', 'specialization',
         'dependency-reuse', 'pending', 'excluded', 'tombstone'}
STATUSES = {'implemented-tested', 'implemented-unverified', 'pending', 'blocked', 'excluded', 'tombstone'}
REQUIRED_RECORD = {'id', 'kind', 'status', 'reason', 'sources', 'targets', 'owner', 'identity',
    'contract', 'format', 'dependencies', 'materialization', 'recipe', 'tests', 'provenance', 'sync', 'lineage'}
MAX_JSON = 16 * 1024 * 1024


def digest(data):
    return hashlib.sha256(data).hexdigest()


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('duplicate JSON key: ' + key)
        result[key] = value
    return result


def parse_json(data):
    if len(data) > MAX_JSON:
        raise ValueError('JSON byte budget exceeded')
    def invalid_constant(value):
        raise ValueError('nonfinite JSON number: ' + value)
    return json.loads(data, object_pairs_hook=unique_object, parse_constant=invalid_constant)


def load_json(path):
    path = Path(path)
    if path.is_symlink() or not path.is_file() or path.stat().st_size > MAX_JSON:
        raise ValueError('unsafe, missing or oversized JSON: ' + str(path))
    return parse_json(path.read_bytes())


def relative_path(value):
    if not isinstance(value, str) or not value or '\\' in value or any(ord(c) < 32 for c in value):
        raise ValueError('invalid relative path')
    path = PurePosixPath(value)
    if path.is_absolute() or any(p in ('.', '..', '') for p in value.split('/')) or ':' in value:
        raise ValueError('unsafe relative path: ' + value)
    return path


def safe_path(root, relative):
    relative_path(relative)
    current = Path(root).absolute()
    if current.is_symlink():
        raise ValueError('symlink root')
    for part in PurePosixPath(relative).parts:
        current = current / part
        if current.is_symlink():
            raise ValueError('symlink path: ' + relative)
    return current


def file_hash(root, relative):
    path = safe_path(root, relative)
    if not path.is_file():
        raise ValueError('missing file: ' + relative)
    hasher = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(65536), b''):
            hasher.update(block)
    return hasher.hexdigest()


def _text(value):
    return isinstance(value, str) and bool(value.strip())


def _hex(pattern, value):
    return isinstance(value, str) and bool(pattern.fullmatch(value))


def _location(ref):
    return (ref.get('repo'), ref.get('path'), ref.get('symbol'))


def validate(document, root, previous=None):
    """Structural and exact-artifact evidence validation; never implies completion."""
    schema = load_json(SCHEMA_PATH)
    Draft202012Validator.check_schema(schema)
    schema_errors = list(Draft202012Validator(schema).iter_errors(document))
    if schema_errors:
        return ['schema ' + '/'.join(str(p) for p in error.absolute_path) + ': ' + error.message
                for error in schema_errors]
    errors = []
    def fail(message):
        errors.append(message)
    if not isinstance(document, dict) or document.get('schema') != 1 or not isinstance(document.get('mappings'), list):
        return ['legacy name-mapping schema must be retained']
    migration = document.get('migration')
    if not isinstance(migration, dict) or migration.get('version') != 1:
        return ['unsupported migration extension version']
    for role in ('source', 'target'):
        repository = migration.get(role, {})
        if not isinstance(repository, dict) or not _hex(COMMIT, repository.get('baseline_commit')):
            fail(role + ' baseline commit must be pinned')
        if not _text(repository.get('repo')):
            fail(role + ' repository required')
    coverage = migration.get('coverage', {})
    for flag in ('source_tree_complete', 'dependency_closure_complete'):
        if type(coverage.get(flag)) is not bool:
            fail('coverage boolean required: ' + flag)
    if not isinstance(migration.get('gates'), list):
        fail('acceptance gates required')
    records = migration.get('records')
    if not isinstance(records, list):
        return errors + ['records must be an array']
    ids = set()
    target_owners = {}
    receipt_cache = {}
    for record in records:
        if not isinstance(record, dict):
            fail('invalid record'); continue
        rid = record.get('id', '<missing>')
        if not _text(rid) or rid in ids:
            fail('duplicate or invalid mapping id: ' + str(rid))
        ids.add(str(rid))
        prefix = str(rid) + ': '
        missing = REQUIRED_RECORD - record.keys()
        if missing:
            fail(prefix + 'missing fields ' + ','.join(sorted(missing))); continue
        if record['kind'] not in KINDS or record['status'] not in STATUSES:
            fail(prefix + 'unsupported kind/status')
        if not _text(record['reason']) or not _text(record['owner']):
            fail(prefix + 'reason and owner required')
        for key in ('identity', 'contract', 'format', 'materialization', 'recipe', 'provenance', 'sync', 'lineage'):
            if not isinstance(record[key], dict):
                fail(prefix + key + ' must be an object')
        if not all(isinstance(record[key], dict) for key in ('recipe', 'sync', 'lineage')):
            continue
        if record['sync'].get('direction') not in {'source-to-target', 'target-to-source-proposal', 'traceability-only'}:
            fail(prefix + 'unsafe port direction')
        tested = record['status'] == 'implemented-tested'
        implemented = record['status'] in {'implemented-tested', 'implemented-unverified'}
        for role in ('sources', 'targets'):
            refs = record[role]
            if not isinstance(refs, list):
                fail(prefix + role + ' must be an array'); continue
            if implemented and not refs:
                fail(prefix + role + ' required for implementation claim')
            for ref in refs:
                if not isinstance(ref, dict):
                    fail(prefix + 'invalid artifact reference'); continue
                if ref.get('commit') is not None and not _hex(COMMIT, ref['commit']):
                    fail(prefix + 'invalid artifact commit')
                if ref.get('revision_role') == 'pinned' and not _hex(COMMIT, ref.get('commit')):
                    fail(prefix + 'pinned artifact needs commit')
                if role == 'sources' and implemented and not _hex(COMMIT, ref.get('commit')):
                    fail(prefix + 'implemented source commit required')
                path = ref.get('path')
                if path is not None:
                    try: relative_path(path)
                    except ValueError as failure: fail(prefix + str(failure))
                if ref.get('sha256') is not None and not _hex(SHA, ref['sha256']):
                    fail(prefix + 'invalid content hash')
                if implemented and (not path or not _hex(SHA, ref.get('sha256'))
                        or not _text(ref.get('symbol')) or not ref.get('signatures')):
                    fail(prefix + 'implementation requires path/hash/symbol/signatures')
                if role == 'targets' and path:
                    previous_owner = target_owners.setdefault((ref.get('repo'), path), record['owner'])
                    if previous_owner != record['owner']:
                        fail(prefix + 'conflicting owner for target ' + path)
                    if implemented and _hex(SHA, ref.get('sha256')):
                        try:
                            if file_hash(root, path) != ref['sha256']:
                                fail(prefix + 'target hash drift: ' + path)
                        except (OSError, ValueError) as failure: fail(prefix + str(failure))
        recipe = record['recipe']
        if recipe.get('path') is not None and implemented:
            try:
                if not _hex(SHA, recipe.get('sha256')) or file_hash(root, recipe['path']) != recipe['sha256']:
                    fail(prefix + 'recipe hash drift')
            except (OSError, ValueError) as failure: fail(prefix + str(failure))
        if tested:
            runs = []
            if not isinstance(record['tests'], list) or not record['tests']:
                fail(prefix + 'test evidence required')
            else:
                for test in record['tests']:
                    try:
                        receipt_path = test['receipt']
                        if receipt_path not in receipt_cache:
                            receipt_cache[receipt_path] = load_json(safe_path(root, receipt_path))
                        receipt = receipt_cache[receipt_path]
                        shape_errors = list(Draft202012Validator(schema['$defs']['receipt']).iter_errors(receipt))
                        if shape_errors:
                            raise ValueError('invalid evidence receipt shape: ' + shape_errors[0].message)
                        matches = [run for run in receipt.get('runs', []) if run.get('id') == test['id']]
                        if len(matches) != 1:
                            raise ValueError('missing/duplicate evidence test id')
                        run = matches[0]
                        if run.get('status') != 'passed' or type(run.get('exit_code')) is not int or run['exit_code'] != 0:
                            raise ValueError('failed or unexecuted evidence')
                        if not run.get('command') or not _hex(SHA, run.get('stdout_sha256')):
                            raise ValueError('incomplete evidence command/output')
                        inputs = run.get('inputs', {})
                        if not inputs:
                            raise ValueError('evidence input closure missing')
                        for path, expected in inputs.items():
                            if not _hex(SHA, expected) or file_hash(root, path) != expected:
                                raise ValueError('evidence input hash drift: ' + path)
                        if run.get('stdout_path') and file_hash(root, run['stdout_path']) != run['stdout_sha256']:
                            raise ValueError('evidence output hash drift')
                        runs.append(run)
                    except (OSError, ValueError, KeyError, TypeError) as failure:
                        fail(prefix + 'evidence: ' + str(failure))
                for target in record['targets']:
                    if not any(run.get('inputs', {}).get(target.get('path')) == target.get('sha256') for run in runs):
                        fail(prefix + 'no exact-candidate evidence for target ' + str(target.get('path')))
    if previous:
        old_records = previous.get('migration', {}).get('records', [])
        current = {record.get('id'): record for record in records if isinstance(record, dict)}
        for old in old_records:
            rid = old['id']
            if rid not in current:
                fail('lineage lost: retain mapping/tombstone ' + rid); continue
            new = current[rid]
            for role in ('sources', 'targets'):
                old_locations = {_location(ref) for ref in old.get(role, [])}
                new_locations = {_location(ref) for ref in new.get(role, [])}
                preserved = {_location(ref) for ref in new.get('lineage', {}).get('previous_' + role, [])}
                if old_locations - new_locations - preserved:
                    fail('lineage missing for moved/split/merged ' + rid + ' ' + role)
    return errors


def completion_errors(document):
    migration = document['migration']
    coverage = migration['coverage']
    errors = []
    if not coverage.get('source_tree_complete'):
        errors.append('full source tree inventory is incomplete')
    if not coverage.get('dependency_closure_complete'):
        errors.append('semantic dependency/ownership closure is incomplete')
    if not coverage.get('inventory_receipt'):
        errors.append('pinned exhaustive inventory receipt is absent')
    if coverage.get('uninspected_domains'):
        errors.append('uninspected domains remain')
    for record in migration['records']:
        if record['status'] not in {'implemented-tested', 'excluded', 'tombstone'}:
            errors.append(record['id'] + ': ' + record['status'])
        if record.get('sync', {}).get('pending') or record.get('sync', {}).get('conflicts'):
            errors.append(record['id'] + ': synchronization gaps remain')
        for dependency in record.get('dependencies', []):
            if dependency.get('resolution') != 'resolved':
                errors.append(record['id'] + ': unresolved dependency')
    for gate in migration['gates']:
        if gate.get('status') not in {'passed', 'not-applicable'}:
            errors.append(gate['id'] + ': ' + gate.get('status', 'missing'))
    source = migration['source']
    if source.get('observed_tip') and source['observed_tip'] != source['baseline_commit']:
        errors.append('source default-branch tip changed since baseline; reconcile rather than silently repin')
    return errors


def read_inventory(path, receipt_path):
    """Read the existing InventoryWriter TSV, preserving every row regardless of name."""
    path = Path(path)
    receipt = load_json(receipt_path)
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 256 * 1024 * 1024:
        raise ValueError('unsafe, missing or oversized inventory')
    content = path.read_bytes()
    if not _hex(COMMIT, receipt.get('commit')) or digest(content) != receipt.get('inventory_sha256'):
        raise ValueError('inventory pin mismatch')
    if type(receipt.get('scope_complete')) is not bool or not _text(receipt.get('repo')):
        raise ValueError('inventory scope/repository receipt required')
    reader = csv.DictReader(io.StringIO(content.decode('utf-8')), delimiter='\t')
    if not reader.fieldnames or len(reader.fieldnames) != len(set(reader.fieldnames)) or not {'path', 'sha256'} <= set(reader.fieldnames):
        raise ValueError('unsupported or ambiguous InventoryWriter TSV header')
    rows = {}
    for row in reader:
        relative_path(row['path'])
        if row['path'] in rows or not _hex(SHA, row.get('sha256')) or None in row:
            raise ValueError('duplicate/invalid inventory row')
        rows[row['path']] = row
    return {'receipt': receipt, 'rows': rows}


def reconcile(document, inventory, target_root):
    """Three-way REVIEW decisions only. Never writes source, targets or mappings."""
    rows, receipt = inventory['rows'], inventory['receipt']
    mapped = set()
    output = []
    for record in document['migration']['records']:
        sources = [ref for ref in record['sources'] if ref.get('repo') == receipt['repo'] and ref.get('path')]
        comparable = [ref for ref in sources if ref.get('tracking_ref') == receipt.get('tracking_ref')]
        mapped.update(ref['path'] for ref in comparable)
        missing = [ref['path'] for ref in comparable if ref['path'] not in rows]
        source_changed = any(ref['path'] in rows and rows[ref['path']]['sha256'] != ref.get('sha256') for ref in comparable)
        target_changed = False
        for ref in record['targets']:
            if ref.get('path') and ref.get('sha256'):
                try: current = file_hash(target_root, ref['path'])
                except (OSError, ValueError): current = None
                target_changed |= current != ref['sha256']
        if not comparable or (missing and not receipt['scope_complete']):
            decision = 'source-unobserved'
        elif missing:
            decision = 'three-way-conflict' if target_changed else 'source-deletion-review'
        elif source_changed and target_changed:
            decision = 'three-way-conflict'
        elif source_changed:
            decision = 'source-change-review'
        elif target_changed:
            decision = 'target-only-adaptation'
        else:
            decision = 'unchanged'
        output.append({'id': record['id'], 'decision': decision, 'unobserved_paths': missing,
                       'automatic_replay_allowed': False})
    return {'source_commit': receipt['commit'], 'source_scope_complete': receipt['scope_complete'],
            'semantic_equivalence_proved': False, 'records': output,
            'unmapped_paths': sorted(set(rows) - mapped)}


def extend_document(original, extension, expected_sha256):
    """One-time additive extension recipe; never overwrites an existing differing migration."""
    if not _hex(SHA, expected_sha256) or digest(original) != expected_sha256:
        raise ValueError('original naming manifest drift')
    document = parse_json(original)
    if document.get('schema') != 1 or not isinstance(document.get('mappings'), list):
        raise ValueError('unsupported legacy naming manifest')
    if not isinstance(extension, dict) or extension.get('version') != 1:
        raise ValueError('unsupported extension')
    if 'migration' in document:
        if document['migration'] != extension:
            raise ValueError('existing migration differs; use reviewed three-way reconciliation')
        return original
    document['migration'] = extension
    errors = list(Draft202012Validator(load_json(SCHEMA_PATH)).iter_errors(document))
    if errors:
        raise ValueError('extension schema: ' + errors[0].message)
    return (json.dumps(document, ensure_ascii=True, indent=2) + '\n').encode('utf-8')


def write_output(path, content):
    """Create a review artifact; an identical existing artifact is an idempotent no-op."""
    path = Path(path).absolute()
    for component in [path, *path.parents]:
        if component.is_symlink():
            raise ValueError('symlink output component')
    if path.exists():
        if not path.is_file() or path.read_bytes() != content:
            raise ValueError('refusing modified output: ' + str(path))
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as output:
        output.write(content)
    return True


def render_report(document):
    migration = document['migration']
    incomplete = completion_errors(document)
    lines = ['# MIndex-to-M3 migration coverage', '', '**' + ('INCOMPLETE' if incomplete else 'COMPLETE') + '**', '',
        'This is the current observed mapping coverage, not an exhaustive source inventory.', '',
        'Source baseline: `' + migration['source']['baseline_commit'] + '`.',
        'Target baseline: `' + migration['target']['baseline_commit'] + '`.', '',
        '| Stable ID | Mapping | State | Canonical responsibility | Reason / limit |',
        '|---|---|---|---|---|']
    clean = lambda text: str(text).replace('|', '\\|').replace('\n', ' ')
    for record in sorted(migration['records'], key=lambda r: r['id']):
        lines.append('| ' + ' | '.join(clean(record[key]) for key in ('id', 'kind', 'status', 'owner', 'reason')) + ' |')
    lines += ['', '## Mandatory gates', '', '| Gate | State | Evidence or blocker |', '|---|---|---|']
    for gate in migration['gates']:
        lines.append('| ' + ' | '.join(clean(gate.get(key, '')) for key in ('id', 'status', 'reason')) + ' |')
    lines += ['', '## Completion blockers', ''] + ['- ' + clean(error) for error in incomplete]
    lines += ['', 'Source-only changes require recipe review. Target-only adaptations are retained. Both-side changes are conflicts.',
        'A missing row in an incomplete or different-branch inventory is unobserved, not a deletion.',
        'No automatic reverse-port, merge, or runtime acceptance is authorized by this report.', '']
    return '\n'.join(lines)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('validate', 'complete', 'report', 'reconcile', 'extend'))
    parser.add_argument('root', type=Path, nargs='?', default=Path('.'))
    for name in ('manifest', 'previous', 'inventory', 'inventory-receipt', 'output', 'base', 'extension'):
        parser.add_argument('--' + name, type=Path)
    parser.add_argument('--expected-sha256')
    args = parser.parse_args(argv)
    try:
        if args.command == 'extend':
            if not all((args.base, args.extension, args.output, args.expected_sha256)):
                parser.error('extend needs --base --extension --output --expected-sha256')
            extension = load_json(args.extension)
            # The canonical naming authority itself is a replayable postimage; no second registry.
            if extension.get('schema') == 1 and 'migration' in extension:
                extension = extension['migration']
            content = extend_document(args.base.read_bytes(), extension, args.expected_sha256)
            write_output(args.output, content)
            print('MANIFEST_EXTENSION_RECIPE_PASS source_unchanged=true output_sha256=' + digest(content))
            return 0
        manifest = args.manifest or args.root / 'm3/docs/name-mapping.json'
        document = load_json(manifest)
        previous = load_json(args.previous) if args.previous else None
        errors = validate(document, args.root, previous)
        if errors:
            print('\n'.join(errors), file=sys.stderr)
            return 1
        if args.command in ('validate', 'complete'):
            blockers = completion_errors(document)
            print('MIGRATION_MANIFEST_VALID completion=' + ('INCOMPLETE' if blockers else 'COMPLETE'))
            if args.command == 'complete' and blockers:
                print('\n'.join(blockers), file=sys.stderr)
                return 2
            return 0
        if args.command == 'reconcile':
            if not args.inventory or not args.inventory_receipt:
                parser.error('reconcile needs --inventory and --inventory-receipt')
            result = reconcile(document, read_inventory(args.inventory, args.inventory_receipt), args.root)
            content = (json.dumps(result, indent=2, sort_keys=True) + '\n').encode()
        else:
            content = render_report(document).encode()
        if args.output:
            write_output(args.output, content)
        else:
            sys.stdout.write(content.decode())
        return 0
    except (OSError, ValueError, TypeError, KeyError) as failure:
        print('MIGRATION_REFUSED: ' + str(failure), file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
