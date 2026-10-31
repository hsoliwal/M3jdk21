#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate the existing name-mapping authority and plan (never apply) future ports."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import re
from recipe import Refusal, checked_path, canonical, digest

KINDS = {'direct-port', 'rename', 'adapter', 'consolidation', 'specialization',
         'dependency-reuse', 'pending', 'excluded', 'target-adaptation', 'tombstone'}
STATES = {'implemented-tested', 'implemented-unverified', 'partial', 'proposed', 'blocked', 'excluded', 'tombstone'}
HEX40 = re.compile(r'[a-f0-9]{40}\Z')
HEX64 = re.compile(r'[a-f0-9]{64}\Z')

def load(path: Path) -> dict:
    def pairs(items):
        value = {}
        for k, v in items:
            if k in value: raise Refusal('duplicate key: ' + k)
            value[k] = v
        return value
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=pairs)

def closure(entries: list[dict], changed: set[str]) -> list[str]:
    """Return changed mappings and all transitive downstream dependents, even in cycles."""
    reached = set(changed)
    while True:
        next_set = reached | {e['id'] for e in entries if reached.intersection(e['dependencies'])}
        if next_set == reached: return sorted(reached)
        reached = next_set

def classify(base_source, now_source, base_target, now_target) -> str:
    """Content-level three-way disposition; not semantic conflict resolution."""
    source_changed = now_source != base_source
    target_changed = now_target != base_target
    if not source_changed and not target_changed: return 'unchanged'
    if not source_changed: return 'target-only-review'
    if not target_changed:
        return 'source-deletion-review' if now_source is None else 'source-change-review'
    if now_source == now_target: return 'converged-content-review'
    return 'CONFLICT-refuse-automatic-overwrite'

def check(manifest: dict, root: Path | None = None, inventory: dict | None = None,
          expected_source: str | None = None, complete: bool = False) -> dict:
    m = manifest.get('migration')
    if not isinstance(m, dict) or m.get('schema') != 'm3.migration/1': raise Refusal('incompatible migration schema')
    for name in ('source_baseline', 'target_baseline'):
        if not HEX40.fullmatch(m.get(name, '')): raise Refusal('invalid baseline: ' + name)
    if expected_source is not None and m['source_baseline'] != expected_source:
        raise Refusal('stale source pin')
    entries = m.get('entries', [])
    if not entries: raise Refusal('empty mapping')
    ids = [e['id'] for e in entries]
    if len(ids) != len(set(ids)): raise Refusal('duplicate mapping ID')
    known = set(ids); evidence_ids = set(m.get('evidence', {})); source_paths = set(); pending = []
    for e in entries:
        for field in ('kind', 'status', 'sources', 'destinations', 'owner', 'identity_rules',
                      'contract_differences', 'compatibility', 'formats', 'bootstrap', 'dependencies',
                      'consumers', 'materialization', 'facts_owner', 'recipe', 'tests', 'license',
                      'publication', 'last_sync', 'target_adaptations', 'conflicts', 'direction', 'rationale', 'lineage'):
            if field not in e: raise Refusal('missing mapping field: ' + e['id'] + ':' + field)
        if e['kind'] not in KINDS or e['status'] not in STATES: raise Refusal('invalid disposition')
        if e['direction'] not in ('source-to-target', 'reviewed-reverse-proposal', 'trace-only'):
            raise Refusal('automatic bidirectional mutation is not supported')
        if not set(e['dependencies']).issubset(known): raise Refusal('broken mapping dependency')
        if not set(e['tests']).issubset(evidence_ids): raise Refusal('unknown evidence reference')
        for side in ('sources', 'destinations'):
            for end in e[side]:
                for field in ('repository', 'commit', 'module', 'path', 'symbol', 'signature_kind', 'signatures', 'sha256', 'inspection'):
                    if field not in end: raise Refusal('missing endpoint field: ' + field)
                checked_path(Path('.'), end['path'])
                if end['commit'] is not None and not HEX40.fullmatch(end['commit']): raise Refusal('invalid endpoint commit')
                if end['sha256'] is not None and not HEX64.fullmatch(end['sha256']): raise Refusal('invalid endpoint digest')
                if side == 'sources': source_paths.add(end['path'])
                if root is not None and side == 'destinations' and end['sha256'] is not None:
                    path = checked_path(root, end['path'])
                    if not path.is_file() or digest(path.read_bytes()) != end['sha256']:
                        raise Refusal('broken/divergent destination: ' + end['path'])
        if e['status'] == 'implemented-tested':
            if not e['sources'] or not e['destinations'] or not e['tests']: raise Refusal('port claim without endpoints/evidence')
            if any(x['commit'] is None or x['sha256'] is None or not x['signatures']
                   for x in e['sources'] + e['destinations']): raise Refusal('port claim without exact pins/contracts')
            for test in e['tests']:
                record = m['evidence'][test]
                if record['status'] != 'PASS' or record['candidate_scope_sha256'] != m['candidate_scope_sha256']:
                    raise Refusal('borrowed/stale/failed test evidence')
        if e['status'] not in ('implemented-tested', 'excluded', 'tombstone'): pending.append(e['id'])
        if e['status'] in ('excluded', 'tombstone') and not e['rationale']: raise Refusal('unexplained exclusion/deletion')
        if e['kind'] == 'tombstone' and not e['lineage']: raise Refusal('deletion lost its lineage')
    if root is not None:
        actual_scope = {}
        for relative, expected in m['scope_files'].items():
            path = checked_path(root, relative)
            if not path.is_file() or digest(path.read_bytes()) != expected:
                raise Refusal('candidate scope drift: ' + relative)
            actual_scope[relative] = expected
        if digest(canonical(actual_scope)) != m['candidate_scope_sha256']:
            raise Refusal('candidate scope root mismatch')
        for record in m.get('evidence', {}).values():
            path = checked_path(root, record['path'])
            if not path.is_file() or digest(path.read_bytes()) != record['sha256']:
                raise Refusal('evidence bytes missing/drifted: ' + record['path'])
    if inventory is not None:
        if inventory.get('commit') != m['source_baseline']: raise Refusal('inventory source pin differs')
        missing = set(inventory.get('candidate_paths', [])) - source_paths
        if missing: raise Refusal('unmapped candidate additions: ' + ','.join(sorted(missing)))
    open_gates = sorted(k for k, v in m['gates'].items() if v != 'PASS')
    if complete and (pending or open_gates or not m['inventory_exhaustive']):
        raise Refusal('completion refused: unresolved mappings, inventory or acceptance gates')
    return {'schema': m['schema'], 'mappings': len(entries), 'pending': pending,
            'inventory_exhaustive': m['inventory_exhaustive'], 'open_gates': open_gates,
            'completion': 'NOT_COMPLETE' if pending or open_gates or not m['inventory_exhaustive'] else 'ELIGIBLE_FOR_REVIEW'}

