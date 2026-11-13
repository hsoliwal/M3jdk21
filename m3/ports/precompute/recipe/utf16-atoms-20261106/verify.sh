#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package of m3/ports/precompute
# (-Xlint:all -Werror; surefire runs the relocated contract tests and the two proof wrappers that print the crate
# figures), the naming plan (0 moves / 0 rewrites / 0 refusals expected) and the donor-vs-relocated differential.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE/../.." && pwd)
ROOT=$(git -C "$MODULE" rev-parse --show-toplevel)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
"$PY" -I "$RECIPE/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -q -B -ntp $REPO -f "$MODULE/pom.xml" package
if [ -f "$ROOT/make/m3/tooling/jdk-naming/jdk_naming.py" ]; then
    (cd "$ROOT" && "$PY" -I make/m3/tooling/jdk-naming/jdk_naming.py plan --only m3/ports/precompute/ | tail -n 1)
fi
bash "$RECIPE/differential.sh" "$1" "$2" "$3"
