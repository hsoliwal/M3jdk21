#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 2 of the relocation (derived from recipe/graph-trie-20261106/differential.sh): the donor catalogue (FILE and
# OWNED rows under src/main/java, compiled from the donor git tree with javac --release 21 -Xlint:all -Werror, every
# member present, the dropped ones unused) and the relocated classes (target/classes) replay the same seeded inputs
# through the dump driver AlgorithmExtrasDump. One driver source, two compiles: the relocated side runs the test-scope
# class of this module; the donor side compiles the same file with the TYPE rows of RELOCATION_MAP.tsv reversed, the
# package line set to the donor package and the donor progress contract imported. The donor dump, normalized by the
# PACKAGE and TYPE rows (type names inside messages), must be byte-identical to the relocated dump, and the relocated
# dump must be byte-identical under -Xint (interpreter only). Every run is bounded by timeout(1).
# Prints DIFFERENTIAL PASS|FAIL with the digests.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [cases-per-member]   (JAVA_HOME = JDK 21; after mvn package)
# env:   DEP_CP = the module's dependency jars for the relocated side (m3-collections: the monitor is an adapter over
#        jdk.m3.collections.ProgressMonitor), joined with ; on Windows and : elsewhere; RUN_SECONDS (default 120)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CASES=${4:-2000}
SEED=20261106
RUN_SECONDS=${RUN_SECONDS:-120}
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
DRIVER_SRC="$MODULE/src/test/java/jdk/m3/algorithm/AlgorithmExtrasDump.java"
JVM_FLAGS="-XX:-OmitStackTraceInFastThrow -Xss16m"
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")

# 1. the donor main sources of the FILE and OWNED rows, compiled from the donor tree
rm -rf "$SCRATCH/src" "$SCRATCH/classes" "$SCRATCH/driver-src" "$SCRATCH/driver-classes"
mkdir -p "$SCRATCH/src" "$SCRATCH/classes" "$SCRATCH/driver-src" "$SCRATCH/driver-classes"
awk -F'\t' '($1=="FILE" || $1=="OWNED") && $3 ~ /^src\/main\/java\// {print $2}' "$MAP" | while read -r path; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"
done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
donor_classes=$(find "$SCRATCH/classes" -name '*.class' | wc -l | tr -d ' ')

# 2. the donor variant of the driver: TYPE rows reversed (whole words), donor package, donor progress import
reverse="$SCRATCH/reverse.sed"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $3 "\\b/" $2 "/g"}' "$MAP" > "$reverse"
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" && $3=="jdk.m3.algorithm" && $2 ~ /algorithms$/ {print $2; exit}' "$MAP")
progress_pkg=$(awk -F'\t' '$1=="PACKAGE" && $3=="jdk.m3.algorithm" && $2 ~ /progress$/ {print $2; exit}' "$MAP")
sed -f "$reverse" "$DRIVER_SRC" \
    | sed "s/^package jdk\.m3\.algorithm;/package $donor_pkg;\nimport $progress_pkg.ProgressMonitor;/" \
    > "$SCRATCH/driver-src/AlgorithmExtrasDump.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$SCRATCH/classes" -d "$SCRATCH/driver-classes" \
    "$SCRATCH/driver-src/AlgorithmExtrasDump.java"

# 3. the dumps: donor, relocated, relocated in the interpreter only
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes${DEP_CP:+$SEP$DEP_CP}"
timeout "$RUN_SECONDS" "$JAVA_BIN/java" $JVM_FLAGS -cp "$SCRATCH/classes$SEP$SCRATCH/driver-classes" \
    "$donor_pkg.AlgorithmExtrasDump" --seed "$SEED" --cases "$CASES" > "$SCRATCH/donor.raw.txt"
start=$(date +%s)
timeout "$RUN_SECONDS" "$JAVA_BIN/java" $JVM_FLAGS -cp "$RELOCATED_CP" jdk.m3.algorithm.AlgorithmExtrasDump \
    --seed "$SEED" --cases "$CASES" > "$SCRATCH/relocated.txt"
mid=$(date +%s)
timeout "$RUN_SECONDS" "$JAVA_BIN/java" $JVM_FLAGS -Xint -cp "$RELOCATED_CP" jdk.m3.algorithm.AlgorithmExtrasDump \
    --seed "$SEED" --cases "$CASES" > "$SCRATCH/relocated-xint.txt"
end=$(date +%s)

# 4. normalize the donor dump by the PACKAGE and TYPE rows
normalize="$SCRATCH/normalize.sed"
awk -F'\t' '$1=="PACKAGE" && $2!=$3 {gsub(/\./, "\\.", $2); print "s/" $2 "/" $3 "/g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
sed -f "$normalize" "$SCRATCH/donor.raw.txt" > "$SCRATCH/donor.txt"
types=$(awk -F'\t' '$1=="TYPE" && $2!=$3 {printf "%s%s", (n++ ? "|" : ""), $2}' "$MAP")
renamed_lines=$(grep -cE "\b($types)\b" "$SCRATCH/donor.raw.txt" || true)

donor_raw_sha=$(sha256sum "$SCRATCH/donor.raw.txt" | cut -d' ' -f1)
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
xint_sha=$(sha256sum "$SCRATCH/relocated-xint.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit sources=$(ls "$SCRATCH/src" | wc -l | tr -d ' ') classes=$donor_classes seed=$SEED cases_per_member=$CASES"
tail -n 1 "$SCRATCH/relocated.txt"
echo "MEMBERS $(cut -d'#' -f1 "$SCRATCH/relocated.txt" | grep -v '^TOTAL' | sort -u | wc -l | tr -d ' ') relocated_seconds=$((mid - start)) xint_seconds=$((end - mid))"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" && cmp -s "$SCRATCH/relocated.txt" "$SCRATCH/relocated-xint.txt"; then
    echo "DIFFERENTIAL PASS donor_raw_sha256=$donor_raw_sha donor_normalized_sha256=$donor_sha relocated_sha256=$relocated_sha xint_sha256=$xint_sha lines=$(wc -l < "$SCRATCH/relocated.txt" | tr -d ' ') bytes=$(wc -c < "$SCRATCH/relocated.txt" | tr -d ' ') normalized_lines=${renamed_lines:-0}"
    rm -f "$SCRATCH/donor.raw.txt" "$SCRATCH/relocated-xint.txt"
else
    echo "DIFFERENTIAL FAIL donor_raw_sha256=$donor_raw_sha donor_normalized_sha256=$donor_sha relocated_sha256=$relocated_sha xint_sha256=$xint_sha"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 10
    diff "$SCRATCH/relocated.txt" "$SCRATCH/relocated-xint.txt" | head -n 10
    exit 1
fi
