#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only, fail-closed M3 source-ledger review queue. NEVER admits receipts."""
import argparse
import csv
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import sys

EXTRA = {
    'MatAdaptiveIntIntMap.java': 'AdaptiveIntToIntMap.java',
    'MatAdaptiveLongLongMap.java': 'AdaptiveLongToLongMap.java',
    'MatIdIdMap.java': 'IdToIdMap.java',
    'M3ArrayStack.java': 'ArrayStack.java',
    'M3NodeCachingLinkedList.java': 'NodeCachingLinkedList.java',
    'M3TreeList.java': 'TreeList.java',
    'M3CountedCatalogue.java': 'CountedCatalogue.java',
}
COLUMNS = ('review_id', 'category', 'status', 'historical_path', 'candidate_path',
           'historical_sha256', 'current_sha256', 'source_scope', 'required_gate')
GATE = 'manual source-history + license + structural/logic hashes + API + Java21 build + tests + recipe'


def require(ok, reason):
    if not ok:
        raise ValueError('HOLD: ' + reason)


def identity(path):
    require(isinstance(path, str) and path.startswith('m3/collections/')
            and path.endswith('.java'), 'bad path identity')
    p = PurePosixPath(path)
    require(p.as_posix() == path and '..' not in p.parts and '\\' not in path
            and ('/src/main/' in path or '/src/test/' in path), 'unsafe Java path: ' + path)
    return path


def digest(value):
    require(isinstance(value, str) and re.fullmatch('[0-9a-f]{64}', value) is not None,
            'invalid SHA-256')
    return value


def source_paths(paths, kind):
    require(isinstance(paths, list), 'bad ' + kind + ' array')
    ids = [identity(x) for x in paths]
    require(len(ids) == len(set(ids)), 'duplicate ' + kind)
    return set(ids)


def receipts(rows, keys, name):
    require(isinstance(rows, list), 'bad ' + name)
    values = {}
    for row in rows:
        require(isinstance(row, dict) and set(row) == set(keys), 'malformed ' + name)
        p = identity(row['path'])
        require(p not in values, 'duplicate receipt ' + name + ' ' + p)
        for key in keys:
            if key != 'path':
                digest(row[key])
        values[p] = row
    return values


