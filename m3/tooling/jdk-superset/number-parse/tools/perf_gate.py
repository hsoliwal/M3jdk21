# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_gate.py <PERF.tsv> <before_label> <after_label> <slack> <ab.tsv>...

Gate PERF_WIRING v1 over perf_ab output (ABBA rounds, see perf_ab.sh): for each '(wiring)' row
  recorded       the before value recorded in PERF.tsv (an earlier host state)
  before_best    best (minimum) ns/op of the before variant over all rounds of all files
  after_best     best ns/op of the after variant
  vs_recorded    after_best / recorded
  ref_drift      best reference ns/op now / reference recorded in PERF.tsv: how far the host state moved
  best_ratio     after_best / before_best
  mean_ratio     mean after / mean before
  paired_median  median over rounds of after / before inside the same round (adjacent runs: drift cancels)
Verdicts (PASS when the ratio is at most slack, 1.05 = not slower by more than 5%):
  verdict_recorded   on vs_recorded; NOT_COMPARABLE when before_best / recorded itself exceeds slack (the host no
                     longer reproduces the recorded state, so the recorded value cannot judge the change)
  verdict_same_host  on paired_median (the operative gate on a drifting host)
Exit 0 when every verdict_same_host is PASS and no verdict_recorded is FAIL."""
import statistics
import sys

WIRING = "(wiring)"
# The output is a TSV (redirected stdout); it starts with the Synexia SPDX header ('#' lines).
TSV_HEADER = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others\n")
# perf_ab rows: round variant atom ns_per_op_atom ns_per_op_reference speedup ('#' lines have fewer fields)
AB_FIELDS = 6
AB_ROUND = 0
AB_VARIANT = 1
AB_ATOM = 2
AB_NS = 3
AB_REF = 4
# PERF.tsv rows: atom ns_per_op_atom ns_per_op_reference speedup
PERF_FIELDS = 4
PERF_ATOM = 0
PERF_NS = 1
PERF_REF = 2
# argv: program, PERF.tsv, before_label, after_label, slack, then the perf_ab files
ARG_PERF = 1
ARG_BEFORE = 2
ARG_AFTER = 3
ARG_SLACK = 4
ARG_FIRST_AB = 5


def load(paths):
    """{atom: {round key: {variant: (ns, ref)}}}"""
    data = {}
    for n, path in enumerate(paths):
        for ln in open(path, encoding="utf-8"):
            f = ln.rstrip("\n").split("\t")
            if len(f) == AB_FIELDS and WIRING in f[AB_ATOM] and f[AB_NS]:
                data.setdefault(f[AB_ATOM], {}).setdefault((n, f[AB_ROUND]), {})[f[AB_VARIANT]] = (
                    float(f[AB_NS]), float(f[AB_REF]))
    return data


def main(argv):
    sys.stdout.reconfigure(newline="\n")  # the redirected TSV keeps LF line ends on Windows too
    perf, before, after = argv[ARG_PERF], argv[ARG_BEFORE], argv[ARG_AFTER]
    slack, paths = float(argv[ARG_SLACK]), argv[ARG_FIRST_AB:]
    recorded = {}
    for ln in open(perf, encoding="utf-8"):
        f = ln.rstrip("\n").split("\t")
        if len(f) == PERF_FIELDS and WIRING in f[PERF_ATOM]:
            recorded[f[PERF_ATOM]] = (float(f[PERF_NS]), float(f[PERF_REF]))
    data = load(paths)
    cols = ("atom", "recorded", "before_best", "after_best", "vs_recorded", "ref_drift", "best_ratio", "mean_ratio",
            "paired_median", "pairs", "verdict_recorded", "verdict_same_host")
    sys.stdout.write(TSV_HEADER)
    sys.stdout.write("\t".join(cols) + "\n")
    ok = True
    for atom in sorted(recorded):
        rounds = [v for v in data[atom].values() if before in v and after in v]
        b = [v[before][0] for v in rounds]
        a = [v[after][0] for v in rounds]
        refs = [x[1] for v in rounds for x in v.values()]
        rec_ns, rec_ref = recorded[atom]
        vr, drift = min(a) / rec_ns, min(refs) / rec_ref
        paired = statistics.median(x / y for x, y in zip(a, b))
        if min(b) / rec_ns > slack:
            v1 = "NOT_COMPARABLE"
        else:
            v1 = "PASS" if vr <= slack else "FAIL"
        v2 = "PASS" if paired <= slack else "FAIL"
        ok &= v2 == "PASS" and v1 != "FAIL"
        sys.stdout.write("%s\t%.2f\t%.2f\t%.2f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%d\t%s\t%s\n"
                         % (atom, rec_ns, min(b), min(a), vr, drift, min(a) / min(b),
                            statistics.mean(a) / statistics.mean(b), paired, len(rounds), v1, v2))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
