#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of recipe null-flow-20261107: javap -p (signatures) and javap -p -c (bytecode) of every donor class compiled by
# differential.sh (<scratch>/donor/classes) against the relocated class (target/classes, <scratch>/relocated-test-classes),
# the donor text normalized by the PACKAGE rows of RELOCATION_MAP.tsv (dotted and slashed) and the TYPE rows whose names
# differ. Prints JAVAP PASS|FAIL with the counts.
# usage: javap_parity.sh <scratch-dir of differential.sh>   env: JAVA_HOME (a JDK 21)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
SCRATCH=$1
JAVAP="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin/javap"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
OUT="$SCRATCH/javap"
rm -rf "$OUT"
mkdir -p "$OUT"
normalize="$OUT/normalize.sed"
awk -F'\t' '$1=="PACKAGE" {d=$2; s=$2; t=$3; ts=$3; gsub(/\./, "\\.", d); gsub(/\./, "/", s); gsub(/\./, "/", ts); print "s#" d "#" t "#g"; print "s#" s "#" ts "#g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
target_dir=$(awk -F'\t' '$1=="PACKAGE"{print $3; exit}' "$MAP" | tr . /)
signatures=0
bytecode=0
total=0
while read -r class; do
    name=$(basename "$class" .class)
    target_name=$(printf '%s\n' "$name" | sed -f "$normalize")
    target="$MODULE/target/classes/$target_dir/$target_name.class"
    [ -f "$target" ] || target="$SCRATCH/relocated-test-classes/$target_dir/$target_name.class"
    [ -f "$target" ] || { echo "JAVAP FAIL no relocated class for $name"; exit 1; }
    total=$((total + 1))
    for mode in p c; do
        flags=-p; [ "$mode" = c ] && flags="-p -c"
        "$JAVAP" $flags "$class" | tr -d '\r' | sed -f "$normalize" > "$OUT/$name.donor.$mode.txt"
        "$JAVAP" $flags "$target" | tr -d '\r' > "$OUT/$name.relocated.$mode.txt"
        if cmp -s "$OUT/$name.donor.$mode.txt" "$OUT/$name.relocated.$mode.txt"; then
            [ "$mode" = p ] && signatures=$((signatures + 1)) || bytecode=$((bytecode + 1))
        else
            echo "JAVAP DIFF $name -$mode"
            diff "$OUT/$name.donor.$mode.txt" "$OUT/$name.relocated.$mode.txt" | head -n 10
        fi
    done
done < <(find "$SCRATCH/donor/classes" -name '*.class' | sort)
if [ "$signatures" -eq "$total" ] && [ "$bytecode" -eq "$total" ]; then
    echo "JAVAP PASS classes=$total signatures_identical=$signatures bytecode_identical=$bytecode (names normalized by the map)"
else
    echo "JAVAP FAIL classes=$total signatures_identical=$signatures bytecode_identical=$bytecode"
    exit 1
fi
