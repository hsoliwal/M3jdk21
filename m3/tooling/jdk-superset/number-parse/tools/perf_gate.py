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


def load(paths):
    """{atom: {round key: {variant: (ns, ref)}}}"""
    data = {}
    for n, path in enumerate(paths):
        for ln in open(path, encoding="utf-8"):
            f = ln.rstrip("\n").split("\t")
            if len(f) == 6 and WIRING in f[2] and f[3]:
                data.setdefault(f[2], {}).setdefault((n, f[0]), {})[f[1]] = (float(f[3]), float(f[4]))
    return data


def main(argv):
    perf, before, after, slack, paths = argv[1], argv[2], argv[3], float(argv[4]), argv[5:]
    recorded = {}
    for ln in open(perf, encoding="utf-8"):
        f = ln.rstrip("\n").split("\t")
        if len(f) == 4 and WIRING in f[0]:
            recorded[f[0]] = (float(f[1]), float(f[2]))
    data = load(paths)
    cols = ("atom", "recorded", "before_best", "after_best", "vs_recorded", "ref_drift", "best_ratio", "mean_ratio",
            "paired_median", "pairs", "verdict_recorded", "verdict_same_host")
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
