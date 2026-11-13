#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern OURS_HUNK_CONTAINED_UPSTREAM): resolve a diff3 conflict hunk to theirs ONLY when every non-blank
line of the ours section, whitespace-normalised and in order, occurs as one contiguous run somewhere in the whole
theirs file, or in any earlier upstream revision of it given after it; that is, upstream already carries the change
ours made (a fix backported twice, or code moved), or carried it and later revised it (the newer upstream text wins).
Second test, for line wrapping: the ours section with ALL whitespace removed is a substring of a revision with all
whitespace removed. Both tests need a hunk of at least 3 non-blank lines and 60 non-blank characters, so a lone
brace or return statement is never "contained" by accident. An ours section that is empty is left to other atoms.
Other hunks stay marked. Byte-preserving except the resolved hunks.
Usage: hunk_contained_upstream.py <diff3 file> <theirs file> [<earlier upstream revision>...]   prints "<file>: <n> of <m> hunks contained upstream";
exit 0 when no conflict is left, 1 otherwise."""
import re, sys
path, theirs_paths = sys.argv[1], sys.argv[2:]
data = open(path, "rb").read(); nl = b"\r\n" if b"\r\n" in data else b"\n"
norm = lambda ls: [re.sub(rb"\s+", b" ", x).strip() for x in ls if x.strip()]
revs = []
for tp in theirs_paths:
    tf = norm(open(tp, "rb").read().replace(b"\r\n", b"\n").split(b"\n")); index = {}
    for i, x in enumerate(tf):
        index.setdefault(x, []).append(i)
    revs.append((tf, index, b"".join(x.replace(b" ", b"") for x in tf)))


def contained(seq):
    flat = b"".join(x.replace(b" ", b"") for x in seq)
    if len(seq) < 3 or len(flat) < 60:
        return False
    return any(any(tf[i:i + len(seq)] == seq for i in index.get(seq[0], [])) or flat in whole for tf, index, whole in revs)


out, ours, base, theirs, state, hunks, done = [], [], [], [], None, 0, 0
for line in data.split(nl):
    if state is None and line.startswith(b"<<<<<<< "):
        state, ours, base, theirs, head = "ours", [], [], [], line; continue
    if state == "ours" and line.startswith(b"||||||| "):
        state, mid = "base", line; continue
    if state == "base" and line == b"=======":
        state = "theirs"; continue
    if state == "theirs" and line.startswith(b">>>>>>> "):
        hunks += 1; state = None
        if contained(norm(ours)):
            out += theirs; done += 1
        else:
            out += [head] + ours + [mid] + base + [b"======="] + theirs + [line]
        continue
    {"ours": ours, "base": base, "theirs": theirs}.get(state, out).append(line)
if state is not None:
    sys.exit(f"{path}: unterminated conflict")
open(path, "wb").write(nl.join(out))
print(f"{path}: {done} of {hunks} hunks contained upstream")
sys.exit(0 if done == hunks else 1)
