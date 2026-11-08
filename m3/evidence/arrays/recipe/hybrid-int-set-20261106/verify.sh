#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package of the module (its own lint flags
# with -Werror; surefire runs the relocated tests and the reference-model test), compiler-warning and failure scan of
# the Maven log (mvn.cmd may exit 0 on failure under Git Bash, so the log is the authority), the naming plan (0 moves /
# 0 rewrites / 0 refusals expected), the donor-vs-relocated differential (JIT and -Xint), javap -p parity, and the
# untouched-classes gate against a baseline build of master's copy of the module.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> <baseline-module-dir>
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
"$PY" -I "$RECIPE/relocate.py" --donor-git "$1" --donor-rev "$2" --check | tail -n 1
"$MVN_BIN" -B -ntp ${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL} -f "$MODULE/pom.xml" package > "$LOG" 2>&1 || true
grep -q 'BUILD SUCCESS' "$LOG" || { echo "GATE package FAIL (see $LOG)"; exit 1; }
warnings=$(grep -cE '\[WARNING\].*\.java|warning:' "$LOG" || true)
failures=$(grep -cE '\[ERROR\]|FAILURE' "$LOG" || true)
echo "GATE package PASS warnings=${warnings:-0} errors=${failures:-0} $(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$LOG" | tail -n 1 | sed 's/.*Tests run/Tests run/')"
[ "${warnings:-0}" -eq 0 ] && [ "${failures:-0}" -eq 0 ]
(cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only "$REL/" | tail -n 1)
bash "$RECIPE/differential.sh" "$1" "$2" "$3"
bash "$RECIPE/javap_parity.sh" "$3/donor-classes" "$3"
bash "$RECIPE/class_identity.sh" "$4"
