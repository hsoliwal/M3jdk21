#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify every differing line of the victim-progress differential (ResidencyEvictionReentryDifferential, master
vs edited output). Derived from qualification/census-small-batch-20261106/oracle/classify_l066.py (per-episode first
divergence, CASCADE after it: a fresh arena per episode bounds a divergence to its episode).

Per episode: identical, or the FIRST differing line is one of
  MASTER_LIVELOCK  master printed HANG at op i (the op did not finish within the watchdog) and the edited op i
                   finished; its events show a nested request from an eviction callback (encode-nest/write-nest)
  CLAIM_REFUSAL    the edited op's events refuse a nested use/install/retain of the slot being admitted with
                   IllegalStateException (reentrant residency transition / slot is not absent) where master accepted
                   the same nested request (same callback, kind, slot, bytes), and master's events up to that nested
                   request are the edited events' prefix (master may refuse a later nested request on that slot: it
                   had published the slot already)
and every later line of that episode is CASCADE. Edited HANG lines and edited OVER states (resident bytes above the
budget, OBSERVE_ONLY excluded) are failures: eviction callbacks run before the admission re-checks the budget.
Exit 1 on any UNCLASSIFIED line, edited HANG or edited OVER.
usage: classify_victim_progress.py MASTER_TXT EDITED_TXT OUT_TSV
"""
import re
import sys
from collections import Counter, defaultdict

FLAGS_PER_CYCLE = 4
NEST = re.compile(r'(encode|write)-nest\d ')
CLAIM = re.compile(r'(encode|write)-nest\d (\d+)/\d+ threw IllegalStateException:'
                   r'(reentrant residency transition for slot (\d+)|slot is not absent);')


def load(path):
    eps, total = defaultdict(list), ''
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            if line.startswith('RESUME'):
                continue
            if line.startswith('TOTAL'):
                total = line
            else:
                eps[int(line.split(':', 1)[0])].append(line)
    return eps, total


def fields(line):
    head, _, ev = line.partition(' ev=')
    return head, ev


def requested(line):
    m = re.match(r'\d+:\d+ (?:use|install|op\d) (\d+)/', line)
    return int(m[1]) if m else -1


def classify_first(m, e):
    mh, mev = fields(m)
    eh, eev = fields(e)
    if ' HANG ' in mh:
        return 'MASTER_LIVELOCK' if NEST.search(eev) else 'UNCLASSIFIED_HANG_WITHOUT_EVICTION_NEST'
    for c in CLAIM.finditer(eev):
        if int(c[2]) != requested(e):
            continue
        header = eev[c.start():eev.index(' threw', c.start())]
        rest = mev[c.start():]
        if mev.startswith(eev[:c.start()]) and rest.startswith(header)                 and not rest[len(header):].startswith(' threw IllegalStateException'):
            return 'CLAIM_REFUSAL'
        break
    return 'UNCLASSIFIED'


def main():
    me, mt = load(sys.argv[1])
    ee, et = load(sys.argv[2])
    c = Counter()
    rows = []
    for e in sorted(set(me) | set(ee)):
        ml, el = me.get(e, []), ee.get(e, [])
        reentry = (e // FLAGS_PER_CYCLE) % 2 == 1
        diverged = False
        for x in el:
            if ' HANG ' in x:
                c['EDITED_HANG'] += 1
                rows.append((e, '-', 'EDITED_HANG', '-', x))
            elif ' OVER' in fields(x)[0]:
                c['EDITED_OVER'] += 1
                rows.append((e, '-', 'EDITED_OVER', '-', x))
        if len(el) != len(ml) and not any(' HANG ' in m for m in ml):
            c['UNCLASSIFIED_LENGTH'] += 1
        for i, (m, x) in enumerate(zip(ml, el)):
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
        if len(el) > len(ml):
            c['CASCADE_AFTER_MASTER_HANG'] += len(el) - len(ml)
        c['EPISODES_DIVERGED_REENTRY' if diverged and reentry else
          'EPISODES_DIVERGED_NO_REENTRY' if diverged else 'EPISODES_IDENTICAL'] += 1
    c['MASTER_HANG_LINES'] += sum(1 for v in me.values() for m in v if ' HANG ' in m)
    c['MASTER_OVER_LINES'] += sum(1 for v in me.values() for m in v if ' OVER' in fields(m)[0])
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
    bad = sum(v for k, v in c.items() if 'UNCLASSIFIED' in k or k in ('EDITED_HANG', 'EDITED_OVER'))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
