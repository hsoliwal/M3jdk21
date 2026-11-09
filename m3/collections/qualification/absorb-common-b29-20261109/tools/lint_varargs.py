# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""lint_varargs.py <javac log> <label>

LINT_SUPPRESS_VARARGS v1.1 (2026-11-09) is a declared repair for donor code under M3's -Xlint:all -Werror build. For
every `<file>:<line>: warning: [varargs]` in the javac log, it walks up from that line to the method's
`@SafeVarargs` and adds `@SuppressWarnings("varargs")` after it, unless one is already there. @SuppressWarnings has
source retention, so the class files do not change. Prints one line per insertion."""
import re
import sys
from collections import defaultdict

log, label = sys.argv[1:3]
sites = defaultdict(set)
for line in open(log, encoding="utf-8", errors="replace"):
    m = re.match(r"(.+\.java):(\d+): warning: \[varargs\]", line.strip())
    if m:
        sites[m.group(1)].add(int(m.group(2)))
for path, lines in sites.items():
    text = open(path, encoding="utf-8").read().split("\n")
    anchors = set()
    for n in lines:
        i = n - 1
        while i >= 0 and "@SafeVarargs" not in text[i]:
            i -= 1
        if i < 0:
            raise SystemExit(f"LINT_SUPPRESS_VARARGS: no @SafeVarargs above {path}:{n}")
        anchors.add(i)
    for i in sorted(anchors, reverse=True):
        inline = "(" in text[i].split("@SafeVarargs", 1)[1]
        if "@SuppressWarnings(\"varargs\")" in (text[i] if inline else text[i + 1]):
            continue
        if inline:
            # v1.1: the method is declared on the @SafeVarargs line itself; annotate it inline
            text[i] = text[i].replace("@SafeVarargs",
                                      f'/* LINT_SUPPRESS (declared, {label}) */ @SuppressWarnings("varargs") @SafeVarargs', 1)
            print("LINT_SUPPRESS_VARARGS", path, i + 1, "inline")
            continue
        indent = re.match(r"\s*", text[i]).group(0)
        text.insert(i + 1, f'{indent}@SuppressWarnings("varargs") // LINT_SUPPRESS (declared, {label}): source-retention')
        print("LINT_SUPPRESS_VARARGS", path, i + 2)
    with open(path, "w", encoding="utf-8", newline="\n") as w:
        w.write("\n".join(text))
