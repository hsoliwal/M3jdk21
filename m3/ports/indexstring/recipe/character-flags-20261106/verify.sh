#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package of m3/ports/indexstring (-Xlint:all
# -Werror; the exec-maven-plugin test-phase mains print EXISTING_SURFACE_PASS, RETAINED_VIEW_PASS and the relocated
# smoke oracle's PASS line with its figures), the naming plan (0 moves / 0 rewrites / 0 refusals expected) and the
# donor-vs-relocated differential. The Maven log goes to <scratch-dir>/mvn-package.log and is grepped for BUILD SUCCESS
# (mvn.cmd exits 0 on failure under Git Bash).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE/../.." && pwd)
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
mkdir -p "$3"
"$PY" -I "$RECIPE/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -B -ntp $REPO -f "$MODULE/pom.xml" package > "$3/mvn-package.log" 2>&1 || true
grep -E "_PASS |PASS actual|BUILD (SUCCESS|FAILURE)|ERROR|warning:" "$3/mvn-package.log" || true
grep -q "BUILD SUCCESS" "$3/mvn-package.log"
if [ -f "$ROOT/m3/tooling/jdk-naming/jdk_naming.py" ]; then
    (cd "$ROOT" && "$PY" -I m3/tooling/jdk-naming/jdk_naming.py plan --only m3/ports/indexstring/ | tail -n 1)
fi
bash "$RECIPE/differential.sh" "$1" "$2" "$3/differential"
