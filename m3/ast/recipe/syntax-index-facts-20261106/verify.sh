#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates of GATES.tsv in order: recipe fixed point (apply.py --check), naming fixed point over m3/ast, m3/ast main
# build (mvn -Dmaven.test.skip=true package: -Xlint:all -Werror, release 21, proc none), m3/ast tests (javac with the
# pom's flags plus the one --add-exports master's pom lacks, then the JUnit console over every *Test class), the crate
# proof figures inside that run, the before/after differential (oracle/differential.sh, once per EDIT row of
# EDITS.tsv: its pinned preimage blob vs the working tree), and mvn package of the
# module, whose testCompile is red on master (M3SyntaxGrowthAgent imports jdk.internal.org.objectweb.asm): every
# compiler error must lie in that one pre-existing file.
# usage: verify.sh <donor-git-dir> <scratch-dir>
# env:   JAVA_HOME (a JDK 21), MAVEN_REPO_LOCAL (optional; also where the JUnit console jar is read), PYTHON
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)
DONOR=$1; OUT=$2
PY=${PYTHON:-python}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}
REPO=${MAVEN_REPO_LOCAL:+-Dmaven.repo.local=$MAVEN_REPO_LOCAL}
CONSOLE="$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
EXPORT=--add-exports=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/test-classes"
"$PY" -I "$RECIPE_DIR/apply.py" --donor-git "$DONOR" --root "$ROOT" --check
"$PY" -I "$ROOT/m3/tooling/jdk-naming/jdk_naming.py" plan --root "$ROOT" --only m3/ast/ | tail -n 1
mvn -B -ntp $REPO -Dmaven.test.skip=true -f "$ROOT/m3/ast/pom.xml" clean package > "$OUT/mvn-main.log" 2>&1 || true
grep -q 'BUILD SUCCESS' "$OUT/mvn-main.log" || { echo "MAIN BUILD FAILED (see $OUT/mvn-main.log)"; exit 1; }
echo "MAIN_BUILD SUCCESS warnings=$(grep -c '^\[WARNING\]' "$OUT/mvn-main.log" || true)"
"$J/javac" --release 21 -proc:none -Xlint:all -Werror -g -d "$OUT/classes" \
    $(find "$ROOT/m3/ast/src/main/java" -name '*.java' | sort)
"$J/javac" -proc:none -Xlint:all -Werror -g "$EXPORT" -cp "$OUT/classes$SEP$CONSOLE" -d "$OUT/test-classes" \
    $(find "$ROOT/m3/ast/src/test/java" -name '*.java' | sort)
SELECT=$(cd "$OUT/test-classes" && find . -name '*Test.class' ! -name '*$*' | sort \
    | sed 's#^\./##; s#\.class$##; s#/#.#g; s#^#--select-class=#')
timeout 600 "$J/java" "$EXPORT" -Xmx1g -jar "$CONSOLE" execute --disable-banner --disable-ansi-colors \
    --details=tree --class-path "$OUT/classes$SEP$OUT/test-classes" $SELECT > "$OUT/junit.log" 2>&1 || true
grep -E 'tests (found|successful|failed)' "$OUT/junit.log"
grep -q ' 0 tests failed' "$OUT/junit.log" || { echo "MODULE TESTS FAILED (see $OUT/junit.log)"; exit 1; }
grep -Fx 'PASS M3_AST_FACT_STABLE_SCATTER maps=20004 fact_rows_large=300000 assertions=20405713' "$OUT/junit.log"
# one differential per EDIT row: its pinned preimage (the master owner, then each image a crate produced) vs the tree
awk -F'\t' '$2 == "EDIT" {print $4}' "$RECIPE_DIR/EDITS.tsv" | while read -r blob; do
    bash "$RECIPE_DIR/oracle/differential.sh" "$blob" "$OUT/differential-$blob" | grep -v '^SIDE\|^TOTAL'
done
mvn -B -ntp $REPO -f "$ROOT/m3/ast/pom.xml" clean package > "$OUT/mvn-package.log" 2>&1 || true
outside=$(grep -E '^\[ERROR\] /.*\.java:\[' "$OUT/mvn-package.log" | grep -vc 'M3SyntaxGrowthAgent\.java' || true)
echo "MVN_PACKAGE testCompile errors=$(grep -cE '^\[ERROR\] /.*\.java:\[' "$OUT/mvn-package.log" || true) outside_master_red_file=${outside:-0}"
[ "${outside:-0}" = 0 ] || { echo "NEW COMPILER ERRORS (see $OUT/mvn-package.log)"; exit 1; }
echo SYNTAX_INDEX_FACTS_GATES_DONE
