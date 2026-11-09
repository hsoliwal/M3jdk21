#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# All gates of recipe durable-extent-store-20261107 in one run, from the repository root of a checkout that has
# m3/file and m3/tooling/jdk-naming (a sparse worktree is enough):
#   recipe   relocate.py --check (changed=0) and apply.py --check (changed=0)
#   naming   jdk_naming.py plan --only m3/file/ (moves=0 rewrites=0 refusals=0)
#   package  mvn package of m3/file with its own pom (-Xlint:all,-module -Werror, surefire); the run passes when the
#            only failing tests are the pre-existing baseline set named in BASELINE_FAILURES (Windows: the CRLF
#            receipt of M3FileDeltaTest) and the extent-store classes ran green
#   diff     differential.sh (dumps JIT and -Xint, crash gate, proof, javap -p, mutants)
# usage: verify.sh <donor-git-dir> <donor-rev> <scratch-dir> <junit-platform-console-standalone jar> [maven repo [tail repo]]
# JAVA_HOME = JDK 21. Every JVM gets -Djava.io.tmpdir=<scratch>/tmp (Maven, surefire forks, harness children).
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)
DONOR_GIT=$1; REV=$2; SCRATCH=$3; CONSOLE=$4
mkdir -p "$SCRATCH/tmp"
TMPDIR_ARG="-Djava.io.tmpdir=$(cd "$SCRATCH/tmp" && (pwd -W 2>/dev/null || pwd))"
REPO_ARGS=()
[ $# -ge 5 ] && REPO_ARGS+=("-Dmaven.repo.local=$5")
[ $# -ge 6 ] && REPO_ARGS+=("-Dmaven.repo.local.tail=$6")
BASELINE_FAILURES="M3FileDeltaTest.fullSourceClassAndCliContract"
status=PASS
gate() { echo "GATE $1 $2"; [ "$2" == PASS ] || status=FAIL; }
cd "$ROOT"
python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --check > "$SCRATCH/relocate.log" 2>&1
r1=$?; tail -1 "$SCRATCH/relocate.log"
python -I "$RECIPE_DIR/apply.py" --check > "$SCRATCH/apply.log" 2>&1
r2=$?; tail -1 "$SCRATCH/apply.log"
gate recipe "$([ $r1 -eq 0 ] && [ $r2 -eq 0 ] && echo PASS || echo FAIL)"
python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/file/ > "$SCRATCH/naming.log" 2>&1
tail -1 "$SCRATCH/naming.log"
gate naming "$(grep -q 'moves=0 rewrites=0 refusals=0' "$SCRATCH/naming.log" && echo PASS || echo FAIL)"
MAVEN_OPTS="$TMPDIR_ARG" mvn -B -ntp "${REPO_ARGS[@]}" "-DargLine=$TMPDIR_ARG" -Dmaven.test.failure.ignore=true \
    -Dexec.skip=true -f m3/file/pom.xml clean package > "$SCRATCH/mvn.log" 2>&1
grep -E 'Tests run:.*Fail' "$SCRATCH/mvn.log" | tail -1
failing=$(grep -hE '^\[ERROR\]   [A-Za-z0-9]+\.[A-Za-z0-9]+' "$SCRATCH/mvn.log" | sed -E 's/^\[ERROR\]   ([A-Za-z0-9]+\.[A-Za-z0-9]+).*/\1/' | sort -u | tr '\n' ' ' | sed 's/ $//')
extent_ok=$(grep -cE 'Tests run: [0-9]+, Failures: 0, Errors: 0, Skipped: 0.* in jdk\.m3\.file\.M3(ContentExtentTest|DurableExtentStoreTest|DurableExtentStoreOracleTest)$' "$SCRATCH/mvn.log")
warnings=$(grep -cE '^\[WARNING\]' "$SCRATCH/mvn.log")
echo "failing=[${failing}] baseline=[${BASELINE_FAILURES}] extent_classes_green=${extent_ok}/3 warning_lines=${warnings}"
if grep -q 'BUILD SUCCESS' "$SCRATCH/mvn.log" && [ "$extent_ok" -eq 3 ] && { [ -z "$failing" ] || [ "$failing" == "$BASELINE_FAILURES" ]; }; then
    gate package PASS
else
    gate package FAIL
fi
bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" "$CONSOLE" > "$SCRATCH/differential.log" 2>&1
r3=$?; grep -E 'IDENTICAL|DIFFERENT|CRASH|mutant|javap|proof|donor JUnit|DIFFERENTIAL' "$SCRATCH/differential.log"
gate differential "$([ $r3 -eq 0 ] && echo PASS || echo FAIL)"
echo "VERIFY $status"
[ "$status" == PASS ]
