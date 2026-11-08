#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: javap -p of every class file compiled from a FILE row's donor source (the donor compile tree
# differential.sh leaves in <scratch>/classes) against the relocated class file (target/classes), the donor text
# normalized by the PACKAGE and TYPE rows of RELOCATION_MAP.tsv; nested and local classes ($WeightedEdge, $1Frame,
# $1QueueEntry, $Node) follow their outer class; carriage returns are removed on both sides (javap prints CRLF on Windows
# and MSYS sed drops the CR, so only the sed side would lose it). Prints one line per class and JAVAP_PARITY PASS|FAIL.
# usage: javap_parity.sh <scratch-dir>   (JAVA_HOME = JDK 21; after differential.sh)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) winpath() { (cd "$1" && pwd -W); } ;; *) winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
SCRATCH=$(winpath "$1")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
mkdir -p "$SCRATCH/javap"
normalize="$SCRATCH/javap/normalize.sed"
awk -F'\t' '$1=="PACKAGE" && $2!=$3 {gsub(/\./, "\\.", $2); print "s/" $2 "/" $3 "/g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
same=0
differ=0
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    case "$target" in src/main/java/*) ;; *) continue ;; esac
    donor_type=$(basename "$donor" .java)
    donor_pkg=$(dirname "$donor" | sed -E 's#^(.*/)?src/main/java/##')
    target_type=$(basename "$target" .java)
    target_pkg=$(dirname "$target" | sed -E 's#^src/main/java/##')
    for class in "$SCRATCH/classes/$donor_pkg/$donor_type.class" "$SCRATCH/classes/$donor_pkg/$donor_type"\$*.class; do
        [ -f "$class" ] || continue
        inner=$(basename "$class" .class)
        inner=${inner#"$donor_type"}
        "$JAVA_BIN/javap" -p "$class" | tr -d '\r' | sed -f "$normalize" > "$SCRATCH/javap/d-$target_type$inner.txt"
        "$JAVA_BIN/javap" -p "$MODULE/target/classes/$target_pkg/$target_type$inner.class" | tr -d '\r' > "$SCRATCH/javap/r-$target_type$inner.txt"
        if cmp -s "$SCRATCH/javap/d-$target_type$inner.txt" "$SCRATCH/javap/r-$target_type$inner.txt"; then
            same=$((same + 1))
            echo "SAME $target_type$inner"
        else
            differ=$((differ + 1))
            echo "DIFFERS $target_type$inner"
            diff "$SCRATCH/javap/d-$target_type$inner.txt" "$SCRATCH/javap/r-$target_type$inner.txt" | head -n 10
        fi
    done
done < "$MAP"
if [ "$differ" -eq 0 ]; then
    echo "JAVAP_PARITY PASS classes=$same"
else
    echo "JAVAP_PARITY FAIL same=$same differ=$differ"
    exit 1
fi
