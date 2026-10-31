# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""rename_sed.py <TYPE_RENAME.tsv> <out.sed> : one GNU sed substitution per type (old FQN -> new FQN, whole name), longest
first, to map a javap surface of the donor onto the absorbed names before comparing."""
import sys

rows = []
with open(sys.argv[1], encoding="utf-8") as f:
    next(f)
    rows = [l.rstrip("\n").split("\t") for l in f if l.strip()]
with open(sys.argv[2], "w", encoding="utf-8", newline="\n") as w:
    for old, new in sorted(rows, key=lambda r: -len(r[0])):
        w.write("s/\\b" + old.replace(".", "\\.") + "\\b/" + new + "/g\n")
print(len(rows))
