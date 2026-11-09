#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Runs every gate of recipe hash-xxhash64-streaming-20261106 in order and prints one GATE line per gate (see GATES.tsv):
#   recipe        relocate.py --check is a fixed point (files=4 owners=1 held=6 changed=0 provenance=UNCHANGED)
#   differential  differential.sh: patch compile (java.base flags), donor vs relocated contract, proof, dump, mutant, probe
#   lane_tests    M3HashAtomsTest + M3HashJitAtomsTest on the patched java.base, once with the prebuilt vendored oracle
#                 (-Dm3.hash.oracle) and once with -Dtest.src (the jtreg path, oracle compiled at run time): 35/35 each
#   naming        jdk_naming.py plan over the leaf's three paths: 0 moves, 0 rewrites, 0 refusals (new files must be
#                 staged: the tool reads tracked paths)
#   scope         git diff --name-only <base> plus the staged and untracked paths stay inside the leaf's three paths
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> <maven-repo> [random-cases] [base-rev]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/qualification/<leaf>/recipe
DONOR_GIT=$1; REV=$2; M2=$4; CASES=${5:-10000}; BASE=${6:-origin/master}
mkdir -p "$3"; S=$(winpath "$3")
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CS="$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
API="$M2/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar"
LEAF=m3/qualification/$(basename "$(dirname "$RECIPE_DIR")")/
SCOPE=(src/java.base/share/classes/jdk/internal/m3/hash/ test/jdk/jdk/internal/m3/hash/ "$LEAF")
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$ROOT" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$S/differential" "$M2" "$CASES" > "$S/differential.log" 2>&1; then
    gate differential PASS "$(grep -E '^STEP' "$S/differential.log" | cut -c6- | tr '\n' ';')"
else
    gate differential FAIL "log=$S/differential.log $(grep -E '^STEP .* FAIL' "$S/differential.log" | tr '\n' ';')"
fi

# lane tests on the patch image of the differential (the restore qualification's invocations, gates_wt.sh)
P="$S/differential/patch"; L="$S/lane"; V="$ROOT/m3/vendor/third_party"; T="$ROOT/test/jdk/jdk/internal/m3/hash"
rm -rf "$L"; mkdir -p "$L/oracle" "$L/testc"
"$J/javac" -proc:none -nowarn -XDsuppressNotes --add-exports java.base/sun.nio.ch=ALL-UNNAMED \
    -sourcepath "$(winpath "$V/annotations/src/main/java")$SEP$(winpath "$V/jsr305/src/main/java")" -d "$L/oracle" \
    $(find "$V/zero-allocation-hashing/src/main/java" -name '*.java') > "$L/oracle.log" 2>&1; orc=$?
"$J/javac" -g -Xlint:all -Xlint:-serial --patch-module "java.base=$P" --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -cp "$API" \
    -d "$L/testc" "$T/M3HashOracle.java" "$T/M3HashAtomsTest.java" "$T/M3HashJitAtomsTest.java" > "$L/testc.log" 2>&1; trc=$?
RUN=(--patch-module "java.base=$P" --add-opens java.base/jdk.internal.m3.hash=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED)
"$J/java" -Xmx1g "${RUN[@]}" "-Dm3.hash.oracle=$L/oracle" -jar "$CS" execute -cp "$L/testc" --select-class M3HashAtomsTest \
    --select-class M3HashJitAtomsTest --details=summary --disable-banner > "$L/junit_prebuilt.txt" 2>&1
"$J/java" -Xmx1g "${RUN[@]}" "-Dtest.src=$(winpath "$T")" -jar "$CS" execute -cp "$L/testc" --select-class M3HashAtomsTest \
    --select-class M3HashJitAtomsTest --details=summary --disable-banner > "$L/junit_testsrc.txt" 2>&1
a=$(grep -E 'tests (successful|failed)' "$L/junit_prebuilt.txt" | tr -s ' ' | tr '\n' ' ')
b=$(grep -E 'tests (successful|failed)' "$L/junit_testsrc.txt" | tr -s ' ' | tr '\n' ' ')
case "$a|$b" in *"35 tests successful"*"0 tests failed"*"|"*"35 tests successful"*"0 tests failed"*) v=PASS ;; *) v=FAIL ;; esac
gate lane_tests $v "oracle_javac rc=$orc classes=$(find "$L/oracle" -name '*.class' | wc -l) test_javac rc=$trc prebuilt: $a test.src: $b"

naming=$(cd "$ROOT" && python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . $(printf -- '--only %s ' "${SCOPE[@]}") 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac

changed=$(cd "$ROOT" && { git diff --name-only "$BASE"; git diff --name-only --cached; git ls-files --others --exclude-standard; } | sort -u)
outside=$(printf '%s\n' "$changed" | grep -v -e "^${SCOPE[0]}" -e "^${SCOPE[1]}" -e "^${SCOPE[2]}" | grep -c .)
v=FAIL; [ "${outside:-0}" = 0 ] && v=PASS
gate scope $v "paths=$(printf '%s\n' "$changed" | grep -c .) outside=${outside:-0} base=$BASE"
echo "VERIFY $status"
[ "$status" = PASS ]
