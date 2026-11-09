# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""carry_map_e1.py <fresh RENAME_MAP_E1.tsv (new master)> <proven RENAME_MAP_E1_final.tsv> <DEQUALIFY.tsv> <out>

CURATE_E rebase: types already placed by the proven fixed point keep that placement; types new on master take the
fresh draft (family rules + prefix drop); the phase-D qualified names lose their qualifier. Rows for types gone from
master drop out. Refuses on a duplicate target."""
import collections
import sys

fresh, proven, deqf, out = sys.argv[1:5]
rd = lambda f: [l.rstrip("\r\n").split("\t") for l in open(f, encoding="utf-8")]
fr, pr = rd(fresh), rd(proven)
hdr = fr[0]
keep = {r[0]: r[1] for r in pr[1:]}
deq = dict(l.rstrip("\r\n").split("\t") for l in open(deqf, encoding="utf-8") if l.strip())
rows, carried, new = [], 0, 0
for r in fr[1:]:
    if r[0] in keep:
        r[1] = keep[r[0]]; carried += 1
    else:
        s = r[0].rsplit(".", 1)[1]
        if s in deq:
            r[1] = r[1].rsplit(".", 1)[0] + "." + deq[s]
        new += 1
    rows.append(r)
tg = collections.Counter(r[1] for r in rows)
dup = [k for k, v in tg.items() if v > 1]
if dup:
    raise SystemExit("duplicate targets: " + " ".join(dup[:8]))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("\t".join(hdr) + "\n")
    for r in rows:
        w.write("\t".join(r) + "\n")
print(f"carried {carried}, new on master {new}, dropped {len(keep) - carried}")
