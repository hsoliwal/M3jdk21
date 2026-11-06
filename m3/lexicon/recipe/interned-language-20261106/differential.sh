#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor substrate (TokenId, PackedText, TokenRope, OverflowInterner, SharedSymbols,
# compiled from the donor git tree with javac --release 21 -Xlint:all -Werror) and the relocated substrate
# (receiver/target/classes) replay the same seeded operations through the reflective driver M3InternedLanguageDump
# (test scope of the receiver module); the donor dump, normalized by the PACKAGE and TYPE rows of RELOCATION_MAP.tsv
# (type names inside messages, if any), must be byte-identical to the relocated dump. Also compares javap -p of every
# donor class (names normalized by the same rows) with the relocated class. Prints DIFFERENTIAL PASS|FAIL with digests.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [cases-per-label]   (JAVA_HOME = JDK 21; after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
RECEIVER="$MODULE/receiver"
DONOR_GIT=$1
REV=$2
CASES=${4:-100000}
SEED=20261106
mkdir -p "$3/src" "$3/classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
DRIVER=jdk.m3.lexicon.M3InternedLanguageDump
# the JIT replaces a hot implicit exception (here the out-of-bounds token(index) and entry(id) probes) by a
# preallocated one without message or trace, at a point that depends on the run: keep the messages deterministic
JVM_FLAGS="-XX:-OmitStackTraceInFastThrow"
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")

# 1. the donor main sources (FILE rows whose target lies under receiver/src/main/java), compiled from the donor tree
rm -rf "$SCRATCH/src" "$SCRATCH/classes" "$SCRATCH/javap"
mkdir -p "$SCRATCH/src" "$SCRATCH/classes" "$SCRATCH/javap"
awk -F'\t' '$1=="FILE" && $3 ~ /^receiver\/src\/main\/java\// {print $2}' "$MAP" | while read -r path; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"
done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
donor_classes=$(find "$SCRATCH/classes" -name '*.class' | wc -l | tr -d ' ')

# 2. label=class pairs: the label is the target type name on both sides, the class the donor or the relocated FQN
donor_args=()
target_args=()
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = TYPE ] || continue
    path=$(awk -F'\t' -v t="/$donor.java" '$1=="FILE" && $3 ~ /^receiver\/src\/main\/java\// && index($2, t) {print $2; exit}' "$MAP")
    [ -n "$path" ] || continue
    pkg=$(dirname "$path" | sed -E 's#^(.*/)?src/main/java/##; s#/#.#g')
    tpkg=$(awk -F'\t' -v p="$pkg" '$1=="PACKAGE" && $2==p {print $3; exit}' "$MAP")
    donor_args+=("$target=$pkg.$donor")
    target_args+=("$target=$tpkg.$target")
done < "$MAP"

# 3. the two dumps
"$JAVA_BIN/java" $JVM_FLAGS -cp "$SCRATCH/classes$SEP$RECEIVER/target/test-classes" "$DRIVER" --cases "$CASES" --seed "$SEED" \
    "${donor_args[@]}" > "$SCRATCH/donor.raw.txt"
"$JAVA_BIN/java" $JVM_FLAGS -cp "$RECEIVER/target/classes$SEP$RECEIVER/target/test-classes" "$DRIVER" --cases "$CASES" --seed "$SEED" \
    "${target_args[@]}" > "$SCRATCH/relocated.txt"

# 4. normalize the donor dump by the map: PACKAGE and TYPE rows (names inside messages, if any)
normalize="$SCRATCH/normalize.sed"
awk -F'\t' '$1=="PACKAGE" && $2!=$3 {gsub(/\./, "\\.", $2); print "s/" $2 "/" $3 "/g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
sed -f "$normalize" "$SCRATCH/donor.raw.txt" > "$SCRATCH/donor.txt"
types=$(awk -F'\t' '$1=="TYPE" && $2!=$3 {printf "%s%s", (n++ ? "|" : ""), $2}' "$MAP")
renamed_lines=$(grep -cE "\b($types)\b" "$SCRATCH/donor.raw.txt" || true)

# 5. javap -p parity: every donor class (names normalized by the map) against the relocated class
javap_same=0
javap_total=0
javap_diff=""
for class in "$SCRATCH"/classes/jdk/internal/m3/m3scale/intern/*.class; do
    name=$(basename "$class" .class)
    outer=${name%%\$*}
    rest=${name#"$outer"}
    target=$(awk -F'\t' -v d="$outer" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/classes" "jdk.internal.m3.m3scale.intern.$name" | sed -f "$normalize" | grep -v '^Compiled from' > "$SCRATCH/javap/$target$rest.donor.txt"
    "$JAVA_BIN/javap" -p -cp "$RECEIVER/target/classes" "jdk.m3.lexicon.$target$rest" | grep -v '^Compiled from' > "$SCRATCH/javap/$target$rest.relocated.txt"
    javap_total=$((javap_total + 1))
    if cmp -s "$SCRATCH/javap/$target$rest.donor.txt" "$SCRATCH/javap/$target$rest.relocated.txt"; then
        javap_same=$((javap_same + 1))
    else
        javap_diff="$javap_diff $target$rest"
    fi
done

donor_raw_sha=$(sha256sum "$SCRATCH/donor.raw.txt" | cut -d' ' -f1)
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
echo "DONOR $donor_commit sources=$(ls "$SCRATCH/src" | wc -l | tr -d ' ') classes=$donor_classes labels=${#donor_args[@]}"
echo "JAVAP identical=$javap_same/$javap_total${javap_diff:+ differing:$javap_diff}"
tail -n 1 "$SCRATCH/relocated.txt"
if cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" && [ "$javap_same" = "$javap_total" ]; then
    echo "DIFFERENTIAL PASS donor_raw_sha256=$donor_raw_sha donor_normalized_sha256=$donor_sha relocated_sha256=$relocated_sha lines=$(wc -l < "$SCRATCH/relocated.txt" | tr -d ' ') bytes=$(wc -c < "$SCRATCH/relocated.txt" | tr -d ' ') normalized_lines=${renamed_lines:-0}"
    rm -f "$SCRATCH/donor.raw.txt"
else
    echo "DIFFERENTIAL FAIL donor_raw_sha256=$donor_raw_sha donor_normalized_sha256=$donor_sha relocated_sha256=$relocated_sha"
    diff "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" | head -n 20
    exit 1
fi
