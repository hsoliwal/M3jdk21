#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation (Java path): the donor bit matrix closure and its oracle, compiled from the donor git tree
# with javac --release 21 -Xlint:all -Werror (-sourcepath resolves the donor closure: the int list view, its plan record
# and storage enum), and the relocated classes (target/classes of m3/arrays) replay the same inputs.
# (a) The relocated Java oracle main and its donor must print byte-identical output (JAVA lane: scenarios, assertions).
# (b) M3BitMatrixDump (test scope) replays the oracle fixtures plus 100,000 seeded random matrices (shapes up to
#     4,096 x 4,096 bits) through both classes; the dumps must be byte-identical.
# It also leaves the donor kernel, the held CSR kernel and the donor closure in the scratch for native-gate.sh (a Linux
# host need not reach the donor git directory). Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides.
# The donor package and type names come from RELOCATION_MAP.tsv next to this script, so no file in the naming scope
# names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [random-rounds]   (JAVA_HOME = JDK 21; after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
ROUNDS=${4:-100000}
mkdir -p "$3/src" "$3/classes" "$3/out" "$3/native"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
# Every Java FILE row of the map plus the donor closure of the list view (the list itself, its plan record and storage enum).
mapfile -t donor_files < <(awk -F'\t' '$1=="FILE" && $2 ~ /\.java$/ {print $2}' "$MAP")
owner_dir=$(dirname "${donor_files[0]}")
closure=("${donor_files[@]}" "$owner_dir/MatIntList.java" "$owner_dir/MatCollectionPlan.java" "$owner_dir/MatStorageKind.java")
for path in "${closure[@]}"; do
    pkg_dir="$SCRATCH/src/$(dirname "$path" | sed 's#.*/src/\(main\|test\)/java/##')"
    mkdir -p "$pkg_dir"
    git -C "$DONOR_GIT" show "$REV:$path" > "$pkg_dir/$(basename "$path")"
done
# The C FILE row (the kernel) and the held CSR kernel (NEC row), for native-gate.sh.
donor_c=$(awk -F'\t' '$1=="FILE" && $2 ~ /\.c$/ {print $2; exit}' "$MAP")
git -C "$DONOR_GIT" show "$REV:$donor_c" > "$SCRATCH/native/kernel.c"
held_c=$(awk -F'\t' '$1=="NEC" && $2 ~ /\.c$/ {print $2; exit}' "$MAP")
[ -n "$held_c" ] && git -C "$DONOR_GIT" show "$REV:$held_c" > "$SCRATCH/native/held-csr.c"
echo "DONOR $donor_commit files=${#closure[@]} kernel=$donor_c held=$held_c"
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
oracles=(MatBitMatrixConvergenceContract)
for oracle in "${oracles[@]}"; do
    read -r donor_fqn relocated_fqn < <(fqn "$oracle")
    pair "$oracle" "$donor_fqn" "$relocated_fqn"
done
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3BitMatrixDump" "$donor_owner" "$ROUNDS" > "$SCRATCH/out/donor-dump.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.M3BitMatrixDump" "$relocated_owner" "$ROUNDS" > "$SCRATCH/out/relocated-dump.txt"
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
