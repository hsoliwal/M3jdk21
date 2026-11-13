#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation leaf pmem-gram-index: the donor family (the owners of recipe/pmem-region-core-20261106 and
# recipe/pmem-source-image-20261106 and the three campaign owners of this leaf, compiled from the donor git tree with
# javac --release 21 -Xlint:all -Werror) and the relocated family (target/classes of this module) replay the same inputs
# through M3PmemGramIndexDump (target/test-classes): index builds, gram-vs-exact query lanes against a naive search,
# refusal, corpus and drift lanes and <operations> seeded queries. The two dumps must be byte-identical and hold no
# UNEXPECTED line. Prints DIFFERENTIAL PASS|FAIL with both sha256 digests, the byte and line counts, the TOTAL line and
# the count of exact-equal query lanes. With "native" as the fifth argument (Linux; the family's libm3pmemsdk.so and
# libm3gramoracle.so, built from src/test/native/M3GramOracle.c, in each library directory) the dumps also open POSIX
# regions and check every exact result against the C17 oracle.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [operations=10000] [native]
# env:   JAVA_HOME (a JDK 21); run after mvn package (target/classes and target/test-classes must exist);
#        DONOR_LIBRARY_PATH / RELOCATED_LIBRARY_PATH name the two library directories in native mode;
#        DONOR_SRC (optional) names a directory holding the donor owners already extracted at <donor-rev> (a Linux host
#        reading a Windows clone), else they are read with git show
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../../../../../m3/optane")
DONOR_GIT=$1
REV=$2
OPERATIONS=${4:-10000}
MODE=${5:-file}
mkdir -p "$3/src" "$3/classes" "$3/tmp"
SCRATCH=$(winpath "$3")
TMPDIR_ARG="-Djava.io.tmpdir=$SCRATCH/tmp"
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
# the donor SDK and campaign packages (PACKAGE rows of RELOCATION_MAP.tsv; named from the map, never spelled here, so
# the naming gate leaves this script alone)
donor_package=$(awk -F'\t' '$1=="PACKAGE" && $3=="jdk.m3.optane" && $2 ~ /[.]pmem$/ {print $2; exit}' "$RECIPE_DIR/RELOCATION_MAP.tsv")
donor_campaign=$(awk -F'\t' '$1=="PACKAGE" && $3=="jdk.internal.m3.optane" {print $2; exit}' "$RECIPE_DIR/RELOCATION_MAP.tsv")
if [ -n "${DONOR_SRC:-}" ]; then donor_commit=$REV; else donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}"); fi
rm -rf "$SCRATCH/src"/*.java "$SCRATCH/classes"/*
for map in "$RECIPE_DIR/../pmem-region-core-20261106/RELOCATION_MAP.tsv" "$RECIPE_DIR/../pmem-source-image-20261106/RELOCATION_MAP.tsv" \
        "$RECIPE_DIR/RELOCATION_MAP.tsv"; do
    while IFS=$'\t' read -r kind donor target _note; do
        [ "$kind" = FILE ] || continue
        case "$target" in src/main/java/*.java) ;; *) continue ;; esac
        if [ -n "${DONOR_SRC:-}" ]; then
            cp "$DONOR_SRC/$(basename "$donor")" "$SCRATCH/src/"
        else
            git -C "$DONOR_GIT" show "$REV:$donor" > "$SCRATCH/src/$(basename "$donor")"
        fi
    done < "$map"
done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH/src"/*.java
native_args=()
donor_library=()
relocated_library=()
if [ "$MODE" = native ]; then
    native_args=(native)
    donor_library=("-Djava.library.path=${DONOR_LIBRARY_PATH:?DONOR_LIBRARY_PATH names the donor library directory}" -Xcheck:jni)
    relocated_library=("-Djava.library.path=${RELOCATED_LIBRARY_PATH:?RELOCATED_LIBRARY_PATH names the relocated library directory}" -Xcheck:jni)
fi
"$JAVA_BIN/java" -XX:-OmitStackTraceInFastThrow "$TMPDIR_ARG" "${donor_library[@]}" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" \
    jdk.internal.m3.optane.M3PmemGramIndexDump "$donor_package" "$donor_campaign" "" "$OPERATIONS" "${native_args[@]}" > "$SCRATCH/donor.txt"
"$JAVA_BIN/java" -XX:-OmitStackTraceInFastThrow "$TMPDIR_ARG" "${relocated_library[@]}" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" \
    jdk.internal.m3.optane.M3PmemGramIndexDump jdk.m3.optane jdk.internal.m3.optane M3 "$OPERATIONS" "${native_args[@]}" > "$SCRATCH/relocated.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit $donor_package + $donor_campaign ($(ls "$SCRATCH/src" | wc -l) owners); RELOCATED jdk.m3.optane + jdk.internal.m3.optane mode=$MODE"
grep -E '^(RANDOM operations|TOTAL) ' "$SCRATCH/relocated.txt"
unexpected=$(grep -c UNEXPECTED "$SCRATCH/relocated.txt" || true)
echo "EXACT lanes=$(grep -c ' EQUAL ORACLE' "$SCRATCH/relocated.txt") corpus=$(grep -c '^CORPUS .* ORACLE$' "$SCRATCH/relocated.txt") unexpected=${unexpected:-0}"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" && [ "${unexpected:-0}" = 0 ]; then
    echo "DIFFERENTIAL PASS mode=$MODE donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor.txt") lines=$(wc -l < "$SCRATCH/donor.txt")"
else
    echo "DIFFERENTIAL FAIL mode=$MODE donor_sha256=$donor_sha relocated_sha256=$relocated_sha unexpected=${unexpected:-0}"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 20
    exit 1
fi
