#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# run_reach.sh <worktree> <out_dir> <REACH.tsv> : the REACH gate. Runs every generated per-atom test under the JaCoCo
# agent (output=none, inclbootstrapclasses: java.desktop is a boot-layer module) with ReachListener on the launcher
# classpath, which writes one execution-data file per dynamic test; then ReachAnalyzer checks that each atom's own
# method executed in its own test. Env: JACOCO_AGENT, JACOCO_CLI (0.8.15 runtime agent and nodeps CLI jars).
set -euo pipefail
wt=$1; out=$2; report=$3
jdk=${JDK:-"/c/Program Files/Java/jdk-21"}
m2="$HOME/.m2/repository"
junit=${JUNIT_JAR:-"$m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"}
agent=${JACOCO_AGENT:-"$m2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar"}
cli=${JACOCO_CLI:-"$m2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar"}
tools="$wt/m3/tooling/jdk-superset/viewport-swing"
win() { cygpath -m "$1"; }
lst="$out/reach/listener"; ana="$out/reach/analyzer"; data="$out/reach/exec"
rm -rf "$out/reach"; mkdir -p "$lst/META-INF/services" "$ana" "$data"
"$jdk/bin/javac" -nowarn -cp "$(win "$junit")" -d "$(win "$lst")" "$(win "$tools/tools/reach/ReachListener.java")"
cp "$tools/tools/reach/META-INF/services/org.junit.platform.launcher.TestExecutionListener" "$lst/META-INF/services/"
"$jdk/bin/javac" -nowarn -cp "$(win "$cli")" -d "$(win "$ana")" "$(win "$tools/tools/reach/ReachAnalyzer.java")"
EXTRA_CP="$(win "$lst")" \
EXTRA_JAVA_OPTS="-javaagent:$(win "$agent")=output=none,inclbootstrapclasses=true,includes=sun.swing.list.*:sun.swing.table.*:sun.swing.tree.* -Dreach.dir=$(win "$data")" \
  bash "$tools/tools/run_atom_tests.sh" "$wt" "$out" list table tree || echo "REACH: some tests failed under the agent"
echo "REACH exec files=$(ls "$data" | wc -l)"
"$jdk/bin/java" -cp "$(win "$ana");$(win "$cli")" ReachAnalyzer "$(win "$out/patch/java.desktop")" "$(win "$data")" \
    "$(win "$tools/ATOMS.tsv")" "$(win "$tools/ATOM_SPEC.tsv")" "$(win "$report")"