def review(doc, head):
    require(re.fullmatch('[0-9a-f]{40}', head) is not None, 'bad checkout SHA')
    require(doc.get('status') == 'HOLD', 'inventory must be HOLD')
    digest(doc['source_manifest_sha256'])
    steps = doc['source_receipt_chain']
    require(isinstance(steps, list) and len(steps) == 12
            and len(set(steps)) == 12, 'historical source chain changed')
    missing = source_paths(doc['missing_paths'], 'missing')
    declared = source_paths(doc['java_declared_not_in_source_roots'], 'declared missing')
    unlisted = source_paths(doc['java_unlisted_in_receipts'], 'unlisted')
    require(not doc['aliased_paths'], 'aliased source path must be reviewed manually')
    require(missing == declared, 'missing and Java-declared source paths diverge')
    before = receipts(doc['missing_source_preimages'], ('path', 'sha256'), 'preimages')
    after = receipts(doc['unlisted_java_postimages'], ('path', 'sha256'), 'postimages')
    drift = receipts(doc['source_digest_drift'], ('path', 'expected', 'actual'), 'drift')
    require(set(before) == missing and set(after) == unlisted, 'source receipt path mismatch')
    require(not (set(drift) & (missing | unlisted)) and not (missing & unlisted),
            'overlapping source buckets')
    require(all(r['expected'] != r['actual'] for r in drift.values()),
            'false source drift')
    counts = {'source_digest_drift': len(drift),
              'source_missing_or_aliased': len(missing),
              'java_unlisted_in_receipts': len(unlisted),
              'java_declared_not_in_source_roots': len(declared)}
    require(counts == doc['counts'], 'reported counts differ from records')
    require(doc['expected'] == doc['observed'] + len(missing),
            'expected/observed receipt accounting differs')
    pairs = {}
    claimed = set()
    for r in doc['candidate_mat_prefix_renames']:
        require(set(r) == {'historical', 'candidate', 'identity'}, 'malformed Mat candidate')
        old, new = identity(r['historical']), identity(r['candidate'])
        p, q = PurePosixPath(old), PurePosixPath(new)
        require(r['identity'] == 'UNVERIFIED_SOURCE_SEMANTICS'
                and p.parent == q.parent and p.name.startswith('Mat')
                and q.name == p.name[3:], 'unverified Mat-prefix identity changed')
        require(old in missing and new in unlisted and old not in pairs
                and new not in claimed, 'ambiguous Mat rename')
        pairs[old] = (new, 'MAT_PREFIX_ONLY')
        claimed.add(new)
    mat_count = len(pairs)
    for old in sorted(missing - set(pairs)):
        p = PurePosixPath(old)
        target = EXTRA.get(p.name)
        if target:
            new = str(p.with_name(target))
            if new in unlisted:
                require(new not in claimed, 'duplicate extra rename')
                pairs[old] = (new, 'ADDITIONAL_NAME_ONLY')
                claimed.add(new)
    rows = []
    def add(category, old='', new='', oldsha='', newsha='', reason=''):
        path = old or new
        fields = (category, old, new, oldsha, newsha)
        uid = hashlib.sha256('\x00'.join(fields).encode()).hexdigest()[:20]
        rows.append(dict(zip(COLUMNS, (uid, category, 'UNVERIFIED_SOURCE_SEMANTICS',
                                        old, new, oldsha, newsha,
                                        'MAIN' if '/src/main/' in path else 'TEST', GATE))))
    for p in sorted(drift):
        add('CHANGED_IN_PLACE', p, p, drift[p]['expected'], drift[p]['actual'])
    for old, (new, reason) in sorted(pairs.items()):
        add('POSSIBLE_MOVE_' + reason, old, new,
            before[old]['sha256'], after[new]['sha256'])
    missing_remaining = missing - set(pairs)
    unlisted_remaining = unlisted - claimed
    for p in sorted(missing_remaining):
        add('UNRESOLVED_MISSING_OR_REMOVED', p, oldsha=before[p]['sha256'])
    for p in sorted(unlisted_remaining):
        add('UNLISTED_NEW_OR_MOVED', new=p, newsha=after[p]['sha256'])
    require(len({x['review_id'] for x in rows}) == len(rows), 'duplicate review ID')
    old_paths = [x['historical_path'] for x in rows if x['historical_path']]
    new_paths = [x['candidate_path'] for x in rows if x['candidate_path']]
    require(len(old_paths) == len(set(old_paths)) == len(missing) + len(drift)
            and len(new_paths) == len(set(new_paths)) == len(unlisted) + len(drift),
            'non-reversible source accounting')
    summary = {'source_head': head, 'status': 'HOLD', 'approved_transitions': 0,
               'original_source_manifest_sha256': doc['source_manifest_sha256'],
               'historical_mat_pairs': mat_count,
               'additional_name_only_pairs': len(pairs) - mat_count,
               'candidate_moves': len(pairs), 'changed_in_place': len(drift),
               'unresolved_missing': len(missing_remaining),
               'unlisted_unpaired': len(unlisted_remaining),
               'review_units': len(rows),
               'source_change_records': len(drift) + len(missing) + len(unlisted)}
    return rows, summary


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--self-test', action='store_true')
    p.add_argument('--inventory', type=Path)
    p.add_argument('--head', type=Path)
    p.add_argument('--output', type=Path)
    args = p.parse_args()
    if args.self_test:
        import importlib.util
        assert re.fullmatch('[0-9a-f]{64}', 'a' * 64)
        try:
            identity('m3/collections/../evil.java')
        except ValueError:
            pass
        else:
            raise AssertionError('source path escape accepted')
        try:
            digest('not-a-digest')
        except ValueError:
            pass
        else:
            raise AssertionError('invalid source digest accepted')
        print('M3_INSTALLED_TRIAGE_SELF_TEST_PASS negative=2')
        return 0
    if not args.inventory or not args.head or not args.output:
        p.error('--inventory, --head, --output are required')
    try:
        source = args.inventory.read_bytes()
        rows, summary = review(json.loads(source), args.head.read_text().strip())
        summary['input_inventory_sha256'] = hashlib.sha256(source).hexdigest()
        args.output.mkdir(parents=True, exist_ok=True)
        with (args.output/'source-review-queue.tsv').open('w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=COLUMNS, delimiter='\t')
            writer.writeheader(); writer.writerows(rows)
        with (args.output/'candidate-moves-unverified.tsv').open('w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=COLUMNS, delimiter='\t')
            writer.writeheader(); writer.writerows(x for x in rows if x['category'].startswith('POSSIBLE_MOVE_'))
        (args.output/'source-review-summary.json').write_text(
            json.dumps(summary, indent=2, sort_keys=True) + '\n')
        (args.output/'TRIAGE_README.md').write_text(
            '# Read-only source ledger review queue\n\n'
            'Source checkout: ' + summary['source_head'] + '\n\n'
            'All ' + str(summary['review_units']) + ' cases are unverified. '
            'Do not edit source hashes, approve name-only moves or bypass installed-source audit.\n')
        print('M3_INSTALLED_TRIAGE_HOLD', json.dumps(summary, sort_keys=True))
        return 0  # Creates a review queue, NOT an installed-source PASS.
    except (ValueError, KeyError, OSError, TypeError) as error:
        print('M3_INSTALLED_TRIAGE_REFUSED', str(error), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
