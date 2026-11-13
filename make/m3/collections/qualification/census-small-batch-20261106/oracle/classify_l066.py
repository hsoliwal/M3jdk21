#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify every differing line of the L066 differential (CensusResidencyDifferential master vs edited output).

Per episode: identical, or the FIRST differing line is one of
  REFUSAL         the edited op threw BudgetExceededException (requested + resident > target) where master
                  published, and the edited callback events are a prefix of master's (same callbacks up to the
                  re-check; master then ran the spill delete and published, OVER unless a nested use in that
                  delete evicted back under the budget)
  NESTED_REFUSAL  a nested use inside a callback threw BudgetExceededException on the edited side only
and every later line of that episode is CASCADE (fresh arena per episode). Probe lines are PROBE_<phase>.
Edited OVER states (resident bytes above the budget, OBSERVE_ONLY excluded) must come from a nested use inside the
spill delete callback, which the crate runs after the re-check by design (its delete probe: refused=false bytes=16),
or carry over from the previous op of the episode. Exit 1 on any UNCLASSIFIED line or an unexplained OVER state.
usage: classify_l066.py MASTER_TXT EDITED_TXT OUT_TSV
"""
import re
import sys
from collections import Counter, defaultdict

FLAGS_PER_CYCLE = 4
NEST_IN_DELETE = re.compile(r'(^|;)del \d+;nest ')


def load(path):
    probes, eps, total = [], defaultdict(list), ''
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            if line.startswith('PUBLICATION'):
                probes.append(line)
            elif line.startswith('TOTAL'):
                total = line
            else:
                eps[int(line.split(':', 1)[0])].append(line)
    return probes, eps, total


def fields(line):
    head, _, ev = line.partition(' ev=')
    return head, ev


REFUSED = re.compile(r'threw BudgetExceededException:residency budget exceeded: requested=(\d+), resident=(\d+), '
                     r'target=(\d+)')


def classify_first(m, e):
    mh, mev = fields(m)
    eh, eev = fields(e)
    r = REFUSED.search(eh)
    if r and 'threw' not in mh and mev.startswith(eev) and int(r[1]) + int(r[2]) > int(r[3]):
        return 'REFUSAL_MASTER_OVER' if ' OVER' in mh else 'REFUSAL_MASTER_DELETE_NEST_EVICTED'
    if eev.count('threw BudgetExceededException') > mev.count('threw BudgetExceededException'):
        return 'NESTED_REFUSAL'
    return 'UNCLASSIFIED'


def main():
    mp, me, mt = load(sys.argv[1])
    ep, ee, et = load(sys.argv[2])
    c = Counter()
    rows = []
    for a, b in zip(mp, ep):
        phase = a.split()[1].split('=')[1]
        c['PROBE_%s_%s' % (phase, 'IDENTICAL' if a == b else 'DIFFERS')] += 1
        rows.append(('probe', phase, 'PROBE', a, b))
    for e in sorted(set(me) | set(ee)):
        ml, el = me.get(e, []), ee.get(e, [])
        reentry = (e // FLAGS_PER_CYCLE) % 2 == 1
        diverged = False
        prev_over = False
        for i, (m, x) in enumerate(zip(ml, el)):
            if ' OVER' in fields(x)[0]:
                if NEST_IN_DELETE.search(fields(x)[1]):
                    c['EDITED_OVER_NEST_IN_DELETE'] += 1
                elif prev_over:
                    c['EDITED_OVER_CARRY'] += 1
                else:
                    c['EDITED_OVER_UNEXPLAINED'] += 1
                    rows.append((e, i, 'EDITED_OVER_UNEXPLAINED', m, x))
            prev_over = ' OVER' in fields(x)[0]
            if m == x:
                c['IDENTICAL_AFTER_DIVERGENCE' if diverged else 'IDENTICAL'] += 1
                continue
            if diverged:
                c['CASCADE'] += 1
                continue
            diverged = True
            kind = classify_first(m, x)
            if not reentry:
                kind = 'UNCLASSIFIED_NO_REENTRY'
            c[kind] += 1
            rows.append((e, i, kind, m, x))
        c['EPISODES_DIVERGED_REENTRY' if diverged and reentry else
          'EPISODES_DIVERGED_NO_REENTRY' if diverged else 'EPISODES_IDENTICAL'] += 1
    c['MASTER_' + mt.replace(' ', '_')] += 1
    c['EDITED_' + et.replace(' ', '_')] += 1
    with open(sys.argv[3], 'w', encoding='utf-8', newline='\n') as f:
        f.write('class\tcount\n')
        for k in sorted(c):
            f.write('%s\t%d\n' % (k, c[k]))
        f.write('# first divergence per episode (episode, op, class, master line, edited line); first 40\n')
        for r in rows[:40]:
            f.write('#\t' + '\t'.join(str(x)[:300] for x in r) + '\n')
    for k in sorted(c):
        print('%s=%d' % (k, c[k]))
    bad = sum(v for k, v in c.items() if 'UNCLASSIFIED' in k or 'UNEXPLAINED' in k)
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
