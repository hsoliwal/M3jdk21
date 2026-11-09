#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of the relocation, on a Linux host with a C17 compiler and a JDK 21: the donor qualifier matrix
# (verify-original-jni-c17.sh of the donor crate m3-doop-pool-checkout-guard-20261031) for ONE lease family, donor or
# relocated. javac -h generates the JNI headers, cc builds m3_scoped_leases.c with -std=c17 -O2 -Wall -Wextra
# -Wpedantic -Werror -fPIC -shared, nm checks the three exported symbols, then: M3ScopedLeaseTableSelfTest and
# M3JniArrayBatchSelfTest without the library (native=false), the same two with the library under -Xcheck:jni in the
# default, -Xint and -XX:TieredStopAtLevel=1 modes (the table self-test stdout must be identical across modes),
# M3NativeConcatParityProbe with the native-required property in the three modes (identical stdout), and the
# window self-test, the preflight oracle (candidate mode) and the code-carrier oracle with the library.
# Prints every PASS line and the sha256 of every stdout; exits non-zero at the first failed contract.
# usage: native-matrix.sh <package> <require-native-property> <c-file> <scratch-dir> <java-file>...
#   <java-file>... : the six owners, the heap advisor (M3RuntimeHeap.java of the same family) and the test mains
#                    (table/window/scopes/batch self-tests, the two oracles, the parity probe); no JUnit classes
# env:   JAVA_HOME (a JDK 21 with include/ and include/linux), CC (default cc); PREFLIGHT_ORACLE and CARRIER_ORACLE
#        name the two oracle classes (defaults: this module's M3NativeLeasePreflightOracle and
#        M3NativeLeaseCodeCarrierOracle; the donor family passes the donor names of the TYPE rows)
set -euo pipefail
PACKAGE=$1
PROPERTY=$2
C_FILE=$3
SCRATCH=$4
shift 4
PREFLIGHT_ORACLE=${PREFLIGHT_ORACLE:-M3NativeLeasePreflightOracle}
CARRIER_ORACLE=${CARRIER_ORACLE:-M3NativeLeaseCodeCarrierOracle}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CC=${CC:-cc}
PREFIX="Java_$(echo "$PACKAGE" | tr . _)_"
mkdir -p "$SCRATCH/headers" "$SCRATCH/classes" "$SCRATCH/no-native"
rm -f "$SCRATCH"/*.txt "$SCRATCH"/*.so
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -h "$SCRATCH/headers" -d "$SCRATCH/classes" "$@"
"$CC" -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$SCRATCH/headers" \
    -o "$SCRATCH/libm3_scoped_leases.so" "$C_FILE"
nm -D --defined-only "$SCRATCH/libm3_scoped_leases.so" > "$SCRATCH/exports.txt"
for symbol in "${PREFIX}M3NativeLeaseScan_collectNative" "${PREFIX}M3NativeLeaseScan_collectNativeWindow" \
        "${PREFIX}M3JniByteConcat_concatNative"; do
    grep -q " $symbol\$" "$SCRATCH/exports.txt"
    echo "SYMBOL $symbol"
done
run() { # <stdout name> <library dir> <jvm flags...>
    local name=$1 lib=$2 main=$3
    shift 3
    "$JAVA_BIN/java" -Xcheck:jni "$@" -Djava.library.path="$lib" -cp "$SCRATCH/classes" "$PACKAGE.$main" > "$SCRATCH/$name.txt"
}
run fallback "$SCRATCH/no-native" M3ScopedLeaseTableSelfTest
grep -Fx 'M3ScopedLeaseTableSelfTest PASS native=false' "$SCRATCH/fallback.txt"
run batch-fallback "$SCRATCH/no-native" M3JniArrayBatchSelfTest
grep -Eq '^M3_JNI_ARRAY_BATCH_PASS checks=[1-9][0-9]* nativeAvailable=false$' "$SCRATCH/batch-fallback.txt"
run window-fallback "$SCRATCH/no-native" M3ScopedLeaseWindowSelfTest
grep -Fx 'M3ScopedLeaseWindowSelfTest PASS native=false' "$SCRATCH/window-fallback.txt"
run native-default "$SCRATCH" M3ScopedLeaseTableSelfTest
run native-interpreter "$SCRATCH" M3ScopedLeaseTableSelfTest -Xint
run native-c1 "$SCRATCH" M3ScopedLeaseTableSelfTest -XX:TieredStopAtLevel=1
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-interpreter.txt"
cmp "$SCRATCH/native-default.txt" "$SCRATCH/native-c1.txt"
grep -Fx 'M3ScopedLeaseTableSelfTest PASS native=true' "$SCRATCH/native-default.txt"
for mode in default interpreter c1; do
    case "$mode" in default) set -- ;; interpreter) set -- -Xint ;; c1) set -- -XX:TieredStopAtLevel=1 ;; esac
    run "batch-$mode" "$SCRATCH" M3JniArrayBatchSelfTest "$@"
    grep -Eq '^M3_JNI_ARRAY_BATCH_PASS checks=[1-9][0-9]* nativeAvailable=true$' "$SCRATCH/batch-$mode.txt"
    run "concat-$mode" "$SCRATCH" M3NativeConcatParityProbe "$@" "-D$PROPERTY=true"
    grep -Fx 'M3_NATIVE_CONCAT_PARITY_PASS cases=1025 native=true' "$SCRATCH/concat-$mode.txt"
done
cmp "$SCRATCH/concat-default.txt" "$SCRATCH/concat-interpreter.txt"
cmp "$SCRATCH/concat-default.txt" "$SCRATCH/concat-c1.txt"
run window-native "$SCRATCH" M3ScopedLeaseWindowSelfTest
grep -Fx 'M3ScopedLeaseWindowSelfTest PASS native=true' "$SCRATCH/window-native.txt"
run scopes "$SCRATCH" M3LeaseScopesSelfTest
grep -Fx 'M3LeaseScopesSelfTest PASS' "$SCRATCH/scopes.txt"
"$JAVA_BIN/java" -Xcheck:jni -Djava.library.path="$SCRATCH" -cp "$SCRATCH/classes" \
    "$PACKAGE.$PREFLIGHT_ORACLE" candidate > "$SCRATCH/preflight.txt"
grep -Eq '^M3_LEASE_JNI_PREFLIGHT_PASS mode=candidate checks=[1-9][0-9]* sha256=[0-9a-f]{64}$' "$SCRATCH/preflight.txt"
"$JAVA_BIN/java" -Xcheck:jni -Djava.library.path="$SCRATCH" -cp "$SCRATCH/classes" \
    "$PACKAGE.$CARRIER_ORACLE" > "$SCRATCH/carrier.txt"
grep -Eq '^M3_LEASE_CODE_CARRIER_PASS permutations=6 sha256=[0-9a-f]{64}$' "$SCRATCH/carrier.txt"
cat "$SCRATCH/fallback.txt" "$SCRATCH/batch-fallback.txt" "$SCRATCH/native-default.txt" "$SCRATCH/batch-default.txt" \
    "$SCRATCH/concat-default.txt" "$SCRATCH/window-native.txt" "$SCRATCH/preflight.txt" "$SCRATCH/carrier.txt"
(cd "$SCRATCH" && sha256sum fallback.txt batch-fallback.txt window-fallback.txt native-default.txt native-interpreter.txt \
    native-c1.txt batch-default.txt batch-interpreter.txt batch-c1.txt concat-default.txt concat-interpreter.txt \
    concat-c1.txt window-native.txt scopes.txt preflight.txt carrier.txt)
echo "NATIVE_MATRIX PASS package=$PACKAGE cc=$("$CC" --version | head -n 1) java=$("$JAVA_BIN/java" -version 2>&1 | head -n 1)"
