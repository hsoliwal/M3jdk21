#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify every differing line of the donor-semantics differential (ResidencyDonorSemanticsDifferential, before vs
after the adopted groups). Derived from ../../residency-victim-progress-20261106/oracle/classify_victim_progress.py
(per-episode first divergence, CASCADE after it: a fresh arena per episode bounds a divergence to its episode).

Per episode: identical, or the FIRST differing line is one of
  ERROR_RESTORE           group a: same outcome, same events (an injected Error fault among them), same resident
                          bytes; only the slot states differ (before: a transitional state left behind)
  REUSE_GUARD             group c: a reuse the edited owner refuses without side effects (=false, no reset event)
                          where the old owner ran the resetter (LAZY_CREATE, OBSERVE_ONLY, destination pin/borrow)
  REUSE_EXCEPTION_IDENTITY group c: the resetter's RuntimeException propagates itself on the edited side, wrapped in
                          ResidencyException before; everything else on the line identical
  REUSE_BUDGET_RECHECK    group d: the edited reuse throws BudgetExceededException after the resetter where the old
                          owner returned true
  REUSE_SOURCE_HELD       groups c+d: the line first differs inside a reset event's resident bytes/states (the source
                          stays resident, counted and CREATING while the resetter runs); later differences on that
                          line (nested admissions, outcome) follow from it
and every later line of that episode is CASCADE. Edited OVER states (resident bytes above the budget, OBSERVE_ONLY
excluded) are failures. Exit 1 on any UNCLASSIFIED first divergence or edited OVER state.
usage: classify_donor_semantics.py BEFORE_TXT AFTER_TXT OUT_TSV
"""
import re
import sys
from collections import Counter, defaultdict

LINE = re.compile(r'^(\d+):(\d+) (.*?) rb=(\d+)( OVER)? st=(\S*) ev=(.*)$')
RESET = re.compile(r'reset \d+#\d rb=\d+ st=\S+?;')


def load(path):
    eps, total = defaultdict(list), ''
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            if line.startswith('TOTAL'):
                total = line
            else:
                eps[int(line.split(':', 1)[0])].append(line)
    return eps, total


def parse(line):
    m = LINE.match(line)
    if not m:
        return None
    return {'outcome': m[3], 'rb': m[4], 'over': bool(m[5]), 'st': m[6], 'ev': m[7]}


def first_diff(a, b):
    i = 0
    while i < min(len(a), len(b)) and a[i] == b[i]:
        i += 1
    return i


def classify_first(m, x):
    pm, px = parse(m), parse(x)
    if pm is None or px is None:
        return 'UNCLASSIFIED_FORMAT'
    reuse = pm['outcome'].startswith('reuse ') or ' threw ' in pm['outcome'] and re.match(r'op[789] ', pm['outcome'])
    if (pm['outcome'] == px['outcome'] and pm['ev'] == px['ev'] and pm['rb'] == px['rb']
            and re.search(r'fault \w+#0;', px['ev'])):
        return 'ERROR_RESTORE'
    if reuse and px['outcome'].endswith('=false') and 'reset ' not in px['ev'] \
            and ('reset ' in pm['ev'] or pm['outcome'].endswith('=true')):
        return 'REUSE_GUARD'
    if reuse and 'threw ResidencyException:projection reset failed' in pm['outcome'] \
            and 'cause=IllegalArgumentException:reset runtime' in pm['outcome'] \
            and 'threw IllegalArgumentException:reset runtime' in px['outcome'] and pm['ev'] == px['ev']:
        return 'REUSE_EXCEPTION_IDENTITY'
    if reuse and pm['outcome'].endswith('=true') and 'threw BudgetExceededException' in px['outcome']:
        return 'REUSE_BUDGET_RECHECK'
    at = first_diff(pm['ev'], px['ev'])
    for r in RESET.finditer(px['ev']):
        if r.start() <= at < r.end() and pm['ev'][:r.start()] == px['ev'][:r.start()]:
            return 'REUSE_SOURCE_HELD'
    return 'UNCLASSIFIED'


def main():
    me, mt = load(sys.argv[1])
    ee, et = load(sys.argv[2])
    c = Counter()
    rows = []
    for e in sorted(set(me) | set(ee)):
        ml, el = me.get(e, []), ee.get(e, [])
        if len(ml) != len(el):
            c['UNCLASSIFIED_LENGTH'] += 1
        diverged = False
        for x in el:
            p = parse(x)
            if p and p['over']:
                c['EDITED_OVER'] += 1
                rows.append((e, '-', 'EDITED_OVER', '-', x))
        for i, (m, x) in enumerate(zip(ml, el)):
            if m == x:
                c['IDENTICAL_AFTER_DIVERGENCE' if diverged else 'IDENTICAL'] += 1
                continue
            if diverged:
                c['CASCADE'] += 1
                continue
            diverged = True
            kind = classify_first(m, x)
            c[kind] += 1
            rows.append((e, i, kind, m, x))
        c['EPISODES_DIVERGED' if diverged else 'EPISODES_IDENTICAL'] += 1
    c['BEFORE_OVER_LINES'] += sum(1 for v in me.values() for m in v if (parse(m) or {}).get('over'))
    c['BEFORE_' + mt.replace(' ', '_')] += 1
    c['AFTER_' + et.replace(' ', '_')] += 1
    with open(sys.argv[3], 'w', encoding='utf-8', newline='\n') as f:
        f.write('class\tcount\n')
        for k in sorted(c):
            f.write('%s\t%d\n' % (k, c[k]))
        f.write('# first divergence per episode (episode, op, class, before line, after line); first 40\n')
        for r in rows[:40]:
            f.write('#\t' + '\t'.join(str(v)[:300] for v in r) + '\n')
    for k in sorted(c):
        print('%s=%d' % (k, c[k]))
    bad = sum(v for k, v in c.items() if k.startswith('UNCLASSIFIED') or k == 'EDITED_OVER')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
