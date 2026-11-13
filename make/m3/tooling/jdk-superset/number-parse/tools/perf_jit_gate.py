# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_jit_gate.py <ab.tsv> <after> <before> <slack> <out.tsv> [<other>...]

Gate PERF_JIT v1 over perf_jit_ab.ps1 output (rounds, variants interleaved ABBA). One row per measured atom:
  atom rounds
  <v>_ns            median ns/op of variant v over the rounds (v = after, before, others)
  <v>_ref_ns        median ns/op of the JDK's original converter measured in the same JVM as v
  <v>_speedup       <v>_ref_ns / <v>_ns
  after_vs_<v>      median over rounds of after / v inside the same round (adjacent runs: host drift cancels)
  load_median load_max   system CPU load (0..1) reported by the harness around the measured rounds, all runs
  verdict           PASS when after_vs_<before> <= slack (1.05: not slower than before by more than 5%)
Prints the rows; exit 0 when every verdict is PASS."""
import statistics
import sys


def main(argv):
    path, after, before, slack, out = argv[1], argv[2], argv[3], float(argv[4]), argv[5]
    others = argv[6:]
    variants = [after, before] + others
    data = {}  # atom -> round -> variant -> (ns, ref, load)
    for n, ln in enumerate(open(path, encoding="utf-8")):
        f = ln.rstrip("\n").split("\t")
        if n == 0 or len(f) != 7 or f[2] == "RUN_FAILED":
            continue
        data.setdefault(f[2], {}).setdefault(f[0], {})[f[1]] = (float(f[3]), float(f[4]), float(f[6]))
    cols = ["atom", "rounds"]
    for v in variants:
        cols += ["%s_ns" % v, "%s_ref_ns" % v, "%s_speedup" % v]
    cols += ["after_vs_%s" % v for v in variants[1:]] + ["load_median", "load_max", "verdict"]
    rows, ok = [], True
    for atom in data:
        rounds = [r for r in data[atom].values() if all(v in r for v in variants)]
        if not rounds:
            continue
        row = [atom, str(len(rounds))]
        for v in variants:
            ns = statistics.median(r[v][0] for r in rounds)
            ref = statistics.median(r[v][1] for r in rounds)
            row += ["%.2f" % ns, "%.2f" % ref, "%.3f" % (ref / ns)]
        ratios = [statistics.median(r[after][0] / r[v][0] for r in rounds) for v in variants[1:]]
        row += ["%.3f" % x for x in ratios]
        loads = [r[v][2] for r in rounds for v in variants]
        verdict = "PASS" if ratios[0] <= slack else "FAIL"
        ok &= verdict == "PASS"
        row += ["%.3f" % statistics.median(loads), "%.3f" % max(loads), verdict]
        rows.append(row)
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("\t".join(cols) + "\n")
        for row in rows:
            w.write("\t".join(row) + "\n")
    sys.stdout.write("ATOMS\t%d\tVERDICT\t%s\n" % (len(rows), "PASS" if ok else "FAIL"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
