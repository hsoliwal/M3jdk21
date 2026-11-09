#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only Git ancestry, source-blob and path-superset recipe verifier."""
import argparse
import csv
import re
import subprocess
from pathlib import Path, PurePosixPath

BASE = 'd7b9e65241b717a387b25234df4cf21531f07d9f'
DONORS = {'1587': 'a16356d918f2ec630e1a2c54f59518222f105333',
          '1629': '5d2bbd7401053eba2f64efe0a71dfb367ce357e6'}
HEADERS = ('donor_pr', 'donor_base', 'donor_head', 'change', 'path',
           'before_blob', 'after_blob', 'mode', 'category')


def run(repo, *args):
    result = subprocess.run(['git', '-C', str(repo), *args], check=True,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return result.stdout


def tree(repo, revision, path):
    value = run(repo, 'ls-tree', '-z', revision, '--', path)
    if not value:
        return None
    rows = value.rstrip(b'\0').split(b'\0')
    if len(rows) != 1:
        raise ValueError('ambiguous Git tree path: ' + path)
    metadata, found = rows[0].split(b'\t', 1)
    mode, kind, blob = metadata.decode('ascii').split(' ')
    if found.decode('utf-8') != path or kind != 'blob':
        raise ValueError('not a regular Git blob: ' + path)
    return mode, blob


def paths(repo, older, newer):
    return set(filter(None, run(repo, 'diff', '--name-only', '-z',
                                older, newer).decode('utf-8').split('\0')))


def ancestors(repo, older, newer):
    try:
        run(repo, 'merge-base', '--is-ancestor', older, newer)
        return True
    except subprocess.CalledProcessError:
        return False


def parents(repo, commit):
    line = run(repo, 'rev-list', '--parents', '-n', '1', commit).decode('ascii').split()
    return line[1:]


def validate(repo, manifest, first, second, tip, allow=()):
    rows = list(csv.DictReader(manifest.open(encoding='utf-8', newline=''), delimiter='\t'))
    if not rows or tuple(rows[0]) != HEADERS:
        raise ValueError('wrong manifest schema')
    items = {}
    for row in rows:
        donor, path = row['donor_pr'], row['path']
        p = PurePosixPath(path)
        if (not path or p.is_absolute() or str(p) != path or '\\' in path
                or '..' in p.parts or '\0' in path or '\r' in path or '\n' in path):
            raise ValueError('unsafe path: ' + path)
        if donor not in DONORS or row['donor_head'] != DONORS[donor]:
            raise ValueError('unqualified donor identity: ' + donor)
        if not re.fullmatch(r'[0-9a-f]{40}', row['donor_base']):
            raise ValueError('unqualified parent revision')
        if row['mode'] != '100644' or row['change'] not in {'ADD', 'REPLACE'}:
            raise ValueError('unsupported mode/change: ' + path)
        if not re.fullmatch(r'[0-9a-f]{40}', row['after_blob']):
            raise ValueError('invalid output Git identity')
        if (row['change'] == 'ADD') != (row['before_blob'] == 'ABSENT'):
            raise ValueError('add/replace preimage mismatch')
        if row['change'] == 'REPLACE' and not re.fullmatch(r'[0-9a-f]{40}', row['before_blob']):
            raise ValueError('invalid preimage Git identity')
        if path in items:
            raise ValueError('duplicate path: ' + path)
        items[path] = row
    if set(r['donor_pr'] for r in rows) != set(DONORS):
        raise ValueError('missing donor')
    if parents(repo, first) != [BASE, DONORS['1587']]:
        raise ValueError('first merge is not exact two-parent ancestry')
    if parents(repo, second) != [first, DONORS['1629']]:
        raise ValueError('second merge is not exact two-parent ancestry')
    if not ancestors(repo, second, tip):
        raise ValueError('tip does not retain donor merge ancestry')
    for donor, revision in DONORS.items():
        group = {r['path'] for r in rows if r['donor_pr'] == donor}
        base = next(r['donor_base'] for r in rows if r['donor_pr'] == donor)
        if {r['donor_base'] for r in rows if r['donor_pr'] == donor} != {base}:
            raise ValueError('mixed donor parent identities')
        if paths(repo, base, revision) != group:
            raise ValueError('donor changes do not match catalogued source files: ' + donor)
    for path, row in items.items():
        mode, wanted = row['mode'], row['after_blob']
        expected = None if row['change'] == 'ADD' else (mode, row['before_blob'])
        if tree(repo, row['donor_base'], path) != expected:
            raise ValueError('donor parent diverged: ' + path)
        if tree(repo, BASE, path) != expected:
            raise ValueError('original M3JDK21 product owner diverged: ' + path)
        if tree(repo, row['donor_head'], path) != (mode, wanted):
            raise ValueError('donor output drift: ' + path)
        if tree(repo, tip, path) != (mode, wanted):
            raise ValueError('received output drift: ' + path)
    for permitted in allow:
        if (not (permitted.startswith('m3/qualification/unified-superset-20261021/native-build-20261021/')
                 or permitted == '.github/workflows/m3-native-build-superset-20261021.yml')
                or permitted in items):
            raise ValueError('out-of-scope evidence target: ' + permitted)
    actual = paths(repo, BASE, tip)
    if actual != (set(items) | set(allow)):
        raise ValueError('non-superset changed path set: missing=' + str(sorted((set(items) | set(allow)) - actual))
                         + ' unexpected=' + str(sorted(actual - (set(items) | set(allow)))))
    print('M3JDK21_NATIVE_BUILD_SUPERSET_PASS exact_paths=' + str(len(items))
          + ' donors=' + str(len(DONORS)) + ' extra_proof_paths=' + str(len(allow)))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--first', required=True)
    parser.add_argument('--second', required=True)
    parser.add_argument('--tip', required=True)
    parser.add_argument('--allow', action='append', default=[])
    args = parser.parse_args()
    validate(args.repository, args.manifest, args.first, args.second,
             args.tip, args.allow)


if __name__ == '__main__':
    main()
