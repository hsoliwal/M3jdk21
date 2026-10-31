# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atom_ledgers.py <repo> <work_dir> <oracle_jars>

Writes the execution ledgers of the jdk.m3.graphics atom tests from the logs that tools/atom_tests.sh leaves in
<work_dir> (CPU only, no LLM; text parsing of the harness rows):

  leaf1/ATOM_PERF.tsv       every PERF row of perf1.log and perf2.log, with the run number
  leaf1/ATOM_COVERAGE.tsv   coverage gaps: tested members whose oracle never returned normally (MEMBER rows of
                            junit_pinned.log) and members with a reference parameter that is always null
                            (ATOM_MEMBERS.tsv null_params: no value without natives)
  leaf1/ATOM_TEST_GATES.tsv one row per gate: what ran and what it printed

Exits 1 when a log is missing or a gate failed (tests, fixed point, mutants, perf outliers).
"""
import hashlib
import os
import re
import sys
from collections import Counter

LEAF = "m3/tooling/skija-absorb/leaf1"
TAB, NL = "\t", "\n"
PINNED = "-XX:+UnlockExperimentalVMOptions -XX:hashCode=2"
PERF_FLAGS = ("-Xmx512m -XX:-TieredCompilation -Xbatch -XX:+UnlockExperimentalVMOptions "
              "-XX:CompileCommand=blackhole,M3GraphicsAtomsPerfSupport::consume")
CONSOLE = re.compile(r"\[\s*(\d+) tests (found|successful|failed|aborted)\s*\]")
SUMMARY = re.compile(r"^(atoms=.*)$", re.M)
RUNS = ("1", "2")
SHA_SHOWN = 12
MEDIAN, P10, P90 = 0.5, 0.1, 0.9


def lines(work, name):
    path = os.path.join(work, name)
    if not os.path.exists(path):
        raise SystemExit("missing log: " + path)
    with open(path, encoding="utf-8", errors="replace") as f:
        return f.read().splitlines()


def rows_tsv(path):
    with open(path, encoding="utf-8") as f:
        head = f.readline().rstrip(NL).split(TAB)
        return [dict(zip(head, line.rstrip(NL).split(TAB))) for line in f if line.strip()]


def console(log):
    out = Counter()
    for line in log:
        m = CONSOLE.search(line)
        if m:
            out[m.group(2)] = int(m.group(1))
    return out


def harness(log):
    """Sums of the ATOM rows: members, cases, mismatches, strings, messages, hash exact/partition/skipped, env."""
    sums = [0] * 9
    for line in log:
        f = line.split(TAB)
        if f[0] == "ATOM" and len(f) == 11:
            sums = [a + int(b) for a, b in zip(sums, f[2:])]
    return dict(zip(("members", "cases", "mismatches", "strings", "messages", "hash_exact", "hash_partition",
                     "hash_skipped", "environment"), sums))


def quantile(values, q):
    v = sorted(values)
    return v[int(round(q * (len(v) - 1)))] if v else float("nan")


def perf(work):
    """{run: [PERF row fields]}, {run: summary text}, {run: PERF_SAME text}."""
    data, summary, same = {}, {}, {}
    for run in RUNS:
        log = lines(work, "perf%s.log" % run)
        data[run] = [line.split(TAB) for line in log if line.startswith("PERF" + TAB) and not line.startswith(
            "PERF" + TAB + "atom")]
        summary[run] = next((line for line in log if line.startswith("PERF_SUMMARY")), "PERF_SUMMARY missing")
        same[run] = next((line for line in log if line.startswith("PERF_SAME")), "PERF_SAME missing")
    return data, summary, same


def write_perf(repo, data):
    path = os.path.join(repo, LEAF, "ATOM_PERF.tsv")
    with open(path, "w", encoding="utf-8", newline=NL) as f:
        f.write("# M3GraphicsAtomsPerf v2 (%s), M3 = jdk.m3.graphics classes on the class path, oracle = Skija jars in "
                "their own class loader%s" % (PERF_FLAGS, NL))
        f.write("# per row: warm-up (>= 12 calls and 40 ms per side), calibration on the compiled loops to 2 ms rounds, "
                "11 rounds alternating the first side; ns/op = median round; no re-measuring%s" % NL)
        f.write("# ratio = ns/op M3 / ns/op oracle (above 1: M3 slower); spread = IQR of the 11 round ratios / their "
                "median; code SAME = same bytecode on both sides (member and module callees, modulo the rename)%s" % NL)
        f.write(TAB.join(("run", "atom", "pattern", "member", "code", "calls", "ns_per_op_m3", "ns_per_op_oracle",
                          "ratio", "spread", "verdict")) + NL)
        for run in RUNS:
            for r in data[run]:
                f.write(TAB.join([run] + r[1:]) + NL)


def same_stats(data):
    """Identical-code rows: per-run ratio distribution and the run-to-run difference of each row's ratio."""
    ratios = {run: {(r[1], r[3]): float(r[8]) for r in data[run] if r[4] == "SAME" and r[10].startswith("OK")}
              for run in RUNS}
    both = sorted(set(ratios[RUNS[0]]) & set(ratios[RUNS[1]]))
    deltas = [abs(ratios[RUNS[0]][k] - ratios[RUNS[1]][k]) for k in both]
    text = []
    for run in RUNS:
        v = list(ratios[run].values())
        text.append("run %s: %d rows, median %.3f, p10..p90 %.3f..%.3f, min %.3f, max %.3f" % (
            run, len(v), quantile(v, MEDIAN), quantile(v, P10), quantile(v, P90), min(v), max(v)))
    text.append("run 1 vs 2 on %d rows: |ratio difference| median %.3f, p90 %.3f, max %.3f" % (
        len(both), quantile(deltas, MEDIAN), quantile(deltas, P90), max(deltas) if deltas else float("nan")))
    return "; ".join(text)


