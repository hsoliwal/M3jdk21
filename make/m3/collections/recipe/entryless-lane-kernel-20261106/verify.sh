#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package without tests (-Xlint:all,-module
# -Werror on main and test sources; the log is grepped for BUILD SUCCESS because mvn's exit code is not reliable under
# Git Bash), the module descriptor, the relocated oracle main, the donor-vs-relocated differential (proof + dump driver),
# and the JDK naming plan over the module (0 refusals). FULL_TESTS=1 adds the module test run and diffs its failing set
# against the base set recorded in TEST_BASELINE_DIFF.txt. On a Linux host M3_LANE_NATIVE_GATE=1 adds gate 4 through
# native-gate.sh <scratch>/differential <scratch>/native.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [seed]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python), FULL_TESTS, M3_LANE_NATIVE_GATE
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
ROOT=$(winpath "$(dirname "$0")/../../../..")
mkdir -p "$3"
SCRATCH=$(winpath "$3")
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
gate() { echo "GATE $1 $2"; }
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check | tail -n 1 && gate recipe PASS || gate recipe FAIL
mvn -B -ntp $REPO -f "$MODULE/pom.xml" -DskipTests package > "$SCRATCH/mvn-package.log" 2>&1 || true
grep -E "BUILD (SUCCESS|FAILURE)|Total time" "$SCRATCH/mvn-package.log" | sed 's/^\[INFO\] //'
grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log" && gate 1_package_lint_werror PASS || gate 1_package_lint_werror FAIL
echo "compiler warnings: $(grep -c 'WARNING.*\.java' "$SCRATCH/mvn-package.log" || true)"
"$JAVA_BIN/jar" --describe-module --file "$MODULE/target/m3-collections-0.1.0-SNAPSHOT.jar" | head -n 1
"$JAVA_BIN/java" -ea -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" jdk.m3.collections.M3IndexMaskLaneKernelProof && gate 2_oracle PASS || gate 2_oracle FAIL
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$SCRATCH/differential" "${4:-100000}" "${5:-20261106}" && gate 3_differential PASS || gate 3_differential FAIL
(cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections/ | tail -n 1 | tee "$SCRATCH/naming.txt")
grep -q "refusals=0" "$SCRATCH/naming.txt" && gate naming PASS || gate naming FAIL
if [ "${FULL_TESTS:-0}" = 1 ]; then
    mvn -B -ntp $REPO -f "$MODULE/pom.xml" test > "$SCRATCH/module-test.log" 2>&1 || true
    grep -E "Tests run: [0-9]+, Failures.*Skipped: [0-9]+" "$SCRATCH/module-test.log" | tail -n 1 | tr -d '\r'
    grep -E "^\[ERROR\]   [A-Z]" "$SCRATCH/module-test.log" | sed 's/^\[ERROR\]   //' | tr -d '\r' | sort -u > "$SCRATCH/failset-head.txt"
    sed -n '/^## base failing set/,/^## head failing set/p' "$RECIPE_DIR/TEST_BASELINE_DIFF.txt" | grep -v '^#' | grep -v '^$' | sort -u > "$SCRATCH/failset-base.txt"
    diff "$SCRATCH/failset-base.txt" "$SCRATCH/failset-head.txt" && gate 2_module_tests_vs_base "PASS ($(wc -l < "$SCRATCH/failset-head.txt") pre-existing failures)" || gate 2_module_tests_vs_base FAIL
fi
if [ "${M3_LANE_NATIVE_GATE:-0}" = 1 ]; then
    bash "$RECIPE_DIR/native-gate.sh" "$SCRATCH/differential" "$SCRATCH/native" && gate 4_native PASS || gate 4_native FAIL
else
    gate 4_native "SKIP (Linux host: M3_LANE_NATIVE_GATE=1)"
fi
