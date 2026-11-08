#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs the gates of GATES.tsv in order (derived from recipe/catalogue-kernels-20261107/verify.sh): the fixed points of
# the relocation recipe and of the owner-edit recipe, the jdk-naming plan of the module, Maven package with the real
# pom (-Xlint:all -Werror; the ported donor tests and the reference oracles in the test phase), the module
# descriptor, the donor-vs-relocated differential (also -Xint), javap -p parity, the mutants, and, when
# BASELINE_CLASSES names the target/classes of a master build, the byte-identity of every untouched class file (the
# one owner edit, M3AlgorithmShapeCatalog, excepted). This leaf has no donor proof main.
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
dep_jar="${GROUP//.//}/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar"
if [ -f "$M2/$dep_jar" ]; then export DEP_CP="$M2/$dep_jar"; else export DEP_CP="$TAIL/$dep_jar"; fi
export LOMBOK_JAR=${LOMBOK_JAR:-$TAIL/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar}
export DONOR_CP=${DONOR_CP:-$LOMBOK_JAR$SEP$TAIL/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar}
CONSOLE=${CONSOLE_JAR:-$TAIL/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar}
DUMP_SHA=ddb7229f419e52f2912de7e85f14830c9698e556320d58ebba6bad4efe1f53a8
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
if [ -n "${BASELINE_CLASSES:-}" ]; then
    same=0; differ=0
    while read -r rel; do
        if cmp -s "$BASELINE_CLASSES/$rel" "$MODULE/target/classes/$rel"; then same=$((same + 1));
        else differ=$((differ + 1)); echo "DIFFERS $rel"; fi
    done < <(cd "$BASELINE_CLASSES" && find . -name '*.class' | sort)
    echo "UNTOUCHED identical=$same differing=$differ (expected differing: the owner edit M3AlgorithmShapeCatalog.class)"
fi
echo "VERIFY PASS"
