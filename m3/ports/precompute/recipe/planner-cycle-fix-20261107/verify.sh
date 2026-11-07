#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: apply.py fixed point (--check), the PATCH-aware fixed point of the relocation
# recipe planner-family-20261107, Maven package of the module (its -Xlint:all -Werror flags; the log is the authority),
# the naming plan, the regression test against the base classes (it must fail with StackOverflowError there), the
# differential (acyclic corpus identical to the base, strict cyclic run), the mutants, and the class-identity gate
# (only the declared classes change).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> <built-base-module-dir> <junit-platform-console-standalone.jar>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python), MVN (default mvn)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE/../.." && pwd)
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
REL=${MODULE#"$ROOT"/}
PY=${PYTHON:-python}
MVN_BIN=${MVN:-mvn}
mkdir -p "$3"
LOG="$3/mvn-package.log"
PACKAGE=jdk.m3.precompute
"$PY" -I "$RECIPE/apply.py" --check | tail -n 1
"$PY" -I "$RECIPE/../planner-family-20261107/relocate.py" --donor-git "$1" --donor-rev "$2" --check | grep -E '^FILE PATCHED|^RELOCATE'
"$MVN_BIN" -B -ntp ${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL} -f "$MODULE/pom.xml" package > "$LOG" 2>&1 || true
grep -q 'BUILD SUCCESS' "$LOG" || { echo "GATE package FAIL (see $LOG)"; exit 1; }
warnings=$(grep -cE '\[WARNING\].*\.java|warning:' "$LOG" || true)
failures=$(grep -cE '\[ERROR\]|FAILURE' "$LOG" || true)
echo "GATE package PASS warnings=${warnings:-0} errors=${failures:-0} $(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$LOG" | tail -n 1 | sed 's/.*Tests run/Tests run/')"
[ "${warnings:-0}" -eq 0 ] && [ "${failures:-0}" -eq 0 ]
(cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only "$REL/" | tail -n 1)
regression() { # regression <name> <main classes>
    "$JAVA_HOME/bin/java" -Djava.io.tmpdir="$3" -Xss1m -jar "$5" execute --disable-banner --disable-ansi-colors \
        --details=tree --details-theme=ascii -cp "$(winpath "$2")$SEP$(winpath "$MODULE/target/test-classes")" \
        --select-class "$PACKAGE.M3IndexPrecomputePlannerCycleTest" 2>&1 | tr -d '\r' > "$3/regression-$1.txt" || true
    echo "REGRESSION $1: $(grep -oE '[0-9]+ tests (successful|failed)' "$3/regression-$1.txt" | tr '\n' ' ')overflows=$(grep -c 'StackOverflowError' "$3/regression-$1.txt" || true)"
}
regression base "$4/target/classes" "$3" "$4" "$5"
regression head "$MODULE/target/classes" "$3" "$4" "$5"
grep -q ' 0 tests successful' "$3/regression-base.txt" && grep -q 'StackOverflowError' "$3/regression-base.txt"
grep -q ' 0 tests failed' "$3/regression-head.txt"
bash "$RECIPE/differential.sh" "$4" "$3"
"$PY" -I "$RECIPE/mutants.py" "$3/mutants" "$MODULE/target/test-classes" "$JAVA_HOME/bin" "$PACKAGE" \
    "$(awk -F'\t' '$1=="LITERAL" && $2 ~ /:$/ {print $3; exit}' "$RECIPE/../planner-family-20261107/RELOCATION_MAP.tsv")"
bash "$RECIPE/class_identity.sh" "$4" \
    '^(classes/jdk/m3/precompute/M3IndexPrecomputePlanner(\$[A-Za-z0-9]+)?\.class|test-classes/jdk/m3/precompute/M3IndexPrecomputePlannerDump(\$[A-Za-z0-9]+)?\.class)$' \
    | grep -vE '^(SAME|NEW) '
