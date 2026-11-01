# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""census_delta.py <before PER_ATOM_TEST_CENSUS.tsv> <after PER_ATOM_TEST_CENSUS.tsv> : group census delta (CPU).

Both inputs come from per_atom_test_census.py (the before run over the base test tree, the after run over the
same tree plus this branch's tests). Writes CENSUS_BEFORE_AFTER.tsv next to this file: per package of the
util-lang-m3 group the UNTESTED / CLASS_ONLY / TESTED / perf-PRESENT counts before and after, a TOTAL row
over all 2,847 atoms, and one SIDE_EFFECT row per atom whose status changed although TEST_PATTERNS.tsv
binds no test to it (name-match artifacts of the census, not claimed as tested)."""
import collections
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
GROUP = ["java.util", "java.lang", "jdk.internal.m3", "jdk.internal.m3.io", "jdk.internal.util",
         "jdk.internal.misc", "java.util.regex"]
STATES = ("UNTESTED", "CLASS_ONLY", "TESTED", "PRESENT")


def load(path):
    with open(path, encoding="utf-8") as f:
        next(f)
        return [line.rstrip("\n").split("\t") for line in f]


def main():
    before, after = load(sys.argv[1]), load(sys.argv[2])
    bound = set()
    with open(os.path.join(HERE, "TEST_PATTERNS.tsv"), encoding="utf-8") as f:
        next(f)
        for line in f:
            owner, atom = line.split("\t")[:2]
            bound.add((owner.rsplit(".", 1)[-1].split("$")[0], atom))
    counts = collections.Counter()
    side = []
    for b, a in zip(before, after):
        assert b[:8] == a[:8], "census rows must align"
        for when, row in (("before", b), ("after", a)):
            for key in (row[2], "TOTAL"):
                counts[(key, when, row[8])] += 1
                counts[(key, when, row[9])] += 1
        if b[8] != a[8] and (b[3].split(".")[0], b[4]) not in bound:
            side.append(("SIDE_EFFECT", b[2], b[3], b[4], b[8] + "->" + a[8]))
    with open(os.path.join(HERE, "CENSUS_BEFORE_AFTER.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("row\tpackage\t" + "\t".join(f"before_{s}" for s in STATES) + "\t"
                + "\t".join(f"after_{s}" for s in STATES) + "\n")
        for p in GROUP + ["TOTAL"]:
            w.write("\t".join(["PACKAGE" if p != "TOTAL" else "TOTAL", p]
                              + [str(counts[(p, "before", s)]) for s in STATES]
                              + [str(counts[(p, "after", s)]) for s in STATES]) + "\n")
        w.writelines("\t".join(r) + "\n" for r in side)
    print(" ".join(f"{s}:{counts[('TOTAL', 'before', s)]}->{counts[('TOTAL', 'after', s)]}" for s in STATES),
          f"side_effects={len(side)}")


main()
