#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Allocation gate of recipe qualification/residency-pressure-alloc-20261107: runs #2653's PressureSamplingAllocationProbe
# (qualification/residency-fold-20261107/oracle, unchanged; 3 samples x CALLS calls per boundary, fixed budget then
# PRESSURE_AWARE in one JVM) on the BEFORE and AFTER classes: RUNS JIT runs, RUNS -Xbatch runs and one -Xint run per side.
# Prints one row per side/mode/boundary: the perCall figure of every sample of every run, then the maximum.
# usage: alloc.sh <before classes> <after classes> <out dir> [runs=8] [calls=100000]
# env: JAVA_HOME = JDK 21; M3_ARRAYS_JAR = the m3-arrays jar the module was built with; JTMP = java.io.tmpdir.
set -u
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
PROBE="$HERE/../../residency-fold-20261107/oracle/jdk/m3/collections/PressureSamplingAllocationProbe.java"
B=$1; A=$2; OUT=$3; RUNS=${4:-8}; CALLS=${5:-100000}
J="${JAVA_HOME:?JAVA_HOME}/bin"
T="-Djava.io.tmpdir=${JTMP:-${TMPDIR:-/tmp}}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
rm -rf "$OUT"; mkdir -p "$OUT/oracle" "$OUT/runs"
"$J/javac" "-J$T" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$B$SEP${M3_ARRAYS_JAR:-}" -d "$OUT/oracle" \
    "$PROBE" || exit 2
probe() { # side mode flags...
  local side=$1 mode=$2 C=$B; shift 2
  [ "$side" = after ] && C=$A
  "$J/java" "$T" "$@" -Xms128m -Xmx128m -cp "$OUT/oracle$SEP$C$SEP${M3_ARRAYS_JAR:-}" \
      jdk.m3.collections.PressureSamplingAllocationProbe "$CALLS" > "$OUT/runs/$side-$mode.txt" || echo "RUN_FAIL $side $mode"
}
for side in before after; do
  for run in $(seq 1 "$RUNS"); do probe $side jit$run; done
  for run in $(seq 1 "$RUNS"); do probe $side xbatch$run -Xbatch; done
  probe $side xint -Xint
done
for side in before after; do
  for kind in jit xbatch xint; do
    cat "$OUT"/runs/$side-$kind*.txt | tr -d '\r' | awk -v side=$side -v kind=$kind '
      { key = $2 " " $3; v = $7; sub("perCall=", "", v); list[key] = list[key] (list[key] == "" ? "" : ",") v;
        if (!(key in max) || v + 0 > max[key]) max[key] = v + 0; if (!(key in order)) order[key] = ++n; keys[n] = key }
      END { for (i = 1; i <= n; i++) { k = keys[i]; printf "%s\t%s\t%s\tmax=%d\tperCall=%s\n", side, kind, k, max[k], list[k] } }'
  done
done | tee "$OUT/alloc-table.tsv"
