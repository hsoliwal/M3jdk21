#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor PMem region family (the ten owners and PmemAtomCases compiled from the donor git
# tree with javac --release 21 -Xlint:all -Werror) and the relocated family (target/classes and target/test-classes of
# this module) replay the same inputs through M3PmemDump on the FILE backend: the FILE oracle lanes and <operations>
# seeded random region/mapping/transfer/search operations. The two dumps must be byte-identical. Prints DIFFERENTIAL
# PASS|FAIL with both sha256 digests, the byte and line counts and the TOTAL line. With "native" as the fifth argument
# (Linux, both libraries built by native-matrix.sh) the dumps also open POSIX regions and run the 19 atoms.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [operations=100000] [native]
# env:   JAVA_HOME (a JDK 21); run after mvn package (target/classes and target/test-classes must exist);
#        DONOR_LIBRARY_PATH / RELOCATED_LIBRARY_PATH name the directories of the two libm3pmemsdk.so in native mode
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
OPERATIONS=${4:-100000}
MODE=${5:-file}
mkdir -p "$3/src" "$3/classes" "$3/tmp"
SCRATCH=$(winpath "$3")
# the dumps write sparse files above 2 GiB (NTFS fills the gap): temporary files stay in the scratch directory
TMPDIR_ARG="-Djava.io.tmpdir=$SCRATCH/tmp"
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" && $2 ~ /\./ {print $2; exit}' "$MAP")
donor_test_package=$(awk -F'\t' '$1=="PACKAGE" && $2 !~ /\./ {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
rm -f "$SCRATCH/src"/*.java
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    case "$target" in src/main/java/*.java|*/PmemAtomCases.java|*PmemAtomCases.java) ;; *) continue ;; esac
    git -C "$DONOR_GIT" show "$REV:$donor" > "$SCRATCH/src/$(basename "$donor")"
done < "$MAP"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH/src"/*.java
native_args=()
donor_library=()
relocated_library=()
if [ "$MODE" = native ]; then
    native_args=(native)
    donor_library=("-Djava.library.path=${DONOR_LIBRARY_PATH:?DONOR_LIBRARY_PATH names the donor libm3pmemsdk.so directory}" -Xcheck:jni)
    relocated_library=("-Djava.library.path=${RELOCATED_LIBRARY_PATH:?RELOCATED_LIBRARY_PATH names the relocated libm3pmemsdk.so directory}" -Xcheck:jni)
fi
"$JAVA_BIN/java" "$TMPDIR_ARG" "${donor_library[@]}" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3PmemDump" \
    "$donor_package" "" "$donor_test_package" "$OPERATIONS" "${native_args[@]}" > "$SCRATCH/donor.txt"
"$JAVA_BIN/java" "$TMPDIR_ARG" "${relocated_library[@]}" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.M3PmemDump" \
    "$target_package" M3 "$target_package" "$OPERATIONS" "${native_args[@]}" > "$SCRATCH/relocated.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit $donor_package (+ $donor_test_package.PmemAtomCases); RELOCATED $target_package mode=$MODE"
grep -E '^(RANDOM operations|TOTAL) ' "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt"; then
    echo "DIFFERENTIAL PASS mode=$MODE donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor.txt") lines=$(wc -l < "$SCRATCH/donor.txt")"
else
    echo "DIFFERENTIAL FAIL mode=$MODE donor_sha256=$donor_sha relocated_sha256=$relocated_sha"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 20
    exit 1
fi
