#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the stitched UTF-16 relocation: the donor closure (the FILE rows of RELOCATION_MAP.tsv, compiled from
# the donor git tree with javac --release 21 -Xlint:all -Werror; a hash-pinned .java.txt postimage is compiled as
# the .java it pins) and the relocated classes (target/classes + target/test-classes of m3/ports/precompute) replay
# the same seeded corpus through M3SparseUtf16StitchedDump: CASES seeded cases plus the refusal, computed-list and
# oracle report lines on the default JIT, and XINT_CASES cases (no oracle report) under -Xint. Each pair of dumps must
# be byte-identical. The donor's main-path lineage of the plan (the OMIT row with the same file name) is replayed as
# a second donor: it must be identical except the COMPUTED_LIST line (the 2026-10-27 snapshot correction), which is
# printed. Prints DIFFERENTIAL PASS|FAIL with the sha256 digests. Donor package and type names come from the map.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [CASES=3000] [XINT_CASES=400]
#        (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CASES=${4:-3000}
XINT_CASES=${5:-400}
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE/RELOCATION_MAP.tsv"
TARGET_PACKAGE=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
DONOR_PACKAGE=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
TARGET_ORACLE=M3SparseUtf16StitchedOracle
DONOR_ORACLE=$(awk -F'\t' -v t="$TARGET_ORACLE" '$1=="TYPE" && $3==t {print $2; exit}' "$MAP")
DUMP="$TARGET_PACKAGE.M3SparseUtf16StitchedDump"
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")

# compile <lineage> <override-path or ->: the FILE rows (main closure + the donor oracle), optionally one file replaced
compile() {
    local dir="$SCRATCH/donor-$1" override=$2
    rm -rf "$dir" && mkdir -p "$dir/src" "$dir/classes"
    awk -F'\t' '$1=="FILE" && ($3 ~ /src\/main\/java\// || $3 ~ /Oracle\.java$/) {print $2}' "$MAP" | while read -r path; do
        name=$(basename "$path" .txt)
        if [ "$override" != - ] && [ "$(basename "$override")" = "$name" ]; then path=$override; fi
        git -C "$DONOR_GIT" show "$REV:$path" > "$dir/src/$name"
        echo "DONOR-$1 $donor_commit $path blob=$(git -C "$DONOR_GIT" rev-parse "$REV:$path" | cut -c1-12)"
    done
    "$JAVA_BIN/javac" --release 21 -encoding UTF-8 -proc:none -Xlint:all -Werror -d "$dir/classes" "$dir"/src/*.java
}

# dump <out> <classpath> <package> <oracle> <cases> [jvm flags]
dump() {
    local out=$1 cp=$2 pkg=$3 oracle=$4 cases=$5
    shift 5
    "$JAVA_BIN/java" "$@" -cp "$cp" "$DUMP" "$pkg" "$oracle" "$cases" > "$SCRATCH/$out.txt"
}

main_path=$(awk -F'\t' '$1=="OMIT" && $2 ~ /\/M3SparseUtf16LiteralPlan\.java$/ {print $2; exit}' "$MAP")
compile b -
compile a "$main_path"
RELOCATED="$MODULE/target/classes$SEP$MODULE/target/test-classes"
dump donor-b "$SCRATCH/donor-b/classes$SEP$MODULE/target/test-classes" "$DONOR_PACKAGE" "$DONOR_ORACLE" "$CASES"
dump relocated "$RELOCATED" "$TARGET_PACKAGE" "$TARGET_ORACLE" "$CASES"
dump donor-b-xint "$SCRATCH/donor-b/classes$SEP$MODULE/target/test-classes" "$DONOR_PACKAGE" "" "$XINT_CASES" -Xint
dump relocated-xint "$RELOCATED" "$TARGET_PACKAGE" "" "$XINT_CASES" -Xint
dump donor-a "$SCRATCH/donor-a/classes$SEP$MODULE/target/test-classes" "$DONOR_PACKAGE" "$DONOR_ORACLE" "$CASES"
status=PASS
for pair in "donor-b relocated" "donor-b-xint relocated-xint"; do
    set -- $pair
    left=$(sha256sum "$SCRATCH/$1.txt" | cut -d' ' -f1)
    right=$(sha256sum "$SCRATCH/$2.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/$1.txt" "$SCRATCH/$2.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 vs $2 $verdict sha256=$left/$right bytes=$(wc -c < "$SCRATCH/$1.txt") lines=$(wc -l < "$SCRATCH/$1.txt")"
done
grep -v '^COMPUTED_LIST' "$SCRATCH/donor-a.txt" > "$SCRATCH/donor-a.cmp"
grep -v '^COMPUTED_LIST' "$SCRATCH/relocated.txt" > "$SCRATCH/relocated.cmp"
if cmp -s "$SCRATCH/donor-a.cmp" "$SCRATCH/relocated.cmp"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
echo "donor-a(main path) vs relocated, COMPUTED_LIST excluded: $verdict sha256=$(sha256sum "$SCRATCH/donor-a.cmp" | cut -d' ' -f1)"
echo "COMPUTED_LIST main-path: $(grep '^COMPUTED_LIST' "$SCRATCH/donor-a.txt")"
echo "COMPUTED_LIST relocated: $(grep '^COMPUTED_LIST' "$SCRATCH/relocated.txt")"
grep '^ORACLE' "$SCRATCH/relocated.txt"
tail -n 1 "$SCRATCH/relocated.txt"
tail -n 1 "$SCRATCH/relocated-xint.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
