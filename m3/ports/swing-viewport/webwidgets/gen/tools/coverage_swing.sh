#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# JaCoCo line/branch coverage of the generated com.m3.swing.web code under the headless JUnit run.
# Usage: coverage_swing.sh <build dir with main/ and test/> <swing-viewport src/main/java> <out dir>
set -euo pipefail
b=$1; src=$2; out=$3
jdk=${M3_JDK21:-"/c/Program Files/Java/jdk-21"}
m2=$(cygpath -m "$HOME/.m2/repository")
agent=$m2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
cli=$m2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
junit=$m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
w() { cygpath -m "$1"; }
mkdir -p "$out"; rm -f "$out/jacoco.exec"
"$jdk/bin/java" -Xmx768m -Djava.awt.headless=true "-javaagent:$agent=destfile=$(w "$out/jacoco.exec"),includes=com.m3.swing.web.*" \
  -jar "$junit" execute -cp "$(w "$b/main");$(w "$b/test")" --disable-banner --details=summary --select-package com.m3.swing.web | grep -E "tests (found|successful|failed)"
"$jdk/bin/java" -jar "$cli" report "$(w "$out/jacoco.exec")" --classfiles "$(w "$b/main/com/m3/swing/web")" --sourcefiles "$(w "$src")" \
  --csv "$(w "$out/coverage.csv")" --html "$(w "$out/html")" >/dev/null
awk -F, 'NR>1{li+=$9; lm+=$8; bi+=$7; bm+=$6} END{printf "COVERAGE line=%.2f%% (%d/%d) branch=%.2f%% (%d/%d)\n", 100*li/(li+lm), li, li+lm, 100*bi/(bi+bm), bi, bi+bm}' "$out/coverage.csv"
