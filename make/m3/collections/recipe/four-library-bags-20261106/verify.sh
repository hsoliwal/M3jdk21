#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order: recipe fixed point (--check), Maven package without tests (-Xlint:all,-module
# -Werror on main and test sources; the log is grepped for BUILD SUCCESS because mvn's exit code is not reliable under
# Git Bash), the module descriptor, the relocated oracle mains (algebra proof, both constrained-heap failure proofs in
# isolated JVMs), the reference model (1,000,000 seeded operations), the donor-vs-relocated differential, and the JDK
# naming plan over the module (its rows must all lie outside this recipe's files). FULL_TESTS=1 adds the module test
# run and diffs its failing set against the base set recorded in TEST_BASELINE_DIFF.txt.
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [seed]
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional -Dmaven.repo.local), PYTHON (default python), FULL_TESTS
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../../../../../m3/collections")
ROOT=$(winpath "$(dirname "$0")/../../../../..")
mkdir -p "$3"
SCRATCH=$(winpath "$3")
PY=${PYTHON:-python}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check
mvn -B -ntp $REPO -f "$MODULE/pom.xml" -DskipTests package > "$SCRATCH/mvn-package.log" 2>&1 || true
grep -E "BUILD (SUCCESS|FAILURE)|Total time" "$SCRATCH/mvn-package.log" | sed 's/^\[INFO\] //'
grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log"
echo "compiler warnings: $(grep -c 'WARNING.*\.java' "$SCRATCH/mvn-package.log" || true)"
"$JAVA_BIN/jar" --describe-module --file "$MODULE/target/m3-collections-0.1.0-SNAPSHOT.jar" | head -n 1
"$JAVA_BIN/java" -cp "$CP" jdk.m3.collections.M3BagAlgebraProof
"$JAVA_BIN/java" -Xmx32m -cp "$CP" jdk.m3.collections.M3BagMapFailureProof rehash
"$JAVA_BIN/java" -Xmx16m -cp "$CP" jdk.m3.collections.M3BagMapFailureProof remove
"$JAVA_BIN/java" -cp "$CP" jdk.m3.collections.M3FourLibraryBagsReferenceModel 1000000 "${5:-20261106}"
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$SCRATCH" "${4:-200000}" "${5:-20261106}"
(cd "$ROOT" && "$PY" -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections/ --tsv "$SCRATCH/naming-plan.tsv" | tail -n 1)
echo "naming rows in this recipe's files: $(grep -cE '/(M3PackedLongLongHashMap|M3PackedLongBag|M3PackedLongBagContract|M3PackedLongLongMap|M3PackedHashAtoms|M3PackedSupport|M3LongLongPredicate|M3BagAlgebraProof|M3BagMapFailureProof|M3FourLibraryBags[A-Za-z]*)[.]java|/four-library-bags-20261106/' "$SCRATCH/naming-plan.tsv" || true)"
if [ "${FULL_TESTS:-0}" = 1 ]; then
    mvn -B -ntp $REPO -f "$MODULE/pom.xml" test > "$SCRATCH/module-test.log" 2>&1 || true
    grep -E "Tests run: [0-9]+, Failures.*Skipped: [0-9]+" "$SCRATCH/module-test.log" | tail -n 1 | tr -d '\r'
    { grep -E "^\[ERROR\]   [A-Z]" "$SCRATCH/module-test.log" || true; } | sed 's/^\[ERROR\]   //' | tr -d '\r' | sort -u > "$SCRATCH/failset-head.txt"
    sed -n '/^## base failing set/,/^## head failing set/p' "$RECIPE_DIR/TEST_BASELINE_DIFF.txt" | { grep -v '^#' || true; } | sort -u > "$SCRATCH/failset-base.txt"
    diff "$SCRATCH/failset-base.txt" "$SCRATCH/failset-head.txt" && echo "FAILSET IDENTICAL ($(wc -l < "$SCRATCH/failset-head.txt") pre-existing failures)"
fi
