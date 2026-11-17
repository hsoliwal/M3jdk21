# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""make_head.py <base-head> <case_family.txt> <escape_markup_family.txt> <out-head>: inserts the escape-markup family
text right after the (verbatim, once) case family text of the base head (leaf31 String.head.java)."""
import sys
base, case, family, out = sys.argv[1:5]
read = lambda p: open(p, encoding="utf-8", newline="").read().replace("\r", "")
head, c, f = read(base), read(case), read(family)
assert head.count(c) == 1
at = head.index(c) + len(c)
open(out, "w", encoding="utf-8", newline="").write(head[:at] + f + head[at:])
print("inserted", f.count("\n"), "lines at offset", at)
