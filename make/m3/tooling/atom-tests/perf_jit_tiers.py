# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_jit_tiers.py <perf_raw.tsv> <print_compilation.log> <package> <out PERF.tsv> [host_load_before] [host_load_after]
ATOM-PERF JIT JOIN v1 (CPU): joins the harness rows (atom label "Owner[.Nested].name#n PERF") with the
-XX:+PrintCompilation log of the same run. For every atom method it records the highest tier reached
(1-3 C1, 4 C2), whether an OSR compile happened and whether a compile was made not entrant, plus the
host CPU load measured outside the JVM before and after the run."""
import re
import sys

LINE = re.compile(r"^\s*\d+\s+\d+\s+([%sbn!\s]{0,6}?)\s*([0-4])\s+(\S+::\S+)\s+\((\d+) bytes\)(.*)$")
LABEL = re.compile(r"^(?P<owner>[\w.]+)\.(?P<name>\w+)#\d+ PERF$")
C2_TIER = 4


def method_of(package, label):
    m = LABEL.match(label)
    if not m:
        return None
    owner = m.group("owner").replace(".", "$")
    return "%s.%s::%s" % (package, owner, m.group("name"))


def main():
    raw, log, package, out = sys.argv[1:5]
    before = sys.argv[5] if len(sys.argv) > 5 else "NA"
    after = sys.argv[6] if len(sys.argv) > 6 else "NA"
    tiers, osr, deopt = {}, set(), set()
    with open(log, encoding="utf-8", errors="replace") as f:
        for line in f:
            m = LINE.match(line)
            if not m:
                continue
            flags, tier, method, _size, tail = m.groups()
            tiers[method] = max(tiers.get(method, 0), int(tier))
            if "%" in flags:
                osr.add(method)
            if "made not entrant" in tail:
                deopt.add(method)
    with open(raw, encoding="utf-8") as f:
        header = next(f).rstrip("\n").split("\t")
        rows = [dict(zip(header, line.rstrip("\n").split("\t"))) for line in f if line.strip()]
    columns = ["atom", "method", "max_tier", "compiler", "osr", "made_not_entrant"] + header[1:] + [
        "host_cpu_pct_before", "host_cpu_pct_after"]
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("\t".join(columns) + "\n")
        for row in rows:
            method = method_of(package, row["atom"]) or "?"
            tier = tiers.get(method, 0)
            compiler = "C2" if tier == C2_TIER else ("C1" if tier > 0 else "interpreted-or-inlined")
            values = [row["atom"], method, str(tier), compiler, "yes" if method in osr else "no",
                      "yes" if method in deopt else "no"] + [row[c] for c in header[1:]] + [before, after]
            w.write("\t".join(values) + "\n")
    print("perf_rows=%d c2=%d" % (len(rows), sum(1 for r in rows if tiers.get(method_of(package, r["atom"]), 0) == C2_TIER)))


main()
