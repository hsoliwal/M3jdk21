#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of the relocation leaf pmem-gram-index, on a Linux host with a C17 compiler and a JDK 21, for ONE family
# (donor or relocated): javac -h generates the JNI headers, cc builds the family's m3pmemsdk.c and the independent C17
# brute-force oracle src/test/native/M3GramOracle.c with -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared
# (PMDK compiled out, never built), then M3PmemGramIndexDump runs in native mode (FILE and POSIX images, every exact
# result also checked against the C17 oracle, <operations> seeded queries per backend) under -Xcheck:jni in the default,
# -Xint and -XX:TieredStopAtLevel=1 modes; the three stdouts must be identical and hold no UNEXPECTED line. The donor
# and relocated families must print identical native-default.txt (compared by the caller).
# usage: native-matrix.sh <sdk package> <campaign package> <type-prefix> <c-file> <oracle-c-file> <scratch-dir> <operations> <java-file>...
#   <java-file>... : the family's owners (NativePmem included), M3NativeByteOracle.java and M3PmemGramIndexDump.java
# env:   JAVA_HOME (a JDK 21 with include/ and include/linux), CC (default cc)
set -euo pipefail
PACKAGE=$1
CAMPAIGN=$2
PREFIX=$3
C_FILE=$4
ORACLE_C=$5
SCRATCH=$6
OPERATIONS=$7
shift 7
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CC=${CC:-cc}
mkdir -p "$SCRATCH/headers" "$SCRATCH/classes" "$SCRATCH/lib"
rm -rf "$SCRATCH"/*.txt "$SCRATCH/lib"/*.so "$SCRATCH/classes"/* "$SCRATCH/headers"/*
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -h "$SCRATCH/headers" -d "$SCRATCH/classes" "$@"
for pair in "m3pmemsdk:$C_FILE" "m3gramoracle:$ORACLE_C"; do
    "$CC" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
        -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$SCRATCH/headers" \
        -o "$SCRATCH/lib/lib${pair%%:*}.so" "${pair#*:}"
done
echo "PMDK not built: M3_HAVE_LIBPMEM2 undefined, libpmem2 neither required nor linked"
echo "SYMBOLS $(nm -D --defined-only "$SCRATCH/lib/libm3pmemsdk.so" | grep -c " Java_$(echo "$PACKAGE" | tr . _)_${PREFIX}NativePmem_") ORACLE $(nm -D --defined-only "$SCRATCH/lib/libm3gramoracle.so" | grep -c ' Java_jdk_m3_optane_M3NativeByteOracle_firstHit$')"
run() { # <stdout name> <jvm flags...>
    local name=$1
    shift
    "$JAVA_BIN/java" -Xcheck:jni -XX:-OmitStackTraceInFastThrow "$@" -Djava.library.path="$SCRATCH/lib" \
        -cp "$SCRATCH/classes" jdk.internal.m3.optane.M3PmemGramIndexDump "$PACKAGE" "$CAMPAIGN" "$PREFIX" "$OPERATIONS" native \
        > "$SCRATCH/$name.txt"
}
run native-default
run native-interpreter -Xint
run native-c1 -XX:TieredStopAtLevel=1
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-interpreter.txt"
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-c1.txt"
if grep -a -q UNEXPECTED "$SCRATCH/native-default.txt"; then
    echo "ORACLE FAIL"; grep -a UNEXPECTED "$SCRATCH/native-default.txt" | head; exit 1
fi
echo "POSIX query lines $(grep -a -c '^QUERY POSIX ' "$SCRATCH/native-default.txt") EXACT+C17 lanes $(grep -a -c ' EQUAL ORACLE' "$SCRATCH/native-default.txt")"
grep -a -E '^TOTAL ' "$SCRATCH/native-default.txt"
(cd "$SCRATCH" && sha256sum native-default.txt native-interpreter.txt native-c1.txt)
echo "NATIVE_MATRIX PASS package=$PACKAGE cc=$("$CC" --version | head -n 1) java=$("$JAVA_BIN/java" -version 2>&1 | head -n 1)"
