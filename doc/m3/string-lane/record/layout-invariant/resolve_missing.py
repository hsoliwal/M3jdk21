# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""resolve_missing.py <javac.log> <INDEX.txt>: RESOLVE_MISSING v1 (2026-11-17). Prints the master java.base source
paths (relative to share/classes) that javac's errors name: static and plain imports resolved to their longest
existing class file, bare capitalised symbols resolved by unique file name (java/lang first), qualified locations,
and whole packages that do not exist (PKG:<path>). Used by compile_closure.sh."""
import re
import sys

text = open(sys.argv[1], encoding="utf-8", errors="replace").read()
index = set(open(sys.argv[2], encoding="utf-8").read().split())
by_name = {}
for path in index:
    by_name.setdefault(path.rsplit("/", 1)[-1][:-5], []).append(path)
out = set()


def dotted(name):
    parts = name.split(".")
    for cut in range(len(parts), 0, -1):
        path = "/".join(parts[:cut]) + ".java"
        if path in index:
            out.add(path)
            return


for match in re.finditer(r"import (?:static )?([\w.]+);", text):
    dotted(match.group(1))
for match in re.finditer(r"symbol:\s+(?:class|variable|interface) ([A-Z]\w*)", text):
    paths = by_name.get(match.group(1), [])
    pick = [p for p in paths if p.startswith("java/lang/")] or paths
    if len(pick) == 1:
        out.add(pick[0])
for match in re.finditer(r"location: (?:class|interface|variable \w+ of type) ([\w.]+)", text):
    if "." in match.group(1):
        dotted(match.group(1))
for match in re.finditer(r"error: package ([\w.]+) does not exist", text):
    out.add("PKG:" + match.group(1).replace(".", "/"))
print("\n".join(sorted(out)))
