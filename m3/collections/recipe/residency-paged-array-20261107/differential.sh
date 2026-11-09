#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Donor-vs-relocated differential of recipe residency-paged-array-20261107: compiles the donor sources of the FILE rows
# at the pinned donor revision in their own package, runs every self-test main (all argument modes that need no JNI
# fixture) and the reference-model oracle on the donor build and on the relocated module classes, normal and -Xint, and
# compares the outputs byte for byte. M3MemoryPressureReportTest prints live heap figures (digits masked); the resources
# observation prints thread-allocated bytes and elapsed nanos (masked; pages, spills and distinctBackings stay exact).
# Every donor path and the donor package are read from RELOCATION_MAP.tsv, so this script carries no donor token.
# usage: differential.sh <donor git dir> <donor rev> <module classes> <module test-classes> <out dir> [episodes ops]
# env: JAVA_HOME = JDK 21; JTMP = java.io.tmpdir for every JVM.
set -u
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
DONOR=$1; REV=$2; MC=$3; MT=$4; OUT=$5; EP=${6:-60}; OPS=${7:-400}
J="${JAVA_HOME:?JAVA_HOME}/bin"
T="-Djava.io.tmpdir=${JTMP:-${TMPDIR:-/tmp}}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
MAP="$HERE/RELOCATION_MAP.tsv"
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
SRC="$OUT/donor-src/${donor_pkg//./\/}"
rm -rf "$OUT"; mkdir -p "$SRC" "$OUT/donor-classes" "$OUT/oracle-relocated" "$OUT/runs"
awk -F'\t' '$1=="FILE" {print $2}' "$MAP" | while read -r path; do
  git -C "$DONOR" show "$REV:$path" > "$SRC/$(basename "$path")" || exit 2
done
sed "s/^package jdk\.m3\.collections;/package $donor_pkg;/" "$HERE/oracle/M3PagedArrayReferenceDifferential.java" \
    > "$SRC/M3PagedArrayReferenceDifferential.java"
echo "DONOR rev=$REV files=$(ls "$SRC" | wc -l) (FILE rows + the reference oracle)"
"$J/javac" "-J$T" --release 21 -encoding UTF-8 -d "$OUT/donor-classes" "$SRC/"*.java || exit 2
"$J/javac" "-J$T" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$MC" -d "$OUT/oracle-relocated" \
    "$HERE/oracle/M3PagedArrayReferenceDifferential.java" || exit 2
run() { # side mode label class args...
  local side=$1 mode=$2 label=$3 cls=$4; shift 4
  local cp pkg X=""
  [ "$mode" = xint ] && X="-Xint"
  if [ "$side" = donor ]; then cp="$OUT/donor-classes"; pkg=$donor_pkg
  else cp="$OUT/oracle-relocated$SEP$MT$SEP$MC"; pkg=jdk.m3.collections; fi
  "$J/java" "$T" $X -Xmx256m -cp "$cp" "$pkg.$cls" "$@" > "$OUT/runs/$label-$side-$mode.txt" 2>&1
  echo "rc=$?" >> "$OUT/runs/$label-$side-$mode.txt"
}
for mode in normal xint; do
  for side in donor relocated; do
    run $side $mode policy M3PressurePolicyTest
    run $side $mode reuse M3PagedArrayBackingReuseTest
    run $side $mode behavior M3PagedArrayBackingReuseTest behavior
    run $side $mode resources M3PagedArrayBackingReuseTest resources
    run $side $mode sampling-java M3MemoryPressureSamplingTest java
    run $side $mode report M3MemoryPressureReportTest
    sed -i 's/[0-9][0-9]*/N/g' "$OUT/runs/report-$side-$mode.txt"
    sed -i -E 's/(allocatedBytes|elapsedNanos)=[0-9]+/\1=N/g' "$OUT/runs/resources-$side-$mode.txt"
    mkdir -p "$OUT/scratch-$side-$mode"
    run $side $mode reference M3PagedArrayReferenceDifferential "$OUT/runs/trace-$side-$mode.txt" "$OUT/scratch-$side-$mode" "$EP" "$OPS"
  done
done
fail=0
for f in "$OUT"/runs/*-donor-*.txt; do
  g=${f/-donor-/-relocated-}
  if cmp -s "$f" "$g"; then s=IDENTICAL; else s=DIFFERENT; fail=1; fi
  echo "$(basename "${f/-donor/}") $s sha256=$(sha256sum < "$f" | cut -c1-12) lines=$(wc -l < "$f") last=$(grep -v '^rc=' "$f" | tail -1 | cut -c1-90)"
done
echo "differential fail=$fail"
