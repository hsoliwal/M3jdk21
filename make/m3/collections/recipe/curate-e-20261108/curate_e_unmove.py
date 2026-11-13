# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_e_unmove.py <maven log> <RENAME_MAP_E.tsv> <STAY_CORE.txt> <why.tsv>   (env CURATE_CORE)

Compiler fixed point for CURATE_E (curate_bc_unmove.py generalized to any core package and any family set). A type whose
move into a family subpackage breaks package coupling keeps its new simple name but stays in CURATE_CORE:
  - a diagnostic in a file under <core path>/<family>/F.java -> the type moved to <family>.F stays core;
  - a diagnostic anywhere whose message or the next 3 lines name <core>.<family>.N -> N stays core.
Exit 0 if STAY_CORE grew (another round is needed), 1 if nothing new (stuck or clean)."""
import os
import re
import sys

log, mapf, stayf, whyf = sys.argv[1:5]
CORE = os.environ.get("CURATE_CORE", "jdk.m3.collections")
back, fams = {}, set()
for l in open(mapf, encoding="utf-8"):
    c = l.rstrip("\r\n").split("\t")
    if len(c) > 1 and c[0].startswith(CORE + ".") and c[1] != c[0]:
        back[c[1]] = c[0][len(CORE) + 1:]
        rest = c[1][len(CORE) + 1:] if c[1].startswith(CORE + ".") else ""
        if "." in rest:
            fams.add(rest.split(".")[0])
stay = set(l.strip() for l in open(stayf, encoding="utf-8") if l.strip()) if os.path.exists(stayf) else set()
fam = "|".join(sorted(fams, key=len, reverse=True)) or "\\b\\B"
cpath = CORE.replace(".", "/")
loc = re.compile(r"\[(?:ERROR|WARNING)\] /\S*?/" + re.escape(cpath) + r"/(?:(" + fam + r")/)?([\w$]+)\.java:\[\d+,\d+\] (.*)")
ref = re.compile(re.escape(CORE) + r"\.(" + fam + r")\.([A-Z]\w*)")
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
    ctx = msg + " " + " ".join(lines[i + 1:i + 4])
    for sub2, n in ref.findall(ctx):
        s = back.get(f"{CORE}.{sub2}.{n}")
        if s and s not in stay:
            new.setdefault(s, f"{f} needs it in its package: {msg[:60]}")
stay |= set(new)
open(stayf, "w", encoding="utf-8", newline="\n").write("\n".join(sorted(stay)) + "\n")
with open(whyf, "a", encoding="utf-8", newline="\n") as w:
    for k, v in sorted(new.items()):
        w.write(f"{k}\t{v}\n")
print(f"stay-core +{len(new)} (total {len(stay)})")
sys.exit(0 if new else 1)
