#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Phase A of the leaf (pattern index onto M3AtomCensus), reproduced: the donor pattern index over the donor atomizer
# and the census semantic census, run over the same bounded corpus (the m3/collections + m3/algorithms Java sources
# of this checkout's HEAD), then joined by source span. The donor index cannot be rebuilt over M3AtomDescriptor (NEC):
# this script prints the evidence of PHASE_A_NEC.tsv (atom and group counts, sha256 of the sorted pattern groups).
#   donor: the NEC/OMIT rows of RELOCATION_MAP.tsv (the atomizer closure + the index) compiled from the donor git tree
#          with javac --release 21 -sourcepath; phase-a/DonorPatternDump.java drives it reflectively (the donor
#          package comes from the map, so no file in the naming scope names it).
#   census: m3/tooling/atom-census/src/main/java compiled as is; phase-a/CensusPerFile.java runs its CLI --semantic
#          once per file (a refused file does not hide the others).
# usage: JAVA_HOME=<JDK 21> phase-a-nec.sh <donor-git-dir> <donor-rev> <scratch-dir>
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
ROOT=$(winpath "$(dirname "$0")/../../../../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
rm -rf "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/census-classes" "$SCRATCH/corpus" "$SCRATCH/donor-out" "$SCRATCH/census-out"
mkdir -p "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/census-classes" "$SCRATCH/corpus"
mapfile -t closure < <(awk -F'\t' '($1=="NEC" || $1=="OMIT") && $2 ~ /\/M3[A-Za-z]+\.java$/ && $2 !~ /UniversalRecipe/ {print $2}' "$MAP")
index_path=$(awk -F'\t' '$1=="NEC" && $2 ~ /M3AtomPatternIndex\.java$/ {print $2; exit}' "$MAP")
package=$(dirname "$index_path" | sed 's#.*/src/main/java/##; s#/#.#g')
for path in "${closure[@]}"; do
    dir="$SCRATCH/donor-src/$(dirname "$path" | sed 's#.*/src/main/java/##')"
    mkdir -p "$dir"
    git -C "$DONOR_GIT" show "$REV:$path" > "$dir/$(basename "$path")"
done
echo "DONOR $(git -C "$DONOR_GIT" rev-parse "$REV^{commit}") closure=${#closure[@]} package=$package"
"$JAVA_BIN/javac" --release 21 -proc:none -nowarn -d "$SCRATCH/donor-classes" -sourcepath "$SCRATCH/donor-src" \
    $(find "$SCRATCH/donor-src" -name '*.java')
"$JAVA_BIN/javac" --release 21 -d "$SCRATCH/donor-classes" "$RECIPE_DIR/phase-a/DonorPatternDump.java"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/census-classes" \
    $(find "$ROOT/m3/tooling/atom-census/src/main/java" -name '*.java')
"$JAVA_BIN/javac" --release 21 -d "$SCRATCH/census-classes" "$RECIPE_DIR/phase-a/CensusPerFile.java"
git -C "$ROOT" archive HEAD m3/collections/src/main/java m3/algorithms/src/main/java | tar -x -C "$SCRATCH/corpus"
echo "CORPUS $(git -C "$ROOT" rev-parse HEAD) files=$(find "$SCRATCH/corpus" -name '*.java' | wc -l)"
(cd "$SCRATCH" && "$JAVA_BIN/java" -cp donor-classes DonorPatternDump "$package" corpus donor-out \
    m3/collections/src/main/java m3/algorithms/src/main/java)
(cd "$SCRATCH" && "$JAVA_BIN/java" -cp census-classes CensusPerFile "$ROOT/m3/tooling/atom-census/PATTERN_RULES.tsv" \
    corpus census-out m3/collections/src/main/java m3/algorithms/src/main/java)
echo "CENSUS_REFUSAL_KINDS $(cut -f3 "$SCRATCH/census-out/CENSUS_REFUSALS.tsv" | sed 's/:.*//' | sort | uniq -c | tr -s ' ' | tr '\n' ';')"
python -I "$RECIPE_DIR/phase-a/nec_join.py" "$SCRATCH"
echo "PHASE_A NEC (the projection-key pattern index needs contract/structure/logic roots that M3AtomDescriptor does not carry)"
