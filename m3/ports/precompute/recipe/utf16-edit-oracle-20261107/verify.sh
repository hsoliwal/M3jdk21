#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package of m3/ports/precompute
# (-Xlint:all -Werror; surefire runs the relocated contract test and the proof wrapper that prints the crate
# receipt), the naming plan (0 moves / 0 rewrites / 0 refusals expected) and, when a baseline module build is given,
# the untouched-classes gate. mvn.cmd under Git Bash may exit 0 on a failed build, so the log is the authority:
# BUILD SUCCESS must be present, compiler warnings and [ERROR] lines are counted.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [<baseline-module-dir>]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), MAVEN_SETTINGS (optional -s),
#        MVN (default mvn), PYTHON (default python)
set -euo pipefail
RECIPE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE/../.." && pwd)
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
PY=${PYTHON:-python}
MVN=${MVN:-mvn}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
SETTINGS=${MAVEN_SETTINGS:+-s $MAVEN_SETTINGS}
mkdir -p "$3"
"$PY" -I "$RECIPE/relocate.py" --donor-git "$1" --donor-rev "$2" --check
LOG="$3/mvn-package.log"
"$MVN" -B -ntp -T1 $SETTINGS $REPO -f "$MODULE/pom.xml" package > "$LOG" 2>&1 || true
grep -q "BUILD SUCCESS" "$LOG" || { echo "PACKAGE FAIL: no BUILD SUCCESS in $LOG"; exit 1; }
warnings=$(grep -c "WARNING.*\.java" "$LOG" || true); warnings=${warnings:-0}
errors=$(grep -c "^\[ERROR\]" "$LOG" || true); errors=${errors:-0}
tests=$(grep -E "^\[(INFO|WARNING)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$" "$LOG" | tail -n 1 || true)
echo "PACKAGE PASS warnings=$warnings errors=$errors ${tests#*\] }"
grep -F "M3_UTF16_BITPARALLEL_EDIT_PROOF_V1" "$LOG" | tail -n 1
[ "$warnings" -eq 0 ] && [ "$errors" -eq 0 ] || exit 1
if [ -f "$ROOT/make/m3/tooling/jdk-naming/jdk_naming.py" ]; then
    (cd "$ROOT" && "$PY" -I make/m3/tooling/jdk-naming/jdk_naming.py plan --only m3/ports/precompute/ | tail -n 1)
fi
if [ $# -ge 4 ]; then
    bash "$RECIPE/class_identity.sh" "$4"
fi
