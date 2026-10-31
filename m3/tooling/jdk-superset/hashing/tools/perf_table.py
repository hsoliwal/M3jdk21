# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_table.py <PERF.tsv in> <PERF.tsv out> <M3HashAtomsPerf rows tsv or "# comment line">...

Writes the leaf's PERF.tsv: the comment lines and rows of the earlier table (6 columns: run, atom, form, ns/op
absorbed, ns/op vendored, speedup, best of 7 rounds, no load recorded) carried into the current columns, then the
rows of every M3HashAtomsPerf output given (-Dm3.hash.perf.out files: interleaved A/B rounds, median and best, rounds,
mean system CPU load), in the order given. speedup = vendored ns/op / absorbed ns/op (above 1: absorbed faster).
"""
import sys

NL = "\n"
TAB = "\t"
NA = "NA"
OLD_ROUNDS = "7"
OLD_LENGTHS = "0..1100"
HEADER = ("run", "group", "atom", "form", "lengths", "ns_per_op_absorbed_median", "ns_per_op_vendored_median",
          "speedup_median", "ns_per_op_absorbed_best", "ns_per_op_vendored_best", "speedup_best", "rounds", "load_pct")
GROUPS = (("hashChars", "string"), ("ByteBuffer", "bytebuffer"))
NOTE = ("# columns since the JIT leaf: median and best of the measured rounds (interleaved A/B), rounds, mean system "
        "CPU load; the earlier runs (6-column table) carried over: their single value was the best of 7 rounds")


def old_rows(path):
    comments, rows = [], []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip(NL)
            if line.startswith("#"):
                comments.append(line)
            elif line and not line.startswith("run\t"):
                run, atom, form, absorbed, vendored, speedup = line.split(TAB)[:6]
                if run in {"run"}:
                    continue
                group = next((g for k, g in GROUPS if k in form), "bytes")
                rows.append((run, group, atom, form, OLD_LENGTHS, NA, NA, NA, absorbed, vendored, speedup, OLD_ROUNDS,
                             NA))
    return comments, rows


def main():
    src, out, *args = sys.argv[1:]
    comments, rows = old_rows(src)
    runs = [a for a in args if not a.startswith("#")]
    for line in [NOTE] + [a for a in args if a.startswith("#")]:
        if line not in comments:
            comments.append(line)
    for path in runs:
        with open(path, encoding="utf-8") as f:
            rows += [tuple(line.rstrip(NL).split(TAB)) for line in f if line.strip() and not line.startswith("run\t")]
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write(NL.join(comments) + NL + TAB.join(HEADER) + NL + "".join(TAB.join(r) + NL for r in rows))
    print(f"{out}: {len(rows)} rows")


main()
