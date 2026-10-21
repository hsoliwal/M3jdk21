#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
"""Reproducible donor specialization; writes a recipe candidate, never product source."""
from pathlib import Path
import hashlib
import re

import argparse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--donor', type=Path, required=True)
parser.add_argument('--candidate', type=Path, required=True)
parser.add_argument('--write-new', action='store_true')
args = parser.parse_args()
donor, target = args.donor, args.candidate
if not target.name.endswith('.java.txt'):
    parser.error('Only a .java.txt recipe candidate may be written or checked')
source = donor.read_bytes().decode('utf-8')
expected_donor = 'f147bfc6470b811546384b17b7fd3ed278586db806f445d66151a34f617c0d19'
if hashlib.sha256(source.encode()).hexdigest() != expected_donor:
    parser.error('Pinned TimSort donor source drift')
text = source.replace('package java.util;', '''/*
 * M3 adaptation: Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * Derived from this JDK's java.util.TimSort. The original run-stack invariants,
 * stable binary insertion, galloping searches and directional merges remain.
 * Storage is specialized to an int permutation lane. M3SortContext compares
 * the represented values and supplies cancellation/progress checkpoints.
 */

package java.util;''')
text = text.replace('Comparator<? super T>', 'M3SortContext')
text = text.replace('class TimSort<T>', 'final class M3TimSort')
text = text.replace('private TimSort(', 'private M3TimSort(')
text = text.replace('TimSort<T> ts = new TimSort<>(', 'M3TimSort ts = new M3TimSort(')
text = text.replace('static <T>', 'static')
text = text.replace('T[]', 'int[]')
text = re.sub(r'\bT\b', 'int', text)
text = text.replace('Object[] a, int lo, int hi)', 'int[] a, int lo, int hi, M3SortContext c)')
text = text.replace('Object t = a[lo];', 'int t = a[lo];')
text = text.replace('reverseRange(a, lo, runHi);', 'reverseRange(a, lo, runHi, c);')
text = text.replace('            a[hi--] = t;', '            a[hi--] = t;\n            c.step();')
for size in ('tlen', 'newSize'):
    old = ('            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})\n'
           '            int[] newArray = (int[])java.lang.reflect.Array.newInstance\n'
           f'                (a.getClass().getComponentType(), {size});')
    assert text.count(old) == 1, size
    text = text.replace(old, f'            int[] newArray = new int[{size}];')
text = text.replace('temporary storage space for n/2 object references;',
                    'temporary storage space for n/2 integer lane entries;')
text = text.replace('                default: System.arraycopy(a, left, a, left + 1, n);',
                    '                default: c.step();\n                         System.arraycopy(a, left, a, left + 1, n);')
text = text.replace('            a[left] = pivot;', '            a[left] = pivot;\n            c.step();')
text = re.sub(r'(?m)^(\s*)System\.arraycopy\(([^\n]+)\);$',
              lambda m: m.group(1) + 'c.step();\n' + m.group(1) + 'System.arraycopy(' + m.group(2) + ');', text)
# The switch's arraycopy has already received a checkpoint.
text = text.replace('                default: c.step();\n                         c.step();',
                    '                default: c.step();')
assert 'Comparator' not in text
assert 'TimSort<' not in text
assert 'reflect.Array' not in text
assert 'Object[]' not in text
assert text.count('private static void reverseRange(int[]') == 1
if args.write_new:
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open('x', encoding='utf-8') as output:
        output.write(text)
elif not target.exists() or target.read_bytes() != text.encode('utf-8'):
    parser.error('Reviewed candidate differs from exact donor specialization')
print('donor_sha256=' + hashlib.sha256(source.encode()).hexdigest())
print('payload_sha256=' + hashlib.sha256(text.encode()).hexdigest())
print(target)
