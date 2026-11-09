#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor owner and its oracle mains (every FILE row of RELOCATION_MAP.tsv, compiled from
# the donor git tree with javac --release 21 -Xlint:all -Werror) and the relocated classes (target/classes and
# target/test-classes of the module) replay the same inputs.
# (a) Every oracle main prints byte-identical output on both sides.
# (b) The reflective dump driver M3IntRingBufferDump (test scope) replays CASES seeded cases through the donor owner and the
#     relocated owner on the default JIT and XINT_CASES cases under -Xint; each pair of dumps must be byte-identical.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests. Donor package and type names come from the map.
# Derived from m3/arrays/recipe/csr-index-20261106/differential.sh (several oracle mains, -Xint pass added).
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [CASES=6000] [XINT_CASES=400]
#        (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CASES=${4:-6000}
XINT_CASES=${5:-400}
mkdir -p "$3/donor-classes" "$3/donor-src"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
donor_owner=$(basename "$owner_path" .java)
target_owner=$(awk -F'\t' -v d="$donor_owner" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
rm -f "$SCRATCH"/donor-src/*.java
awk -F'\t' '$1=="FILE" {print $2}' "$MAP" | while read -r path; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/donor-src/$(basename "$path")"
    echo "DONOR $donor_commit $path blob=$(git -C "$DONOR_GIT" rev-parse "$REV:$path" | cut -c1-12)"
done
"$JAVA_BIN/javac" --release 21 -encoding UTF-8 -proc:none -Xlint:all -Werror -d "$SCRATCH/donor-classes" "$SCRATCH"/donor-src/*.java
RELOCATED="$MODULE/target/classes$SEP$MODULE/target/test-classes"
DUMP="$target_package.M3IntRingBufferDump"
status=PASS
compare() {
    local left right verdict
    left=$(sha256sum "$SCRATCH/$1.txt" | cut -d' ' -f1)
    right=$(sha256sum "$SCRATCH/$2.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/$1.txt" "$SCRATCH/$2.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 vs $2 $verdict sha256=$left/$right bytes=$(wc -c < "$SCRATCH/$1.txt") lines=$(wc -l < "$SCRATCH/$1.txt")"
}
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    case "$target" in src/test/java/*) ;; *) continue ;; esac
    donor_oracle=$(basename "$donor" .java)
    target_oracle=$(basename "$target" .java)
    "$JAVA_BIN/java" -cp "$SCRATCH/donor-classes" "$donor_package.$donor_oracle" > "$SCRATCH/donor-$target_oracle.txt"
    "$JAVA_BIN/java" -cp "$RELOCATED" "$target_package.$target_oracle" > "$SCRATCH/relocated-$target_oracle.txt"
    compare "donor-$target_oracle" "relocated-$target_oracle"
    tr '\n' ' ' < "$SCRATCH/relocated-$target_oracle.txt"; echo
done < "$MAP"
"$JAVA_BIN/java" -cp "$SCRATCH/donor-classes$SEP$MODULE/target/test-classes" "$DUMP" "$donor_package.$donor_owner" "$CASES" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$RELOCATED" "$DUMP" "$target_package.$target_owner" "$CASES" > "$SCRATCH/relocated-dump.txt"
compare donor-dump relocated-dump
"$JAVA_BIN/java" -Xint -cp "$SCRATCH/donor-classes$SEP$MODULE/target/test-classes" "$DUMP" "$donor_package.$donor_owner" "$XINT_CASES" > "$SCRATCH/donor-dump-xint.txt"
"$JAVA_BIN/java" -Xint -cp "$RELOCATED" "$DUMP" "$target_package.$target_owner" "$XINT_CASES" > "$SCRATCH/relocated-dump-xint.txt"
compare donor-dump-xint relocated-dump-xint
tail -n 1 "$SCRATCH/relocated-dump.txt"
tail -n 1 "$SCRATCH/relocated-dump-xint.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
