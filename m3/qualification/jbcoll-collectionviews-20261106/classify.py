#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Classify every differing line of two CowViewsDifferential outputs (DIFF_COW_VIEWS v1, same seed and op count).

A differing line is ROOT when its subject's owner state (the digest after the subject's last mutation in the current
4096-operation epoch) is still equal in both runs before the line, else CASCADE (the subject already diverged in this
epoch; every subject is re-populated at each epoch). ROOT lines must match a row of CLASSES (subject regex, operation
set, required pattern of the right-hand result); a ROOT line that matches none is UNCLASSIFIED and the exit status is 1.
CLASSES holds only the causes this leaf established; adding a row is a reviewed change of this file.

usage: python -I classify.py LEFT RIGHT LABEL [--tsv OUT]   (prints one CLASS row per class and a summary line)
"""
from __future__ import annotations

import argparse
import re
import sys
from collections import Counter

EPOCH = 4096
MUTATING = {"removeIf", "removeAll", "retainAll", "clear", "remove", "add", "addAll", "iteratorRemove"}
# class, subject regex, operations, regex the right-hand line must match, cause
CLASSES = (
    ("IDENTITY_VALUES_EQUALS", r"^IdentityHashMap\.values$", {"contains", "remove"}, r"",
     "pre-existing owner semantics, not the census hunk: FilteredCollection.contains/remove confirm the owner's identity"
     " contains() and then pick the first Objects.equals stored occurrence, so an identical hidden value next to an"
     " equal visible one reads as visible (contains) or removes the equal one (remove); the stock model compares by"
     " identity (the owner's own equivalence). The held helper IdentityHashMap.collectionViewIdentityKind (pr1619/"
     "pr1627) is the identity classification its retained CollectionViews lineage used"),
    ("COW_SUBCLASS_BULK_UOE", r"^(nested:)?CowSetSub$", {"removeIf", "removeAll", "retainAll", "clear"},
     r"-> UnsupportedOperationException",
     "census rules R1+R2 (getClass() == dispatch): a CopyOnWriteArraySet subclass leaves the removeIf delegation, its"
     " snapshot iterator refuses remove(), and the owner's indexed fallback applies to Lists only"),
    ("CRATE_COW_NO_FALLBACK_UOE",
     r"^(nested:)?(CowListSub|CowSetSub)$|^(CopyOnWriteArrayList|CowListSub)\.subList$",
     {"removeIf", "removeAll", "retainAll", "clear"}, r"-> UnsupportedOperationException",
     "full crate postimage only: getClass() == dispatch and no UnsupportedOperationException fallback in"
     " removeMatching, so CopyOnWrite subclasses and CopyOnWrite sub-lists refuse bulk removal"),
)


def parse(line: str):
    index, subject, op, rest = line.split(" ", 3)
    state = rest.split(" | ", 1)[1] if op in MUTATING and " | " in rest else None
    return int(index), subject, op, state


def classify(left_path: str, right_path: str, label: str):
    with open(left_path, encoding="utf-8") as lf, open(right_path, encoding="utf-8") as rf:
        left, right = lf.read().splitlines(), rf.read().splitlines()
    if len(left) != len(right):
        raise SystemExit("line counts differ: %d vs %d" % (len(left), len(right)))
    last = {}
    counts = Counter()
    first = {}
    unclassified = []
    for a, b in zip(left, right):
        index, subject, op, state_a = parse(a)
        _, subject_b, op_b, state_b = parse(b)
        if (subject, op) != (subject_b, op_b):
            raise SystemExit("streams desynchronised at %d" % index)
        if index % EPOCH == 0:
            last.clear()
        diverged = last.get(subject, (None, None))
        diverged = diverged[0] != diverged[1]
        if a != b:
            if diverged:
                key = ("CASCADE", subject, op)
            else:
                key = None
                for name, subject_re, ops, pattern, _cause in CLASSES:
                    if re.search(subject_re, subject) and op in ops and re.search(pattern, b):
                        key = (name, subject, op)
                        break
                if key is None:
                    key = ("UNCLASSIFIED", subject, op)
                    unclassified.append(index)
            counts[key] += 1
            first.setdefault(key, index)
        if state_a is not None or state_b is not None:
            last[subject] = (state_a, state_b)
    rows = [(label, k[0], k[1], k[2], str(v), str(first[k])) for k, v in sorted(counts.items())]
    return rows, sum(counts.values()), unclassified


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("left")
    parser.add_argument("right")
    parser.add_argument("label")
    parser.add_argument("--tsv", help="append the CLASS rows to this TSV")
    args = parser.parse_args(argv)
    rows, total, unclassified = classify(args.left, args.right, args.label)
    for row in rows:
        print("CLASS\t" + "\t".join(row))
    if args.tsv:
        with open(args.tsv, "a", encoding="utf-8", newline="\n") as out:
            for row in rows:
                out.write("\t".join(row) + "\n")
    roots = sum(int(r[4]) for r in rows if r[1] not in ("CASCADE",))
    print("CLASSIFY label=%s differing=%d roots=%d cascade=%d unclassified=%d first_unclassified=%s" % (
        args.label, total, roots, total - roots, len(unclassified), unclassified[:5]))
    return 1 if unclassified else 0


if __name__ == "__main__":
    sys.exit(main())