def plan(manifest: dict, observations: dict) -> dict:
    entries = manifest['migration']['entries']; decisions = []
    for e in entries:
        for index, endpoint in enumerate(e['sources']):
            key = e['id'] + ':' + str(index)
            if key not in observations: continue
            obs = observations[key]
            base_target = e['last_sync'].get('target_hashes', [])
            if len(e['destinations']) != 1 or len(base_target) != 1:
                decisions.append({'id': e['id'], 'decision': 'multi-endpoint-review', 'writes': 0}); continue
            decisions.append({'id': e['id'], 'decision': classify(endpoint['sha256'], obs.get('source_sha256'),
                                base_target[0], obs.get('target_sha256')), 'writes': 0})
    affected = closure(entries, {x['id'] for x in decisions if x['decision'] != 'unchanged'})
    return {'decisions': decisions, 'dependency_closure': affected, 'writes': 0,
            'semantic_api_format_performance_classification': 'REQUIRES_REVIEW', 'automatic_reverse_port': False}

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('mode', choices=('validate', 'plan', 'coverage'))
    p.add_argument('--manifest', type=Path, required=True);p.add_argument('--root', type=Path)
    p.add_argument('--inventory', type=Path);p.add_argument('--source-pin');p.add_argument('--complete', action='store_true')
    p.add_argument('--observations', type=Path)
    args = p.parse_args()
    try:
        manifest = load(args.manifest)
        result = check(manifest, args.root, load(args.inventory) if args.inventory else None, args.source_pin, args.complete)
        if args.mode == 'plan':
            if args.observations is None: raise Refusal('plan requires explicit observations')
            result = plan(manifest, load(args.observations))
        print(json.dumps(result, sort_keys=True, indent=2))
    except (Refusal, OSError, KeyError, TypeError, ValueError) as error:
        p.exit(2, 'REFUSED: ' + str(error) + '\n')
