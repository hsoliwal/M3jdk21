# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""sprawl_census.py <files.txt (pkg/Name.java under src/main/java)> <out.tsv>

SPRAWL_CENSUS v1: mechanical naming/packaging sprawl metrics for m3/collections.
  1. redundant prefix: M3-prefixed types (the package already says m3);
  2. package-in-name: a type whose name is <Segment><OtherType> where Segment is a package segment of the module;
  3. public/internal twins: internal M3X while public X exists;
  4. variant families: types grouped by (element types, container kind); a family with many variants is one API
     with several implementations exposed as separate types.
Writes one row per type (path, name, base, flags, family) and prints the summary."""
import re
import sys

files = [l.strip() for l in open(sys.argv[1], encoding="utf-8") if l.strip()]
out = sys.argv[2]
rows = []
for f in files:
    pkg, name = f.rsplit("/", 1)
    rows.append((pkg.replace("/", "."), name[:-5]))
base = lambda n: n[2:] if n.startswith("M3") and n[2:3].isupper() else n
public = {base(n) for p, n in rows if not p.startswith("jdk.internal")}
segments = {s.capitalize() for p, _ in rows for s in p.split(".")} | {"Selection", "Shapes", "Collections", "Foss", "Jdk"}
names = {base(n) for _, n in rows}
TYPES = r"(?:Int|Long|Byte|Short|Char|Float|Double|Boolean|Object|Ref)"
KIND = r"(Map|BiMap|MultiMap|Set|List|Deque|Queue|Heap|Bag|Table|Cache|Index|Tree|Store|Array|Sequence|Stack|Trie)"
fam_rx = re.compile(r"^(?:Packed|Primitive|Frozen|Concurrent|Segmented|Lazy)*((?:" + TYPES + r")+)\w*?" + KIND + r"$")
stat = {"total": 0, "m3prefix": 0, "pkg_in_name": 0, "twins": 0}
fams = {}
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("package\tname\tbase\tflags\tfamily\n")
    for p, n in sorted(rows):
        b = base(n)
        flags = []
        stat["total"] += 1
        if b != n:
            flags.append("M3_PREFIX"); stat["m3prefix"] += 1
        m = re.match(r"^([A-Z][a-z]+)([A-Z]\w+)$", b)
        if m and m.group(1) in segments and m.group(2) in names:
            flags.append("PKG_IN_NAME"); stat["pkg_in_name"] += 1
        if p.startswith("jdk.internal") and b in public:
            flags.append("PUBLIC_TWIN"); stat["twins"] += 1
        fm = fam_rx.match(b)
        fam = fm.group(1) + fm.group(2) if fm else "-"
        if fm:
            fams.setdefault(fam, []).append(b)
        w.write(f"{p}\t{n}\t{b}\t{','.join(flags) or '-'}\t{fam}\n")
print(f"types {stat['total']}: M3-prefixed {stat['m3prefix']}, package-in-name {stat['pkg_in_name']}, "
      f"internal twins of public types {stat['twins']}")
big = sorted(((len(v), k) for k, v in fams.items() if len(v) >= 4), reverse=True)
print(f"variant families with >=4 types: {len(big)} covering {sum(c for c, _ in big)} types")
for c, k in big[:12]:
    print(f"  {k}: {c}  e.g. {', '.join(sorted(fams[k])[:5])}")
