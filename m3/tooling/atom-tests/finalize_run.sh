#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# finalize_run.sh <worktree> <out_dir> <run_dir> <census_root> <reference_test_root> <group> <package>
# ATOM-TEST RUN v1: regenerates the tests from the specs, runs JUnit, runs the JIT/perf harness with
# -XX:+PrintCompilation, re-runs PER-ATOM TEST CENSUS v1 on the reference test tree overlaid with this
# worktree's test files, and writes TEST_PATTERNS.tsv, JUNIT_SUMMARY.tsv, PERF.tsv, ATOMIZE.tsv,
# CENSUS_BEFORE_AFTER.tsv and GATES.tsv into <run_dir>. Every stage prints one progress line.
set -uo pipefail
WT=${1:?worktree}; OUT=${2:?out}; RUN=${3:?run_dir}; CENSUS=${4:?census_root}; REF=${5:?reference_test_root}
GROUP=${6:?group}; PKG=${7:?package}
T="$WT/m3/tooling/atom-tests"
JDK=${JDK:-"C:/Program Files/Java/jdk-21"}
JUNIT_JAR=${JUNIT_JAR:-"$(cygpath -m "$HOME")/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
PERF_CLASS=${PERF_CLASS:-M3MindexAtomPerf}
export MSYS_NO_PATHCONV=1
mkdir -p "$RUN"
gates="$RUN/GATES.tsv"
printf 'stage\tstatus\tcommand\tevidence\n' > "$gates"
gate() { printf '%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$4" >> "$gates"; echo "GATE $1 $2 $4"; }
host_load() { powershell -NoProfile -Command "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average" 2>/dev/null | tr -d '\r'; }

gen=$(python "$T/gen_atom_tests.py" "$T/specs/$GROUP" "$T/templates" "$WT/test/jdk/jdk/internal/$GROUP/atoms" "$RUN/TEST_PATTERNS.tsv" | tail -1)
atoms=$(awk -F'\t' 'NR>1 && $2!="PERF"' "$RUN/TEST_PATTERNS.tsv" | wc -l)
perfs=$(awk -F'\t' 'NR>1 && $2=="PERF"' "$RUN/TEST_PATTERNS.tsv" | wc -l)
gate ATOMIZE PASS "atom_census_join.py (spec rows per census atom)" "spec_atom_rows=$atoms perf_rows=$perfs ($gen)"

bash "$T/run_atom_tests.sh" "$WT" "$OUT" > "$OUT/run.log" 2>&1
junit=$(python "$T/junit_summary.py" "$OUT/reports/TEST-junit-jupiter.xml" "$RUN/JUNIT_SUMMARY.tsv")
jstatus=$?
gate JUNIT "$([ $jstatus -eq 0 ] && echo PASS || echo FAIL)" "run_atom_tests.sh (stock JDK 21.0.9 + --patch-module java.base)" "$junit"

before=$(host_load)
"$JDK/bin/java" -Djava.awt.headless=true --patch-module "java.base=$OUT/patch" \
  --add-exports "java.base/$PKG=ALL-UNNAMED" --add-opens "java.base/$PKG=ALL-UNNAMED" \
  -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -Dm3.perf.out="$OUT/PERF_raw.tsv" \
  -cp "$OUT/tests;$JUNIT_JAR" "$PERF_CLASS" > "$OUT/perf.log" 2>&1
pstatus=$?
after=$(host_load)
perf=$(python "$T/perf_jit_tiers.py" "$OUT/PERF_raw.tsv" "$OUT/perf.log" "$PKG" "$RUN/PERF.tsv" "$before" "$after")
gate JIT_PERF "$([ $pstatus -eq 0 ] && grep -q 'M3_ATOM_PERF PASS' "$OUT/perf.log" && echo PASS || echo FAIL)" \
  "$PERF_CLASS -XX:+PrintCompilation (warm-up then interleaved rounds)" "$perf host_cpu_pct_before=$before after=$after"

rm -rf "$OUT/census-tree"; mkdir -p "$OUT/census-tree"
(cd "$REF" && find . -name '*.java' -print0 | xargs -0 cp --parents -t "$OUT/census-tree")
(cd "$WT/test" && find "jdk/jdk/internal/$GROUP" -name '*.java' -print0 | xargs -0 cp --parents -t "$OUT/census-tree")
python "$CENSUS/per_atom_test_census.py" "$CENSUS/m3jdk" unused "$OUT/census-tree" "$OUT/census-after" > "$OUT/census.log"
join=$(python "$T/atom_census_join.py" "$CENSUS/m3jdk/PER_ATOM_TEST_CENSUS.tsv" "$OUT/census-after/PER_ATOM_TEST_CENSUS.tsv" \
  "$RUN/TEST_PATTERNS.tsv" "$PKG" "$RUN")
gate PATTERNIZE PASS "per_atom_test_census.py + atom_census_join.py" "$join"
if [ -f "$RUN/MUTATION.tsv" ]; then
  m=$(awk -F'\t' 'NR>1' "$RUN/MUTATION.tsv" | wc -l)
  k=$(awk -F'\t' 'NR>1 && $7=="KILLED"' "$RUN/MUTATION.tsv" | wc -l)
  gate MUTATION "$([ "$m" = "$k" ] && echo PASS || echo FAIL)" "mutate_once.py + run_atom_tests.sh per mutant (MUTATION.tsv)" "mutants=$m killed=$k"
fi
echo "RUN $RUN done"
