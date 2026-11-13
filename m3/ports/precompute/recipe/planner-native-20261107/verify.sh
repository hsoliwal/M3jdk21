#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Host-side gates of recipe planner-native-20261107 (the native gate is native-gate.sh on Linux): recipe fixed point
# (--check: both kernels and the CMake registration), Maven package of the module (its -Xlint:all -Werror flags; the
# log is the authority), the naming plan, and the class-identity gate against a built base (no Java file changes, so
# every class file must be identical and none new).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> <built-base-module-dir>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python), MVN (default mvn)
set -euo pipefail
RECIPE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE/../.." && pwd)
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
REL=${MODULE#"$ROOT"/}
PY=${PYTHON:-python}
MVN_BIN=${MVN:-mvn}
mkdir -p "$3"
LOG="$3/mvn-package.log"
"$PY" -I "$RECIPE/relocate.py" --donor-git "$1" --donor-rev "$2" --check | grep -E '^(FILE|BUILD|RELOCATE)'
"$MVN_BIN" -B -ntp ${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL} -f "$MODULE/pom.xml" package > "$LOG" 2>&1 || true
grep -q 'BUILD SUCCESS' "$LOG" || { echo "GATE package FAIL (see $LOG)"; exit 1; }
warnings=$(grep -cE '\[WARNING\].*\.java|warning:' "$LOG" || true)
failures=$(grep -cE '\[ERROR\]|FAILURE' "$LOG" || true)
echo "GATE package PASS warnings=${warnings:-0} errors=${failures:-0} $(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$LOG" | tail -n 1 | sed 's/.*Tests run/Tests run/')"
[ "${warnings:-0}" -eq 0 ] && [ "${failures:-0}" -eq 0 ]
(cd "$ROOT" && "$PY" -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . --only "$REL/" | tail -n 1)
bash "$RECIPE/../planner-family-20261107/class_identity.sh" "$4" | grep -vE '^SAME '
