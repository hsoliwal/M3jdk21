#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census-g5-20261106: the crate oracle M3CircularFifoShiftProof run three ways -
#  (a) donor proof on the donor baseline CircularFifoQueue (the crate preimage), (b) donor proof on the rejected donor
#  candidate (the crate postimage), (c) relocated proof on the owner as it stands in the commons-collections 4.5.0
#  fork. Donor sides resolve the rest of the fork closure (BoundedCollection, ...) from the owner fork sources
#  re-packaged into the donor package (sed of the package prefix); outputs must be identical (the receipt pins
#  baselineChecks = candidateChecks = 20939053). No donor class is written into the module.
# usage: differential.sh <donor git dir> <donor rev> <out dir>      env: JAVA_HOME = JDK 21
set -euo pipefail
DONOR=$1; REV=$2; OUT=$3
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
MAP="$HERE/RELOCATION_MAP.tsv"
FORK="$MODULE/implementations/commons-collections-4.5.0"
J="${JAVA_HOME:?JAVA_HOME}/bin"
JC=("$J/javac" --release 21 -proc:none -encoding UTF-8 -nowarn)
DONOR_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP"); HOST_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
DONOR_ROOT=${DONOR_PKG%.queue}; HOST_ROOT=${HOST_PKG%.queue}
PROOF=$(awk -F'\t' '$1=="FILE" && $2 ~ /M3CircularFifoShiftProof.java.txt$/ {print $2; exit}' "$MAP")
POST=$(awk -F'\t' '$1=="SKIP" && $2 ~ /CircularFifoQueue.java.txt$/ {print $2; exit}' "$MAP")
PRE=${POST%/src/test/resources/*}/src/test/resources/before/CircularFifoQueue.java.txt
rm -rf "$OUT"; mkdir -p "$OUT/sourcepath" "$OUT/relocated"
(cd "$FORK/src/main/java" && find . -name "*.java") | while read -r f; do
  t="$OUT/sourcepath/${f//${HOST_ROOT//.//}/${DONOR_ROOT//.//}}"; mkdir -p "$(dirname "$t")"
  sed "s/\b${HOST_ROOT//./\.}\b/$DONOR_ROOT/g" "$FORK/src/main/java/$f" > "$t"
done
for side in baseline candidate; do
  mkdir -p "$OUT/$side-src" "$OUT/$side"
  git -C "$DONOR" cat-file blob "$REV:$PROOF" > "$OUT/$side-src/M3CircularFifoShiftProof.java"
  git -C "$DONOR" cat-file blob "$REV:$([ $side = baseline ] && echo "$PRE" || echo "$POST")" > "$OUT/$side-src/CircularFifoQueue.java"
  "${JC[@]}" -d "$OUT/$side" -sourcepath "$OUT/sourcepath" "$OUT/$side-src"/*.java
  "$J/java" -cp "$OUT/$side" "$DONOR_PKG.M3CircularFifoShiftProof" > "$OUT/$side.out"
done
"${JC[@]}" -d "$OUT/relocated" -sourcepath "$FORK/src/main/java" "$FORK/src/test/java/${HOST_PKG//.//}/M3CircularFifoShiftProof.java"
"$J/java" -cp "$OUT/relocated" "$HOST_PKG.M3CircularFifoShiftProof" > "$OUT/relocated.out"
for side in baseline candidate relocated; do echo "$side $(sha256sum < "$OUT/$side.out" | cut -c1-64) $(cat "$OUT/$side.out")"; done
if cmp -s "$OUT/baseline.out" "$OUT/relocated.out" && cmp -s "$OUT/candidate.out" "$OUT/relocated.out"; then
  echo "DIFFERENTIAL PASS donor-baseline = donor-candidate = relocated-on-owner"; else echo "DIFFERENTIAL FAIL"; exit 1; fi
