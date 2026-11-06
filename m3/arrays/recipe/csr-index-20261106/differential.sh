#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor CSR index and its oracle (compiled from the donor git tree with javac
# --release 21 -Xlint:all -Werror) and the relocated class (target/classes of m3/arrays) replay the same inputs.
# (a) Both oracle mains must print byte-identical output (MAT_CSR_DIRECT_CHECKS=<count>, RESULT=PASS).
# (b) M3CsrIndexDump (test scope) dumps every oracle input through both classes; the dumps must be byte-identical.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides. The donor package and type names come from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
oracle_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/test\/java\// {print $2; exit}' "$MAP")
donor_owner=$(basename "$owner_path" .java)
donor_oracle=$(basename "$oracle_path" .java)
target_owner=$(awk -F'\t' -v d="$donor_owner" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
target_oracle=$(awk -F'\t' -v d="$donor_oracle" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
git -C "$DONOR_GIT" show "$REV:$owner_path" > "$SCRATCH/src/$donor_owner.java"
git -C "$DONOR_GIT" show "$REV:$oracle_path" > "$SCRATCH/src/$donor_oracle.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH/src/$donor_owner.java" "$SCRATCH/src/$donor_oracle.java"
echo "DONOR $donor_commit $owner_path $oracle_path"
"$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_package.$donor_oracle" > "$SCRATCH/donor-oracle.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.$target_oracle" > "$SCRATCH/relocated-oracle.txt"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3CsrIndexDump" "$donor_package.$donor_owner" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.M3CsrIndexDump" "$target_package.$target_owner" > "$SCRATCH/relocated-dump.txt"
status=PASS
for pair in oracle dump; do
    donor_sha=$(sha256sum "$SCRATCH/donor-$pair.txt" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$SCRATCH/relocated-$pair.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/donor-$pair.txt" "$SCRATCH/relocated-$pair.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$pair $verdict donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor-$pair.txt") lines=$(wc -l < "$SCRATCH/donor-$pair.txt")"
done
tr '\n' ' ' < "$SCRATCH/relocated-oracle.txt"; echo
tail -n 1 "$SCRATCH/relocated-dump.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
