#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of the relocation: the donor leaf classes (verbatim donor blobs, written by relocate.py --donor-subset together
# with the donor oracles minus the declared DROP/REWRITE rows, compiled with javac --release 21 -Xlint:all -Werror) and
# the relocated classes (target/classes + target/test-classes of m3/collections) replay the same inputs.
# (a) The three oracle mains must print byte-identical output (PASS: <n> ... checks).
# (b) M3ImmutablePoolsDump (test scope, reflective, not a test) replays <ops> seeded random pool/canonicalizer/packed-id/
#     UUID/vector/hash-index/footprint operations through both class sets; the dumps must be byte-identical.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides. Donor package, type and oracle names come from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [seed]   (JAVA_HOME = JDK 21; run after
#        mvn -DskipTests package, which compiles the test sources)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../../../../../m3/collections")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes"
SCRATCH=$(winpath "$3")
OPS=${4:-100000}
SEED=${5:-20261106}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
PY=${PYTHON:-python}
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_monitor=$(awk -F'\t' '$1=="TYPE" && $3=="M3PoolProgressMonitor" {print $2; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
echo "DONOR $donor_commit package=$donor_package monitor=$donor_monitor"

# donor side: main files verbatim, oracles minus the declared rows (the recipe refuses a non-verbatim main file)
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --donor-subset "$SCRATCH/src" \
    > "$SCRATCH/subset.log"
grep -c "SUBSET_VERBATIM=TRUE" "$SCRATCH/subset.log" | sed 's/^/subset verbatim main files: /'
tail -n 1 "$SCRATCH/subset.log"
find "$SCRATCH/src" -name '*.java' | sort > "$SCRATCH/sources.txt"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" @"$SCRATCH/sources.txt"
echo "javac donor sources: $(wc -l < "$SCRATCH/sources.txt") files, classes in $SCRATCH/classes"

# (a) the oracles
: > "$SCRATCH/donor-oracles.txt"
: > "$SCRATCH/relocated-oracles.txt"
awk -F'\t' '$1=="FILE" && $3 ~ /src\/test\/java\// {print $2}' "$MAP" | while read -r oracle_path; do
    donor_oracle=$(basename "$oracle_path" .java)
    target_oracle=$(awk -F'\t' -v d="$donor_oracle" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
    "$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_package.$donor_oracle" >> "$SCRATCH/donor-oracles.txt"
    "$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.$target_oracle" \
        >> "$SCRATCH/relocated-oracles.txt"
done

# (b) the reflective dump: same driver, two class sets
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$MODULE/target/test-classes" "$target_package.M3ImmutablePoolsDump" \
    "$donor_package" "" "$donor_monitor" "$OPS" "$SEED" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "$target_package.M3ImmutablePoolsDump" \
    "$target_package" M3 M3PoolProgressMonitor "$OPS" "$SEED" > "$SCRATCH/relocated-dump.txt"

status=PASS
for pair in oracles dump; do
    donor_sha=$(sha256sum "$SCRATCH/donor-$pair.txt" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$SCRATCH/relocated-$pair.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/donor-$pair.txt" "$SCRATCH/relocated-$pair.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$pair $verdict donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor-$pair.txt") lines=$(wc -l < "$SCRATCH/donor-$pair.txt")"
done
tr '\n' ' ' < "$SCRATCH/relocated-oracles.txt"; echo
tail -n 1 "$SCRATCH/relocated-dump.txt"
grep -c '^[0-9]* refuse ' "$SCRATCH/relocated-dump.txt" | sed 's/^/refusal lines: /'
grep -c '!' "$SCRATCH/relocated-dump.txt" | sed 's/^/lines carrying a thrown exception: /'
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
