#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of recipe pmem-accelerator-20261107, on a Linux host with a C17 compiler and a JDK 21, for ONE family (donor or
# relocated): javac -h generates the JNI header of the accelerator owner, cc builds the family's C file with the CMake
# flags of the m3pmemaccel target (-std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror -ffp-contract=off
# -fno-fast-math), the eight JNI exports are counted, then the family's self-test main runs with the library on
# java.library.path under -Xcheck:jni -XX:-UsePerfData -Xmx64m (the donor's "run with -Xmx64m" advice: a 2 GiB read must
# refuse before it allocates) in the default, -Xint and -XX:TieredStopAtLevel=1 modes; the three stdouts must be
# identical and start with the PASS figure. The donor and relocated families must print identical default.txt (compared
# by the caller). The classes stay in <scratch>/classes for javap_parity.sh.
# usage: native-gate.sh <package> <self-test simple name> <library name> <c-file> <scratch-dir> <java-file>...
#        (library name = the System.loadLibrary name: m3pmem for the donor family, m3pmemaccel for the relocated one)
# env:   JAVA_HOME (a JDK 21 with include/ and include/linux), CC (default cc), TMPDIR_JAVA (java.io.tmpdir of the runs)
set -euo pipefail
PACKAGE=$1
SELFTEST=$2
LIBRARY=$3
C_FILE=$4
SCRATCH=$5
shift 5
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CC=${CC:-cc}
TMP=${TMPDIR_JAVA:-$SCRATCH/tmp}
mkdir -p "$SCRATCH/headers" "$SCRATCH/classes" "$SCRATCH/lib" "$TMP"
rm -rf "$SCRATCH"/*.txt "$SCRATCH/lib"/*.so "$SCRATCH/classes"/* "$SCRATCH/headers"/*
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -h "$SCRATCH/headers" -d "$SCRATCH/classes" "$@"
"$CC" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror -ffp-contract=off -fno-fast-math \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$SCRATCH/headers" \
    -o "$SCRATCH/lib/lib$LIBRARY.so" "$C_FILE"
header=$(ls "$SCRATCH/headers"/*.h | head -n 1)
symbols=$(nm -D --defined-only "$SCRATCH/lib/lib$LIBRARY.so" | awk '{print $3}' | grep -c "^Java_$(echo "$PACKAGE" | tr . _)_" || true)
declared=$(grep -c "^JNIEXPORT" "$header" || true)
echo "SYMBOLS exported=$symbols declared=$declared header=$(basename "$header")"
[ "$symbols" -eq 8 ] && [ "$declared" -eq 8 ] || { echo "NATIVE_GATE FAIL: 8 JNI symbols expected"; exit 1; }
run() { # <stdout name> <jvm flags...>
    local name=$1
    shift
    "$JAVA_BIN/java" -Xcheck:jni -XX:-UsePerfData -XX:-OmitStackTraceInFastThrow -Xmx64m "$@" \
        -Djava.io.tmpdir="$TMP" -Djava.library.path="$SCRATCH/lib" -cp "$SCRATCH/classes" "$PACKAGE.$SELFTEST" > "$SCRATCH/$name.txt"
}
run default
run interpreter -Xint
run c1 -XX:TieredStopAtLevel=1
cmp "$SCRATCH/default.txt" "$SCRATCH/interpreter.txt"
cmp "$SCRATCH/default.txt" "$SCRATCH/c1.txt"
grep -q '^PASS m3-pmem Java21/JNI cases=' "$SCRATCH/default.txt" || { echo "NATIVE_GATE FAIL: no PASS figure"; cat "$SCRATCH/default.txt"; exit 1; }
cat "$SCRATCH/default.txt"
(cd "$SCRATCH" && sha256sum default.txt interpreter.txt c1.txt)
echo "NATIVE_GATE PASS package=$PACKAGE cc=$("$CC" --version | head -n 1) java=$("$JAVA_BIN/java" -version 2>&1 | head -n 1) cpu=$(grep -o -E 'avx2|fma|avx512[a-z_]*' /proc/cpuinfo | sort -u | tr '\n' ' ')"
