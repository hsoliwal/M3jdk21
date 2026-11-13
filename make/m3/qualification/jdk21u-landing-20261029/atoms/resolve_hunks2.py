#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Mechanical conflict resolution per file (v2): mode ours | theirs | union | superset:ours | superset:theirs
| union-sep:<text> | list:<m1,m2,...> (one of ours|theirs|union per hunk).
superset:<side> = union of the hunks where both sides only inserted (base empty) or one side is empty, the named side
where both modified the same region (diff3 markers needed; the compiler gate arbitrates).
Corrections over resolve_hunks.py (adversarial review of 2026-10-23): in a diff3 file the separator is the =======
that follows the ||||||| base section, a ======= content line inside the ours or base section is text; a union keeps
the lines common to the head and to the tail of both sides once (longest common prefix and suffix) and concatenates
the differing middles, ours first, so an insertion both sides made is not duplicated. Byte-preserving except the
markers. Exit status 2 when a conflict is unterminated. Usage: resolve_hunks2.py <file> <mode>"""
import sys, re
path, mode = sys.argv[1], sys.argv[2]
data = open(path, "rb").read(); nl = b"\r\n" if b"\r\n" in data else b"\n"
lines = data.split(nl); diff3 = any(l.startswith(b"||||||| ") for l in lines)
out = []; state = None; ours = []; theirs = []; base = []; hunks = 0
sep = mode.split(":", 1)[1].encode() if mode.startswith("union-sep:") else None
per_hunk = mode.split(":", 1)[1].split(",") if mode.startswith("list:") else None


def union(a, b):
    """a's lines, then b's lines, with the common prefix and suffix of the two kept once."""
    p = 0
    while p < len(a) and p < len(b) and a[p] == b[p]:
        p += 1
    s = 0
    while s < len(a) - p and s < len(b) - p and a[len(a) - 1 - s] == b[len(b) - 1 - s]:
        s += 1
    mid = a[p:len(a) - s]
    if sep is not None:
        mid = mid + [b"", b"\t" + sep]
    mid = mid + b[p:len(b) - s]
    return a[:p] + mid + a[len(a) - s:]


for line in lines:
    if state is None and line.startswith(b"<<<<<<< "):
        state = "ours"; ours = []; theirs = []; base = []; continue
    if state == "ours" and line.startswith(b"||||||| "):
        state = "base"; base = []; continue
    if line == b"=======" and (state == "base" or (state == "ours" and not diff3)):
        state = "theirs"; continue
    if state == "theirs" and line.startswith(b">>>>>>> "):
        hunks += 1
        m = per_hunk[hunks - 1] if per_hunk else mode
        if m.startswith("superset:"):
            nz = lambda ls: [re.sub(rb"\s+", b" ", x).strip() for x in ls if x.strip()]
            if set(nz(ours)) == set(nz(theirs)):
                m = "ours"
            elif not nz(ours) or not nz(theirs) or not nz(base):
                m = "union"
            else:
                m = m.split(":", 1)[1]
        if m == "ours":
            out += ours
        elif m == "theirs":
            out += theirs
        else:
            out += union(ours, theirs)
        state = None; continue
    if state == "base":
        base.append(line); continue
    (out if state is None else ours if state == "ours" else theirs).append(line)
if state is not None:
    sys.stderr.write("unterminated conflict in " + path + "\n"); sys.exit(2)
open(path, "wb").write(nl.join(out))
print(f"{path}: {hunks} hunks resolved as {mode}")
