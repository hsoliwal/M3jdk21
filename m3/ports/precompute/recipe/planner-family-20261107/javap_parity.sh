#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# javap -p parity gate: every class file compiled from a main-scope FILE row's donor source (the donor compile tree
# differential.sh leaves in <donor-classes-dir>) against the relocated class file (target/classes), the donor text
# normalized by the PACKAGE and TYPE rows of RELOCATION_MAP.tsv; nested classes follow their outer class; carriage
# returns are removed on both sides. A hash-pinned .java.txt donor source names the class of the .java it pins.
# Prints one line per class and JAVAP_PARITY PASS|FAIL.
# Derived from m3/algorithms/recipe/graph-trie-20261106/javap_parity.sh (the donor package comes from the PACKAGE row).
# Copied from m3/ports/precompute/recipe/utf16-stitch-20261106/javap_parity.sh with two corrections: the PACKAGE rows also
# map the $-separated package spelling javac uses in synthetic names (an enum switch emits Outer$1 with the field
# $SwitchMap$<package with $>$<Enum>), and a DIFFERS listing no longer ends the script under pipefail (diff exits 1).
# Copied from m3/ports/precompute/recipe/consumer-profiles-20261107/javap_parity.sh; recipe v4 adds the QUALIFIED rows:
# a qualified donor type (a row without a call) maps to the target type in the target package (javap prints it qualified).
# usage: javap_parity.sh <donor-classes-dir> <scratch-dir>   (JAVA_HOME = JDK 21; after differential.sh and mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) winpath() { (cd "$1" && pwd -W); } ;; *) winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_CLASSES=$(winpath "$1")
mkdir -p "$2/javap"
SCRATCH=$(winpath "$2")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
normalize="$SCRATCH/javap/normalize.sed"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
awk -F'\t' -v p="$target_package" '$1=="QUALIFIED" && $2 !~ /\(/ {gsub(/\./, "\\.", $2); print "s/\\b" $2 "\\b/" p "." $3 "/g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="PACKAGE" && $2!=$3 {gsub(/\./, "\\.", $2); print "s/" $2 "/" $3 "/g"}' "$MAP" >> "$normalize"
awk -F'\t' '$1=="PACKAGE" && $2!=$3 {gsub(/\./, "\\$", $2); gsub(/\./, "$", $3); print "s/" $2 "/" $3 "/g"}' "$MAP" >> "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" {gsub(/\./, "/", $2); print $2; exit}' "$MAP")
same=0
differ=0
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    case "$target" in src/main/java/*) ;; *) continue ;; esac
    donor_type=$(basename "$(basename "$donor" .txt)" .java)
    target_type=$(basename "$target" .java)
    target_pkg=$(dirname "$target" | sed -E 's#^src/main/java/##')
    for class in "$DONOR_CLASSES/$donor_pkg/$donor_type.class" "$DONOR_CLASSES/$donor_pkg/$donor_type"\$*.class; do
        [ -f "$class" ] || continue
        inner=$(basename "$class" .class)
        inner=${inner#"$donor_type"}
        "$JAVA_BIN/javap" -p "$class" | tr -d '\r' | sed -f "$normalize" > "$SCRATCH/javap/d-$target_type$inner.txt"
        "$JAVA_BIN/javap" -p "$MODULE/target/classes/$target_pkg/$target_type$inner.class" | tr -d '\r' > "$SCRATCH/javap/r-$target_type$inner.txt"
        if cmp -s "$SCRATCH/javap/d-$target_type$inner.txt" "$SCRATCH/javap/r-$target_type$inner.txt"; then
            same=$((same + 1))
            echo "SAME $target_type$inner $(wc -l < "$SCRATCH/javap/r-$target_type$inner.txt") lines"
        else
            differ=$((differ + 1))
            echo "DIFFERS $target_type$inner"
            diff "$SCRATCH/javap/d-$target_type$inner.txt" "$SCRATCH/javap/r-$target_type$inner.txt" | head -n 10 || true
        fi
    done
done < "$MAP"
if [ "$differ" -eq 0 ] && [ "$same" -gt 0 ]; then
    echo "JAVAP_PARITY PASS classes=$same"
else
    echo "JAVAP_PARITY FAIL same=$same differ=$differ"
    exit 1
fi
