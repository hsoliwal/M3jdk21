#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor class (compiled from the donor git tree) and the relocated class (target/classes
# of this module) replay the same seeded corpora and queries through M3ScopedNgramsDifferentialDump (the oracle's 384
# corpora and 10,000 further scoped queries); the two dumps must be byte-identical. The donor oracle is compiled from
# the donor tree as well and run against the donor class, so its M3_TOPK_ORACLE_PASS line (digest) can be compared
# with the relocated oracle's line of the test phase. Prints DIFFERENTIAL PASS|FAIL with both sha256 digests.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
MODULE=$(winpath "$(dirname "$0")/..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$MODULE/RELOCATION_MAP.tsv"
main_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
oracle_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /Oracle\.java$/ {print $2; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_type=$(basename "$main_path" .java)
donor_oracle=$(basename "$oracle_path" .java)
target_type=$(awk -F'\t' -v t="$donor_type" '$1=="TYPE" && $2==t {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
git -C "$DONOR_GIT" show "$REV:$main_path" > "$SCRATCH/src/$donor_type.java"
git -C "$DONOR_GIT" show "$REV:$oracle_path" > "$SCRATCH/src/$donor_oracle.java"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" \
    "$SCRATCH/src/$donor_type.java" "$SCRATCH/src/$donor_oracle.java"
"$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_package.$donor_oracle" verify > "$SCRATCH/donor-oracle.txt"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3ScopedNgramsDifferentialDump" \
    "$donor_package.$donor_type" > "$SCRATCH/donor.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.M3ScopedNgramsDifferentialDump" \
    "$target_package.$target_type" > "$SCRATCH/relocated.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit $main_path"
sed -n 's/^M3_TOPK_ORACLE_PASS /DONOR_ORACLE /p' "$SCRATCH/donor-oracle.txt"
tail -n 1 "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt"; then
    echo "DIFFERENTIAL PASS donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor.txt") lines=$(wc -l < "$SCRATCH/donor.txt")"
else
    echo "DIFFERENTIAL FAIL donor_sha256=$donor_sha relocated_sha256=$relocated_sha"
    exit 1
fi
