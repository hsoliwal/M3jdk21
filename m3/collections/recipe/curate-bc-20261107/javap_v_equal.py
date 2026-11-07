# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""javap_v_equal.py <a.txt> <b.txt>: compares two name-normalized `javap -v -p -c -constants` dumps independent of
constant-pool numbering. The constant pool is compared as a sorted multiset of entries with their index and #N references
removed (the resolved // comments carry the values, so string-concat recipes, annotation values and bootstrap arguments
count); every other line is compared in order with #N references removed. Prints EQUAL or the first differences; exit 1
when they differ. Closes the gap of javap -c, which does not print indy string-concat literals or annotation values."""
import re
import sys

CP = re.compile(r"^\s+#\d+ = ")
REF = re.compile(r"#\d+(?::#\d+)?(?:\.#\d+)?")


def split(path):
    pool, rest = [], []
    for line in open(path, encoding="utf-8", errors="replace"):
        line = line.rstrip("\n")
        if CP.match(line):
            pool.append(REF.sub("#", CP.sub("", line)).strip())
        else:
            rest.append(REF.sub("#", line).rstrip())
    return sorted(pool), rest


pa, ra = split(sys.argv[1])
pb, rb = split(sys.argv[2])
bad = []
if pa != pb:
    sa, sb = set(pa), set(pb)
    bad += ["pool only in a: " + x for x in sorted(sa - sb)[:5]] + ["pool only in b: " + x for x in sorted(sb - sa)[:5]]
    if not bad:
        bad.append("pool multiset counts differ")
if ra != rb:
    for i, (x, y) in enumerate(zip(ra, rb)):
        if x != y:
            bad += [f"line {i}: a: {x}", f"line {i}: b: {y}"]
            break
    if len(ra) != len(rb):
        bad.append(f"line counts {len(ra)} vs {len(rb)}")
print("EQUAL" if not bad else "DIFFERENT\n" + "\n".join(bad))
sys.exit(1 if bad else 0)
