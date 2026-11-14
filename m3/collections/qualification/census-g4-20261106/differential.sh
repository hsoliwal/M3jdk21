#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census-g4-20261106 (run after mvn package of m3/collections): the donor
# M3LongLongIndexedSortedView (+ its proof) compiled in the donor package against the module's own M3IndexMaskCompactor
# source re-packaged there (the class it calls, byte-identical body), vs the relocated module classes; driven by
# differential/M3LongLongIndexedSortedViewDump.java.tmpl (TreeMap model) and the crate proof on both sides.
# usage: differential.sh <donor git dir> <donor rev> <out dir> [snapshots] [seed]      env: JAVA_HOME = JDK 21
set -euo pipefail
DONOR=$1; REV=$2; OUT=$3; SNAPS=${4:-20000}; SEED=${5:-20261106}
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
MAP="$HERE/RELOCATION_MAP.tsv"
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JC=("$J/javac" --release 21 -Xlint:all,-module -Werror -proc:none)
DONOR_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
HOST_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
rm -rf "$OUT"; mkdir -p "$OUT"/{donor-src,donor,relocated-src,relocated}
for type in M3LongLongIndexedSortedView M3LongLongIndexedSortedViewProof; do
  path=$(awk -F'\t' -v t="/$type.java.txt" '$1=="FILE" && index($2, t) {print $2; exit}' "$MAP")
  git -C "$DONOR" cat-file blob "$REV:$path" > "$OUT/donor-src/$type.java"
done
sed "s/^package $HOST_PKG;/package $DONOR_PKG;/" "$MODULE/src/main/java/${HOST_PKG//.//}/M3IndexMaskCompactor.java" > "$OUT/donor-src/M3IndexMaskCompactor.java"
"${JC[@]}" -d "$OUT/donor" "$OUT"/donor-src/*.java
sed "s/@PKG@/$DONOR_PKG/" "$HERE/differential/M3LongLongIndexedSortedViewDump.java.tmpl" > "$OUT/donor-src/M3LongLongIndexedSortedViewDump.java.txt"
mkdir -p "$OUT/driver-donor"; cp "$OUT/donor-src/M3LongLongIndexedSortedViewDump.java.txt" "$OUT/driver-donor/M3LongLongIndexedSortedViewDump.java"
sed "s/@PKG@/$HOST_PKG/" "$HERE/differential/M3LongLongIndexedSortedViewDump.java.tmpl" > "$OUT/relocated-src/M3LongLongIndexedSortedViewDump.java"
"${JC[@]}" -d "$OUT/donor" -cp "$OUT/donor" "$OUT/driver-donor/M3LongLongIndexedSortedViewDump.java"
"${JC[@]}" -d "$OUT/relocated" -cp "$MODULE/target/classes" "$OUT/relocated-src/M3LongLongIndexedSortedViewDump.java"
"$J/java" -cp "$OUT/donor" M3LongLongIndexedSortedViewDump "$SNAPS" "$SEED" > "$OUT/donor.dump" 2> "$OUT/donor.err"
"$J/java" -cp "$OUT/relocated$SEP$MODULE/target/classes" M3LongLongIndexedSortedViewDump "$SNAPS" "$SEED" > "$OUT/relocated.dump" 2> "$OUT/relocated.err"
"$J/java" -cp "$OUT/donor" "$DONOR_PKG.M3LongLongIndexedSortedViewProof" > "$OUT/donor-proof.out"
"$J/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$HOST_PKG.M3LongLongIndexedSortedViewProof" > "$OUT/relocated-proof.out"
cat "$OUT/donor.err" "$OUT/relocated.err"
d=$(sha256sum < "$OUT/donor.dump"); r=$(sha256sum < "$OUT/relocated.dump")
echo "dump lines=$(wc -l < "$OUT/relocated.dump") donor=${d%% *} relocated=${r%% *} $([ "$d" = "$r" ] && echo IDENTICAL || echo DIFFERENT)"
dp=$(sha256sum < "$OUT/donor-proof.out"); rp=$(sha256sum < "$OUT/relocated-proof.out")
echo "proof donor=${dp%% *} relocated=${rp%% *} $([ "$dp" = "$rp" ] && echo IDENTICAL || echo DIFFERENT): $(cat "$OUT/relocated-proof.out")"
if [ "$d" != "$r" ] || [ "$dp" != "$rp" ] || ! grep -q " mismatches=0$" "$OUT/donor.err" || ! grep -q " mismatches=0$" "$OUT/relocated.err"; then
  echo "DIFFERENTIAL FAIL"; exit 1
fi
echo "DIFFERENTIAL PASS snapshots=$SNAPS seed=$SEED"
