#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of the relocation: the overlap agreement table. Every donor member the census (METHOD_CENSUS.tsv) marks
# OVERLAP_LANDED or OVERLAP_OTHER_OWNER runs next to its owner on the same seeded fixtures (OverlapAgreement.java, one
# JVM): the whole donor catalogue (21 sources + the progress contract, javac --release 21 -Xlint:all -Werror), the
# landed jdk.m3.algorithm classes (target/classes; byte-identical to the master build by gate 7), jdk.m3.collections
# (DEP_CP), the cpu-tools owners (compiled from m3/tooling/module-packs/cpu-tools) and the java.base owner
# jdk.internal.m3.M3IntAlgorithms (compiled from this tree's src/java.base and patched into java.base). Writes
# OVERLAP.tsv into the recipe directory (deterministic: same seed, same table) and prints its digest.
# usage: overlap.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; after mvn package)
# env:   DEP_CP = the m3-collections jar (; on Windows, : elsewhere); RUN_SECONDS (default 120)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
RUN_SECONDS=${RUN_SECONDS:-120}
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CPU="$MODULE/../tooling/module-packs/cpu-tools/src"
rm -rf "$SCRATCH/overlap"
mkdir -p "$SCRATCH/overlap/donor-src" "$SCRATCH/overlap/donor" "$SCRATCH/overlap/cpu" "$SCRATCH/overlap/javabase-src/jdk/internal/m3" "$SCRATCH/overlap/javabase" "$SCRATCH/overlap/driver"

# 1. the whole donor catalogue (the directory of the FILE rows) and its progress contract (the OWNED row); donor paths
#    and packages come from RELOCATION_MAP.tsv only, so no donor package name is spelled in this script
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_dir=$(awk -F'\t' '$1=="FILE" {sub(/[^\/]*$/, "", $2); print $2; exit}' "$MAP")
progress_path=$(awk -F'\t' '$1=="OWNED" {print $2; exit}' "$MAP")
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" && $2 ~ /algorithms$/ {print $2; exit}' "$MAP")
git -C "$DONOR_GIT" ls-tree --name-only "$REV" "$donor_dir" \
    | while read -r path; do git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/overlap/donor-src/$(basename "$path")"; done
git -C "$DONOR_GIT" show "$REV:$progress_path" > "$SCRATCH/overlap/donor-src/$(basename "$progress_path")"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/overlap/donor" "$SCRATCH"/overlap/donor-src/*.java

# 2. the cpu-tools owners and the java.base owner
"$JAVA_BIN/javac" --release 21 -Xlint:all -d "$SCRATCH/overlap/cpu" \
    "$CPU/jdk.m3.tools.cpu/jdk/m3/tools/cpu/M3ArrayGraph.java" "$CPU/jdk.m3.tools.cpu/jdk/m3/tools/cpu/M3BipartiteMatching.java" \
    "$CPU/jdk.m3.tools.cpu/jdk/m3/tools/cpu/M3SuffixArray.java" "$CPU/jdk.m3.tools.cpu/jdk/m3/tools/cpu/M3KnapsackSelection.java"
git -C "$MODULE" show HEAD:src/java.base/share/classes/jdk/internal/m3/M3IntAlgorithms.java \
    > "$SCRATCH/overlap/javabase-src/jdk/internal/m3/M3IntAlgorithms.java"
"$JAVA_BIN/javac" --patch-module "java.base=$SCRATCH/overlap/javabase-src" -d "$SCRATCH/overlap/javabase" \
    "$SCRATCH/overlap/javabase-src/jdk/internal/m3/M3IntAlgorithms.java"

# 3. the driver, then the table
CP="$SCRATCH/overlap/donor$SEP$MODULE/target/classes$SEP$SCRATCH/overlap/cpu${DEP_CP:+$SEP$DEP_CP}"
mkdir -p "$SCRATCH/overlap/driver-src"
sed "s/^import DONOR_PACKAGE\./import $donor_pkg./" "$RECIPE_DIR/OverlapAgreement.java" > "$SCRATCH/overlap/driver-src/OverlapAgreement.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all,-processing -Werror -cp "$CP" -d "$SCRATCH/overlap/driver" \
    "$SCRATCH/overlap/driver-src/OverlapAgreement.java"
timeout "$RUN_SECONDS" "$JAVA_BIN/java" --patch-module "java.base=$SCRATCH/overlap/javabase" \
    --add-exports java.base/jdk.internal.m3=ALL-UNNAMED -XX:-OmitStackTraceInFastThrow \
    -cp "$SCRATCH/overlap/driver$SEP$CP" OverlapAgreement > "$SCRATCH/overlap/OVERLAP.tsv"
{
    echo "# SPDX-License-Identifier: Apache-2.0"
    echo "# Overlap agreement table of recipe/algorithm-extras-20261106 (gate 4), written by overlap.sh from OverlapAgreement.java (seed 20261106): donor member vs its owner on the same fixtures; AGREE = equal results or the same exception class, EQUIVALENT = different but both valid answers, DISAGREE = an NEC observation (the owner's body wins; see METHOD_CENSUS.tsv)."
    cat "$SCRATCH/overlap/OVERLAP.tsv"
} > "$RECIPE_DIR/OVERLAP.tsv"
rows=$(grep -vc '^#' "$RECIPE_DIR/OVERLAP.tsv")
cases=$(awk -F'\t' '!/^#/ && NR>3 {s+=$3} END {print s+0}' "$RECIPE_DIR/OVERLAP.tsv")
agree=$(awk -F'\t' '!/^#/ && NR>3 {s+=$4} END {print s+0}' "$RECIPE_DIR/OVERLAP.tsv")
equivalent=$(awk -F'\t' '!/^#/ && NR>3 {s+=$5} END {print s+0}' "$RECIPE_DIR/OVERLAP.tsv")
disagree=$(awk -F'\t' '!/^#/ && NR>3 {s+=$6} END {print s+0}' "$RECIPE_DIR/OVERLAP.tsv")
members=$(awk -F'\t' '!/^#/ && NR>3 && $6>0' "$RECIPE_DIR/OVERLAP.tsv" | wc -l | tr -d ' ')
echo "OVERLAP rows=$((rows - 1)) cases=$cases agree=$agree equivalent=$equivalent disagree=$disagree rows_with_disagreement=$members sha256=$(sha256sum "$RECIPE_DIR/OVERLAP.tsv" | cut -d' ' -f1)"