def coverage(repo, work):
    members = rows_tsv(os.path.join(repo, LEAF, "ATOM_MEMBERS.tsv"))
    shorts = {r["atom"]: r["donor"][len("io.github.humbleui."):] for r in rows_tsv(os.path.join(repo, LEAF,
                                                                                                  "ATOM_TESTS.tsv"))}
    out = []
    for line in lines(work, "junit_pinned.log"):
        f = line.split(TAB)
        if f[0] == "MEMBER" and f[5] == "0":
            out.append((f[1], f[2], f[3], f[4], f[5], "THROWS_ONLY", "every oracle case threw " + f[6]))
    for r in members:
        if r["status"] == "TESTED" and r["null_params"] != "-":
            out.append((shorts[r["atom"]], r["kind"], r["member"], r["cases"], "-", "NULL_ONLY",
                        "always null (no value without natives): " + r["null_params"]))
    path = os.path.join(repo, LEAF, "ATOM_COVERAGE.tsv")
    with open(path, "w", encoding="utf-8", newline=NL) as f:
        f.write(TAB.join(("atom", "kind", "member", "cases", "oracle_returned", "gap", "detail")) + NL)
        for row in sorted(out, key=lambda r: (r[5], r[0], r[2])):
            f.write(TAB.join(row) + NL)
    return Counter(r[5] for r in out)


def sha(path):
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()


