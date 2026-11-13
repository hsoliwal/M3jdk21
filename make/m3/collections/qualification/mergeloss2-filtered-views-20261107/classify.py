#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify every differing line of the MergelossFilteredViewsDifferential base/head outputs (CPU, deterministic).

Lines pair by position (both sides run the same seeded operations). Classes:
  R42_DEEP_DESCENDING  a 60000-layer read-only chain: descendingIterator/last throw StackOverflowError on the base
                       and return on the head (leaf 42), all else on the line equal;
  R27_MEMBERSHIP_*     contains() on a chain with at least one generic Collection (C) or Set (S) layer (leaf 27):
                       RESULT (the answer changed: a comparator alias of a visible stored member), TAPE (predicate
                       arguments/demand changed: the stored representative replaces the equality scan, which also
                       iterated the inner filtered layers), CMP (comparator call count only);
  UNCLASSIFIED         anything else (exit 1).
usage: python -I classify.py <base.out> <head.out> <DIFFERENTIAL.tsv>
"""
import collections
import re
import sys

LINE = re.compile(r"^s=(\d+) t=(\d+) (\S+) (.*?) -> (.*) pred=(.*) cmp=(\d+)$")


def parse(line):
    match = LINE.match(line)
    if not match:
        raise SystemExit("unparsable line: " + line[:120])
    return match.groups()


def classify(base, head):
    b, h = parse(base), parse(head)
    if b[:4] != h[:4]:
        return "UNCLASSIFIED"
    op, tags = b[2], b[3]
    if " deep" in tags and op in ("descendingIterator", "last") and b[4] == "EXC:StackOverflowError" \
            and not h[4].startswith("EXC:") and b[5] == h[5] == "[]":
        return "R42_DEEP_DESCENDING"
    layers = re.search(r"layers=(\S+)", tags)
    if op == "contains" and layers and re.fullmatch(r"[CSZN]*[CS][CSZN]*", layers.group(1)):
        if b[4] != h[4]:
            return "R27_MEMBERSHIP_RESULT"
        if b[5] != h[5]:
            return "R27_MEMBERSHIP_TAPE"
        return "R27_MEMBERSHIP_CMP"
    return "UNCLASSIFIED"


def main(argv):
    base = open(argv[1], encoding="utf-8").read().splitlines()
    head = open(argv[2], encoding="utf-8").read().splitlines()
    if len(base) != len(head):
        raise SystemExit("line counts differ: %d vs %d" % (len(base), len(head)))
    counts = collections.Counter()
    rows = []
    for b, h in zip(base, head):
        if b == h:
            counts["IDENTICAL"] += 1
            continue
        kind = classify(b, h)
        counts[kind] += 1
        rows.append((kind, b, h))
    with open(argv[3], "w", encoding="utf-8", newline="\n") as out:
        out.write("# SPDX-License-Identifier: Apache-2.0\n# classify.py over MergelossFilteredViewsDifferential base/head"
                  " outputs; counts first, then every differing line (base, head).\n")
        out.write("class\tcount\n")
        for kind, count in sorted(counts.items()):
            out.write("%s\t%d\n" % (kind, count))
        out.write("class\tbase\thead\n")
        for kind, b, h in rows:
            out.write("%s\t%s\t%s\n" % (kind, b, h))
    print(" ".join("%s=%d" % kv for kv in sorted(counts.items())))
    return 1 if counts["UNCLASSIFIED"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
