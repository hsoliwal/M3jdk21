#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order (derived from recipe/algorithm-extras-20261106/verify.sh): the fixed points of
# the relocation recipe and of the owner-edit recipe, the jdk-naming plan of the module, Maven package with the real
# pom (-Xlint:all -Werror; the ported donor tests and the reference oracles in the test phase), the module
# descriptor, the donor-vs-relocated differential (also -Xint), javap -p parity, the mutants, the donor's own proof
# main (its figure must hold), and, when BASELINE_CLASSES names the target/classes of a master build, the
# byte-identity of every untouched class file (the one owner edit, AlgorithmShapeCatalog, excepted).
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [cases]
# env:   JAVA_HOME (a JDK 21); MAVEN_REPO_LOCAL (-Dmaven.repo.local, a drive path on Windows); MAVEN_REPO_TAIL
#        (optional -Dmaven.repo.local.tail, read-only tail repository); JAVA_TOOL_TMP (optional java.io.tmpdir of every
#        JVM, Maven and surefire forks included); BASELINE_CLASSES (optional); PYTHON
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
ROOT=$(cd "$MODULE/../.." && pwd)
PY=${PYTHON:-python}
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
M2=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}
TAIL=${MAVEN_REPO_TAIL:-$M2}
GROUP=com.m3   # spelled through the variable: the naming recipe rewrites a dotted group as a package path
TMP=${JAVA_TOOL_TMP:+-Djava.io.tmpdir=$JAVA_TOOL_TMP}
export JAVA_TOOL_TMP
export DEP_CP="$TAIL/${GROUP//.//}/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar"
export LOMBOK_JAR=${LOMBOK_JAR:-$TAIL/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar}
export DONOR_CP=${DONOR_CP:-$LOMBOK_JAR$SEP$TAIL/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar}
CONSOLE=${CONSOLE_JAR:-$TAIL/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar}
DUMP_SHA=9a521edbce0ebbed2a313991e98d19903d57d68cc975472d93d2d393244b62b7
PROOF="M3_INT_LCS_WITNESS_V1 cases=32651 sha256=97fe8bdf180b3e91f7072f7e8aac60b94f5fbaf998e76efad0eb018d8c21ff20"
mkdir -p "$3"

"$PY" -I -B "$RECIPE_DIR/relocate.py" --donor-git "$1" --donor-rev "$2" --check | tail -n 1
"$PY" -I -B "$RECIPE_DIR/apply.py" --check | tail -n 1
(cd "$ROOT" && "$PY" -I -B m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/algorithms/ | tail -n 2)
MAVEN_OPTS="${MAVEN_OPTS:-} $TMP" mvn -q -B -ntp -T1 -Dmaven.repo.local="$M2" ${MAVEN_REPO_TAIL:+-Dmaven.repo.local.tail=$MAVEN_REPO_TAIL} \
    "-DargLine=$TMP" -f "$MODULE/pom.xml" clean package
"${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/jar" --describe-module \
    --file "$MODULE/target/m3-algorithms-0.1.0-SNAPSHOT.jar" | head -n 1
bash "$RECIPE_DIR/differential.sh" "$1" "$2" "$3/diff" "${4:-4000}" | tee "$3/diff.log" | tail -n 1
grep -q "relocated_sha256=$DUMP_SHA" "$3/diff.log" || { echo "differential digest changed"; exit 1; }
"$PY" -I -B "$RECIPE_DIR/javap_parity.py" "$3/diff" --javap "$JAVA_HOME/bin/javap" | tail -n 1
"$PY" -I -B "$RECIPE_DIR/mutants.py" "$3/mutants" --dump-sha256 "$DUMP_SHA" --console "$CONSOLE" --dep-cp "$DEP_CP" \
    ${JAVA_TOOL_TMP:+--tmp "$JAVA_TOOL_TMP"} | tail -n 1
# the donor crate's own proof main, Java mode, on the donor compile of differential.sh (JIT and -Xint)
proof_main=$(awk -F'\t' '$1=="FILE" && $3 ~ /\/IntLcsWitness\.java$/ {p=$2; sub(/^.*\/src\/main\/java\//, "", p);
    sub(/\.java$/, "", p); gsub(/\//, ".", p); print p}' "$RECIPE_DIR/RELOCATION_MAP.tsv")
for mode in "" "-Xint"; do
    out=$("$JAVA_HOME/bin/java" $mode -Xmx256m $TMP -cp "$3/diff/classes" "$proof_main" --java NO_LIB)
    echo "$out" | grep -q "^$PROOF " || { echo "donor proof figure changed: $out"; exit 1; }
    echo "DONOR_PROOF ${mode:-JIT} $out"
done
if [ -n "${BASELINE_CLASSES:-}" ]; then
    same=0; differ=0
    while read -r rel; do
        if cmp -s "$BASELINE_CLASSES/$rel" "$MODULE/target/classes/$rel"; then same=$((same + 1));
        else differ=$((differ + 1)); echo "DIFFERS $rel"; fi
    done < <(cd "$BASELINE_CLASSES" && find . -name '*.class' | sort)
    echo "UNTOUCHED identical=$same differing=$differ (expected differing: the owner edit AlgorithmShapeCatalog.class)"
fi
echo "VERIFY PASS"
