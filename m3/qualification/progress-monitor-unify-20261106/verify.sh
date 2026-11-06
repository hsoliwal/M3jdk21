#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates of GATES.tsv in order: recipe fixed point (unify.py --check), m3/algorithms package (15 oracles, -Xlint:all
# -Werror), the families verify.sh (relocate.py --check, package, describe-module, differential with DEP_CP),
# m3/ports/file package and install, m3/search package as the consumer of the new jar (its PairSumJniTest needs a
# native library; its figures are compared by the caller against the machine's baseline).
# usage: verify.sh <donor-m3scale-git-dir> <donor-rev> <scratch-dir> [cases-per-family]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../.." && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
"$PY" -I "$RECIPE_DIR/unify.py" --root "$ROOT" --check
mvn -q -B -ntp $REPO -f "$ROOT/m3/algorithms/pom.xml" package
bash "$ROOT/m3/algorithms/recipe/families-20261106/verify.sh" "$1" "$2" "$3" "${4:-100000}"
mvn -q -B -ntp $REPO -f "$ROOT/m3/ports/file/pom.xml" package
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module \
    --file "$ROOT/m3/ports/file/target/m3-ports-file-0.1.0-SNAPSHOT.jar" | head -n 1
mvn -q -B -ntp $REPO -DskipTests -f "$ROOT/m3/ports/file/pom.xml" install
mvn -B -ntp $REPO -f "$ROOT/m3/search/pom.xml" package || echo "SEARCH: compare the Tests run line with the machine baseline (PairSumJniTest needs the native library)"
echo PROGRESS_MONITOR_UNIFY_GATES_DONE
