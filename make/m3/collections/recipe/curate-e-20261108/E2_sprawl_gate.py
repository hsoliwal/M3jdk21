# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""sprawl_gate.py <m3 worktree (base)> <java_names.txt> <new main file>...   CURATE_E pass E4 (user decision 2026-11-08:
no naming/packaging sprawl in the JDK). Every NEW m3/collections main type of a landing is checked against the base tree:
  M3_PREFIX     M3<Name> where <Name> is not a java.* simple name (the package already says m3)
  PKG_IN_NAME   <Segment><Type> where Segment is a package segment of the module and <Type> an existing type name
                (packages disambiguate; M3SelectionCollectionRequest-style names are refused)
  ROOT_DUMP     a new type in the internal root jdk.internal.m3.collections whose name matches a family stem
                (bits, concurrent, graph, packed, range, sketch, text) - it belongs in that family package
  SECOND_OWNER  the bare name already names a type in another m3/collections package (one owner per concept: merge into
                the owner or hold)
Prints one row per finding; exit 1 when any REFUSE finding exists, 0 otherwise."""
import os
import re
import subprocess
import sys

wt, javaf, *new = sys.argv[1:]
new = [x for a in new for x in ([l.strip() for l in open(a[1:], encoding="utf-8") if l.strip()] if a.startswith("@") else [a])]
JAVA = set(l.strip() for l in open(javaf, encoding="utf-8") if l.strip())
FAMILY = [
    ("concurrent", r"Spsc|Mpmc|Mpsc|Spmc|Ring|Semaphore|Concurrent"),
    ("bits", r"Bits|BitSet|Bit[A-Z]|Roaring|RankSelect|Bloom|Bitmap|Xor"),
    ("graph", r"Graph|Flow|Matching|Matcher|Bipartite|Connectivity|LowLink|Components|Forest|Lca|Csr|Dag|Paths|"
              r"BellmanFord|ShortestPath|SpanningTree|Hungarian|LinkCut|HeavyLight|Cartesian"),
    ("range", r"Segment|Fenwick|SparseTable|Sqrt|Difference|LiChao|PrefixSum|Range|Interval|Monoid|SumTree"),
    ("sketch", r"HyperLogLog|CountMin|Reservoir|SpaceSaving|TopK|Histogram|Sketch|Sampler|CountingBloom"),
    ("text", r"Suffix|AhoCorasick|Manacher|Palindrome|RollingHash|StringSearch|Substring|CharTrie|Radix|Kmp"),
    ("packed", r"Packed|Big[A-Z]|Packed48|EliasFano|RunLength|DeltaEncoded|Unsigned"),
]
ls = subprocess.run(["git", "-C", wt, "ls-files", "m3/collections/src/main/java/"], capture_output=True, text=True,
                    encoding="utf-8").stdout.split()
existing = {}
segments = set()
for p in ls:
    if p.endswith(".java") and not p.endswith(("module-info.java", "package-info.java")):
        fq = p.split("/src/main/java/", 1)[1][:-5].replace("/", ".")
        existing.setdefault(fq.rsplit(".", 1)[1], set()).add(fq.rsplit(".", 1)[0])
        segments |= {s.capitalize() for s in fq.rsplit(".", 1)[0].split(".")}
# qualifiers may also come from the Synexia source package (M3SelectionCollectionRequest from ...collections.selection):
# with env LANDED_MAP the segments of every landed Synexia package count too
if os.environ.get("LANDED_MAP"):
    for l in list(open(os.environ["LANDED_MAP"], encoding="utf-8"))[1:]:
        s = l.split("\t", 1)[0]
        segments |= {x.capitalize() for x in s.rsplit(".", 1)[0].split(".")}
segments -= {"Jdk", "M3", "Com"}
refuse = 0
for f in new:
    fq = f.replace("\\", "/").split("/src/main/java/", 1)[-1][:-5].replace("/", ".")
    pkg, n = fq.rsplit(".", 1)
    rows = []
    if n.startswith("M3") and n[2:3].isupper() and n[2:] not in JAVA:
        rows.append(("M3_PREFIX", f"drop the prefix: {n[2:]}"))
    m = re.match(r"^(?:M3)?([A-Z][a-z]+)([A-Z]\w+)$", n)
    # v1.1: <Impl><Interface> is the JDK implementation convention (ArrayList implements List): no finding when the class
    # implements or extends the remainder type
    src = ""
    gsrc = os.environ.get("GATE_SRC", "")  # the batch output (main/ tree) holding the new sources
    rel = f.replace("\\", "/").split("/src/main/java/", 1)[-1]
    for cand in (f, os.path.join(wt, f), os.path.join(gsrc, rel) if gsrc else f):
        if os.path.exists(cand):
            src = open(cand, encoding="utf-8", errors="replace").read(); break
    decl = re.search(r"\b(?:class|interface|record|enum)\s+" + re.escape(n) + r"\b([^{]*)\{", src)
    subtype = bool(m and decl and re.search(r"\b(?:extends|implements)\b[^{]*\b(?:M3)?" + re.escape(m.group(2)) + r"\b", decl.group(1)))
    if m and m.group(1) in segments and m.group(2) in existing and not subtype:
        rows.append(("PKG_IN_NAME", f"{m.group(1)} is a package segment and {m.group(2)} an existing type; use the package"))
    base = n[2:] if n.startswith("M3") and n[2:3].isupper() else n
    if pkg == "jdk.internal.m3.collections":
        fam = next((f2 for f2, p2 in FAMILY if re.search(p2, base)), None)
        if fam:
            rows.append(("ROOT_DUMP", f"belongs in jdk.internal.m3.collections.{fam}"))
    others = {p2 for p2 in existing.get(base, set()) | existing.get("M3" + base, set()) if p2 != pkg}
    if others:
        rows.append(("SECOND_OWNER", f"{base} already owned in {', '.join(sorted(others))}; merge into the owner or hold"))
    for k, why in rows:
        print(f"REFUSE\t{k}\t{fq}\t{why}")
        refuse += 1
    if not rows:
        print(f"PASS\t-\t{fq}\t-")
print(f"SPRAWL_GATE new={len(new)} refused_findings={refuse}")
sys.exit(1 if refuse else 0)
