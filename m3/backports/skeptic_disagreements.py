#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Rebuild SKEPTIC_DISAGREEMENTS.tsv from a workflow journal: the 'started' rows map agentId -> label (check:<chunk>);
the 'result' rows of the check agents carry {structure_ok, structure_note, disagreements: [{jbs, reading, proposed,
reason}]}. Duplicate rows (a chunk checked twice) collapse. Usage: skeptic_disagreements.py <journal.jsonl> <out.tsv>"""
import csv, json, sys
journal, out = sys.argv[1], sys.argv[2]
labels, rows, checks = {}, set(), {}
for line in open(journal, encoding="utf-8"):
    line = line.strip()
    if not line:
        continue
    o = json.loads(line)
    if o.get("type") == "started":
        labels[o["agentId"]] = o.get("label", "")
    elif o.get("type") == "result":
        label, r = labels.get(o.get("agentId"), ""), o.get("result")
        if not label.startswith("check:") or not isinstance(r, dict):
            continue
        chunk = label.split(":", 1)[1]
        checks[chunk] = (r.get("structure_ok"), len(r.get("disagreements") or []))
        for d in r.get("disagreements") or []:
            rows.add((chunk, d.get("jbs", ""), d.get("reading", ""), d.get("proposed", ""), " ".join(str(d.get("reason", "")).split())))
with open(out, "w", encoding="utf-8", newline="") as f:
    w = csv.writer(f, delimiter="\t", lineterminator="\n")
    w.writerow(["chunk", "jbs", "reading_disposition", "skeptic_proposed", "skeptic_reason"])
    w.writerows(sorted(rows))
print(f"checks {len(checks)} chunks, disagreements {len(rows)}")
for c in sorted(checks):
    print(f"  {c}: structure_ok={checks[c][0]} disagreements={checks[c][1]}")
