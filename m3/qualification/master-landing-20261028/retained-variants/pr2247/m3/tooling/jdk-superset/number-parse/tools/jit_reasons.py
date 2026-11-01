# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""jit_reasons.py <JIT.tsv>

JIT_BATCHES v1 helper (CPU, read-only): the gated C2 inlining failures of a jit_inline.py merge table (JIT.tsv),
summed per reason, before and after. Prints one line: gated_before gated_after reasons_before reasons_after, tab
separated; a reason list is 'reason=count' joined by ',' in reason order ('-' when empty). Reasons are the gated
reasons of jit_inline.py, abbreviated: hot (hot method too big), big / medium (already compiled into a big / medium
method), toobig (too big), large (callee is too large), nodecount (NodeCountInliningCutoff), deep (inlining too
deep), desired (size > DesiredMethodLimit)."""
import collections
import sys

BEFORE_COLUMN = 7
AFTER_COLUMN = 8
NO_FAILURE = "-"
PART_SEPARATOR = "; "
CALLER_SEPARATOR = ">"
COUNT_SEPARATOR = "="
LIST_SEPARATOR = ","
FIELD_SEPARATOR = "\t"
ENCODING = "utf-8"
SHORT = {"hot method too big": "hot", "already compiled into a big method": "big", "too big": "toobig",
         "already compiled into a medium method": "medium", "callee is too large": "large",
         "NodeCountInliningCutoff": "nodecount", "inlining too deep": "deep", "size > DesiredMethodLimit": "desired"}


def sums(rows, column):
    counts = collections.Counter()
    for r in rows:
        if r[column] == NO_FAILURE:
            continue
        for part in r[column].split(PART_SEPARATOR):
            reason, n = part.split(CALLER_SEPARATOR, 1)[1].rsplit(COUNT_SEPARATOR, 1)
            counts[SHORT.get(reason, reason)] += int(n)
    return counts


def listed(counts):
    return LIST_SEPARATOR.join("%s=%d" % kv for kv in sorted(counts.items())) or NO_FAILURE


def main(argv):
    with open(argv[1], encoding=ENCODING) as f:
        rows = [line.rstrip("\n").split(FIELD_SEPARATOR) for line in f][1:]
    before, after = sums(rows, BEFORE_COLUMN), sums(rows, AFTER_COLUMN)
    sys.stdout.write(FIELD_SEPARATOR.join((str(sum(before.values())), str(sum(after.values())), listed(before),
                                           listed(after))) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
