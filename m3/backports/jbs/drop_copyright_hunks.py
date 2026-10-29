#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom COPYRIGHT_HUNK_DROP: filter a git patch (stdin -> stdout), dropping every hunk whose changed lines are all
copyright-year lines, and every file section left without hunks. Backports keep the target's own header years, so a
patch that applies after this filter differs from upstream only in header years. Exit 3 when nothing was dropped."""
import re, sys
data = sys.stdin.buffer.read().decode("utf-8", "surrogateescape").split("\n")
if data and data[-1] == "": data.pop()
files, cur = [], None
for line in data:
    if line.startswith("diff --git "): cur = {"head": [line], "hunks": []}; files.append(cur)
    elif cur is None: continue
    elif line.startswith("@@"): cur["hunks"].append([line])
    elif cur["hunks"]: cur["hunks"][-1].append(line)
    else: cur["head"].append(line)
dropped, out = 0, []
for f in files:
    keep = []
    for h in f["hunks"]:
        ch = [l for l in h[1:] if l[:1] in "+-"]
        if ch and all(re.search(r"Copyright", l) for l in ch): dropped += 1
        else: keep.append(h)
    if keep or not f["hunks"]:
        out += f["head"]; [out.extend(h) for h in keep]
sys.stdout.buffer.write(("\n".join(out) + "\n").encode("utf-8", "surrogateescape"))
sys.exit(0 if dropped else 3)
