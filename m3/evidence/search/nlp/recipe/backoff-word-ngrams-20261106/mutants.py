#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Writes the fault variants and the mutants of the relocated M3BackoffWordNgrams.java used by differential.sh.

Each variant is one exact text replacement of the relocated source, refused (exit 2) unless the needle occurs exactly
the expected number of times, so a drift of the owner body cannot silently turn a mutant into the original:
  fault-rows     the five Arrays.copyOf calls of MutableCounts -> M3BackoffWordNgramsTrainingArrayFault.copyOf
                 (the donor's verify-training.sh fault variant; M3BackoffWordNgramsTrainingGrowthOracle must PASS)
  fault-context  the six Arrays.copyOf calls of MutableContextIndex -> M3BackoffWordNgramsTrainingContextArrayFault.copyOf
                 (verify-training-context.sh; M3BackoffWordNgramsTrainingContextGrowthOracle must PASS)
  eager-publish  fault-context that publishes the grown labels array before the next copy (must FAIL with
                 "array reference unchanged labels failure=2")
  double-compare the ranking comparison on (double) counts instead of long counts (QUALIFICATION.md mutant; the rank
                 oracle --long-counts must FAIL with "exact long ranking")
  root-backoff   every history lookup misses (verify-context.sh; the context oracle must FAIL with
                 "reverse suffix / depth / ordering parity")
  stale-child    AVL insertion assigns the child slot from a stale array reference (verify-training.sh; the training
                 oracle --layout must FAIL with "reachable node 4")
  unbalanced     AVL rotations disabled (verify-training.sh; the training oracle --layout must FAIL with "AVL_BALANCE")
usage: python -I mutants.py <relocated M3BackoffWordNgrams.java> <out-dir>   (writes <out-dir>/<variant>/M3BackoffWordNgrams.java)
"""
import sys
from pathlib import Path

ROWS_FAULT = "M3BackoffWordNgramsTrainingArrayFault.copyOf("
CONTEXT_FAULT = "M3BackoffWordNgramsTrainingContextArrayFault.copyOf("


class Refused(Exception):
    pass


def once(text: str, needle: str, replacement: str, count: int = 1) -> str:
    if text.count(needle) != count:
        raise Refused("needle occurs %d times, expected %d: %r" % (text.count(needle), count, needle[:80]))
    return text.replace(needle, replacement)


def block(text: str, start: str, end: str, copies: int, helper: str) -> str:
    a = text.index(start)
    b = text.index(end, a)
    return text[:a] + once(text[a:b], "Arrays.copyOf(", helper, copies) + text[b:]


def within(text: str, start: str, end: str, edit) -> str:
    a = text.index(start)
    b = text.index(end, a)
    return text[:a] + edit(text[a:b]) + text[b:]


ROWS = ("    private static final class MutableCounts {", "    /** Training-local reverse-context trie")


def variants(s: str) -> dict:
    # The donor's verify-training.sh ends the MutableCounts block at "/** Reverse-token trie" and disables every
    # "if (balance ...)": both predate MutableContextIndex (b027a57284d), which added 6 copies and a second AVL; the
    # blocks here end at the next nested class, and the unbalanced mutant edits the training row only.
    rows = block(s, ROWS[0], ROWS[1], 5, ROWS_FAULT)
    context = block(s, "    private static final class MutableContextIndex {",
                    "    /** Reverse-token trie in compressed rows", 6, CONTEXT_FAULT)
    labels = ("            String[] newLabels = " + CONTEXT_FAULT + "labels, capacity);\n"
              "            int[] newChildRoots = " + CONTEXT_FAULT + "childRoots, capacity);")
    eager = once(context, labels, labels.replace("capacity);\n", "capacity);\n            labels = newLabels;\n", 1))
    double = once(s, "            return frequencies[a] < frequencies[b]\n"
                     "                    || (frequencies[a] == frequencies[b] && a > b);",
                  "            return (double) frequencies[a] < (double) frequencies[b]\n"
                  "                    || ((double) frequencies[a] == (double) frequencies[b] && a > b);")
    root = once(s, "int child = table.child(node, frozen.get(frozen.size() - 1 - length));", "int child = -1;")
    stale = once(once(s, "                int child = insert(left[node], token);\n                left[node] = child;",
                      "                left[node] = insert(left[node], token);"),
                 "                int child = insert(right[node], token);\n                right[node] = child;",
                 "                right[node] = insert(right[node], token);")
    unbalanced = within(s, ROWS[0], ROWS[1],
                        lambda t: once(once(t, "if (balance > 1)", "if (false)"), "if (balance < -1)", "if (false)"))
    return {"fault-rows": rows, "fault-context": context, "eager-publish": eager, "double-compare": double,
            "root-backoff": root, "stale-child": stale, "unbalanced": unbalanced}


def main() -> int:
    source, out = Path(sys.argv[1]), Path(sys.argv[2])
    try:
        made = variants(source.read_bytes().decode("utf-8"))
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2
    for name, text in made.items():
        (out / name).mkdir(parents=True, exist_ok=True)
        (out / name / source.name).write_bytes(text.encode("utf-8"))
        print("MUTANT %s written" % name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
