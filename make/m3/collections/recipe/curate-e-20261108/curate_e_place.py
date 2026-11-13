# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""curate_e_place.py <RENAME_MAP_E1.tsv (intended)> <javac log> <tree src root> <edges.tsv (accumulated)> <out map>

CURATE_E placement round. From a javac log (no error cap) every package-coupling diagnostic
("is not public in", "cannot be accessed from outside package", "is not visible") yields an edge between the type of the
failing file and the type the message names; edges accumulate in edges.tsv across rounds. Union-find over all edges;
each component gets ONE package = the most common INTENDED package of its main members (root unless a family has the
strict majority over root). Tests that follow a subject go with it. Writes the next map; prints new edge count
(0 = placement stable)."""
import collections
import os
import re
import sys

mapf, logf, src, edgef, out = sys.argv[1:6]
CORE = "jdk.internal.m3.collections"
rows = [l.rstrip("\r\n").split("\t") for l in open(mapf, encoding="utf-8")]
hdr, rows = rows[0], rows[1:]
intended = {r[0]: r[1] for r in rows}
newsimple = collections.defaultdict(set)
for o, n in intended.items():
    newsimple[n.rsplit(".", 1)[1]].add(o)
edges = set()
if os.path.exists(edgef):
    edges = {tuple(l.rstrip("\n").split("\t")) for l in open(edgef, encoding="utf-8") if l.strip()}
before = len(edges)
pat = re.compile(r"^(.*?\.java):\d+: error: (.*)$")
key = re.compile(r"is not public in|cannot be accessed from outside package|is not visible|has protected access")
# current location of every type in the tree (file stem -> old fqn) via the map's new names
lines = open(logf, encoding="utf-8", errors="replace").read().splitlines()
# v1.1: "cannot find symbol" for a member of another type (an inherited or same-package package-private member that the
# move made invisible) names only the member; the partner is the internal type referenced by the failing file that
# DECLARES that member (source scan of the curated tree)
src_of = {}
for base_, _, fs in os.walk(src):
    for f in fs:
        if f.endswith(".java") and os.sep + "jdk" + os.sep + "internal" + os.sep + "m3" + os.sep + "collections" in base_ + os.sep:
            src_of.setdefault(f[:-5], []).append(os.path.join(base_, f))
_txt = {}


def text(p):
    if p not in _txt:
        _txt[p] = open(p, encoding="utf-8", errors="replace").read()
    return _txt[p]


def declares(p, member):
    return re.search(r"(?:[\w>\]]\s+)" + re.escape(member) + r"\s*(?:[=;(,)]|\[)", text(p)) is not None


for i, l in enumerate(lines):
    m = pat.match(l)
    if m and "cannot find symbol" in m.group(2):
        sym = next((re.search(r"symbol:\s+(?:variable|method|class)\s+(\w+)", x) for x in lines[i + 1:i + 4]
                    if "symbol:" in x), None)
        stem = os.path.basename(m.group(1))[:-5]
        if sym:
            refd = set(re.findall(r"\b([A-Z][A-Za-z0-9_]*)\b", text(m.group(1)) if os.path.exists(m.group(1)) else ""))
            for a in newsimple.get(stem, set()):
                for nm in refd:
                    for p in src_of.get(nm, ()):
                        if nm != stem and declares(p, sym.group(1)):
                            for b in newsimple.get(nm, ()):
                                if b != a and b.startswith(CORE + "."):
                                    edges.add(tuple(sorted((a, b))))
        continue
    if not m:
        continue
    # v1.3: continuation lines end at the next diagnostic; the partner is exactly the type the access message names:
    # "<T> is not public in <package>" -> T; "<member> is not public in <Class>" / "has protected access in <Class>" -> Class
    cont = []
    for x in lines[i + 1:i + 8]:
        if pat.match(x):
            break
        cont.append(x)
    msg = m.group(2) + " " + " ".join(cont)
    if not key.search(msg):
        continue
    stem = os.path.basename(m.group(1))[:-5]
    partners = set()
    for nm, cont_in in re.findall(r"\b(\w+)(?:\([^)]*\))? (?:is not public|has (?:protected|private) access|is not visible) in ([\w.]+)", msg):
        partners.add(nm if cont_in[:1].islower() else cont_in.rsplit(".", 1)[-1])
    for a in newsimple.get(stem, set()):
        for nm in partners:
            for b in newsimple.get(nm, ()):
                if b != a and b.startswith(CORE + "."):
                    edges.add(tuple(sorted((a, b))))
with open(edgef, "w", encoding="utf-8", newline="\n") as w:
    for e in sorted(edges):
        w.write("\t".join(e) + "\n")
parent = {}


def find(x):
    parent.setdefault(x, x)
    while parent[x] != x:
        parent[x] = parent[parent[x]]
        x = parent[x]
    return x


# v1.3: a test with a subject always shares the subject's package, so its edges attach to the subject
subject_of = {}
_mains = {r[0].rsplit(".", 1)[1]: r[0] for r in rows if r[2] == "main"}
for r in rows:
    if r[2].startswith("test:") and r[2] != "test:-":
        s = r[2][5:]
        s = _mains.get(s) or _mains.get("M3" + s)
        if s:
            subject_of[r[0]] = s
for a, b in edges:
    parent[find(subject_of.get(a, a))] = find(subject_of.get(b, b))
mains = [r[0] for r in rows if r[2] == "main"]
lone_tests = [r[0] for r in rows if r[2] == "test:-"]
comp = collections.defaultdict(list)
for o in mains:
    comp[find(o)].append(o)
# v1.2: a subject-less test coupled to main types follows their component (votes stay main-only)
follow = {o: find(o) for o in lone_tests if find(o) in comp}
pkg_of = lambda fq: fq.rsplit(".", 1)[0]
place = {}
for members in comp.values():
    votes = collections.Counter(pkg_of(intended[m]) for m in members)
    fams = sorted(((n, p) for p, n in votes.items() if p != CORE), reverse=True)
    best = fams[0][1] if fams and fams[0][0] > votes.get(CORE, 0) else CORE
    for m in members:
        place[m] = best
subj = {}
res = []
for o, n, k in rows:
    if k == "main":
        n = place[o] + "." + n.rsplit(".", 1)[1]
        subj[o.rsplit(".", 1)[1]] = n
    elif k == "test:-" and o in follow:
        n = place[comp[follow[o]][0]] + "." + n.rsplit(".", 1)[1]
    res.append([o, n, k])
for r in res:
    if r[2].startswith("test:") and r[2] != "test:-":
        s = r[2][5:]
        s = s if s in subj else ("M3" + s if "M3" + s in subj else None)
        if s:
            r[1] = pkg_of(subj[s]) + "." + r[1].rsplit(".", 1)[1]
tg = collections.Counter(r[1] for r in res)
dup = [k for k, v in tg.items() if v > 1]
if dup:
    raise SystemExit("duplicate targets: " + " ".join(dup[:8]))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("\t".join(hdr) + "\n")
    for r in res:
        w.write("\t".join(r) + "\n")
dist = collections.Counter(pkg_of(r[1]) for r in res if r[2] == "main")
print(f"edges {len(edges)} (+{len(edges) - before}); components>1 {sum(1 for v in comp.values() if len(v) > 1)}; "
      + " ".join(f"{k.replace(CORE, '') or 'root'}={v}" for k, v in sorted(dist.items(), key=lambda x: -x[1])))
sys.exit(0 if len(edges) > before else 1)
