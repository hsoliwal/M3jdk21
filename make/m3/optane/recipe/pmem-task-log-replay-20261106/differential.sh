#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation leaf pmem-task-log-replay: the donor family (the region-core owners of
# recipe/pmem-region-core-20261106 and the two owners of this leaf, compiled from the donor git tree with
# javac --release 21 -Xlint:all -Werror) and the relocated family (target/classes of this module) replay the same inputs
# through M3PmemTaskLogDump (target/test-classes): open refusals, appends, corruption, interrupted appends, progress
# cancellation and replay lanes and <operations> seeded operations. The two dumps must be byte-identical. Prints
# DIFFERENTIAL PASS|FAIL with both
# sha256 digests, the byte and line counts and the TOTAL line. With "native" as the fifth argument (Linux, both
# libraries built from the two m3pmemsdk.c) the dumps also run over POSIX regions and over FILE/POSIX regions of a
# sparse file at a file offset above 2 GiB.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [operations=100000] [native]
# env:   JAVA_HOME (a JDK 21); run after mvn package (target/classes and target/test-classes must exist);
#        DONOR_LIBRARY_PATH / RELOCATED_LIBRARY_PATH name the directories of the two libm3pmemsdk.so in native mode;
#        DONOR_SRC (optional) names a directory holding the donor owners already extracted at <donor-rev> (a Linux host
#        reading a Windows clone), else they are read with git show
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../../../../../m3/optane")
DONOR_GIT=$1
REV=$2
OPERATIONS=${4:-100000}
MODE=${5:-file}
mkdir -p "$3/src" "$3/classes" "$3/tmp"
SCRATCH=$(winpath "$3")
TMPDIR_ARG="-Djava.io.tmpdir=$SCRATCH/tmp"
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
# the donor package of the owners (the PACKAGE row of RELOCATION_MAP.tsv that maps the region-core package; named
# from the map, never spelled here, so the naming gate leaves this script alone)
donor_package=$(awk -F"	" '$1=="PACKAGE" && $3=="jdk.m3.optane" && $2 ~ /[.]pmem$/ {print $2; exit}' "$RECIPE_DIR/RELOCATION_MAP.tsv")
if [ -n "${DONOR_SRC:-}" ]; then donor_commit=$REV; else donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}"); fi
rm -rf "$SCRATCH/src"/*.java "$SCRATCH/classes"/*
for map in "$RECIPE_DIR/../pmem-region-core-20261106/RELOCATION_MAP.tsv" "$RECIPE_DIR/RELOCATION_MAP.tsv"; do
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
    donor_library=("-Djava.library.path=${DONOR_LIBRARY_PATH:?DONOR_LIBRARY_PATH names the donor libm3pmemsdk.so directory}" -Xcheck:jni)
    relocated_library=("-Djava.library.path=${RELOCATED_LIBRARY_PATH:?RELOCATED_LIBRARY_PATH names the relocated libm3pmemsdk.so directory}" -Xcheck:jni)
fi
"$JAVA_BIN/java" -XX:-OmitStackTraceInFastThrow "$TMPDIR_ARG" "${donor_library[@]}" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" jdk.m3.optane.M3PmemTaskLogDump \
    "$donor_package" "" "$OPERATIONS" "${native_args[@]}" > "$SCRATCH/donor.txt"
"$JAVA_BIN/java" -XX:-OmitStackTraceInFastThrow "$TMPDIR_ARG" "${relocated_library[@]}" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" jdk.m3.optane.M3PmemTaskLogDump \
    jdk.m3.optane M3 "$OPERATIONS" "${native_args[@]}" > "$SCRATCH/relocated.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit $donor_package ($(ls "$SCRATCH/src" | wc -l) owners); RELOCATED jdk.m3.optane mode=$MODE"
grep -E '^(RANDOM operations|TOTAL) ' "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt"; then
    echo "DIFFERENTIAL PASS mode=$MODE donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor.txt") lines=$(wc -l < "$SCRATCH/donor.txt")"
else
    echo "DIFFERENTIAL FAIL mode=$MODE donor_sha256=$donor_sha relocated_sha256=$relocated_sha"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 20
    exit 1
fi
