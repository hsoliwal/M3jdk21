#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# run_atom_perf.sh <out_dir> <perf_tsv>
#   Runs the generated *AtomsPerf classes (compiled by run_atom_tests.sh into <out_dir>) in one JVM with
#   -XX:+PrintCompilation, then writes <perf_tsv>: per hot atom the median ns/op of the invokeExact call
#   site, the JDK baseline, their ratio, the host CPU load seen by the JVM, and the highest JIT tier at
#   which the atom method was compiled as a root (or 'inlined/none' when it never was).
set -euo pipefail
OUT=$1; PERF_TSV=$2
JDK=${JDK:-"C:/Program Files/Java/jdk-21"}
JUNIT=${JUNIT:-"$HOME/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
OPENS="java.lang java.util java.util.concurrent jdk.internal.m3 jdk.internal.util jdk.internal.misc"
OPENFLAGS=(); for p in $OPENS; do OPENFLAGS+=(--add-opens "java.base/$p=ALL-UNNAMED"); done
rm -f "$OUT/perf_raw.tsv"
echo "perf: start $(date +%H:%M:%S)"
"$JDK/bin/java" -XX:+PrintCompilation -Dm3.perf.out="$OUT/perf_raw.tsv" -Djava.awt.headless=true \
    --patch-module "java.base=$OUT/classes" "${OPENFLAGS[@]}" \
    -jar "$JUNIT" execute --class-path "$OUT/testclasses" --scan-class-path --include-classname '.*AtomsPerf' \
    --reports-dir "$OUT/reports-perf" --disable-banner --details=none --disable-ansi-colors \
    > "$OUT/logs/perf-compilation.log" 2>&1
echo "perf: done $(date +%H:%M:%S)"
python - "$OUT/perf_raw.tsv" "$OUT/logs/perf-compilation.log" "$PERF_TSV" <<'PY'
import re, sys
raw, log, out = sys.argv[1:4]
line_re = re.compile(r"^\s*\d+\s+\d+\s+([%sbn! ]*?)\s*(\d)\s+([\w.$]+)::([\w$<>]+)")
tiers = {}
for line in open(log, encoding="utf-8", errors="replace"):
    m = line_re.match(line)
    if m:
        key = (m.group(3).rsplit(".", 1)[-1].split("$")[0], m.group(4))
        osr = "%" in m.group(1)
        best = tiers.get(key, (-1, False))
        tiers[key] = (max(best[0], int(m.group(2))), best[1] or osr)
rows = [l.rstrip("\n").split("\t") for l in open(raw, encoding="utf-8") if l.strip()]
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("owner\tatom\tm3_ns_per_op\tjdk_baseline\tjdk_ns_per_op\tm3_over_jdk\tjvm_cpu_load\tcpus"
            "\twarmup_rounds_ops\tmax_root_tier\tosr\n")
    for r in rows:
        tier, osr = tiers.get((r[0], r[1]), (-1, False))
        w.write("\t".join(r + [str(tier) if tier >= 0 else "inlined/none", "yes" if osr else "no"]) + "\n")
print(f"perf rows={len(rows)} tiers_found={sum(1 for r in rows if (r[0], r[1]) in tiers)}")
PY
