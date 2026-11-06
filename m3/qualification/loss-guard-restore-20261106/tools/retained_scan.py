# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""retained_scan.py <RETAINED_PATHS.txt> <MASTER_PATHS.txt> <out.tsv>
Every m3/qualification/master-landing-20261028/retained-variants/<label>/<rel> on master whose live twin <rel> is
absent from master (set difference). Rows: rel, label, retained_path."""
import sys
PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"
ret_f, mp_f, out_f = sys.argv[1:4]
master = set(open(mp_f, encoding="utf-8").read().split("\n"))
rows = []
for line in open(ret_f, encoding="utf-8"):
    p = line.rstrip("\n")
    if not p.startswith(PREFIX):
        continue
    label, _, rel = p[len(PREFIX):].partition("/")
    if rel and rel not in master:
        rows.append((rel, label, p))
with open(out_f, "w", encoding="utf-8", newline="\n") as o:
    o.write("rel\tlabel\tretained_path\n")
    for r in sorted(rows):
        o.write("\t".join(r) + "\n")
print("retained", sum(1 for _ in open(ret_f)), "orphans", len(rows), "unique rel", len({r[0] for r in rows}))
