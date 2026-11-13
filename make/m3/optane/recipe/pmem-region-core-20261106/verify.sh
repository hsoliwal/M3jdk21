#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the Java gates of GATES.tsv in order: recipe fixed point (--check), Maven package of this module (-Xlint:all
# -Werror; the donor's FILE oracle and the Java-path replay in the test phase, the native lanes excluded), the module
# description, the donor-vs-relocated differential on the FILE backend and the JDK naming plan over m3/optane. The
# native matrix (gate 4) is a separate Linux run of native-matrix.sh for each family plus differential.sh in native
# mode (see README.md).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [operations=100000]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
ROOT=$(cd "$MODULE/../.." && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -B -ntp $REPO -f "$MODULE/pom.xml" package | grep -E "BUILD|Tests run:|_PASS|FILE observation|ERROR"
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module \
    --file "$MODULE/target/m3-optane-0.1.0-SNAPSHOT.jar" | head -n 3
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$3" "${4:-100000}"
"$PY" -I "$ROOT/m3/tooling/jdk-naming/jdk_naming.py" plan --root "$ROOT" --only m3/optane/
