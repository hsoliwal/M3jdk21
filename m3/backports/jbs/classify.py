#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom BACKPORT_CLASSIFY: join BACKLOG.tsv with the standalone apply checks (apply_check.sh) into one class per JBS issue:
IN_JDK21U (arrives with the jdk21u merge), NO_UPSTREAM_COMMIT, CLEAN (every commit of the issue applies to M3jdk21 master),
CONFLICT_MISSING_FILE (a touched file does not exist on master: an earlier upstream change is a prerequisite),
CONFLICT_CONTEXT (hunk context differs), CONFLICT_HEADER_HUNK (the first failing hunk starts in lines 1-5), CLEAN_EXCEPT_COPYRIGHT (recheck_copyright.sh: applies once copyright-year hunks are dropped),
NOT_CHECKED. Writes CLASSIFY.tsv and CLASS_COUNTS.tsv. Usage: classify.py <BACKLOG.tsv> <apply dir> <out dir>"""
import csv, glob, os, re, sys, collections
csv.field_size_limit(1 << 24)
BL, AD, OUT = sys.argv[1:4]
res = {}
for p in glob.glob(os.path.join(AD, "*.tsv")):
    for line in open(p, encoding="utf-8", errors="replace"):
        f = line.rstrip("\n").split("\t")
        if len(f) >= 3: res[f[0]] = (f[1], f[2])
relaxed = {}   # optional 4th arg: recheck_copyright.sh outputs (commit, CLEAN_EXCEPT_COPYRIGHT | CONFLICT)
for p in glob.glob(sys.argv[4]) if len(sys.argv) > 4 else []:
    for line in open(p, encoding="utf-8"):
        f = line.rstrip("\n").split("\t")
        if len(f) == 2: relaxed[f[0]] = f[1]
def cls(r):
    c = cls0(r)
    if c == "CONFLICT_HEADER_HUNK":
        bad = [s for s in r["upstream_commit"].split(",") if res[s][0] != "CLEAN"]
        if all(relaxed.get(s) == "CLEAN_EXCEPT_COPYRIGHT" for s in bad): return "CLEAN_EXCEPT_COPYRIGHT"
    return c
def cls0(r):
    if r["type"] == "JEP": return "JEP"
    if r["state"] == "IN_JDK21U": return "IN_JDK21U"
    if r["state"] == "IN_M3JDK21": return "IN_M3JDK21"
    if r["state"] == "NO_UPSTREAM_COMMIT": return "NO_UPSTREAM_COMMIT"
    out = [res.get(c) for c in r["upstream_commit"].split(",")]
    if any(o is None for o in out): return "NOT_CHECKED"
    if all(o[0] == "CLEAN" for o in out): return "CLEAN"
    ds = [o[1] for o in out if o[0] != "CLEAN"]
    if any("does not exist in index" in d for d in ds): return "CONFLICT_MISSING_FILE"
    if any(o[0] == "MISSING_BLOB" for o in out): return "NOT_CHECKED"
    if all(re.search(r":[1-5]$", d) for d in ds): return "CONFLICT_HEADER_HUNK"
    return "CONFLICT_CONTEXT"
rows, cnt = [], collections.Counter()
for r in csv.DictReader(open(BL, encoding="utf-8", newline=""), delimiter="\t"):
    c = cls(r); r["class"] = c; rows.append(r)
    cnt[(c, r["area"])] += 1
H = ["jbs_id", "type", "fix_version", "component", "area", "class", "upstream_commit", "apply_detail", "title"]
with open(os.path.join(OUT, "CLASSIFY.tsv"), "w", encoding="utf-8", newline="\n") as o:
    o.write("\t".join(H) + "\n")
    for r in rows:
        det = "; ".join(res[c][1] for c in r["upstream_commit"].split(",") if c in res and res[c][0] != "CLEAN") or "-"
        o.write("\t".join([r["jbs_id"], r["type"], r["fix_version"], r["component"], r["area"], r["class"],
                           r["upstream_commit"], det.replace("\t", " ")[:300], r["title"]]) + "\n")
areas = ["java-only", "test-only", "build", "native", "hotspot", "unknown", "-"]
classes = sorted({c for c, _ in cnt})
with open(os.path.join(OUT, "CLASS_COUNTS.tsv"), "w", encoding="utf-8", newline="\n") as o:
    o.write("class\t" + "\t".join(areas) + "\ttotal\n")
    for c in classes:
        o.write(c + "\t" + "\t".join(str(cnt[(c, a)]) for a in areas) + f"\t{sum(cnt[(c, a)] for a in areas)}\n")
print(open(os.path.join(OUT, "CLASS_COUNTS.tsv"), encoding="utf-8").read())
