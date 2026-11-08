#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2 (donor side), 3 and 4 of the scoped-ngram-perplexity leaf. The donor scorer and the donor producer it reads
# (the TYPE row without a FILE row) are taken from the donor git tree at the REPO pin of RELOCATION_MAP.tsv and
# compiled together with javac --release 21 -proc:none -Xlint:all -Werror into the scratch; then
#   (a) when the JUnit console launcher is in the Maven repository, the sealed donor test (FILE row) and the develop
#       test (SKIP row) run against the donor classes and print their counts (DONOR_TEST lines);
#   (b) M3NgramPerplexityDifferentialDump (test scope of this module) replays its 2,000 seeded sentence sets through the
#       donor pair and through the relocated pair (target/classes), each also under -Xint; the four dumps must be
#       byte-identical (the dump prints no type name, so the TYPE map needs no rewrite of the output);
#   (c) javap -p of the donor scorer, with the PACKAGE and TYPE rows applied, must equal javap -p of the relocated one.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests. Donor package and type names come from the map only.
# usage: differential.sh <donor-git-dir> <scratch-dir>   (JAVA_HOME = JDK 21; MAVEN_REPO_LOCAL optional; after package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
mkdir -p "$2/src" "$2/classes" "$2/test-sealed" "$2/test-develop"
SCRATCH=$(winpath "$2")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
col() { awk -F'\t' -v k="$1" -v c="$2" -v m="${3:-}" '$1==k && (m=="" || $0 ~ m) {print $c; exit}' "$MAP"; }
pin=$(col REPO 3)
main_path=$(col FILE 2 'src/main/java/')
sealed_test=$(col FILE 2 'Test[.]java')
develop_test=$(col SKIP 2 'src/test/java/')
donor_package=$(col PACKAGE 2)
target_package=$(col PACKAGE 3)
donor_type=$(basename "$main_path" .java)
target_type=$(awk -F'\t' -v t="$donor_type" '$1=="TYPE" && $2==t {print $3; exit}' "$MAP")
# the producer: the TYPE row whose donor name is not the basename of any FILE row
read -r producer_donor producer_target < <(awk -F'\t' '$1=="FILE" {n=$2; sub(/.*\//, "", n); sub(/\..*/, "", n); f[n]=1}
    $1=="TYPE" && !($2 in f) && $2 !~ /Test$/ {print $2, $3; exit}' "$MAP")
owner_dir=$(dirname "$main_path")
git -C "$DONOR_GIT" show "$pin:$main_path" > "$SCRATCH/src/$donor_type.java"
git -C "$DONOR_GIT" show "$pin:$owner_dir/$producer_donor.java" > "$SCRATCH/src/$producer_donor.java"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" \
    "$SCRATCH/src/$donor_type.java" "$SCRATCH/src/$producer_donor.java"
echo "DONOR $pin $main_path + $owner_dir/$producer_donor.java"

repo=${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}
console=$(ls "$repo"/org/junit/platform/junit-platform-console-standalone/*/junit-platform-console-standalone-*.jar 2>/dev/null | tail -n 1 || true)
if [ -n "$console" ]; then
    for lane in sealed:$sealed_test develop:$develop_test; do
        name=${lane%%:*}; path=${lane#*:}
        git -C "$DONOR_GIT" show "$pin:$path" > "$SCRATCH/test-$name/${donor_type}Test.java"
        "$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$SCRATCH/classes$SEP$(winpath "$(dirname "$console")")/$(basename "$console")" \
            -d "$SCRATCH/test-$name" "$SCRATCH/test-$name/${donor_type}Test.java"
        "$JAVA_BIN/java" -jar "$console" execute -cp "$SCRATCH/classes$SEP$SCRATCH/test-$name" \
            --select-class "$donor_package.${donor_type}Test" --details=summary --disable-banner --disable-ansi-colors \
            < /dev/null > "$SCRATCH/donor-test-$name.txt" 2>&1 || true
        echo "DONOR_TEST $name $(grep -E 'tests (found|successful|failed)' "$SCRATCH/donor-test-$name.txt" | tr -s ' []' ' ' | tr '\n' ' ')"
    done
else
    echo "DONOR_TEST SKIP (no junit-platform-console-standalone in $repo)"
fi

dump() { "$JAVA_BIN/java" "${@:3}" -cp "$1$SEP$MODULE/target/test-classes" "$target_package.M3NgramPerplexityDifferentialDump" $2; }
dump "$SCRATCH/classes" "$donor_package.$donor_type $donor_package.$producer_donor" > "$SCRATCH/donor.txt"
dump "$MODULE/target/classes" "$target_package.$target_type $target_package.$producer_target" > "$SCRATCH/relocated.txt"
dump "$SCRATCH/classes" "$donor_package.$donor_type $donor_package.$producer_donor" -Xint > "$SCRATCH/donor-xint.txt"
dump "$MODULE/target/classes" "$target_package.$target_type $target_package.$producer_target" -Xint > "$SCRATCH/relocated-xint.txt"
status=PASS
for side in donor relocated donor-xint relocated-xint; do
    echo "DUMP $side sha256=$(sha256sum "$SCRATCH/$side.txt" | cut -d' ' -f1) bytes=$(wc -c < "$SCRATCH/$side.txt") lines=$(wc -l < "$SCRATCH/$side.txt")"
    cmp -s "$SCRATCH/donor.txt" "$SCRATCH/$side.txt" || status=FAIL
done
tail -n 1 "$SCRATCH/relocated.txt"

sed_map=$(awk -F'\t' '$1=="PACKAGE" {gsub(/[.]/, "[.]", $2); printf "s/\\b%s\\b/%s/g;", $2, $3}
    $1=="TYPE" {printf "s/\\b%s\\b/%s/g;", $2, $3}' "$MAP")
"$JAVA_BIN/javap" -p -cp "$SCRATCH/classes" "$donor_package.$donor_type" | tr -d '\r' | sed -E "$sed_map" > "$SCRATCH/javap-donor.txt"
"$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.$target_type" | tr -d '\r' > "$SCRATCH/javap-relocated.txt"
if diff "$SCRATCH/javap-donor.txt" "$SCRATCH/javap-relocated.txt" > "$SCRATCH/javap.diff"; then
    echo "JAVAP PASS members=$(grep -c ';' "$SCRATCH/javap-relocated.txt") $target_package.$target_type"
else
    echo "JAVAP FAIL $(wc -l < "$SCRATCH/javap.diff") diff lines"; status=FAIL
fi
echo "DIFFERENTIAL $status donor_sha256=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1) relocated_sha256=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)"
[ "$status" = PASS ]
