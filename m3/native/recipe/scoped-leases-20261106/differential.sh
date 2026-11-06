#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor lease family (compiled from the donor git tree together with the donor's own
# M3RuntimeHeap) and the relocated family (target/classes of this module, with jdk.m3.util.M3RuntimeHeap compiled
# from m3/ports/collections) replay the same inputs through M3ScopedLeaseDump: the probe's 1,025 concat cases on the
# Java path, the self-tests' deterministic full and window scans, and <operations> seeded random lease/pin/pool/JNI-
# borrow/scan/concat/scope operations. The two dumps must be byte-identical. Prints DIFFERENTIAL PASS|FAIL with both
# sha256 digests, the byte and line counts and the CONCAT_PARITY, RANDOM and TOTAL lines.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [operations=100000]
# env:   JAVA_HOME (a JDK 21); run after mvn package (target/classes and target/test-classes must exist)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
OPERATIONS=${4:-100000}
mkdir -p "$3/src" "$3/classes" "$3/heap"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
heap_type=$(awk -F'\t' '$1=="DEPENDENCY" && $3 ~ /\.util\./ {print $2; exit}' "$MAP")
heap_target=$(awk -F'\t' '$1=="DEPENDENCY" && $3 ~ /\.util\./ {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
donor_dir=""
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    case "$target" in src/main/java/*.java) ;; *) continue ;; esac
    git -C "$DONOR_GIT" show "$REV:$donor" > "$SCRATCH/src/$(basename "$donor")"
    donor_dir=$(dirname "$donor")
done < "$MAP"
# the donor's own heap advisor sits next to the donor owners; the relocated family uses the M3JDK owner instead
git -C "$DONOR_GIT" show "$REV:$donor_dir/$heap_type.java" > "$SCRATCH/src/$heap_type.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH/src"/*.java
heap_source="$MODULE/../ports/collections/src/main/java/$(echo "$heap_target" | tr . /).java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/heap" "$heap_source"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3ScopedLeaseDump" \
    "$donor_package" "$OPERATIONS" > "$SCRATCH/donor.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$SCRATCH/heap$SEP$MODULE/target/test-classes" \
    "$target_package.M3ScopedLeaseDump" "$target_package" "$OPERATIONS" > "$SCRATCH/relocated.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit $donor_dir (+ $heap_type from the donor tree); RELOCATED $target_package (+ $heap_target from $heap_source)"
grep -E '^(CONCAT_PARITY|RANDOM|TOTAL) ' "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt"; then
    echo "DIFFERENTIAL PASS donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor.txt") lines=$(wc -l < "$SCRATCH/donor.txt")"
else
    echo "DIFFERENTIAL FAIL donor_sha256=$donor_sha relocated_sha256=$relocated_sha"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 20
    exit 1
fi
