#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# JaCoCo line/branch coverage of the generated Nebula m3web bundle: the headless model tests and the SWT canvas tests
# (M3<Concept>_Test, a Display per JVM) in one JVM on the hidden test desktop (ui_lock.sh slot + hidden_java.sh, so
# no window reaches the interactive desktop). CSV + XML (no HTML); coverage_missed.sh lists the open lines.
# M3_NEBULA_CP: optional class path of existing Nebula widget bundles for the viewport adapter tests (tests of
# bundles that are absent print NOT_RUN).
# Usage: coverage_nebula.sh <build dir of gate_nebula.sh> <widgets/m3web dir> <swt jar> <junit4 jar> <hamcrest jar> <out dir> [COVERAGE_EXCEPTIONS.tsv]
set -euo pipefail
b=$1; src=$2; swt=$3; junit=$4; ham=$5; out=$6; exc=${7:-/dev/null}
jdk=${M3_JDK21:-"/c/Program Files/Java/jdk-21"}
m2=$(cygpath -m "$HOME/.m2/repository")
agent=$m2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
cli=$m2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
w() { cygpath -m "$1"; }
mkdir -p "$out"; rm -f "$out/jacoco.exec" "$out/junit.log"
(cd "$b/test" && find . -name '*_Test.class' | sed 's|^\./||;s|\.class$||;s|/|.|g' | sort) > "$out/classes.txt"
bash C:/m3-superset/tools/ui_lock.sh m3web-coverage bash C:/m3-superset/tools/hidden_java.sh "${M3_TIMEOUT:-1200}" "$out/junit.log" \
  -Xmx768m "-javaagent:$agent=destfile=$(w "$out/jacoco.exec"),includes=org.eclipse.nebula.widgets.m3web.*" \
  -cp "$(w "$b/main");$(w "$b/test");$(w "$swt");$(w "$junit");$(w "$ham")${M3_NEBULA_CP:+;$M3_NEBULA_CP}" org.eclipse.nebula.widgets.m3web.M3WebTestMain "$(w "$out/classes.txt")" || true
grep -E "^OK|^Tests run|^FAILURES" "$out/junit.log" || tail -5 "$out/junit.log"
"$jdk/bin/java" -jar "$cli" report "$(w "$out/jacoco.exec")" --classfiles "$(w "$b/main")" \
  --sourcefiles "$(w "$src/org.eclipse.nebula.widgets.m3web/src")" --csv "$(w "$out/coverage.csv")" --xml "$(w "$out/coverage.xml")" >/dev/null
awk -F, 'NR>1{li+=$9; lm+=$8; bi+=$7; bm+=$6} END{printf "COVERAGE line=%.2f%% (%d/%d) branch=%.2f%% (%d/%d)\n", 100*li/(li+lm), li, li+lm, 100*bi/(bi+bm), bi, bi+bm}' "$out/coverage.csv"
bash "$(dirname "$0")/coverage_missed.sh" "$out/coverage.xml" "$src/org.eclipse.nebula.widgets.m3web/src" "$exc" > "$out/missed.tsv"
echo "OPEN=$(grep -c "	OPEN	" "$out/missed.tsv" || true) EXCEPTION=$(grep -c "	EXCEPTION	" "$out/missed.tsv" || true)"
