#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation (Java path): the donor posting index closure and its oracles, compiled from the donor git
# tree with javac --release 21 -Xlint:all -Werror (-sourcepath resolves the donor closure: selection, bitmap, list,
# plan record and storage enum), and the relocated classes (target/classes of m3/arrays) replay the same inputs.
# (a) Every relocated oracle main and its donor must print byte-identical output (JAVA_ONLY lanes: the galloping
#     digest, the skew suite, the convergence, cardinality and bounded-count figures).
# (b) M3IntPostingIndexDump (test scope) replays the oracle fixtures plus 100,000 seeded random posting lists through
#     both classes (any/all lanes, counts, bounded counts, probes, rows(key)); the dumps must be byte-identical.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides. The donor package and type names come from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes" "$3/out"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
# Every Java FILE row of the map plus the donor closure of the list view (its plan record and storage enum).
mapfile -t donor_files < <(awk -F'\t' '$1=="FILE" && $2 ~ /\.java$/ {print $2}' "$MAP")
owner_dir=$(dirname "${donor_files[0]}")
closure=("${donor_files[@]}" "$owner_dir/MatCollectionPlan.java" "$owner_dir/MatStorageKind.java")
for path in "${closure[@]}"; do
    pkg_dir="$SCRATCH/src/$(dirname "$path" | sed 's#.*/src/\(main\|test\)/java/##')"
    mkdir -p "$pkg_dir"
    git -C "$DONOR_GIT" show "$REV:$path" > "$pkg_dir/$(basename "$path")"
done
# The C FILE row (the retained-variant kernel) and the develop main-path kernel, for native-gate.sh (a Linux host
# need not reach this git directory).
mkdir -p "$SCRATCH/native"
donor_c=$(awk -F'\t' '$1=="FILE" && $2 ~ /\.c$/ {print $2; exit}' "$MAP")
git -C "$DONOR_GIT" show "$REV:$donor_c" > "$SCRATCH/native/kernel.c"
git -C "$DONOR_GIT" show "$REV:$(echo "$donor_c" | sed 's#^make/m3/qualification/[^/]*/retained-variants/[^/]*/##')" > "$SCRATCH/native/mainpath.c"
echo "DONOR $donor_commit files=${#closure[@]} kernel=$donor_c"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" -sourcepath "$SCRATCH/src" $(find "$SCRATCH/src" -name '*.java')
pair() { # label donor-fqn relocated-fqn [args]
    local label=$1 donor_fqn=$2 relocated_fqn=$3; shift 3
    "$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_fqn" "$@" > "$SCRATCH/out/donor-$label.txt"
    "$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$relocated_fqn" "$@" > "$SCRATCH/out/relocated-$label.txt"
}
fqn() { # donor simple name -> "donor_fqn relocated_fqn" from the FILE and TYPE rows
    local donor_simple=$1
    local donor_path donor_pkg target_simple
    donor_path=$(awk -F'\t' -v s="/$donor_simple.java" '$1=="FILE" && index($2, s) {print $2; exit}' "$MAP")
    donor_pkg=$(dirname "$donor_path" | sed 's#.*/src/\(main\|test\)/java/##; s#/#.#g')
    target_simple=$(awk -F'\t' -v d="$donor_simple" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
    echo "$donor_pkg.$donor_simple $target_package.$target_simple"
}
owner_simple=$(basename "${donor_files[0]}" .java)
read -r donor_owner relocated_owner < <(fqn "$owner_simple")
oracles=(MatPostingGallopingContract MatPostingSkewSearchContract MatPostingIndexConvergenceContract MatPostingCardinalityContract MatPostingBoundedCountContract)
for oracle in "${oracles[@]}"; do
    read -r donor_fqn relocated_fqn < <(fqn "$oracle")
    pair "$oracle" "$donor_fqn" "$relocated_fqn"
done
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3IntPostingIndexDump" "$donor_owner" > "$SCRATCH/out/donor-dump.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.M3IntPostingIndexDump" "$relocated_owner" > "$SCRATCH/out/relocated-dump.txt"
status=PASS
for label in "${oracles[@]}" dump; do
    donor_sha=$(sha256sum "$SCRATCH/out/donor-$label.txt" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$SCRATCH/out/relocated-$label.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/out/donor-$label.txt" "$SCRATCH/out/relocated-$label.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$label $verdict donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/out/donor-$label.txt") lines=$(wc -l < "$SCRATCH/out/donor-$label.txt")"
done
for label in "${oracles[@]}"; do tr '\n' ' ' < "$SCRATCH/out/relocated-$label.txt"; echo; done
tail -n 1 "$SCRATCH/out/relocated-dump.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
