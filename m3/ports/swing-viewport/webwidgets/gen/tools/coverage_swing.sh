#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# JaCoCo line/branch coverage of the generated com.m3.swing.web code under the headless JUnit run (CSV + XML,
# no HTML: disk is the binding constraint; coverage_missed.sh lists the open lines from the XML). The tests run on the
# port's own launcher over the JUnit classes run-tests.sh compiles from m3/vendor/third_party; only the JaCoCo agent
# and CLI (an evidence tool, not part of the port's build or tests) come from ~/.m2.
# Usage: coverage_swing.sh <build dir with main/ and test/> <swing-viewport src/main/java> <out dir> [COVERAGE_EXCEPTIONS.tsv]
set -euo pipefail
b=$1; src=$2; out=$3
jdk=${M3_JDK21:-"/c/Program Files/Java/jdk-21"}
m2=$(cygpath -m "$HOME/.m2/repository")
agent=$m2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
cli=$m2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
junit=${JUNIT_OUT:-"$b.junit"}
w() { cygpath -m "$1"; }
mkdir -p "$out"; rm -f "$out/jacoco.exec"
"$jdk/bin/java" -Xmx768m -Djava.awt.headless=true "-javaagent:$agent=destfile=$(w "$out/jacoco.exec"),includes=com.m3.swing.web.*" \
  -cp "$(w "$b/main");$(w "$b/test");$(w "$junit")" com.m3.swing.testkit.M3JUnitLauncher --select-package com.m3.swing.web | grep -E "tests (found|successful|failed)"
"$jdk/bin/java" -jar "$cli" report "$(w "$out/jacoco.exec")" --classfiles "$(w "$b/main/com/m3/swing/web")" --sourcefiles "$(w "$src")" \
  --csv "$(w "$out/coverage.csv")" --xml "$(w "$out/coverage.xml")" >/dev/null
awk -F, 'NR>1{li+=$9; lm+=$8; bi+=$7; bm+=$6} END{printf "COVERAGE line=%.2f%% (%d/%d) branch=%.2f%% (%d/%d)\n", 100*li/(li+lm), li, li+lm, 100*bi/(bi+bm), bi, bi+bm}' "$out/coverage.csv"
bash "$(dirname "$0")/coverage_missed.sh" "$out/coverage.xml" "$src" "${4:-/dev/null}" > "$out/missed.tsv"; echo "OPEN=$(grep -c "	OPEN	" "$out/missed.tsv" || true) EXCEPTION=$(grep -c "	EXCEPTION	" "$out/missed.tsv" || true)"
