# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""gen_map_e2.py <m3/collections/src dir> <java_names.txt> <out RENAME_MAP_E2.tsv>

CURATE_E pass E2 map: every M3-prefixed type of the public core package jdk.m3.collections (main and test trees) drops the
prefix, except where the bare name is a java.* simple name (M3HashMap, M3ArrayList, ...). A test keeps M3 when the
longest main type its name starts with is such a kept type (M3ArrayListRemoveIfTest stays with M3ArrayList).
Refuses on a target that already exists."""
import os
import sys

src, javaf, out = sys.argv[1:4]
JAVA = set(l.strip() for l in open(javaf, encoding="utf-8") if l.strip())
core = lambda tree: os.path.join(src, tree, "java", "jdk", "m3", "collections")
names = lambda tree: sorted(f[:-5] for f in os.listdir(core(tree)) if f.endswith(".java"))
mains, tests = names("main"), names("test")
m3main = [n for n in mains if n.startswith("M3") and n[2:3].isupper()]
kept = {n for n in m3main if n[2:] in JAVA}
renamed = [n for n in m3main if n not in kept]
rows = [("jdk.m3.collections." + n, "jdk.m3.collections." + n[2:], "main") for n in renamed]
for t in tests:
    if not (t.startswith("M3") and t[2:3].isupper()) or t[2:] in JAVA:
        continue
    pref = lambda s: t.startswith(s) and (len(t) == len(s) or t[len(s)].isupper())
    bk = max((k for k in kept if pref(k)), key=len, default="")
    br = max((m for m in renamed if pref(m)), key=len, default="")
    if bk and len(bk) >= len(br):
        continue
    rows.append(("jdk.m3.collections." + t, "jdk.m3.collections." + t[2:], "test"))
existing = set(mains) | set(tests)
clash = [r for r in rows if r[1].rsplit(".", 1)[1] in existing]
if clash or len({r[1] for r in rows}) != len(rows):
    raise SystemExit("E2 target clash: " + " ".join(r[1] for r in clash[:8]))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("old_fqn\tnew_fqn\tkind\n")
    for r in rows:
        w.write("\t".join(r) + "\n")
print(f"E2 map: {sum(1 for r in rows if r[2] == 'main')} main, {sum(1 for r in rows if r[2] == 'test')} tests; kept M3: {len(kept)}")
