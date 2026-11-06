#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates of GATES.tsv in order: recipe fixed point (unify.py --check), m3/algorithms package and install (its oracles, -Xlint:all
# -Werror), the families verify.sh (relocate.py --check, package, describe-module, differential with DEP_CP), the
# jdk.m3.file module (FILE_MODULE: m3/ports/file until #2580, m3/file since; jar name from its pom.xml) compiled,
# packaged and installed, then its tests, and m3/search package as the consumer of the new jar (m3/file's
# M3FileDeltaTest CLI receipt and m3/search's PairSumJniTest fail on hosts without the native build; their Tests run
# lines are compared by the caller against the machine's baseline).
# The recipe fixed point is checked at APPLIED, the commit that applied the recipe (#2524): later landings edit its
# targets on purpose (aa1986b9bab m3/algorithms/pom.xml, #2580 m3/ports/file -> m3/file), so unify.py --check runs over
# that commit's copy of the rule targets under <scratch-dir>, reading the pinned preimages from the same object store.
# usage: verify.sh <donor-m3scale-git-dir> <donor-rev> <scratch-dir> [cases-per-family]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../.." && pwd)
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
APPLIED=614d0a1baf53974dcdb15c449ea34e32bb8d005b
FILE_MODULE="$ROOT/m3/file"
pom_value() { awk -F'[<>]' -v key="$1" '$2 == key {print $3; exit}' "$FILE_MODULE/pom.xml"; }
FILE_JAR="$FILE_MODULE/target/$(pom_value artifactId)-$(pom_value version).jar"
mkdir -p "$3"
UNIFY_ROOT="$(cd "$3" && pwd)/unify-at-${APPLIED:0:12}"
rm -rf "$UNIFY_ROOT" && mkdir -p "$UNIFY_ROOT"
awk -F'\t' 'NR > 1 && $2 != "RETIRE" {print $1}' "$RECIPE_DIR/PLAN.tsv" \
    | xargs git -C "$ROOT" archive "$APPLIED" -- | tar -xf - -C "$UNIFY_ROOT"
GIT_DIR=$(git -C "$ROOT" rev-parse --path-format=absolute --git-common-dir) \
    "$PY" -I "$RECIPE_DIR/unify.py" --root "$UNIFY_ROOT" --check
# install, not package: m3/search depends on m3-algorithms since #2592
mvn -q -B -ntp $REPO -f "$ROOT/m3/algorithms/pom.xml" install
bash "$ROOT/m3/algorithms/recipe/families-20261106/verify.sh" "$1" "$2" "$3" "${4:-100000}"
# -Dexec.skip: the module's test-phase exec (verify.py through python3) is its own gate, run with the tests below
mvn -q -B -ntp $REPO -DskipTests -Dexec.skip=true -f "$FILE_MODULE/pom.xml" install
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module --file "$FILE_JAR" | head -n 1
mvn -B -ntp $REPO -f "$FILE_MODULE/pom.xml" test || echo "FILE: compare the Tests run line with the machine baseline (M3FileDeltaTest CLI receipt)"
mvn -B -ntp $REPO -f "$ROOT/m3/search/pom.xml" package || echo "SEARCH: compare the Tests run line with the machine baseline (PairSumJniTest needs the native library)"
echo PROGRESS_MONITOR_UNIFY_GATES_DONE