def gates(repo, work, oracle, data, summary, same, gaps):
    gen = lines(work, "gen.log")
    check = lines(work, "gen_check.log")
    m3javac = lines(work, "m3javac.log")
    testjavac = lines(work, "testjavac.log")
    pinned, unpinned, none = (lines(work, n) for n in ("junit_pinned.log", "junit_unpinned.log",
                                                         "junit_nooracle.log"))
    mutants = rows_tsv(os.path.join(repo, LEAF, "ATOM_MUTANTS.tsv"))
    skip = lines(work, "perf_skip.log")
    sources = len(lines(work, "m3srcs.txt"))
    classes = sum(len([f for f in fs if f.endswith(".class")]) for _, _, fs in os.walk(os.path.join(work,
                                                                                                    "m3classes")))
    tests = len([f for f in os.listdir(os.path.join(repo, "test/jdk/jdk/m3/graphics")) if f.endswith(".java")])
    jars = ", ".join("%s (sha256 %s...)" % (os.path.basename(j), sha(j)[:SHA_SHOWN]) for j in oracle.split(os.pathsep))
    mr = [line.split(TAB)[1] + ": " + line.split(TAB)[2] for line in gen if line.startswith("MR_VARIANT")]
    hp, hu = harness(pinned), harness(unpinned)
    cp, cu, cn = console(pinned), console(unpinned), console(none)
    killed = sum(m["result"] == "KILLED" for m in mutants)
    killed_unpinned = sum(m["result_unpinned"] == "KILLED" for m in mutants)
    errors = lambda log: sum(" error" in x for x in log)
    warnings = lambda log: sum(" warning" in x for x in log)
    rows = [
        ("base", "worktree HEAD with #2208 head c500a57e merged (MR_SELECT v1); module compiled with javac --release 21 "
                 "-proc:none (JDK 21.0.9), no module-info",
         "%d sources, %d classes, %d errors" % (sources, classes, errors(m3javac))),
        ("oracle", "the oracle jars, loaded in their own class loader (parent: platform loader); javap --multi-release 21",
         jars + "; " + "; ".join(mr)),
        ("generate", "python tools/gen_atom_tests.py <repo> <m3 classes> <oracle jars> (v2)",
         SUMMARY.search(NL.join(gen)).group(1) if SUMMARY.search(NL.join(gen)) else "summary missing"),
        ("rename_rule", "gen_atom_tests.py: every TYPE_RENAME_L1.tsv row equals the runner's rename rule "
                        "(M3GraphicsAtomsOracle.m3Name), used to map donor names in exception messages",
         "%d mismatches" % sum(line.startswith("RENAME_RULE_MISMATCH") for line in gen)),
        ("fixed_point", "python tools/gen_atom_tests.py <repo> <m3 classes> <oracle jars> --check",
         "NOT_FIXED_POINT" if any("NOT_FIXED_POINT" in x for x in check) else "0 files differ, 0 stale"),
        ("compile", "javac --release 21 -Xlint:all -encoding UTF-8 -cp junit-platform-console-standalone-1.11.4 "
                    "test/jdk/jdk/m3/graphics/*.java",
         "%d sources, %d errors, %d warnings" % (tests, errors(testjavac), warnings(testjavac))),
        ("junit", "java %s -Dm3.graphics.oracle=<jars> -jar junit-platform-console-standalone-1.11.4.jar execute "
                  "(identity hashes pinned, as the jtreg @run lines)" % PINNED,
         "%d tests found, %d successful, %d failed; %d members, %d cases, %d mismatching; %d messages matched after "
         "normalization; hashCode: %d exact, %d by partition; %d URL comparisons by presence" % (
             cp["found"], cp["successful"], cp["failed"], hp["members"], hp["cases"], hp["mismatches"],
             hp["messages"], hp["hash_exact"], hp["hash_partition"], hp["environment"])),
        ("junit_unpinned", "same without %s" % PINNED,
         "%d tests found, %d successful, %d failed; %d mismatching; hashCode: %d exact, %d by partition, %d nested "
         "skipped" % (cu["found"], cu["successful"], cu["failed"], hu["mismatches"], hu["hash_exact"],
                      hu["hash_partition"], hu["hash_skipped"])),
        ("junit_no_oracle", "same without -Dm3.graphics.oracle",
         "%d tests found, %d aborted by the JUnit assumption, %d successful (contract tests), %d failed" % (
             cn["found"], cn["aborted"], cn["successful"], cn["failed"])),
        ("coverage", "tools/atom_ledgers.py -> leaf1/ATOM_COVERAGE.tsv",
         "%d members THROWS_ONLY (the oracle never returned normally), %d members NULL_ONLY (a reference parameter "
         "without a native-free value)" % (gaps["THROWS_ONLY"], gaps["NULL_ONLY"])),
        ("mutants", "python tools/atom_mutants.py (leaf1/ATOM_MUTANTS.tsv): 8 single-edit mutants compiled ahead of "
                    "the module classes",
         "%d/%d killed with pinned identity hashes; %d/%d without (%s)" % (
             killed, len(mutants), killed_unpinned, len(mutants),
             ", ".join(m["mutant"] + " survives" for m in mutants if m["result_unpinned"] != "KILLED") or "-")),
    ]
    for run in RUNS:
        rows.append(("perf_run%s" % run, "java %s M3GraphicsAtomsPerf (leaf1/ATOM_PERF.tsv run %s)" % (PERF_FLAGS, run),
                     summary[run].replace(TAB, " ") + "; " + same[run].replace(TAB, " ")))
    rows.append(("perf_same_code", "ATOM_PERF.tsv rows with code SAME and verdict OK*: the harness's own spread",
                 same_stats(data)))
    rows.append(("perf_skip", "M3GraphicsAtomsPerf without -Dm3.graphics.oracle",
                 next((x for x in skip if x.startswith("PERF_SKIPPED")), "no PERF_SKIPPED line").replace(TAB, " ")))
    with open(os.path.join(repo, LEAF, "ATOM_TEST_GATES.tsv"), "w", encoding="utf-8", newline=NL) as f:
        f.write(TAB.join(("gate", "command", "result")) + NL)
        for r in rows:
            f.write(TAB.join(r) + NL)
    outliers = sum("outliers=0" not in summary[run] for run in RUNS)
    ok = (cp["failed"] == 0 and cp["found"] > 0 and cu["failed"] == 0 and cn["failed"] == 0 and killed == len(mutants)
          and not any("NOT_FIXED_POINT" in x for x in check) and outliers == 0)
    return ok, rows


def main(argv):
    if len(argv) != 4:
        sys.stderr.write(__doc__)
        return 2
    repo, work, oracle = argv[1:]
    data, summary, same = perf(work)
    write_perf(repo, data)
    gaps = coverage(repo, work)
    ok, rows = gates(repo, work, oracle, data, summary, same, gaps)
    for r in rows:
        print("GATE%s%s%s%s" % (TAB, r[0], TAB, r[2]))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
