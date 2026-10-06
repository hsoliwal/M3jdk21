#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Check this bounded packet's custody and source seals; no migration promotion."""
import csv
import hashlib
import io
import json
from pathlib import Path


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    crate = Path(__file__).resolve().parent
    repo = crate.parents[2]
    resources = crate / 'src/main/resources/com/m3/rewrite/backport'
    checked = {}
    for kind, name in [('jdk21-hash-pinned', 'jdk22-m3-mr'),
                       ('jdk21-hash-pinned-text', 'm3-mr')]:
        root = resources / kind / name
        for line in (root / 'manifest.tsv').read_text().splitlines():
            if not line or line.startswith('#'):
                continue
            path, before, after, template = line.split('\t')
            assert digest(root / template) == after, ('template drift', path)
            assert digest(repo / path) == after, ('delivered target drift', path)
            if before != 'ABSENT':
                assert digest(root / (template + '.before')) == before, ('preimage drift', path)
            checked[path] = after
    for line in (crate / 'inputs.tsv').read_text().splitlines():
        if not line or line.startswith('#'):
            continue
        path, expected = line.split('\t')
        assert digest(repo / path) == expected, ('dependency drift', path)
        checked[path] = expected

    authority = json.loads((repo / 'm3/docs/name-mapping.json').read_text())
    original = json.loads((resources / 'jdk21-hash-pinned-text/m3-mr/name-mapping.json.txt.before').read_text())
    records = {record['id']: record for record in authority['migration']['records']}
    assert len(records) == len(authority['migration']['records']), 'duplicate mapping IDs'
    assert authority['migration']['records'][:len(original['migration']['records'])] == original['migration']['records'], 'lost or changed lineage'
    assert not authority['migration']['coverage']['source_tree_complete']
    assert not authority['migration']['coverage']['dependency_closure_complete']
    for rid in ('synexia.regex.rxm', 'synexia.regex.rxa'):
        assert records[rid]['status'] == 'pending' and records[rid]['targets'] == []
    tq = records['synexia.regex.mr']
    for target in tq['targets']:
        assert digest(repo / target['path']) == target['sha256'], ('target identity', target['path'])
    assert digest(repo / tq['recipe']['path']) == tq['recipe']['sha256']

    census = json.loads((crate / 'evidence/census.json').read_text())
    assert digest(crate / 'evidence/census.tsv') == census['sha256']
    rows = list(csv.DictReader(io.StringIO((crate / 'evidence/census.tsv').read_text()), delimiter='\t'))
    assert len(rows) == census['production_blobs'] == 4288
    assert len({row['path'] for row in rows}) == len(rows)
    assert sum(row['prefix_match'] == 'true' for row in rows) == census['prefix_matches'] == 2543
    assert all(row['mapping_id'] in records and row['disposition'] == 'REVIEW_REQUIRED' for row in rows)
    assert not census['source_tree_complete'] and not census['semantic_dependency_closure_complete']
    assert not any(tree['truncated'] for tree in census['trees'])

    runtime = crate / 'target'
    semantic = (runtime / 'stock-semantic.log').read_text().strip()
    assert semantic.startswith('MR_SEMANTIC\t')
    for mode in ('interpreter', 'mixed', 'c1', 'c2'):
        assert (runtime / ('before-' + mode + '.log')).read_text().strip() == semantic
        assert (runtime / ('after-' + mode + '.log')).read_text().strip() == semantic
        assert (runtime / ('reuse-' + mode + '.log')).read_text().startswith('MR_REUSE\t')
    result = {'scope': 'eight delivered targets, six unchanged compile/recipe owners, protected preimages, '
                       'mapping lineage and bounded inventory; not whole-repository admission',
              'sources': checked, 'semantic': semantic, 'production_blobs': len(rows),
              'complete_migration': False}
    (runtime / 'source-check.json').write_text(json.dumps(result, indent=2) + '\n')
    print('MR_CHECK_PASS targets=8 unchanged_owners=6 inventory=4288 prefix_files=2543')


if __name__ == '__main__':
    main()
