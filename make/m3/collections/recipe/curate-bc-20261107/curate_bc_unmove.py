# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_bc_unmove.py <maven log> <RENAME_MAP.tsv> <STAY_CORE.txt> <why.tsv>

Compiler fixed point for curation B+C: a type whose move into a family subpackage breaks package coupling keeps its
new simple name but stays in the core package jdk.m3.collections (it is added to STAY_CORE). From the -Werror log:
  an error/warning in a file under jdk/m3/collections/<family>/ -> that file's type stays core;
  a diagnostic elsewhere naming jdk.m3.collections.<family>.<N> (message or the symbol/location lines) -> N stays core.
Exit 0 if STAY_CORE grew."""
import re
import sys

log, mapf, stayf, whyf = sys.argv[1:5]
CORE = "jdk.m3.collections"
back = {}
for l in open(mapf, encoding="utf-8"):
    c = l.rstrip("\n").split("\t")
    if len(c) > 1 and c[0].startswith(CORE + ".") and c[1] != c[0]:
        back[c[1]] = c[0][len(CORE) + 1:]
stay = set(l.strip() for l in open(stayf, encoding="utf-8") if l.strip()) if __import__("os").path.exists(stayf) else set()
fam = "bits|concurrent|graph|range|sketch|text|packed"
loc = re.compile(r"\[(?:ERROR|WARNING)\] /\S*?/jdk/m3/collections/(?:(" + fam + r")/)?([\w$]+)\.java:\[\d+,\d+\] (.*)")
lines = open(log, encoding="utf-8", errors="replace").read().splitlines()
new = {}
for i, line in enumerate(lines):
    m = loc.search(line)
    if not m:
        continue
    sub, f, msg = m.group(1), m.group(2), m.group(3)
    if sub:
        s = back.get(f"{CORE}.{sub}.{f}")
        if s and s not in stay:
            new.setdefault(s, f"moved {sub}.{f} breaks coupling: {msg[:70]}")
        continue
    ctx = msg + " " + " ".join(lines[i + 1:i + 4])
    for sub2, n in re.findall(r"jdk\.m3\.collections\.(" + fam + r")\.([A-Z]\w*)", ctx):
        s = back.get(f"{CORE}.{sub2}.{n}")
        if s and s not in stay:
            new.setdefault(s, f"core {f} needs it in its package: {msg[:60]}")
stay |= set(new)
open(stayf, "w", encoding="utf-8", newline="\n").write("\n".join(sorted(stay)) + "\n")
with open(whyf, "a", encoding="utf-8", newline="\n") as w:
    for k, v in sorted(new.items()):
        w.write(f"{k}\t{v}\n")
print(f"stay-core +{len(new)} (total {len(stay)})")
sys.exit(0 if new else 1)
