#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# atom_tests.sh <work_dir> <oracle_jars> <junit_console_jar>
#
# Runs the leaf-1 atom test recipe end to end from the repository root (CPU only, deterministic):
#   1. jdk.m3.graphics classes      javac --release 21 -proc:none of src/jdk.m3.graphics (no module-info)
#   2. generate + fixed point       tools/gen_atom_tests.py, then --check (0 files differ)
#   3. test classes                 javac --release 21 -Xlint:all of test/jdk/jdk/m3/graphics
#   4. JUnit                        identity hashes pinned (the jtreg @run lines), unpinned, without the oracle
#   5. mutants                      tools/atom_mutants.py (leaf1/ATOM_MUTANTS.tsv)
#   6. perf                         M3GraphicsAtomsPerf twice, and once without the oracle (PERF_SKIPPED)
#   7. ledgers                      tools/atom_ledgers.py (leaf1/ATOM_PERF.tsv, ATOM_COVERAGE.tsv, ATOM_TEST_GATES.tsv)
# <oracle_jars> is skija-shared-0.143.18.jar and types-0.2.0.jar joined by the JVM path separator; paths are given in
# the form the JVM takes (C:/... on Windows). javac/java come from $JDK_BIN, else $JAVA_HOME/bin, else PATH. Every
# step logs into <work_dir>; a step that runs long prints a heartbeat every HEARTBEAT_SECONDS.
set -euo pipefail

HEARTBEAT_SECONDS=180
PINNED=(-XX:+UnlockExperimentalVMOptions -XX:hashCode=2)
PERF_FLAGS=(-Xmx512m -XX:-TieredCompilation -Xbatch -XX:+UnlockExperimentalVMOptions
            -XX:CompileCommand=blackhole,M3GraphicsAtomsPerfSupport::consume)
TOOLS=m3/tooling/skija-absorb/tools
TESTS=test/jdk/jdk/m3/graphics

if [ "$#" -ne 3 ]; then
    sed -n '5,17p' "$0"
    exit 2
fi
WORK=$1
ORACLE=$2
CONSOLE=$3
BIN=${JDK_BIN:-${JAVA_HOME:+$JAVA_HOME/bin}}
JAVAC=${BIN:+$BIN/}javac
JAVA=${BIN:+$BIN/}java
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
    *) SEP=':' ;;
esac
mkdir -p "$WORK"

heartbeat() {
    while :; do
        sleep "$HEARTBEAT_SECONDS"
        echo "HEARTBEAT $(date +%H:%M:%S) $1 running"
    done
}

# step <name> <log> <command...>: runs the command into <log>, with a heartbeat; returns the command's status.
step() {
    local name=$1 log=$2
    shift 2
    echo "STEP $(date +%H:%M:%S) $name"
    heartbeat "$name" &
    local hb=$!
    local rc=0
    "$@" > "$WORK/$log" 2>&1 || rc=$?
    kill "$hb" 2> /dev/null || true
    wait "$hb" 2> /dev/null || true
    echo "STEP $(date +%H:%M:%S) $name exit $rc"
    return "$rc"
}

junit() {
    "$JAVA" "$@" -jar "$CONSOLE" execute --disable-banner --disable-ansi-colors --details=summary \
        -cp "$WORK/tests$SEP$WORK/m3classes" --include-classname='.*' --scan-class-path="$WORK/tests"
}

find src/jdk.m3.graphics/share/classes -name '*.java' ! -name module-info.java | sort > "$WORK/m3srcs.txt"
rm -rf "$WORK/m3classes" "$WORK/tests"
step module m3javac.log "$JAVAC" --release 21 -proc:none -nowarn -encoding UTF-8 -d "$WORK/m3classes" \
    "@$WORK/m3srcs.txt"
step generate gen.log python "$TOOLS/gen_atom_tests.py" . "$WORK/m3classes" "$ORACLE"
step fixed_point gen_check.log python "$TOOLS/gen_atom_tests.py" . "$WORK/m3classes" "$ORACLE" --check
step tests testjavac.log "$JAVAC" --release 21 -Xlint:all -encoding UTF-8 -cp "$CONSOLE" -d "$WORK/tests" \
    $TESTS/*.java
step junit junit_pinned.log junit "${PINNED[@]}" "-Dm3.graphics.oracle=$ORACLE" || true
step junit_unpinned junit_unpinned.log junit "-Dm3.graphics.oracle=$ORACLE" || true
step junit_no_oracle junit_nooracle.log junit "${PINNED[@]}" || true
step mutants mutants.log python "$TOOLS/atom_mutants.py" . "$WORK" "$WORK/m3classes" "$WORK/tests" "$ORACLE" \
    "$CONSOLE" || true
for run in 1 2; do
    step "perf_run$run" "perf$run.log" "$JAVA" "${PERF_FLAGS[@]}" "-Dm3.graphics.oracle=$ORACLE" \
        -cp "$WORK/tests$SEP$WORK/m3classes" M3GraphicsAtomsPerf || true
done
step perf_skip perf_skip.log "$JAVA" "${PERF_FLAGS[@]}" -cp "$WORK/tests$SEP$WORK/m3classes" M3GraphicsAtomsPerf
rc=0
step ledgers ledgers.log python "$TOOLS/atom_ledgers.py" . "$WORK" "$ORACLE" || rc=$?
cat "$WORK/ledgers.log"
exit "$rc"
