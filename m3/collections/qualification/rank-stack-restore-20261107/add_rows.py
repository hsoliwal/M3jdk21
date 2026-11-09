#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""RESTORE_ADD_ROWS v2: restore the dropped pr1882 rank-batch stack (and the clean range/rank leaves of PRs #2149 and
#1515) from the retained variants into master's owners, multi-target.

1. THREE_WAY.tsv: per target and step, reads base/ours/theirs by blob id (git hash-object must agree), re-runs
   git merge-file -p --diff3 and refuses unless outcome and hunk count equal the recorded row; a MERGE step's output
   must equal its recorded result blob and becomes the target's pre text, otherwise pre = ours of step 1.
2. ADD_ROWS.tsv: the target = pre plus exactly the declared rows, in table order, after the named pre line
   (T<step>:<a>-<b> slices of that step's theirs blob, sha256 prefix checked; ANCHOR = one CNPCDA line).
3. Invariants: result minus the inserted lines == pre byte-for-byte; the top-level member multiset of the result is
   the pre multiset plus the MEMBERS rows' symbols (Java: fields/methods; C: functions/enums, so the JNI export set is
   master's plus the restored exports); no pre member body changes.
4. RESTORED_FILES.tsv: whole files absent on master, copied byte-for-byte from the retained variant (blob id checked).
5. Writes the targets (default) or, with --check, compares them and prints changed=<n>.
CPU only; usage: python -I add_rows.py [--check] [--repo <git dir>]
"""
import argparse
import hashlib
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE / 'tools'))
from gen_rows import members  # noqa: E402


def git(repo, *args, data=None):
    return subprocess.run(['git', '-C', str(repo), *args], input=data, check=True, capture_output=True).stdout


def table(name):
    rows = [line.split('\t') for line in (HERE / name).read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    return [dict(zip(rows[0], row)) for row in rows[1:]]


def blob(repo, oid):
    data = git(repo, 'cat-file', '-p', oid)
    if git(repo, 'hash-object', '--stdin', data=data).decode().strip() != oid:
        sys.exit(f'REFUSE: blob id mismatch {oid}')
    return data


def three_way(blobs):
    with tempfile.TemporaryDirectory() as tmp:
        paths = {}
        for role in ('ours', 'base', 'theirs'):
            paths[role] = Path(tmp) / role
            paths[role].write_bytes(blobs[role])
        done = subprocess.run(['git', 'merge-file', '-p', '--diff3', '-L', 'ours', '-L', 'base', '-L', 'theirs',
                               str(paths['ours']), str(paths['base']), str(paths['theirs'])], capture_output=True)
    text = done.stdout.decode()
    hunks = text.count('\n<<<<<<< ours\n') + text.startswith('<<<<<<< ours\n')
    return done.returncode, hunks, done.stdout


def member_names(text, java):
    return Counter(key.split('#')[0] for key, *_ in members(text.splitlines(), java))


def build(target, pre, theirs, rows):
    lines = pre.decode().splitlines(keepends=True)
    inserts, added, inserted = {}, Counter(), 0
    for row in rows:
        if row['kind'] == 'ANCHOR':
            chunk = [row['text'] + '\n']
        else:
            step, span = row['source'][1:].split(':')
            first, last = map(int, span.split('-'))
            chunk = theirs[step].decode().splitlines(keepends=True)[first - 1:last]
            digest = hashlib.sha256(''.join(chunk).encode()).hexdigest()[:16]
            if digest != row['sha256_16']:
                sys.exit(f"REFUSE {row['row']}: slice sha256 {digest} != {row['sha256_16']}")
        if row['kind'] == 'MEMBERS':
            added.update(row['symbols'].split())
        inserts.setdefault(int(row['after_line']), []).extend(chunk)
        inserted += len(chunk)
    out, kept = [], []
    for number, line in enumerate(lines, 1):
        out.append(line)
        kept.append(True)
        for extra in inserts.get(number, []):
            out.append(extra)
            kept.append(False)
    if ''.join(line for line, k in zip(out, kept) if k) != ''.join(lines):
        sys.exit(f"REFUSE {target['target']}: result minus ADD rows differs from pre")
    text = ''.join(out)
    java = target['path'].endswith('.java')
    if member_names(text, java) != member_names(pre.decode(), java) + added:
        diff = (member_names(text, java) - member_names(pre.decode(), java)) - added
        sys.exit(f"REFUSE {target['target']}: member set != pre + MEMBERS symbols ({dict(diff)})")
    return text, inserted, sum(added.values())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--repo', default=str(HERE.parents[3]))
    args = parser.parse_args()
    repo = Path(args.repo)
    steps = {}
    for row in table('THREE_WAY.tsv'):
        steps.setdefault(row['target'], []).append(row)
    rows = table('ADD_ROWS.tsv')
    changed, report = 0, []
    for name, plan in steps.items():
        pre, theirs, first = None, {}, plan[0]
        for step in plan:
            blobs = {role: blob(repo, step[role]) for role in ('base', 'ours', 'theirs')}
            code, hunks, merged = three_way(blobs)
            want = 0 if step['outcome'] == 'CLEAN' else int(step['hunks'])
            if code != want or hunks != int(step['hunks']):
                sys.exit(f"REFUSE {name}.{step['step']}: 3-way exit={code} hunks={hunks} != THREE_WAY.tsv")
            if git(repo, 'hash-object', '--stdin', data=merged).decode().strip() != step['result']:
                sys.exit(f"REFUSE {name}.{step['step']}: 3-way output differs from the recorded result blob")
            theirs[step['step']] = blobs['theirs']
            if step['strategy'] == 'MERGE':
                pre = merged
        pre = pre if pre is not None else blob(repo, first['ours'])
        text, inserted, symbols = build(first, pre, theirs, [r for r in rows if r['target'] == name])
        target = repo / first['path']
        current = target.read_bytes() if target.exists() else b''
        if current not in (text.encode(), blob(repo, first['ours'])) and not args.check:
            sys.exit(f'REFUSE {name}: working file is neither master ours nor the recipe result')
        delta = int(current != text.encode())
        changed += delta
        if not args.check and delta:
            with open(target, 'w', encoding='utf-8', newline='') as handle:
                handle.write(text)
        report.append(f"{name}:rows={sum(1 for r in rows if r['target'] == name)},lines=+{inserted},members=+{symbols},"
                      f"blob={git(repo, 'hash-object', '--stdin', data=text.encode()).decode().strip()[:12]}")
    for row in table('RESTORED_FILES.tsv'):
        data = blob(repo, row['blob'])
        target = repo / row['path']
        current = target.read_bytes() if target.exists() else b''
        delta = int(current != data)
        changed += delta
        if not args.check and delta:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    print(f"RESTORE_ADD_ROWS v2 targets={len(steps)} rows={len(rows)} files={len(table('RESTORED_FILES.tsv'))} "
          f"{'check' if args.check else 'apply'} changed={changed} {' '.join(report)}")
    return 1 if args.check and changed else 0


if __name__ == '__main__':
    sys.exit(main())
