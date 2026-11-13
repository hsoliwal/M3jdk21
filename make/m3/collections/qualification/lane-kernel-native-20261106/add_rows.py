#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""RESTORE_ADD_ROWS v1: restore JNI exports from a retained variant into a master C owner.

1. Reads THREE_WAY.tsv (base/ours/theirs blobs), checks each blob id with git hash-object and re-runs
   git merge-file -p --diff3; refuses unless the outcome equals the recorded result row (CONFLICT, 1 hunk).
2. Builds the owner as ours plus the declared rows of ADD_ROWS.tsv (theirs:<a>-<b> slices whose sha256
   prefix must match, ANCHOR lines from the text column), in table order, after the named ours line.
3. Invariants: removing every inserted line gives ours byte-for-byte; every JNIEXPORT symbol of ours is
   kept; the added exports are exactly the EXPORT rows' Java_* symbols.
4. Writes the owner (default) or, with --check, compares it and prints changed=0|1.
CPU only; usage: python -I add_rows.py [--check] [--repo <git dir>]
"""
import argparse
import hashlib
import re
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
OWNER = 'm3/collections/src/main/native/collections/m3_long_address_kernel.c'
EXPORT = re.compile(r'^(Java_[A-Za-z0-9_]+)\(', re.M)


def git(repo, *args, data=None):
    return subprocess.run(['git', '-C', str(repo), *args], input=data, check=True,
                          capture_output=True).stdout


def table(name):
    rows = [line.split('\t') for line in (HERE / name).read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    return [dict(zip(rows[0], row)) for row in rows[1:]]


def three_way(repo, blobs):
    with tempfile.TemporaryDirectory() as tmp:
        paths = {}
        for role in ('ours', 'base', 'theirs'):
            paths[role] = Path(tmp) / role
            paths[role].write_bytes(blobs[role])
        done = subprocess.run(['git', 'merge-file', '-p', '--diff3', '-L', 'ours', '-L', 'base', '-L', 'theirs',
                               str(paths['ours']), str(paths['base']), str(paths['theirs'])], capture_output=True)
    hunks = done.stdout.decode().count('\n<<<<<<< ours\n') + done.stdout.decode().startswith('<<<<<<< ours\n')
    return done.returncode, hunks


def build(blobs, rows):
    ours = blobs['ours'].decode().splitlines(keepends=True)
    theirs = blobs['theirs'].decode().splitlines(keepends=True)
    inserts, added = {}, []
    for row in rows:
        if row['kind'] == 'ANCHOR':
            lines = [row['text'] + '\n']
        else:
            first, last = map(int, row['source'].removeprefix('theirs:').split('-'))
            lines = theirs[first - 1:last]
            digest = hashlib.sha256(''.join(lines).encode()).hexdigest()[:16]
            if digest != row['sha256_16']:
                sys.exit(f"REFUSE {row['row']}: slice sha256 {digest} != {row['sha256_16']}")
        if row['kind'] == 'EXPORT':
            added += [s for s in row['symbols'].split() if s.startswith('Java_')]
        inserts.setdefault(int(row['after_ours_line']), []).extend(lines)
    out, kept = [], []
    for number, line in enumerate(ours, 1):
        out.append(line)
        kept.append(True)
        for extra in inserts.get(number, []):
            out.append(extra)
            kept.append(False)
    if ''.join(l for l, k in zip(out, kept) if k) != ''.join(ours):
        sys.exit('REFUSE: result minus ADD rows differs from ours')
    return ''.join(out), added, sum(1 for k in kept if not k)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--repo', default=str(HERE.parents[4]))
    args = parser.parse_args()
    repo = Path(args.repo)
    roles = {row['role']: row for row in table('THREE_WAY.tsv')}
    blobs = {}
    for role in ('base', 'ours', 'theirs'):
        blobs[role] = git(repo, 'cat-file', '-p', roles[role]['blob'])
        if git(repo, 'hash-object', '--stdin', data=blobs[role]).decode().strip() != roles[role]['blob']:
            sys.exit(f'REFUSE: {role} blob id mismatch')
    code, hunks = three_way(repo, blobs)
    recorded = roles['result']['note']
    if not (recorded.startswith('CONFLICT') and code == 1 and hunks == 1):
        sys.exit(f'REFUSE: 3-way outcome exit={code} hunks={hunks} differs from THREE_WAY.tsv')
    text, added, inserted = build(blobs, table('ADD_ROWS.tsv'))
    before = EXPORT.findall(blobs['ours'].decode())
    after = EXPORT.findall(text)
    if after[:len(before)] != before or sorted(after[len(before):]) != sorted(added):
        sys.exit(f'REFUSE: export set {after} != ours {before} + {added}')
    target = repo / OWNER
    current = target.read_bytes() if target.exists() else b''
    changed = int(current != text.encode())
    if not args.check and changed:
        with open(target, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    print(f"RESTORE_ADD_ROWS v1 three_way=CONFLICT hunks={hunks} rows={len(table('ADD_ROWS.tsv'))} "
          f"inserted_lines={inserted} exports={len(before)}+{len(added)} "
          f"{'check' if args.check else 'apply'} changed={changed} "
          f"result_blob={git(repo, 'hash-object', '--stdin', data=text.encode()).decode().strip()}")
    return 1 if args.check and changed else 0


if __name__ == '__main__':
    sys.exit(main())
