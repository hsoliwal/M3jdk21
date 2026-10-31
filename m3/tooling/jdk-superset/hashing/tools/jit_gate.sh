#!/bin/sh
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# jit_gate.sh <repo root> <out dir> <variant> <java.base patch classes> <perf classes> <oracle classes>
# The JIT stage of the hashing leaf on a stock JDK 21 (M3_GATE_JDK) with java.base patched. For every path
# (M3HashAtomsPerf row group) one JVM runs the perf harness twice:
#   jit    -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining (text log) and
#          -XX:+LogCompilation (xml), both written under <out dir>/<variant>/, then jit_inlining.py scan;
#   clean  no diagnostic flags (the PERF rows of record).
# The paths: xxh3-64, xxh3-128 (one row per XXH3 input-length class), bytes (every algorithm over byte[]), string,
# bytebuffer, access+kernel, and all (every group in one JVM: the profiles of the shared kernels see every input
# kind). Rows go to <out dir>/PERF_ROWS.tsv, the JIT summary to <out dir>/JIT_SUMMARY.tsv and JIT_FAILS.tsv; the
# logs are gzipped after the scan. Windows paths (C:/...) are expected, since the class paths use ';'.
set -eu
repo=$1
out=$2
variant=$3
patch=$4
perfc=$5
oracle=$6
jbin=${M3_GATE_JDK:-"C:/Program Files/Java/jdk-21"}/bin
lane=$repo/m3/tooling/jdk-superset/hashing
logs=$out/$variant
mkdir -p "$logs"
run="--patch-module java.base=$patch --add-opens java.base/jdk.internal.m3.hash=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED"
diag="-XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining -XX:+LogCompilation"
host_load() {
    powershell -NoProfile -Command "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average" 2>/dev/null | tr -d '\r' || echo NA
}
for path in xxh3-64 xxh3-128 bytes string bytebuffer access,kernel all; do
    name=$(echo "$path" | tr ',' '+')
    only=""
    [ "$path" = all ] || only="-Dm3.hash.perf.only=$path"
    echo "[$(date +%H:%M:%S)] $variant $name: jit run (host load $(host_load)%)"
    # shellcheck disable=SC2086
    "$jbin/java" -Xmx512m $run $diag "-XX:LogFile=$logs/$name.xml" "-Dm3.hash.oracle=$oracle" $only \
        "-Dm3.hash.perf.run=$variant-jit-$name" "-Dm3.hash.perf.out=$out/PERF_ROWS.tsv" -cp "$perfc" M3HashAtomsPerf \
        > "$logs/$name.log" 2>&1
    python -I "$lane/tools/jit_inlining.py" scan "$variant" "$name" "$logs/$name.xml" "$logs/$name.log" \
        "$out/JIT_SUMMARY.tsv" "$out/JIT_FAILS.tsv"
    gzip -f "$logs/$name.xml" "$logs/$name.log"
    echo "[$(date +%H:%M:%S)] $variant $name: clean run (host load $(host_load)%)"
    # shellcheck disable=SC2086
    "$jbin/java" -Xmx512m $run "-Dm3.hash.oracle=$oracle" $only "-Dm3.hash.perf.run=$variant-clean-$name" \
        "-Dm3.hash.perf.out=$out/PERF_ROWS.tsv" -cp "$perfc" M3HashAtomsPerf > "$logs/$name.clean.log" 2>&1
    echo "[$(date +%H:%M:%S)] $variant $name: done (host load $(host_load)%)"
done
