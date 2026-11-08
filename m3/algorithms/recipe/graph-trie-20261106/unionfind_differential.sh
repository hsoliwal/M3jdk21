#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Owner check 1 of the relocation: the donor IntUnionFind (SKIP row, compiled from the donor git tree with its monitor
# closure, javac --release 21 -Xlint:all -Werror) against the M3JDK owner jdk.m3.collections.M3IntDisjointSet (the
# m3-collections jar). UnionFindPartitionDump replays the same seeded union / connected / find / componentSize /
# componentCount sequences on both (sizes 0..100,000) and writes one dump per side with every partition-level
# observation and a canonical-labelling sha256 per sequence, never a representative; the two dumps must be
# byte-identical. Representatives are only counted (the structures compress paths differently). Prints
# UNIONFIND PASS|FAIL with the two sha256 digests and the program's summary line.
# usage: unionfind_differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops]   (JAVA_HOME = JDK 21)
# env:   DEP_CP = the m3-collections jar (and its dependencies), joined with ; on Windows and : elsewhere
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
DONOR_GIT=$1
REV=$2
OPS=${4:-1000000}
SEED=20261106
mkdir -p "$3/uf-src" "$3/uf-classes" "$3/uf-dump"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
: "${DEP_CP:?DEP_CP must name the m3-collections jar}"
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")

# 1. the donor union-find (the SKIP row naming IntUnionFind) and its closure (OWNED rows), compiled from the donor tree
rm -rf "$SCRATCH/uf-src" "$SCRATCH/uf-classes" "$SCRATCH/uf-dump"
mkdir -p "$SCRATCH/uf-src" "$SCRATCH/uf-classes" "$SCRATCH/uf-dump"
uf_path=$(awk -F'\t' '$1=="SKIP" && $2 ~ /IntUnionFind\.java$/ {print $2; exit}' "$MAP")
monitor_path=$(awk -F'\t' '$1=="OWNED" && $2 ~ /ProgressMonitor\.java$/ {print $2; exit}' "$MAP")
for path in "$uf_path" "$monitor_path"; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/uf-src/$(basename "$path")"
done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/uf-classes" "$SCRATCH"/uf-src/*.java
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$DEP_CP" -d "$SCRATCH/uf-classes" "$RECIPE_DIR/UnionFindPartitionDump.java"
donor_fqn=$(dirname "$uf_path" | sed -E 's#^(.*/)?src/main/java/##; s#/#.#g').IntUnionFind
monitor_fqn=$(dirname "$monitor_path" | sed -E 's#^(.*/)?src/main/java/##; s#/#.#g').ProgressMonitor
donor_blob=$(git -C "$DONOR_GIT" rev-parse "$REV:$uf_path")

# 2. one program, both structures, two dumps
summary=$("$JAVA_BIN/java" -cp "$SCRATCH/uf-classes$SEP$DEP_CP" UnionFindPartitionDump --ops "$OPS" --seed "$SEED" \
    --out "$SCRATCH/uf-dump" "Donor=$donor_fqn" "Monitor=$monitor_fqn")
donor_sha=$(sha256sum "$SCRATCH/uf-dump/donor.txt" | cut -d' ' -f1)
owner_sha=$(sha256sum "$SCRATCH/uf-dump/owner.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit IntUnionFind blob $donor_blob vs owner jdk.m3.collections.M3IntDisjointSet ($DEP_CP)"
echo "$summary"
if cmp -s "$SCRATCH/uf-dump/donor.txt" "$SCRATCH/uf-dump/owner.txt"; then
    echo "UNIONFIND PASS donor_sha256=$donor_sha owner_sha256=$owner_sha lines=$(wc -l < "$SCRATCH/uf-dump/donor.txt" | tr -d ' ') bytes=$(wc -c < "$SCRATCH/uf-dump/donor.txt" | tr -d ' ')"
else
    echo "UNIONFIND FAIL donor_sha256=$donor_sha owner_sha256=$owner_sha"
    diff "$SCRATCH/uf-dump/donor.txt" "$SCRATCH/uf-dump/owner.txt" | head -n 20
    exit 1
fi
