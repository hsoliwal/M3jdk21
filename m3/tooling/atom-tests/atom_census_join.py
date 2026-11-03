# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atom_census_join.py <PER_ATOM_TEST_CENSUS.tsv before> <PER_ATOM_TEST_CENSUS.tsv after> <TEST_PATTERNS.tsv> <package>
<out_dir> : ATOMIZE/PATTERNIZE JOIN v1 (CPU). Numbers every census atom of the package by its occurrence of
(owner, name) in source order (the n of the spec rows), joins the generated TEST_PATTERNS.tsv rows on
Owner.name#n and writes:
  ATOMIZE.tsv            owner, name, n, kind, n_tokens, status_before, status_after, perf_after, pattern, oracle, test,
                         perf_harness
                         (pattern PENDING when no spec row covers the atom yet)
  CENSUS_BEFORE_AFTER.tsv status counts before/after and the per-owner coverage of the spec rows."""
import csv
import os
import sys
from collections import Counter, defaultdict


def numbered(path, package):
    rows = [r for r in csv.DictReader(open(path, encoding="utf-8"), delimiter="\t") if r["package"] == package]
    seen = Counter()
    out = {}
    for r in rows:
        seen[(r["owner"], r["name"])] += 1
        out["%s.%s#%d" % (r["owner"], r["name"], seen[(r["owner"], r["name"])])] = r
    return out


def main():
    before_path, after_path, patterns_path, package, out_dir = sys.argv[1:6]
    before = numbered(before_path, package)
    after = numbered(after_path, package)
    rows = list(csv.DictReader(open(patterns_path, encoding="utf-8"), delimiter="\t"))
    patterns = {r["atom"]: r for r in rows if r["pattern"] != "PERF"}
    perf = {r["atom"]: r["test"] for r in rows if r["pattern"] == "PERF"}
    os.makedirs(out_dir, exist_ok=True)
    per_owner = defaultdict(Counter)
    with open(os.path.join(out_dir, "ATOMIZE.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("owner\tname\tn\tkind\tn_tokens\tstatus_before\tstatus_after\tperf_after\tpattern\toracle\ttest"
                "\tperf_harness\n")
        for key, r in before.items():
            a = after.get(key, r)
            p = patterns.get(key)
            top = r["owner"].split(".")[0]
            per_owner[top]["atoms"] += 1
            per_owner[top]["spec" if p else "pending"] += 1
            w.write("\t".join([r["owner"], r["name"], key.rsplit("#", 1)[1], r["kind"], r["n_tokens"],
                               r["test_status"], a["test_status"], a["perf_status"],
                               p["pattern"] if p else "PENDING", p["oracle"] if p else "-",
                               p["test"] if p else "-", perf.get(key, "-")]) + "\n")
    counts_before = Counter(r["test_status"] for r in before.values())
    counts_after = Counter(r["test_status"] for r in after.values())
    perf_before = Counter(r["perf_status"] for r in before.values())
    perf_after = Counter(r["perf_status"] for r in after.values())
    with open(os.path.join(out_dir, "CENSUS_BEFORE_AFTER.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("scope\tmetric\tbefore\tafter\n")
        for status in ("UNTESTED", "CLASS_ONLY", "TESTED"):
            w.write("%s\t%s\t%d\t%d\n" % (package, status, counts_before[status], counts_after[status]))
        w.write("%s\tperf_PRESENT\t%d\t%d\n" % (package, perf_before["PRESENT"], perf_after["PRESENT"]))
        for owner in sorted(per_owner):
            c = per_owner[owner]
            w.write("%s\tspec_rows/atoms\t-\t%d/%d\n" % (owner, c["spec"], c["atoms"]))
    print("atoms=%d spec_covered=%d pending=%d before=%s after=%s" % (
        len(before), sum(c["spec"] for c in per_owner.values()), sum(c["pending"] for c in per_owner.values()),
        dict(counts_before), dict(counts_after)))


main()
