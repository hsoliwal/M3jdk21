#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package without tests (-Xlint:all,-module
# -Werror on main and test sources; the log is grepped for BUILD SUCCESS because mvn's exit code is not reliable under
# Git Bash), the module descriptor, the three relocated oracle mains, the donor-vs-relocated differential, and the JDK
# naming plan over the module (0 refusals). FULL_TESTS=1 adds the module test run and diffs its failing set against the
# base set recorded in TEST_BASELINE_DIFF.txt (the base set is the pre-existing JNI-library failures on Windows).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [seed]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python), FULL_TESTS
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
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -B -ntp $REPO -f "$MODULE/pom.xml" -DskipTests package > "$SCRATCH/mvn-package.log" 2>&1 || true
grep -E "BUILD (SUCCESS|FAILURE)|Total time" "$SCRATCH/mvn-package.log" | sed 's/^\[INFO\] //'
grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log"
echo "compiler warnings: $(grep -c 'WARNING.*\.java' "$SCRATCH/mvn-package.log" || true)"
"$JAVA_BIN/jar" --describe-module --file "$MODULE/target/m3-collections-0.1.0-SNAPSHOT.jar" | head -n 1
for oracle in M3ImmutablePoolSelfTest M3Uuid128SelfTest M3UuidBinarySelfTest; do
    "$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "jdk.m3.collections.$oracle"
done
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$SCRATCH" "${4:-100000}" "${5:-20261106}"
(cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections/ | tail -n 1)
if [ "${FULL_TESTS:-0}" = 1 ]; then
    mvn -B -ntp $REPO -f "$MODULE/pom.xml" test > "$SCRATCH/module-test.log" 2>&1 || true
    grep -E "Tests run: [0-9]+, Failures.*Skipped: [0-9]+$" "$SCRATCH/module-test.log" | tail -n 1
    grep -E "^\[ERROR\]   [A-Z]" "$SCRATCH/module-test.log" | sed 's/^\[ERROR\]   //' | tr -d '' | sort -u > "$SCRATCH/failset-head.txt"
    sed -n '/^## base failing set/,/^## head failing set/p' "$RECIPE_DIR/TEST_BASELINE_DIFF.txt" | grep -v '^#' | sort -u > "$SCRATCH/failset-base.txt"
    diff "$SCRATCH/failset-base.txt" "$SCRATCH/failset-head.txt" && echo "FAILSET IDENTICAL ($(wc -l < "$SCRATCH/failset-head.txt") pre-existing failures)"
fi
