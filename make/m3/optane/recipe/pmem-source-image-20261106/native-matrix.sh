#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of the relocation leaf pmem-source-image, on a Linux host with a C17 compiler and a JDK 21, for ONE family
# (donor or relocated): javac -h generates the JNI header of the family's NativePmem, cc builds its m3pmemsdk.c with
# -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared WITHOUT libpmem2 (PMDK compiled out, never built), then
# M3PmemSourceImageDump runs in native mode (path index, image/refusal/slice lanes over FILE and POSIX regions, the
# corpus-atom lane, <operations> seeded operations over FILE and POSIX images) under -Xcheck:jni in the default, -Xint
# and -XX:TieredStopAtLevel=1 modes; the three stdouts must be identical. The donor and relocated families must print
# identical native-default.txt (compared by the caller: cmp of the two scratch directories).
# usage: native-matrix.sh <package> <type-prefix> <c-file> <scratch-dir> <operations> <java-file>...
#   <java-file>... : the family's owners (NativePmem included) and M3PmemSourceImageDump (this module's src/test/java)
# env:   JAVA_HOME (a JDK 21 with include/ and include/linux), CC (default cc)
set -euo pipefail
PACKAGE=$1
PREFIX=$2
C_FILE=$3
SCRATCH=$4
OPERATIONS=$5
shift 5
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CC=${CC:-cc}
mkdir -p "$SCRATCH/headers" "$SCRATCH/classes" "$SCRATCH/lib"
rm -rf "$SCRATCH"/*.txt "$SCRATCH/lib"/*.so "$SCRATCH/classes"/* "$SCRATCH/headers"/*
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -h "$SCRATCH/headers" -d "$SCRATCH/classes" "$@"
"$CC" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$SCRATCH/headers" \
    -o "$SCRATCH/lib/libm3pmemsdk.so" "$C_FILE"
echo "PMDK not built: M3_HAVE_LIBPMEM2 undefined, libpmem2 neither required nor linked"
echo "SYMBOLS $(nm -D --defined-only "$SCRATCH/lib/libm3pmemsdk.so" | grep -c " Java_$(echo "$PACKAGE" | tr . _)_${PREFIX}NativePmem_")"
run() { # <stdout name> <jvm flags...>
    local name=$1
    shift
    "$JAVA_BIN/java" -Xcheck:jni -XX:-OmitStackTraceInFastThrow "$@" -Djava.library.path="$SCRATCH/lib" \
        -cp "$SCRATCH/classes" jdk.m3.optane.M3PmemSourceImageDump "$PACKAGE" "$PREFIX" "$OPERATIONS" native \
        > "$SCRATCH/$name.txt"
}
run native-default
run native-interpreter -Xint
run native-c1 -XX:TieredStopAtLevel=1
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-interpreter.txt"
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-c1.txt"
if grep -a -q -E 'MISMATCH|NAIVE=|DIFFERENT-ROOT|mismatches=[1-9]' "$SCRATCH/native-default.txt"; then
    echo "ORACLE FAIL"; grep -a -E 'MISMATCH|NAIVE=|DIFFERENT-ROOT|mismatches=[1-9]' "$SCRATCH/native-default.txt" | head; exit 1
fi
echo "POSIX lines $(grep -a -c ' POSIX ' "$SCRATCH/native-default.txt") ATOM lines $(grep -a -c '^ATOM ' "$SCRATCH/native-default.txt")"
grep -a -E '^(RANDOM operations|TOTAL) ' "$SCRATCH/native-default.txt"
(cd "$SCRATCH" && sha256sum native-default.txt native-interpreter.txt native-c1.txt)
echo "NATIVE_MATRIX PASS package=$PACKAGE cc=$("$CC" --version | head -n 1) java=$("$JAVA_BIN/java" -version 2>&1 | head -n 1)"
