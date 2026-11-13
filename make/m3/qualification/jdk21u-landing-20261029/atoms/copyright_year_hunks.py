#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern COPYRIGHT_YEAR_MAX): resolve a diff3 hunk whose three sections are each one line that differs only
in the years of an "Copyright (c) <first>, <last>" header line: keep the side with the later <last> year (the
first-year field must agree). 2026-10-29: 5 of the 6 conflicts of JDK 22 backports replayed onto 21.0.14 were
the Oracle header year alone. Other hunks stay marked.
Usage: copyright_year_hunks.py <diff3 file>   prints "<file>: <n> of <m> hunks copyright-year"; exit 0 = none left."""
import re, sys
path = sys.argv[1]; data = open(path, "rb").read(); nl = b"\r\n" if b"\r\n" in data else b"\n"
YR = re.compile(rb"^(\s*\*?\s*Copyright \(c\) )(\d{4})(?:, (\d{4}))?(,? .*)$")


def year(sec):
    if len(sec) != 1:
        return None
    m = YR.match(sec[0])
    return (m.group(1), m.group(2), int(m.group(3) or m.group(2)), m.group(4)) if m else None


out, st, o, b, t, hunks, done = [], None, [], [], [], 0, 0
for line in data.split(nl):
    if st is None and line.startswith(b"<<<<<<< "):
        st, o, b, t, head = "o", [], [], [], line; continue
    if st == "o" and line.startswith(b"||||||| "):
        st, mid = "b", line; continue
    if st == "b" and line == b"=======":
        st = "t"; continue
    if st == "t" and line.startswith(b">>>>>>> "):
        hunks += 1; st = None; yo, yb, yt = year(o), year(b), year(t)
        if yo and yt and yb and yo[0] == yt[0] and yo[1] == yt[1] and yo[3] == yt[3]:
            out += o if yo[2] >= yt[2] else t; done += 1
        else:
            out += [head] + o + [mid] + b + [b"======="] + t + [line]
        continue
    {"o": o, "b": b, "t": t}.get(st, out).append(line)
if st is not None:
    sys.exit(f"{path}: unterminated conflict")
open(path, "wb").write(nl.join(out))
print(f"{path}: {done} of {hunks} hunks copyright-year")
sys.exit(0 if done == hunks else 1)
