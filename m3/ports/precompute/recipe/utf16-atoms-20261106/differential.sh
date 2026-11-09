#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the UTF-16 atom relocation: the donor classes (compiled from the donor git tree, javac --release 21
# -Xlint:all -Werror) and the relocated classes (target/classes + target/test-classes of m3/ports/precompute) replay
# the same seeded inputs through M3Utf16AtomsDump (100,000 LCS pairs, 100,000 glob pairs, bound and refusal cases);
# the two dumps must be byte-identical. Prints DIFFERENTIAL PASS|FAIL with both sha256 digests and the TOTAL line.
# Donor package and type names come from RELOCATION_MAP.tsv (PACKAGE and TYPE rows); nothing here names them.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE/RELOCATION_MAP.tsv"
TARGET_PACKAGE=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
donor_lcs=$(awk -F'\t' '$1=="TYPE" && $3=="M3LcsPlan" {print $2; exit}' "$MAP")
donor_glob=$(awk -F'\t' '$1=="TYPE" && $3=="M3Utf16Glob" {print $2; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
awk -F'\t' '$1=="FILE" && $2 ~ /src\/main\/java\// {print $2}' "$MAP" | sort -u | while read -r donor_path; do
    git -C "$DONOR_GIT" show "$REV:$donor_path" > "$SCRATCH/src/$(basename "$donor_path")"
    echo "DONOR $donor_commit $donor_path"
done
"$JAVA_BIN/javac" --release 21 -encoding UTF-8 -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
DUMP="$TARGET_PACKAGE.M3Utf16AtomsDump"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$DUMP" \
    "$donor_package.$donor_lcs" "$donor_package.$donor_lcs" "$donor_package.$donor_glob" "$donor_package.$donor_glob" \
    > "$SCRATCH/donor.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$DUMP" \
    "$TARGET_PACKAGE.M3LcsPlan" "$TARGET_PACKAGE.M3LcsPlanOracle" "$TARGET_PACKAGE.M3Utf16Glob" "$TARGET_PACKAGE.M3Utf16GlobOracle" \
    > "$SCRATCH/relocated.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
tail -n 1 "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt"; then
    echo "DIFFERENTIAL PASS donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor.txt") lines=$(wc -l < "$SCRATCH/donor.txt")"
else
    echo "DIFFERENTIAL FAIL donor_sha256=$donor_sha relocated_sha256=$relocated_sha"
    exit 1
fi
