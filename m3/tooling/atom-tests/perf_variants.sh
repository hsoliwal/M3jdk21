#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# perf_variants.sh <worktree> <out_dir> <owner file name> <runs> <label>=<variant source> ... : ATOM-PERF VARIANTS v1.
# Times alternative forms of one owner source file of the package with the generated perf harness: every variant is
# the package with <owner file name> replaced by <variant source>, compiled into its own --patch-module java.base
# patch (stock JDK 21, -implicit:none, as run_atom_tests.sh). The harness classes are compiled once (the variants
# keep the owner's API). The runs interleave the variants (run 1 of every variant, then run 2, ...) so host-load
# drift hits them alike; each run is the harness with -XX:+PrintCompilation, joined by perf_jit_tiers.py into
# <out>/<label>/PERF.r<n>.tsv (ns_per_op, bytes_per_op, JIT tier per atom). Prints one progress line per run and writes
# <out>/VARIANTS.tsv: label, run, atom, ns_per_op_median, ns_per_op_min, bytes_per_op, max_tier, compiler,
# host load before/after the run. Environment: JDK, JUNIT_JAR, M3_SRC_ROOT, PKG_DIR, TEST_DIR, PKG, PERF_CLASS and
# PERF_ARGS (atom-label prefixes: the harness runs only the cases whose label starts with one of them).
set -euo pipefail
WT=${1:?worktree}; OUT=${2:?out_dir}; OWNER=${3:?owner file name}; RUNS=${4:?runs}
shift 4
JDK=${JDK:-"C:/Program Files/Java/jdk-21"}
JUNIT_JAR=${JUNIT_JAR:-"$(cygpath -m "$HOME")/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
M3_SRC_ROOT=${M3_SRC_ROOT:-"C:/m3-superset/vendored-atoms/m3jdk21-vendored/src/java.base/share/classes"}
PKG_DIR=${PKG_DIR:-"src/java.base/share/classes/jdk/internal/mindex"}
TEST_DIR=${TEST_DIR:-"test/jdk/jdk/internal/mindex/atoms"}
PKG=${PKG:-"jdk.internal.mindex"}
PERF_CLASS=${PERF_CLASS:-M3MindexAtomPerf}
T="$WT/m3/tooling/atom-tests"
PKG_REL=${PKG_DIR#src/java.base/share/classes/}
export MSYS_NO_PATHCONV=1
host_load() { powershell -NoProfile -Command "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average" 2>/dev/null | tr -d '\r'; }
mkdir -p "$OUT"
labels=()
for spec in "$@"; do
  label=${spec%%=*}; source=${spec#*=}
  labels+=("$label")
  root="$OUT/$label/src"
  rm -rf "$OUT/$label"; mkdir -p "$root/$PKG_REL" "$OUT/$label/patch"
  cp "$WT/$PKG_DIR"/*.java "$root/$PKG_REL/"
  cp "$source" "$root/$PKG_REL/$OWNER"
  "$JDK/bin/javac" -nowarn -implicit:none -XDignore.symbol.file \
    --patch-module "java.base=$root;$WT/src/java.base/share/classes;$M3_SRC_ROOT" \
    -d "$OUT/$label/patch" "$root/$PKG_REL"/*.java
  echo "VARIANT $label owner_sha256=$(sha256sum "$source" | cut -c1-16) classes=$(find "$OUT/$label/patch" -name '*.class' | wc -l)"
done
rm -rf "$OUT/tests"; mkdir -p "$OUT/tests"
"$JDK/bin/javac" -nowarn -Xlint:none -g --patch-module "java.base=$OUT/${labels[0]}/patch" \
  --add-exports "java.base/$PKG=ALL-UNNAMED" -cp "$JUNIT_JAR" -d "$OUT/tests" "$WT/$TEST_DIR"/*.java
echo "HARNESS classes=$(find "$OUT/tests" -name '*.class' | wc -l)"
printf 'label\trun\tatom\tns_per_op_median\tns_per_op_min\tbytes_per_op\tmax_tier\tcompiler\thost_cpu_pct_before\thost_cpu_pct_after\n' \
  > "$OUT/VARIANTS.tsv"
for run in $(seq 1 "$RUNS"); do
  for label in "${labels[@]}"; do
    before=$(host_load)
    set +e
    "$JDK/bin/java" -Djava.awt.headless=true --patch-module "java.base=$OUT/$label/patch" \
      --add-exports "java.base/$PKG=ALL-UNNAMED" --add-opens "java.base/$PKG=ALL-UNNAMED" \
      -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -Dm3.perf.out="$OUT/$label/PERF_raw.r$run.tsv" \
      -cp "$OUT/tests;$JUNIT_JAR" "$PERF_CLASS" ${PERF_ARGS:-} > "$OUT/$label/perf.r$run.log" 2>&1
    status=$?
    set -e
    after=$(host_load)
    if [ "$status" -ne 0 ] || [ ! -f "$OUT/$label/PERF_raw.r$run.tsv" ]; then
      # the last exception text is the root cause; PrintCompilation lines may interleave with the stack trace
      reason=$(grep -oE 'java\.lang\.[A-Za-z]+(Exception|Error): [a-z][a-z ]*' "$OUT/$label/perf.r$run.log" | tail -1 || true)
      echo "RUN $run $label exit=$status FAILED ${reason:-no PERF rows} host=$before/$after"
      printf '%s\t%s\tFAILED: %s\t-\t-\t-\t-\t-\t%s\t%s\n' "$label" "$run" "${reason:-no PERF rows}" "$before" "$after" \
        >> "$OUT/VARIANTS.tsv"
      continue
    fi
    joined=$(python "$T/perf_jit_tiers.py" "$OUT/$label/PERF_raw.r$run.tsv" "$OUT/$label/perf.r$run.log" "$PKG" \
      "$OUT/$label/PERF.r$run.tsv" "$before" "$after")
    echo "RUN $run $label exit=$status $(grep -o 'M3_ATOM_PERF [A-Z]*' "$OUT/$label/perf.r$run.log" | head -1) $joined host=$before/$after"
    awk -F'\t' -v l="$label" -v r="$run" 'NR==1{for(i=1;i<=NF;i++) c[$i]=i; next}
      {print l"\t"r"\t"$c["atom"]"\t"$c["ns_per_op_median"]"\t"$c["ns_per_op_min"]"\t"$c["bytes_per_op"]"\t"$c["max_tier"]"\t"$c["compiler"]"\t"$c["host_cpu_pct_before"]"\t"$c["host_cpu_pct_after"]}' \
      "$OUT/$label/PERF.r$run.tsv" >> "$OUT/VARIANTS.tsv"
  done
done
echo "VARIANTS $OUT/VARIANTS.tsv rows=$(($(wc -l < "$OUT/VARIANTS.tsv") - 1))"
