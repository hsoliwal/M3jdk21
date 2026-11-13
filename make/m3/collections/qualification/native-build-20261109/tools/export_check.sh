#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# export_check.sh <make/m3/collections build dir> <NATIVE_CENSUS.tsv>
#
# NATIVE_EXPORT_CHECK v1.1 (2026-11-09). Lists the JNI symbols exported by every library in the build dir (.so with
# nm -D, .dll with objdump -p) and compares them with the BOUND rows of the census for the jdk.m3.collections
# classes (jdk.m3.arrays rows belong to make/m3/arrays). It also lists exported symbols that are not JNI entry points,
# which C_VISIBILITY_PRESET hidden should leave empty on ELF. Exit 0 only when the two sets are equal.
set -euo pipefail
build=$1
census=$2
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
shopt -s nullglob
for lib in "$build"/*.so; do
  nm -D --defined-only "$lib" | awk '{print $3}'
done > "$work/all"
for lib in "$build"/*.dll; do
  objdump -p "$lib" | awk '/\[Ordinal\/Name Pointer\] Table/{f=1; next} f && !NF {f=0} f && /^[[:space:]]*\[/ {print $NF}'
done >> "$work/all"
sed 's/@.*//' "$work/all" | grep '^Java_' | sort -u > "$work/exported" || true
# v1.1: classes whose library the build skipped on this platform (m3_skipped_jni_classes.txt) are not expected.
skipped_file="$build/m3_skipped_jni_classes.txt"
touch "$work/skipped"
[ -f "$skipped_file" ] && tr -d '\r' < "$skipped_file" | sed '/^$/d' > "$work/skipped"
awk -F'\t' 'FILENAME == ARGV[1] {skip[$1] = 1; next}
            FNR > 1 && $3 == "BOUND" && $1 !~ /^jdk\.m3\.arrays/ && !($1 in skip) {print $4}' \
    "$work/skipped" "$census" | sort -u > "$work/expected"
missing=$(comm -13 "$work/exported" "$work/expected" | wc -l)
extra=$(comm -23 "$work/exported" "$work/expected" | wc -l)
other=$(grep -v '^Java_' "$work/all" | grep -cv '^_' || true)
echo "NATIVE_EXPORT_CHECK exported $(wc -l < "$work/exported") expected $(wc -l < "$work/expected") missing $missing extra $extra non_jni_exports ${other:-0} skipped_classes $(wc -l < "$work/skipped")"
sed 's/^/SKIPPED_CLASS /' "$work/skipped"
comm -13 "$work/exported" "$work/expected" | sed 's/^/MISSING /'
comm -23 "$work/exported" "$work/expected" | sed 's/^/EXTRA /'
[ "$missing" -eq 0 ] && [ "$extra" -eq 0 ]
