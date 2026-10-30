#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern DOUBLE_INSERTION_AFTER_CLEAN_MERGE): both sides of a merge inserted the same block a line or two
apart, so git merged it cleanly twice (2026-10-29: the ML-DSA/ML-KEM KnownOIDs constants, a compile error; in a
table or a test it can be silent). Detects every run of >= <k> non-blank lines (default 3) that occurs more often in
the merged file than in either parent, and with --fix removes the later copy when the copies are separated only by
blank lines (the blank line that preceded the removed copy goes with it). Other excess runs are only reported.
Usage: dedupe_double_insertion.py <merged> <ours> <theirs> [--fix] [--k N]   exit 0 = no excess run left."""
import re, sys
args = [a for a in sys.argv[1:] if not a.startswith("--")]; fix = "--fix" in sys.argv
k = int(sys.argv[sys.argv.index("--k") + 1]) if "--k" in sys.argv else 3
if "--k" in sys.argv: args.remove(str(k))
merged, ours, theirs = args[:3]
read = lambda p: open(p, "rb").read().replace(b"\r\n", b"\n").split(b"\n")
norm = lambda x: re.sub(rb"\s+", b" ", x).strip()


def runs(lines):
    nz = [(i, norm(x)) for i, x in enumerate(lines) if x.strip()]
    out = {}
    for j in range(len(nz) - k + 1):
        out.setdefault(tuple(v for _, v in nz[j:j + k]), []).append((nz[j][0], nz[j + k - 1][0]))
    return out


M = read(merged); rm, ro, rt = runs(M), runs(read(ours)), runs(read(theirs))
excess = [key for key, occ in rm.items() if len(occ) >= 2 and len(occ) > max(len(ro.get(key, [])), len(rt.get(key, [])))]
drop = set()
for key, occ in rm.items():
    if len(occ) <= max(len(ro.get(key, [])), len(rt.get(key, []))):
        continue
    for (s1, _), (s2, _) in zip(occ, occ[1:]):
        span = s2 - s1   # a block repeated back to back: M[s1:s2] == M[s2:s2 + span] (whitespace-normalised)
        if s1 in drop or s2 in drop or s2 + span > len(M):
            continue
        if [norm(x) for x in M[s1:s2]] == [norm(x) for x in M[s2:s2 + span]]:
            print(f"{merged}: block at lines {s1 + 1}-{s2} repeated at {s2 + 1}-{s2 + span}")
            if fix:
                drop.update(range(s2, s2 + span))
if fix and drop:
    nl = b"\r\n" if b"\r\n" in open(merged, "rb").read() else b"\n"
    open(merged, "wb").write(nl.join(x for i, x in enumerate(M) if i not in drop))
    print(f"{merged}: removed {len(drop)} lines")
left = 0 if (fix and drop) else len(excess)
print(f"{merged}: {len(excess)} excess runs{' (fixed adjacent copies)' if fix and drop else ''}")
sys.exit(0 if not excess or (fix and drop) else 1)
