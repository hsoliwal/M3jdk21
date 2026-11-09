#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census-g6-20261106 (run after mvn package of m3/collections): the two crate oracles on
# the donor side (donor package, compiled with the crate's composed M3LongLongHashMap postimage and its two parser
# dependencies) vs the relocated oracles on the module owner; outputs must be identical and equal the crate README
# figures (240,012 smoke checks; trace checks=302311 sha256 c13579351e14...).
# usage: differential.sh <donor git dir> <donor rev> <out dir>      env: JAVA_HOME = JDK 21
set -euo pipefail
DONOR=$1; REV=$2; OUT=$3
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
MAP="$HERE/RELOCATION_MAP.tsv"
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
DONOR_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP"); HOST_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
rm -rf "$OUT"; mkdir -p "$OUT/donor-src" "$OUT/donor"
awk -F'\t' '$1=="FILE" {print $2}' "$MAP" | while read -r p; do git -C "$DONOR" cat-file blob "$REV:$p" > "$OUT/donor-src/$(basename "$p")"; done
OWNER=$(awk -F'\t' '$1=="SKIP" && $2 ~ /hash-pinned-java\/[^\/]*\/M3LongLongHashMap.java.txt$/ {print $2; exit}' "$MAP")
DIR=${OWNER%/*}
for t in M3LongLongHashMap M3LongIterator M3LongLongConsumer; do git -C "$DONOR" cat-file blob "$REV:$DIR/$t.java.txt" > "$OUT/donor-src/$t.java"; done
"$J/javac" --release 21 -Xlint:all -Werror -proc:none -d "$OUT/donor" "$OUT"/donor-src/*.java
for c in M3LongMapRecoverySmoke M3LongLongMapSharedTrace; do
  "$J/java" -cp "$OUT/donor" "$DONOR_PKG.$c" > "$OUT/donor-$c.out"
  "$J/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$HOST_PKG.$c" > "$OUT/relocated-$c.out"
  d=$(sha256sum < "$OUT/donor-$c.out"); r=$(sha256sum < "$OUT/relocated-$c.out")
  echo "$c donor=${d%% *} relocated=${r%% *} $([ "$d" = "$r" ] && echo IDENTICAL || echo DIFFERENT): $(cat "$OUT/relocated-$c.out")"
  [ "$d" = "$r" ] || { echo "DIFFERENTIAL FAIL"; exit 1; }
done
grep -q "checks=240012" "$OUT/relocated-M3LongMapRecoverySmoke.out" && grep -q "checks=302311 sha256=c13579351e14187bea41fde5181c944e7873b35bccda3f1391b604f7729492e4" "$OUT/relocated-M3LongLongMapSharedTrace.out" \
  || { echo "DIFFERENTIAL FAIL (crate README figures)"; exit 1; }
echo "DIFFERENTIAL PASS donor = relocated = crate README figures"
