#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Derive the bench-only EAGER page-prefix candidate from the head M3LongTreeSet (G1 design measurement).

The landed G1 owner rebuilds its page-prefix lane lazily (revision = modCount). The rejected alternative keeps the
lane consistent under every mutation: allocated with the page lanes, +1/-1 on every later page per add/remove,
shifted with split/remove, recomputed by the filter compaction. This script rewrites a copy of the head source into
class M3LongTreeSetEagerPrefixCandidate with anchored edits (each anchor exactly once) so BENCH.tsv is reproducible;
the candidate never enters src/.

usage: python -I make_eager_candidate.py HEAD_M3LongTreeSet.java OUT_DIR
"""
import sys
from pathlib import Path

EDITS = [
    ("public final class M3LongTreeSet implements", "public final class M3LongTreeSetEagerPrefixCandidate implements"),
    ("    public M3LongTreeSet() {\n        this(0, DEFAULT_PAGE_CAPACITY);",
     "    public M3LongTreeSetEagerPrefixCandidate() {\n        this(0, DEFAULT_PAGE_CAPACITY);"),
    ("    public M3LongTreeSet(int expectedSize) {", "    public M3LongTreeSetEagerPrefixCandidate(int expectedSize) {"),
    ("    public M3LongTreeSet(int expectedSize, int pageCapacity) {",
     "    public M3LongTreeSetEagerPrefixCandidate(int expectedSize, int pageCapacity) {"),
    ("        freeSlots = new int[pages];\n    }\n",
     "        freeSlots = new int[pages];\n        pagePrefix = new int[pages];\n        prefixRevision = -1;\n    }\n"),
    # add: every later page gains one rank
    ("        pageMaxima[page] = values[offset + count];\n        size++;\n        modCount++;\n        return true;\n",
     "        pageMaxima[page] = values[offset + count];\n        shiftPrefixAfter(page, 1);\n        size++;\n"
     "        modCount++;\n        return true;\n"),
    ("        pageMaxima[0] = value;\n        pageCount = 1;\n",
     "        pageMaxima[0] = value;\n        pagePrefix[0] = 0;\n        pageCount = 1;\n"),
    # remove: every later page loses one rank (before removePage/merge reshape the lanes)
    ("        count--;\n        pageCounts[page] = count;\n        size--;\n",
     "        count--;\n        pageCounts[page] = count;\n        shiftPrefixAfter(page, -1);\n        size--;\n"),
    # split: the new right page starts at prefix + left
    ("            System.arraycopy(pageMaxima, page + 1, pageMaxima, page + 2, after);\n        }\n",
     "            System.arraycopy(pageMaxima, page + 1, pageMaxima, page + 2, after);\n"
     "            System.arraycopy(pagePrefix, page + 1, pagePrefix, page + 2, after);\n        }\n"),
    ("        pageCounts[page + 1] = right;\n",
     "        pageCounts[page + 1] = right;\n        pagePrefix[page + 1] = pagePrefix[page] + left;\n"),
    ("            System.arraycopy(pageMaxima, page + 1, pageMaxima, page, after);\n        }\n        pageCount--;\n",
     "            System.arraycopy(pageMaxima, page + 1, pageMaxima, page, after);\n"
     "            System.arraycopy(pagePrefix, page + 1, pagePrefix, page, after);\n        }\n        pageCount--;\n"),
    ("        size = written;\n        modCount++;\n    }\n",
     "        size = written;\n        modCount++;\n        recomputePrefix();\n    }\n"),
    # growth and trim keep the lane allocated with the page lanes
    ("        int[] nextFreeSlots = Arrays.copyOf(freeSlots, grown);\n",
     "        int[] nextFreeSlots = Arrays.copyOf(freeSlots, grown);\n        pagePrefix = Arrays.copyOf(pagePrefix, grown);\n"),
    ("        int[] nextPrefix = pagePrefix == null ? null : Arrays.copyOf(pagePrefix, pages);\n",
     "        int[] nextPrefix = Arrays.copyOf(pagePrefix, pages);\n"),
    # tail append: the opened page starts at the current size
    ("        pageCounts[pageCount] = 0;\n        return pageCount++;\n",
     "        pageCounts[pageCount] = 0;\n        pagePrefix[pageCount] = size;\n        return pageCount++;\n"),
    ("        modCount++;\n        for (int i = from; i < to; i++) {\n",
     "        for (int i = from; i < to; i++) {\n"),
    # the lane is always current: no rebuild
    ("    private int[] pagePrefix() {\n        int[] prefix = pagePrefix;\n",
     "    private void shiftPrefixAfter(int page, int delta) {\n"
     "        for (int next = page + 1; next < pageCount; next++) {\n            pagePrefix[next] += delta;\n        }\n"
     "    }\n\n    private void recomputePrefix() {\n        int rank = 0;\n"
     "        for (int page = 0; page < pageCount; page++) {\n            pagePrefix[page] = rank;\n"
     "            rank += pageCounts[page];\n        }\n    }\n\n"
     "    private int[] pagePrefix() {\n        if (pagePrefix != null) {\n            return pagePrefix;\n        }\n"
     "        int[] prefix = pagePrefix;\n"),
]


def main() -> int:
    source, out_dir = Path(sys.argv[1]), Path(sys.argv[2])
    text = source.read_bytes().decode("utf-8").replace("\r\n", "\n")
    for old, new in EDITS:
        if text.count(old) != 1:
            print("anchor count %d: %r" % (text.count(old), old[:70]), file=sys.stderr)
            return 2
        text = text.replace(old, new)
    # appendAscending lost its modCount++; restore it after the loop (one bump per batch)
    marker = "                appendValue(sorted[i]);\n            }\n        }\n    }\n"
    if text.count(marker) != 1:
        print("append marker missing", file=sys.stderr)
        return 2
    text = text.replace(marker, "                appendValue(sorted[i]);\n            }\n        }\n        modCount++;\n    }\n")
    out = out_dir / "M3LongTreeSetEagerPrefixCandidate.java"
    out.write_bytes(text.encode("utf-8"))
    print("wrote", out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
