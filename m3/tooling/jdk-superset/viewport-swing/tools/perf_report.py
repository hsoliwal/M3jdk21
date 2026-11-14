# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_report.py <out_dir> <PERF_SPEC.tsv> <PERF.tsv> : PERF.tsv from perf_<pkg>.log/.comp of run_atom_perf.sh (CPU).

Per PERF line: the atom, its route, the median/min/max ns per operation over the interleaved rounds, the JDK reference
measured in the same rounds (the closest preceding reference row of the spec) and the ratio reference/M3, the highest
compilation tier -XX:+PrintCompilation shows for the atom's own method (0-4; '-' when it only ran inlined into its
caller or was never compiled on its own), whether one of its compilations was made not entrant, and the machine load
(system CPU % at start, after warm-up and at the end; the host is shared with other sessions)."""
import csv
import os
import re
import sys


PACKAGES = ("list", "table", "tree")
PERF_CLASS = {"list": "M3ListAtomsPerf", "table": "M3TableAtomsPerf", "tree": "M3TreeAtomsPerf"}
#: Below this the loop was folded (a loop-invariant getter hoisted out of the measured loop): no measurement.
FOLD_NS = 0.1


def java_method(atom, pkg):
    """sun.swing.<pkg>.<Top>[$Nested]::<name> for an atom id Owner[.Nested]#name(params)."""
    if ":" in atom:
        return None
    owner, rest = atom.split("#", 1)
    name = rest[:rest.index("(")]
    parts = owner.split(".")
    cls = f"sun.swing.{pkg}.{parts[0]}" + "".join("$" + p for p in parts[1:])
    return f"{cls}::{'<init>' if name == parts[-1] else name}"


def compilations(lines):
    """Highest tier per method, and the methods one of whose tier-4 compilations was made not entrant."""
    tiers, deopt = {}, set()
    for line in lines:
        tok = line.split()
        if len(tok) < 4 or not tok[0].isdigit() or not tok[1].isdigit():
            continue
        for j in range(2, min(len(tok) - 1, 5)):
            if tok[j] in ("0", "1", "2", "3", "4") and "::" in tok[j + 1]:
                m = tok[j + 1]
                tiers[m] = max(tiers.get(m, -1), int(tok[j]))
                if "made not entrant" in line and tok[j] == "4":
                    deopt.add(m)
                break
    return tiers, deopt


def main():
    out_dir, spec_tsv, perf_tsv = sys.argv[1:4]
    spec = list(csv.DictReader(open(spec_tsv, encoding="utf-8"), delimiter="\t"))
    ref_for, last = {}, None
    for r in spec:
        if r["route"] == "reference":
            ref_for[r["atom"]] = last
        else:
            last = r["atom"]
    rows, loads = [], []
    for pkg in PACKAGES:
        path = os.path.join(out_dir, f"perf_{pkg}.log")
        if not os.path.exists(path):
            continue
        lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
        comp = os.path.join(out_dir, f"perf_{pkg}.comp")
        tiers, deopt = compilations(open(comp, encoding="utf-8", errors="replace").read().splitlines()
                                    if os.path.exists(comp) else lines)
        load = {l.split("\t")[1]: l.split("\t")[2] for l in lines if l.startswith("LOAD\t")}
        loads.append(f"{pkg}: system CPU % start {load.get('start', '?')}, warm {load.get('warm', '?')}, "
                     f"end {load.get('end', '?')}")
        perf_lines = [l for l in lines if l.startswith("PERF\t")]
        for index, l in enumerate(perf_lines):
            _, atom, label, route, med, lo, hi, ops, rounds = l.split("\t")
            m = java_method(atom, pkg)
            loop = f"{PERF_CLASS[pkg]}::b{index}"
            rows.append(dict(pkg=pkg, atom=atom, label=label, route=route, median=float(med), min=float(lo),
                             max=float(hi), ops=ops, rounds=rounds, tier=str(tiers[m]) if m in tiers else "-",
                             loop_tier=str(tiers[loop]) if loop in tiers else "-",
                             deopt="yes" if m in deopt else "-", load=load.get("start", "?") + "/"
                             + load.get("warm", "?") + "/" + load.get("end", "?")))
    by_atom = {r["atom"]: r for r in rows}
    with open(perf_tsv, "w", encoding="utf-8", newline="\n") as w:
        w.write("# host: Windows 11, 32 logical CPUs, stock JDK 21.0.9 + --patch-module java.desktop=<M3 classes>; "
                "static final MethodHandles (invokeExact), interleaved rounds, ns per operation\n")
        for l in loads:
            w.write("# load " + l + "\n")
        w.write("# tier: highest -XX:+PrintCompilation tier of the atom's own method (1 = trivial method, C1 without "
                "profiling; 3 = its standalone compile stayed at C1 while the loop inlined it); loop_tier: tier of the "
                "measured loop that calls the atom through its constant MethodHandle (4 = C2 with the atom inlined)\n")
        w.write("# tier4_deopt: a tier-4 compilation of the atom was made not entrant; note FOLDED: below "
                f"{FOLD_NS} ns the JIT hoisted a loop-invariant getter out of the loop: no measurement\n")
        w.write("pkg\tatom\tlabel\troute\tns_median\tns_min\tns_max\tops_per_round\trounds\ttier\tloop_tier\t"
                "tier4_deopt\tcpu_load_start/warm/end\treference_of\tspeedup_vs_reference\tnote\n")
        for r in rows:
            target = ref_for.get(r["atom"])
            speed = "-"
            if target and target in by_atom and by_atom[target]["median"] > 0:
                speed = f"{r['median'] / by_atom[target]['median']:.2f}"
            note = "FOLDED" if r["median"] < FOLD_NS else "-"
            w.write(f"{r['pkg']}\t{r['atom']}\t{r['label']}\t{r['route']}\t{r['median']:.3f}\t{r['min']:.3f}\t"
                    f"{r['max']:.3f}\t{r['ops']}\t{r['rounds']}\t{r['tier']}\t{r['loop_tier']}\t{r['deopt']}\t"
                    f"{r['load']}\t{target or '-'}\t{speed}\t{note}\n")
    atoms = [r for r in rows if ":" not in r["atom"]]
    print(f"perf rows={len(rows)} atoms={len(atoms)} tier4={sum(1 for r in atoms if r['tier'] == '4')} "
          f"loop_tier4={sum(1 for r in atoms if r['loop_tier'] == '4')} "
          f"folded={sum(1 for r in atoms if r['median'] < FOLD_NS)} "
          f"tier4_deopt={sum(1 for r in atoms if r['deopt'] == 'yes')}")


main()
