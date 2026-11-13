#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Surefire XML -> failing set TSV (class, testcase, kind) and totals; stdlib only.
usage: failset.py SUREFIRE_DIR OUT_TSV"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

src, out = sys.argv[1], sys.argv[2]
rows, tot = [], [0, 0, 0, 0]
for p in sorted(glob.glob(os.path.join(src, 'TEST-*.xml'))):
    r = ET.parse(p).getroot()
    for i, k in enumerate(('tests', 'failures', 'errors', 'skipped')):
        tot[i] += int(r.get(k, '0'))
    for tc in r.iter('testcase'):
        for kind in ('failure', 'error'):
            e = tc.find(kind)
            if e is not None:
                rows.append((tc.get('classname', ''), tc.get('name', ''), kind, (e.get('type') or '').rsplit('.', 1)[-1]))
rows.sort()
with open(out, 'w', encoding='utf-8', newline='\n') as f:
    f.write('class\ttestcase\tkind\ttype\n')
    for r in rows:
        f.write('\t'.join(r) + '\n')
classes = sorted({r[0] for r in rows})
print('tests=%d failures=%d errors=%d skipped=%d failing_tests=%d failing_classes=%d' % (*tot, len(rows), len(classes)))
