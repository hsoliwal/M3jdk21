# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_summary.py <PERF.tsv or M3HashAtomsPerf rows tsv> <run>...

The figures the README quotes, from the perf rows: for every run named (exact run labels, or a prefix ending in '*')
and every row group, the number of rows, the range (min..max) and the median of speedup_median (vendored ns/op /
absorbed ns/op: above 1, absorbed faster), and the mean system load of its rows. With M3_PERF_ATOMS=1 one line per
row follows (atom, form, speedup median and best, load). Lines starting with '#' are skipped.
"""
import os
import statistics
import sys

NL = "\n"
TAB = "\t"
COMMENT = "#"
PREFIX = "*"
NA = "NA"


def rows_of(path):
    with open(path, encoding="utf-8") as f:
        lines = [line.rstrip(NL) for line in f if line.strip() and not line.startswith(COMMENT)]
    header = lines[0].split(TAB)
    return [dict(zip(header, line.split(TAB))) for line in lines[1:]]


def matches(run, wanted):
    return run == wanted or (wanted.endswith(PREFIX) and run.startswith(wanted[:-len(PREFIX)]))


def main():
    path, wanted = sys.argv[1], sys.argv[2:]
    rows = rows_of(path)
    per_atom = os.environ.get("M3_PERF_ATOMS") == "1"
    for w in wanted:
        runs = sorted({r["run"] for r in rows if matches(r["run"], w)})
        for run in runs:
            groups = {}
            for r in rows:
                if r["run"] == run:
                    groups.setdefault(r["group"], []).append(r)
            for group, rs in sorted(groups.items()):
                s = [float(r["speedup_median"]) for r in rs if r["speedup_median"] != NA]
                loads = [float(r["load_pct"]) for r in rs if r["load_pct"] != NA]
                load = f"{statistics.mean(loads):.0f}%" if loads else NA
                if s:
                    print(f"{run}\t{group}\trows={len(rs)}\tspeedup {min(s):.2f}..{max(s):.2f}\t"
                          f"median {statistics.median(s):.2f}\tload {load}")
                if per_atom:
                    for r in rs:
                        print(f"\t{r['atom']}\t{r['form']}\t{r['lengths']}\t{r['speedup_median']}\t"
                              f"{r['speedup_best']}\t{r['load_pct']}")


main()
