#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Donor side of the native gate, prepared on the host that holds the donor git clone: the Java closure of the JNI host
# class (every main-scope FILE row and every TYPE row without a FILE row of recipe planner-family-20261107, in package
# layout; a hash-pinned .java.txt compiles as the .java it pins) and the two donor kernels of this recipe's FILE rows.
# native-gate.sh (Linux) reads <out-dir>/src and <out-dir>/kernels.
# usage: prepare-donor.sh <donor-git-dir> <donor-rev> <out-dir>
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
FAMILY_MAP="$RECIPE_DIR/../planner-family-20261107/RELOCATION_MAP.tsv"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
DONOR_GIT=$1
REV=$2
OUT=$3
package_path=$(awk -F'\t' '$1=="PACKAGE" {gsub(/\./, "/", $2); print $2; exit}' "$FAMILY_MAP")
main_dir=$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// && $2 ~ /\.java$/ {print $2; exit}' "$FAMILY_MAP")
main_dir=${main_dir%/*}
rm -rf "$OUT/src" "$OUT/kernels"
mkdir -p "$OUT/src/$package_path" "$OUT/kernels"
fetch() { git -C "$DONOR_GIT" show "$REV:$1" > "$2"; echo "DONOR $1 blob=$(git -C "$DONOR_GIT" rev-parse "$REV:$1" | cut -c1-12)"; }
awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {print $2}' "$FAMILY_MAP" | while read -r path; do
    fetch "$path" "$OUT/src/$package_path/$(basename "$path" .txt)"
done
awk -F'\t' 'NR==FNR {if ($1=="FILE") {n=split($2, p, "/"); sub(/\.txt$/, "", p[n]); sub(/\.java$/, "", p[n]); f[p[n]]=1}; next}
    $1=="TYPE" && !($2 in f) {print $2}' "$FAMILY_MAP" "$FAMILY_MAP" | while read -r type; do
    fetch "$main_dir/$type.java" "$OUT/src/$package_path/$type.java"
done
awk -F'\t' '$1=="FILE" {print $2 "\t" $3}' "$MAP" | while IFS=$'\t' read -r donor target; do
    fetch "$donor" "$OUT/kernels/$(basename "$target")"
done
echo "PREPARED $(find "$OUT/src" -name '*.java' | wc -l) java sources, $(ls "$OUT/kernels" | wc -l) donor kernels (named after their targets)"
