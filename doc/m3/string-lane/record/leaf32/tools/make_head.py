# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""make_head.py <base-head> <case_family.txt> <escape_code_family.txt> <out-head>: inserts the escape-code family
text right after the (verbatim, once) case family text of the base head."""
import sys
base, case, escape, out = sys.argv[1:5]
read = lambda p: open(p, encoding="utf-8", newline="").read().replace("\r", "")
head, c, e = read(base), read(case), read(escape)
assert head.count(c) == 1
at = head.index(c) + len(c)
open(out, "w", encoding="utf-8", newline="").write(head[:at] + e + head[at:])
print("inserted", e.count("\n"), "lines at offset", at)
