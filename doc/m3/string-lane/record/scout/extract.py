# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""extract.py <workflows-dir> <out-dir>: SCOUT_EXTRACT v1. Reads every run's journal.jsonl (one result line per agent)
and writes SCOUT_CANDIDATES.tsv (one row per candidate with the skeptic verdict when one exists) plus one
<run>.notes.txt per run with the scouts' notes and the skeptics' missed lists. CPU only; nothing is summarised."""
import json, os, sys
root, out = sys.argv[1:3]
clean = lambda s: " ".join(str(s or "").split())
rows = []
for run in sorted(os.listdir(root)):
    path = os.path.join(root, run, "journal.jsonl")
    if not os.path.isfile(path):
        continue
    cands, verdicts, notes = [], {}, []
    for line in open(path, encoding="utf-8"):
        try:
            e = json.loads(line)
        except ValueError:
            continue
        res = e.get("result")
        if e.get("type") != "result" or not isinstance(res, dict):
            continue
        if "candidates" in res:
            cands.extend(res["candidates"])
            notes.append("SCOUT NOTES\n" + str(res.get("notes") or ""))
        if "verdicts" in res:
            verdicts.update({v["id"]: v for v in res["verdicts"]})
            notes.append("SKEPTIC MISSED\n" + str(res.get("missed") or ""))
    for c in cands:
        v = verdicts.get(c["id"])
        rows.append((run, c["id"], "UNVERIFIED" if v is None else ("REFUTED" if v["refuted"] else "SURVIVED"),
                     str(c.get("confidence")), clean(c["title"]), clean(c["file"]).split("/")[-1], clean(c["lines"])[:160],
                     clean(c["algorithm"])[:240], clean((v or {}).get("corrected_claim"))[:900],
                     clean((v or {}).get("implementation_hazards"))[:700]))
    with open(os.path.join(out, run + ".notes.txt"), "w", encoding="utf-8", newline="") as f:
        f.write("\n\n".join(notes) + "\n")
with open(os.path.join(out, "SCOUT_CANDIDATES.tsv"), "w", encoding="utf-8", newline="") as f:
    f.write("run\tid\tverdict\tconfidence\ttitle\tfile\tlines\talgorithm\tcorrected_claim\thazards\n")
    f.writelines("\t".join(r) + "\n" for r in rows)
count = {}
for r in rows:
    count[(r[0], r[2])] = count.get((r[0], r[2]), 0) + 1
print(len(rows), sorted(count.items()))
