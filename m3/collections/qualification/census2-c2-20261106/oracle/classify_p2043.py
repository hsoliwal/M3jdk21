#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify the master/edited transcripts of CensusFilteredListDifferential (census pass-2 leaf P2-043).

Per episode the first differing line must be a cursor write (op 4 remove, 5 set, 6 add) that the edited owner
refuses with ConcurrentModificationException after a structural write to the backing list that did not go through
this cursor (op 8 view.add, 9 view.remove, 11 source.add, 12 source.remove) since the cursor was opened (episode
start or op 10 reopen). Categories: STALE_WRITE_REFUSED (master wrote), STALE_WRITE_REFUSED_MASTER_FAILED (master
threw another exception), UNCLASSIFIED (anything else, must be 0). Later lines of a diverged episode are CASCADE.
usage: classify_p2043.py MASTER_TRANSCRIPT EDITED_TRANSCRIPT"""
import sys
from collections import Counter


def episodes(path):
    current, out = None, []
    for line in open(path, encoding='utf-8'):
        line = line.rstrip('\n')
        if line.startswith('DIGEST'):
            continue
        if line.startswith('E '):
            current = [line]
            out.append(current)
        else:
            current.append(line)
    return out


master, edited = episodes(sys.argv[1]), episodes(sys.argv[2])
if len(master) != len(edited):
    sys.exit('episode count differs: %d vs %d' % (len(master), len(edited)))
counts, cascade, identical = Counter(), 0, 0
for m, e in zip(master, edited):
    if m == e:
        identical += 1
        continue
    first = next(i for i in range(min(len(m), len(e))) if m[i] != e[i])
    stale = False
    for line in m[1:first]:
        op, result = line.split(' ')[0], line.split(' ')[1]
        if op == '10' and result == 'reopen':
            stale = False
        elif op in ('8', '11') and not result.endswith('Exception'):
            stale = True
        elif op in ('9', '12') and result != 'empty' and not result.endswith('Exception'):
            stale = True
    mop, mres = m[first].split(' ')[0], m[first].split(' ')[1]
    eop, eres = e[first].split(' ')[0], e[first].split(' ')[1]
    if mop == eop and eop in ('4', '5', '6') and eres == 'ConcurrentModificationException' and stale:
        counts['STALE_WRITE_REFUSED_MASTER_FAILED' if mres.endswith('Exception') else 'STALE_WRITE_REFUSED'] += 1
    else:
        counts['UNCLASSIFIED'] += 1
        print('UNCLASSIFIED', m[0], '|', m[first], '|', e[first])
    cascade += len(m) - first - 1
print('episodes=%d identical=%d diverged=%d %s cascade_lines=%d unclassified=%d' % (
    len(master), identical, len(master) - identical,
    ' '.join('%s=%d' % kv for kv in sorted(counts.items())), cascade, counts['UNCLASSIFIED']))
