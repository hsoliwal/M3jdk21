#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom JBS_BACKLOG_JOIN: one row per JBS issue fixed in JDK 22..27 (Bug, Enhancement, New Feature, Sub-task) plus every
JEP in every state, joined mechanically with the openjdk/jdk commit history (subject/body lines "NNNNNNN: title"), the
jdk21u-dev history (same id convention) and M3jdk21 master. No network, no LLM; inputs are immutable snapshots.
Usage: build_backlog.py <data dir> <out dir>"""
import csv, os, re, sys, collections
csv.field_size_limit(1 << 24)
D, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
rd = lambda p: list(csv.DictReader(open(os.path.join(D, p), encoding="utf-8", newline=""), delimiter="\t"))

def ids(p):
    m = collections.defaultdict(list)
    for line in open(os.path.join(D, p), encoding="utf-8"):
        sha, key = line.rstrip("\n").split("\t"); m[key].append(sha)
    return m

up_ids, u21, m3 = ids("upstream_commit_ids.tsv"), ids("jdk21u_dev_commit_ids.tsv"), ids("m3jdk21_master_commit_ids.tsv")
meta = {}
for line in open(os.path.join(D, "upstream_commits_raw.txt"), encoding="utf-8"):
    sha, date, author, subj = line[2:].rstrip("\n").split("\t", 3); meta[sha] = (date, subj)
paths, cur = collections.defaultdict(list), None
for line in open(os.path.join(D, "upstream_commit_paths.txt"), encoding="utf-8"):
    line = line.rstrip("\n")
    if line.startswith("@@"): cur = line[2:]
    elif line: paths[cur].append(line)

NATIVE = re.compile(r"^src/[^/]+/[^/]+/native/|\.(c|cc|cpp|h|hpp|m|mm|s|S|asm)$")
CLASSES = re.compile(r"^src/[^/]+/[^/]+/(classes|data|conf|legal|man)/")
RANK = ["test-only", "java-only", "build", "native", "hotspot"]
def area_of(ps):
    a = 0
    for p in ps:
        if p.startswith("test/"): continue
        if p.startswith("src/hotspot/"): a = max(a, 4)
        elif NATIVE.search(p): a = max(a, 3)
        elif CLASSES.match(p): a = max(a, 1)
        else: a = max(a, 2)
    return RANK[a]

sub = {r["jbs"]: r["subcomponent"] for r in rd("scaffold/JBS_ENHANCEMENTS_22_27.tsv")}
rows = []
REL = [str(v) for v in range(22, 28)]
OWN = os.path.exists(os.path.join(D, "jbs/fixed2227.tsv"))   # own snapshot carries subcomponent and every issue type
for r in rd("jbs/fixed2227.tsv" if OWN else "peer/mainline22plus.tsv"):
    if OWN and r["subcomponent"] != "-": sub[r["key"]] = r["subcomponent"]
    k = r["key"]; shas = sorted(up_ids.get(k, []), key=lambda s: meta.get(s, ("",))[0])
    fv = [v for v in r["fixVersions"].split(",") if v in REL] or [r["fixVersions"]]
    area = area_of([p for s in shas for p in paths.get(s, [])]) if shas else "unknown"
    state = "IN_M3JDK21" if k in m3 else "IN_JDK21U" if k in u21 else "NO_UPSTREAM_COMMIT" if not shas else "CANDIDATE"
    rows.append([k, r["issuetype"], ",".join(fv), r["components"] or "-", sub.get(k, "-"), r["summary"],
                 ",".join(shas) or "-", "yes" if k in u21 else "no", area, state])

jep_key, jep_rows = {}, []
for r in rd("peer/jeps.tsv"):
    m = re.match(r"JEP (\d+):", r["summary"]); n = m.group(1) if m else ""
    if n: jep_key[n] = r["key"]
    st = f'{r["status"]}/{r["resolution"] or "-"}'
    vet = "VETOED_WITHDRAWN" if r["resolution"] == "Withdrawn" else "VETOED_REJECTED" if r["resolution"] == "Rejected" else "JEP_" + st.replace("/", "_").replace(" ", "_").upper().rstrip("_-")
    jep_rows.append([r["key"], "JEP", r["fixVersions"] or "-", r["components"] or "-", "-", r["summary"], "-", "-", "-", vet])
sha_keys = collections.defaultdict(list)
for k, v in up_ids.items():
    for s in v: sha_keys[s].append(k)
jep_commits = collections.defaultdict(list)
for sha, (date, subj) in meta.items():
    for n in re.findall(r"JEP[ -](\d{3})\b", subj):
        if n in jep_key: jep_commits[jep_key[n]].append((date, sha))
for jr in jep_rows:
    cs = [s for _, s in sorted(jep_commits.get(jr[0], []))]
    if cs:
        jr[6] = ",".join(cs); jr[8] = area_of([p for s in cs for p in paths.get(s, [])])
        jr[7] = "yes" if any(k in u21 for s in cs for k in sha_keys[s]) else "no"
rows += jep_rows
rows.sort(key=lambda r: int(r[0].split("-")[1]))
H = ["jbs_id", "type", "fix_version", "component", "subcomponent", "title", "upstream_commit", "in_jdk21u", "area", "state"]
with open(os.path.join(OUT, "BACKLOG.tsv"), "w", encoding="utf-8", newline="\n") as o:
    o.write("\t".join(H) + "\n")
    for r in rows: o.write("\t".join(c.replace("\t", " ") for c in r) + "\n")
known = {r[0] for r in rows}
with open(os.path.join(OUT, "UPSTREAM_IDS_OUTSIDE_22_27.tsv"), "w", encoding="utf-8", newline="\n") as o:
    o.write("jbs_id\tupstream_commit\tdate\tarea\tin_jdk21u\tsubject\n")
    for k in sorted(set(up_ids) - known, key=lambda k: int(k.split("-")[1])):
        s = sorted(up_ids[k], key=lambda s: meta[s][0])
        o.write(f'{k}\t{",".join(s)}\t{meta[s[0]][0][:10]}\t{area_of([p for x in s for p in paths[x]])}\t{"yes" if k in u21 else "no"}\t{meta[s[0]][1]}\n')
print("backlog", len(rows), "outside", len(set(up_ids) - known))
