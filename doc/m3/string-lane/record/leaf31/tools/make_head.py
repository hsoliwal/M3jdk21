# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""make_head.py <base-head> <slice_family.txt> <case_family.txt> <out-head>: inserts the case family text right after
the (verbatim, once) slice family text of the base head."""
import sys
base, slice_, case, out = sys.argv[1:5]
read = lambda p: open(p, encoding="utf-8", newline="").read().replace("\r", "")
head, s, c = read(base), read(slice_), read(case)
assert head.count(s) == 1
at = head.index(s) + len(s)
open(out, "w", encoding="utf-8", newline="").write(head[:at] + c + head[at:])
print("inserted", c.count("\n"), "lines at offset", at)
