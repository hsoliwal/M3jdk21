# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""reach_census.py <worktree> <out REACH.tsv>

REACH_CENSUS v1: which jdk.internal.m3.collections types does the JDK actually need? Roots = every public
jdk.m3.collections* type plus every main type of other m3 modules that names an m3/collections type. A type reaches another
when its source (comments and string literals stripped) uses the other's simple name (m3/collections simple names are
unique per package; internal names are M3-prefixed). Main sources only: a type reachable only from tests is NOT needed.
Writes class, package, reachable(0/1), first referrer; prints totals."""
import os
import re
import subprocess
import sys

wt, out = sys.argv[1:3]
ls = subprocess.run(["git", "-C", wt, "ls-files", "m3/*.java"], capture_output=True, text=True, encoding="utf-8").stdout.split()
ls = [p for p in ls if "/src/main/java/" in p and not re.search(r"/(qualification|retained-variants|snapshots|history|target)/", p)]


def strip(t):
    t = re.sub(r"/\*.*?\*/", " ", t, flags=re.S)
    t = re.sub(r"//[^\n]*", " ", t)
    return re.sub(r'"(?:\\.|[^"\\\n])*"', '""', t)


coll = {}
other = []
for p in ls:
    fq = p.split("/src/main/java/", 1)[1][:-5].replace("/", ".")
    if p.startswith("m3/collections/src/main/java/"):
        if not fq.endswith(("module-info", "package-info")):
            coll[fq] = p
    else:
        other.append(p)
simple = {}
for fq in coll:
    simple.setdefault(fq.rsplit(".", 1)[1], []).append(fq)
IDENT = re.compile(r"\b[A-Z][A-Za-z0-9_]*\b")
uses = {}
for fq, p in coll.items():
    t = strip(open(os.path.join(wt, p), encoding="utf-8", errors="replace").read())
    uses[fq] = {x for n in set(IDENT.findall(t)) for x in simple.get(n, ()) if x != fq}
roots = {fq for fq in coll if fq.startswith("jdk.m3.collections")}
ext = {}
for p in other:
    t = strip(open(os.path.join(wt, p), encoding="utf-8", errors="replace").read())
    if "jdk.m3.collections" not in t and "jdk.internal.m3.collections" not in t:
        continue
    for n in set(IDENT.findall(t)):
        for x in simple.get(n, ()):
            ext.setdefault(x, p)
roots |= set(ext)
via = {r: "ROOT" for r in roots}
todo = list(roots)
while todo:
    a = todo.pop()
    for b in uses.get(a, ()):
        if b not in via:
            via[b] = a
            todo.append(b)
internal = sorted(fq for fq in coll if fq.startswith("jdk.internal."))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("class\treachable\tvia\n")
    for fq in internal:
        w.write(f"{fq}\t{1 if fq in via else 0}\t{via.get(fq, '-')}\n")
r = sum(1 for fq in internal if fq in via)
print(f"internal types {len(internal)}: needed by the public API / other modules {r}, unreachable from main code {len(internal) - r}")
print(f"public types {sum(1 for fq in coll if fq.startswith('jdk.m3.collections'))}; other-module referrers {len(set(ext.values()))}")
