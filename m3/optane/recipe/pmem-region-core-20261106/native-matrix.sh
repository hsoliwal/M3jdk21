#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of the relocation, on a Linux host with a C17 compiler and a JDK 21, for ONE PMem family (donor or relocated):
# javac -h generates the JNI header, cc builds m3pmemsdk.c with -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC
# -shared WITHOUT libpmem2 (M3_HAVE_LIBPMEM2 undefined: the PMDK backend is compiled out, PMDK is not built), nm checks
# the fourteen exported symbols, the independent KMP prefix-32 C oracle is built and run, then M3PmemDump runs in native
# mode (the 19 atoms, the oracle lanes with POSIX regions, <operations> seeded operations over FILE and POSIX regions)
# under -Xcheck:jni in the default, -Xint and -XX:TieredStopAtLevel=1 modes; the three stdouts must be identical.
# Prints the symbol list, the oracle result, the sha256 of every stdout and NATIVE_MATRIX PASS; exits non-zero at the
# first failed contract. The donor and relocated families must print identical native-default.txt (differential.sh
# native compares them).
# usage: native-matrix.sh <package> <type-prefix> <test-package> <c-file> <kmp-oracle-c-file> <scratch-dir> <operations> <java-file>...
#   <java-file>... : the ten owners, PmemAtomCases and M3PmemDump (this module's src/test/java) of the family
# env:   JAVA_HOME (a JDK 21 with include/ and include/linux), CC (default cc)
set -euo pipefail
PACKAGE=$1
PREFIX=$2
TEST_PACKAGE=$3
C_FILE=$4
KMP_ORACLE=$5
SCRATCH=$6
OPERATIONS=$7
shift 7
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CC=${CC:-cc}
SYMBOL_PREFIX="Java_$(echo "$PACKAGE" | tr . _)_${PREFIX}NativePmem_"
DUMP_PACKAGE=$(for f in "$@"; do case "$f" in *M3PmemDump.java) sed -n 's/^package \(.*\);/\1/p' "$f";; esac; done)
mkdir -p "$SCRATCH/headers" "$SCRATCH/classes" "$SCRATCH/lib"
rm -f "$SCRATCH"/*.txt "$SCRATCH/lib"/*.so "$SCRATCH"/kmp-oracle
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -h "$SCRATCH/headers" -d "$SCRATCH/classes" "$@"
"$CC" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$SCRATCH/headers" \
    -o "$SCRATCH/lib/libm3pmemsdk.so" "$C_FILE"
echo "PMDK not built: M3_HAVE_LIBPMEM2 undefined, libpmem2 neither required nor linked"
nm -D --defined-only "$SCRATCH/lib/libm3pmemsdk.so" > "$SCRATCH/exports.txt"
count=0
for symbol in capabilities pageSize open selectedBackend granularity read write persist close copy equalRange \
        copyIfDifferent indexOf boundLongLE; do
    grep -q " ${SYMBOL_PREFIX}${symbol}\$" "$SCRATCH/exports.txt"
    echo "SYMBOL ${SYMBOL_PREFIX}${symbol}"
    count=$((count + 1))
done
echo "SYMBOLS $count"
"$CC" -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -o "$SCRATCH/kmp-oracle" "$KMP_ORACLE"
"$SCRATCH/kmp-oracle" > "$SCRATCH/kmp-oracle.txt"
cat "$SCRATCH/kmp-oracle.txt"
run() { # <stdout name> <jvm flags...>
    local name=$1
    shift
    "$JAVA_BIN/java" -Xcheck:jni "$@" -Djava.library.path="$SCRATCH/lib" -cp "$SCRATCH/classes" "$DUMP_PACKAGE.M3PmemDump" \
        "$PACKAGE" "$PREFIX" "$TEST_PACKAGE" "$OPERATIONS" native > "$SCRATCH/$name.txt"
}
run native-default
run native-interpreter -Xint
run native-c1 -XX:TieredStopAtLevel=1
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-interpreter.txt"
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-c1.txt"
grep -c '^ATOM .* -> PASS$' "$SCRATCH/native-default.txt" | sed 's/^/ATOMS_PASS /'
if grep -q '^ATOM .* -> FAIL$' "$SCRATCH/native-default.txt"; then echo "ATOM FAIL"; grep '^ATOM .* -> FAIL$' "$SCRATCH/native-default.txt"; exit 1; fi
grep -E '^(capabilities|RANDOM operations|TOTAL) ' "$SCRATCH/native-default.txt"
(cd "$SCRATCH" && sha256sum native-default.txt native-interpreter.txt native-c1.txt kmp-oracle.txt)
echo "NATIVE_MATRIX PASS package=$PACKAGE cc=$("$CC" --version | head -n 1) java=$("$JAVA_BIN/java" -version 2>&1 | head -n 1)"
