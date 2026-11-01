# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""census_union.py <atoms_dir> <census_py> <base_test_root> <work_dir> <out_tsv> <name>=<worktree>:<dir>[,<dir>...]...
: CENSUS UNION v1 (CPU, deterministic): the per-atom test census of several branches, alone and together.

Runs per_atom_test_census.py (com.synexia PR #14999: name-based evidence for the work list, not coverage) over
  before    the base test tree (the 48 M3 test directories of the census, without any branch's tests),
  <name>    the base tree plus that branch's test directories (copied from its worktree),
  union     the base tree plus every branch's test directories,
and writes <out_tsv>: one row per scope (all M3JDK atoms, then each package whose counts change) and status
(TESTED, CLASS_ONLY, UNTESTED, perf PRESENT) with the before, per-branch and union counts and the union delta.
A branch directory must not already exist in the base tree (its files would be counted twice)."""
import collections
import os
import shutil
import subprocess
import sys

STATES = ("TESTED", "CLASS_ONLY", "UNTESTED", "PRESENT")
ALL = "all M3JDK atoms"


def tree(work, label, base, branches):
    root = os.path.join(work, label, "test")
    shutil.rmtree(os.path.join(work, label), ignore_errors=True)
    shutil.copytree(base, root)
    for worktree, dirs in branches:
        for d in dirs:
            target = os.path.join(root, os.path.relpath(d, "test"))
            if os.path.exists(target) and os.listdir(target):
                raise SystemExit(f"{d} already exists in the base tree")
            shutil.copytree(os.path.join(worktree, d), target, dirs_exist_ok=True)
    return root


def census(census_py, atoms, root, out):
    run = subprocess.run([sys.executable, census_py, atoms, "-", root, out], capture_output=True, text=True)
    if run.returncode != 0:
        raise SystemExit(run.stderr)
    counts = collections.Counter()
    with open(os.path.join(out, "PER_ATOM_TEST_CENSUS.tsv"), encoding="utf-8") as f:
        next(f)
        for line in f:
            row = line.rstrip("\n").split("\t")
            for scope in (ALL, row[2]):
                counts[(scope, row[8])] += 1
                counts[(scope, row[9])] += 1
    return counts, run.stdout.strip()


def main():
    atoms, census_py, base, work, out_tsv = sys.argv[1:6]
    branches = []
    for spec in sys.argv[6:]:
        name, _, rest = spec.partition("=")
        worktree, _, dirs = rest.rpartition(":")
        branches.append((name, worktree, dirs.split(",")))
    labels = ["before"] + [b[0] for b in branches] + ["union"]
    members = {"before": [], "union": [(w, d) for _, w, d in branches]}
    members.update({n: [(w, d)] for n, w, d in branches})
    counts = {}
    for label in labels:
        root = tree(work, label, base, members[label])
        counts[label], summary = census(census_py, atoms, root, os.path.join(work, "out-" + label))
        print(f"{label}: {summary}", flush=True)
    scopes = [ALL] + sorted({s for c in counts.values() for s, _ in c if s != ALL
                             and any(counts[l][(s, st)] != counts["before"][(s, st)] for l in labels for st in STATES)})
    with open(out_tsv, "w", encoding="utf-8", newline="\n") as w:
        w.write("scope\tstatus\t" + "\t".join(labels) + "\tunion_delta\n")
        for scope in scopes:
            for st in STATES:
                values = [counts[l][(scope, st)] for l in labels]
                status = "perf PRESENT" if st == "PRESENT" else st
                w.write("\t".join([scope, status] + [str(v) for v in values] + [f"{values[-1] - values[0]:+d}"]) + "\n")
    print(f"wrote {out_tsv}: scopes={len(scopes)}")


if __name__ == "__main__":
    main()
