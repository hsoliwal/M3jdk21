#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom BATCH_PLAN: the CLEAN issues of CLASSIFY.tsv as cherry-pick batches of at most N upstream commits, oldest commit
first, one area per batch in the order java-only, test-only, build, native, hotspot. Issues linked to a CSR (spec or
behaviour change; csr_linked.tsv from the JBS issuelinks) stay out of the mechanical batches: they form the CSR lane
(CSR_LANE.tsv), because M3jdk21 keeps the Java SE 21 specification until a reviewed decision. Writes BATCHES.tsv
(batch, area, seq, upstream_commit, commit_date, jbs_ids) and batches/<nnn>.txt commit lists.
Already landed batches stay fixed: env LANDED=<batch>:<commit list>[,...] keeps those rows and plans the rest after them.
Usage: plan_batches.py <CLASSIFY.tsv> <data dir> <out dir> [N=50] [csr_linked.tsv]"""
import csv, os, sys, collections
csv.field_size_limit(1 << 24)
CL, D, OUT = sys.argv[1:4]; N = int(sys.argv[4]) if len(sys.argv) > 4 else 50
csr = set()
if len(sys.argv) > 5: csr = {l.split("\t")[0] for l in open(sys.argv[5], encoding="utf-8")}
date = {}
for line in open(os.path.join(D, "upstream_commits_raw.txt"), encoding="utf-8"):
    sha, d, _ = line[2:].split("\t", 2); date[sha] = d
by_area, ids, lane = collections.defaultdict(set), collections.defaultdict(set), []
for r in csv.DictReader(open(CL, encoding="utf-8", newline=""), delimiter="\t"):
    if r["class"] != "CLEAN": continue
    if r["jbs_id"] in csr: lane.append([r["jbs_id"], r["area"], r["upstream_commit"], r["title"]]); continue
    for c in r["upstream_commit"].split(","): by_area[r["area"]].add(c); ids[c].add(r["jbs_id"])
os.makedirs(os.path.join(OUT, "batches"), exist_ok=True)
b, rows, landed = 0, [], set()
for spec in filter(None, os.environ.get("LANDED", "").split(",")):
    nb, path = spec.split(":", 1); cs = [l.strip() for l in open(path) if l.strip()]
    b = max(b, int(nb)); landed.update(cs)
    rows += [[f"{int(nb):03d}", "landed", str(j + 1), c, date[c][:10], ",".join(sorted(ids.get(c, [])))] for j, c in enumerate(cs)]
for a in by_area: by_area[a] -= landed
for area in ["java-only", "test-only", "build", "native", "hotspot"]:
    cs = sorted(by_area[area], key=lambda c: date[c])
    for i in range(0, len(cs), N):
        b += 1; chunk = cs[i:i + N]
        open(os.path.join(OUT, "batches", f"{b:03d}.txt"), "w", newline="\n").write("".join(c + "\n" for c in chunk))
        rows += [[f"{b:03d}", area, str(j + 1), c, date[c][:10], ",".join(sorted(ids[c]))] for j, c in enumerate(chunk)]
with open(os.path.join(OUT, "BATCHES.tsv"), "w", encoding="utf-8", newline="\n") as o:
    o.write("batch\tarea\tseq\tupstream_commit\tcommit_date\tjbs_ids\n")
    for r in rows: o.write("\t".join(r) + "\n")
with open(os.path.join(OUT, "CSR_LANE.tsv"), "w", encoding="utf-8", newline="\n") as o:
    o.write("jbs_id\tarea\tupstream_commit\ttitle\n")
    for r in sorted(lane, key=lambda r: date[r[2].split(",")[0]]): o.write("\t".join(r) + "\n")
print("csr_lane", len(lane), "batches", b, "commits", len(rows), {a: len(v) for a, v in by_area.items()})
