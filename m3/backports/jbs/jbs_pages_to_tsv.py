#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom JBS_PAGES_TSV: flatten jbs_snapshot.sh pages into one TSV row per issue (key, issuetype, status, resolution,
fixVersions, components, subcomponent, labels, priority, resolved, csr, summary), plus csr_linked.tsv (issue -> CSR).
Usage: jbs_pages_to_tsv.py <pages dir> <out tsv> <csr out tsv>"""
import glob, json, sys
d, out, csr_out = sys.argv[1:4]
rows, csr = {}, []
j = lambda xs: ",".join(sorted(x.get("name", "") for x in (xs or [])))
for p in sorted(glob.glob(d + "/p*.json")):
    for i in json.load(open(p, encoding="utf-8"))["issues"]:
        f = i["fields"]; cs = []
        for l in f.get("issuelinks") or []:
            if l.get("type", {}).get("name") == "CSR":
                o = l.get("outwardIssue") or l.get("inwardIssue") or {}
                cs.append(o.get("key", "")); csr.append((i["key"], o.get("key", ""), ((o.get("fields") or {}).get("status") or {}).get("name", "")))
        rows[i["key"]] = [i["key"], f["issuetype"]["name"], f["status"]["name"], (f.get("resolution") or {}).get("name", ""),
                          j(f.get("fixVersions")), j(f.get("components")), (f.get("customfield_10008") or {}).get("name", "-"),
                          ",".join(sorted(f.get("labels") or [])), (f.get("priority") or {}).get("name", ""),
                          (f.get("resolutiondate") or "")[:10], ",".join(cs) or "-", " ".join((f.get("summary") or "").split())]
with open(out, "w", encoding="utf-8", newline="\n") as o:
    o.write("key\tissuetype\tstatus\tresolution\tfixVersions\tcomponents\tsubcomponent\tlabels\tpriority\tresolved\tcsr\tsummary\n")
    for k in sorted(rows, key=lambda k: int(k.split("-")[1])): o.write("\t".join(rows[k]) + "\n")
with open(csr_out, "w", encoding="utf-8", newline="\n") as o:
    o.write("jbs_id\tcsr\tcsr_status\n" + "".join("\t".join(r) + "\n" for r in csr))
print("rows", len(rows), "csr_links", len(csr))
