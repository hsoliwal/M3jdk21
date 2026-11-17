# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""make_head.py <base String.head.java> <after-text> <new-text> <out String.head.java>

FAMILY_INSERT v1 (2026-11-17). Writes <out> = <base> with <new-text> inserted right after the one verbatim occurrence
of <after-text> (the previous family's method text). LF line ends, bytes otherwise unchanged."""
import sys

base, after, new, out = sys.argv[1:5]
read = lambda p: open(p, encoding="utf-8", newline="").read().replace("\r", "")
head, prev, text = read(base), read(after), read(new)
assert head.count(prev) == 1, "previous family text is not once in the base head"
assert head.count(text) == 0, "new family text is already in the base head"
cut = head.index(prev) + len(prev)
open(out, "w", encoding="utf-8", newline="").write(head[:cut] + text + head[cut:])
print("inserted\t%d lines after offset %d" % (text.count("\n"), cut))
