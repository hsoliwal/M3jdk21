# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_e_classify.py <sprawl_files.txt> <LANDED_MAP.tsv> <java_names.txt> <out RENAME_MAP_E.tsv>

CURATE_E draft (user decisions 2026-11-08: drop M3 everywhere unless a java.* simple name clashes; internal family
subpackages; one owner per concept). Per jdk.internal.m3.collections type:
  family = ORIGIN family when the lineage names a coherent Synexia subsystem (matcollections/matvalue -> mat,
           common.collections.shapes -> shape, common.collections.selection -> select), else the B+C name-stem family
           (curation_tiers.py FAMILY: concurrent, bits, graph, range, sketch, text, packed), else the package root;
  name   = the type name without the M3 prefix, unless that simple name is a java.lang / java.util / java.util.function /
           java.util.concurrent / java.io / java.nio type (then M3 stays).
Public jdk.m3.collections* types keep their package and only drop the prefix (same clash rule).
Refuses (row status CLASH) when two types would get the same FQN. Prints the distribution."""
import re
import sys

files, lmap, javanames, out = sys.argv[1:5]
JAVA = set(l.strip() for l in open(javanames, encoding="utf-8") if l.strip())
origin = {}
for l in list(open(lmap, encoding="utf-8"))[1:]:
    s, m = l.rstrip("\r\n").split("\t")[:2]
    origin[m] = s
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
# internal-only container families, tried after the B+C stems (order matters: first match wins)
KIND = [
    ("function", r"(Consumer|Predicate|Function|Supplier|Operator|Comparator|Procedure)$"),
    ("nativeaccess", r"Native|JNI|Jni|Lease|Sdk|Recycler"),
    ("lane", r"Lane|Lanes$"),
    ("view", r"View$|Views$|^Filtered|^Forwarding|^Validating|^Observed|^Locked"),
    ("frozen", r"^Frozen|^Immutable|^ReadOnly|^Cow"),
    ("map", r"Map$|Multimap$|BiMap$|Maps$|Cache$|Caches$"),
    ("set", r"Set$|Sets$|Bag$|Bags$|Multiset$"),
    ("seq", r"List$|Lists$|Sequence$|Queue$|Queues$|Deque$|Deques$|Stack$|Heap$|Heaps$|Array$|Arrays$|Buffer$|Window$"),
    ("index", r"Index$|Indexes$|Tree$|Trees$|Table$|Tables$|Trie$|Search$|Order$|Sort$|Sorter$"),
    ("select", r"^Collection[A-Z]|Provider$|Providers$|^Provider|Admission$|Plan$|Factory$|^Algorithm|Catalog$|Selection$"),
]
ORIGIN = [("mat", r"^com\.synexia\.(matcollections|matvalue)\."), ("shape", r"^com\.synexia\.common\.collections\.shapes\."),
          ("select", r"^com\.synexia\.common\.collections\.selection\.")]
rows, fq_new = [], {}
for f in (l.strip() for l in open(files, encoding="utf-8") if l.strip()):
    pkg, n = f[:-5].rsplit("/", 1)
    pkg = pkg.replace("/", ".")
    old = f"{pkg}.{n}"
    base = n[2:] if n.startswith("M3") and n[2:3].isupper() else n
    name = n if base in JAVA else base
    if pkg == "jdk.internal.m3.collections":
        o = origin.get(old, "")
        # v2 (user 2026-11-08: no packaging sprawl): internal families mirror the public family set exactly; no invented
        # packages (ORIGIN / KIND families retired)
        # v3 (user 2026-11-08: keep, repackage only; no invented packages): Synexia's own subsystem packages (ORIGIN: mat,
        # shape, select) first, then the public family set; the KIND families stay retired
        fam = next((f2 for f2, p in ORIGIN if re.search(p, o)), None) or next((f2 for f2, p in FAMILY if re.search(p, base)), "")
        npkg = pkg + ("." + fam if fam else "")
        rule = ("origin:" if fam and any(f2 == fam for f2, _ in ORIGIN) else "stem:") + (fam or "root")
    else:
        npkg, rule = pkg, "public:prefix"
    new = f"{npkg}.{name}"
    rows.append([old, new, rule])
    fq_new.setdefault(new, []).append(old)
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("old_fqn\tnew_fqn\trule\tstatus\n")
    for old, new, rule in rows:
        st = "CLASH" if len(fq_new[new]) > 1 else ("SAME" if old == new else "RENAME")
        w.write(f"{old}\t{new}\t{rule}\t{st}\n")
dist = {}
for old, new, rule in rows:
    dist[new.rsplit(".", 1)[0]] = dist.get(new.rsplit(".", 1)[0], 0) + 1
for k, v in sorted(dist.items(), key=lambda x: -x[1]):
    print(f"{v:5d} {k}")
print("renamed", sum(1 for o, n, _ in rows if o != n), "clashes", sum(1 for v in fq_new.values() if len(v) > 1),
      "kept M3 (java clash)", sum(1 for o, n, _ in rows if n.rsplit(".", 1)[1].startswith("M3")))
for k, v in fq_new.items():
    if len(v) > 1:
        print("CLASH", k, "<-", " ".join(v))
