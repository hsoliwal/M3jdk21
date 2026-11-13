#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Driver of the residency fold leaf PS oracles: compiles PressureSamplingDifferential and PressureSamplingAllocationProbe
# once (against BEFORE), runs the behaviour trace on BEFORE and AFTER (normal and -Xint) and the allocation probe on
# both (-Xint and normal), then classifies every differing allocation line.
# usage: psdiff.sh <before classes> <after classes> <out dir> <episodes> <ops> [calls=100000]
# env: JAVA_HOME = JDK 21; M3_ARRAYS_JAR = the m3-arrays jar the module was built with; JTMP = java.io.tmpdir.
set -u
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
B=$1; A=$2; OUT=$3; EP=$4; OPS=$5; CALLS=${6:-100000}
J="${JAVA_HOME:?JAVA_HOME}/bin"
T="-Djava.io.tmpdir=${JTMP:-${TMPDIR:-/tmp}}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
rm -rf "$OUT"; mkdir -p "$OUT/oracle"
"$J/javac" "-J$T" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$B$SEP${M3_ARRAYS_JAR:-}" -d "$OUT/oracle" \
    "$HERE/jdk/m3/collections/PressureSamplingDifferential.java" \
    "$HERE/jdk/m3/collections/PressureSamplingAllocationProbe.java" || exit 2
for side in before after; do
  if [ $side = before ]; then C=$B; else C=$A; fi
  CP="$OUT/oracle$SEP$C$SEP${M3_ARRAYS_JAR:-}"
  for mode in normal xint; do
    X=""; [ $mode = xint ] && X="-Xint"
    "$J/java" "$T" $X -cp "$CP" jdk.m3.collections.PressureSamplingDifferential "$OUT/ps-$side-$mode.txt" "$EP" "$OPS" \
        || echo "RUN_FAIL trace $side $mode"
    echo "trace $side $mode sha256=$(sha256sum < "$OUT/ps-$side-$mode.txt" | cut -c1-12) lines=$(wc -l < "$OUT/ps-$side-$mode.txt")"
    "$J/java" "$T" $X -Xms128m -Xmx128m -cp "$CP" jdk.m3.collections.PressureSamplingAllocationProbe "$CALLS" \
        > "$OUT/alloc-$side-$mode.txt" || echo "RUN_FAIL alloc $side $mode"
  done
done
for mode in normal xint; do
  if cmp -s "$OUT/ps-before-$mode.txt" "$OUT/ps-after-$mode.txt"; then echo "trace $mode IDENTICAL"; else echo "trace $mode DIFFERENT"; fi
done
cmp -s "$OUT/ps-after-normal.txt" "$OUT/ps-after-xint.txt" && echo "trace after normal==xint IDENTICAL"
# Allocation classes: a fixed_budget line may drop to perCall=0 (FIXED_BUDGET_SNAPSHOT_REMOVED); pressure_aware lines and
# resident_use lines must keep their -Xint figure; anything else is UNCLASSIFIED.
paste -d'|' "$OUT/alloc-before-xint.txt" "$OUT/alloc-after-xint.txt" | awk -F'|' '
  { gsub(/\r/, ""); split($1, b, " "); split($2, a, " "); key = b[2] " " b[3] " " b[4];
    bp = b[7]; ap = a[7]; sub("perCall=", "", bp); sub("perCall=", "", ap);
    if (bp == ap) cls = "SAME"; else if (b[2] == "fixed_budget" && ap == 0 && bp > 0) cls = "FIXED_BUDGET_SNAPSHOT_REMOVED";
    else cls = "UNCLASSIFIED";
    print key "\tbefore=" bp "\tafter=" ap "\t" cls; n[cls]++ }
  END { for (c in n) print "CLASS " c " " n[c] }' > "$OUT/alloc-classes-xint.tsv"
cat "$OUT/alloc-classes-xint.tsv"
