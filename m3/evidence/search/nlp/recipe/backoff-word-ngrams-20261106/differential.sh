#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2 (figures, witnesses, mutants), 3 and 4 of the backoff-word-ngrams leaf. Everything donor-side comes from the
# donor git tree at the REPO pin of RELOCATION_MAP.tsv (package and type names from the map only):
#   (a) donor lane: the owner template (FILE row 1) compiled with the donor oracles (the other Java FILE rows) by
#       javac --release 21 -proc:none -Xlint:all -Werror; each oracle mode runs in its own JVM;
#   (b) relocated lane: the same modes on target/classes + target/test-classes; the PASS lines (checks, digests) of
#       every behavior mode must be identical between the lanes (ORACLE lines);
#   (c) red witness: the frozen preimage (first SKIP row naming before/) with the donor rank oracle --allocation must
#       fail with QUERY_SORT_ALLOCATION (the donor's 220,736 B/query side); the relocated lane must pass;
#   (d) -Xint: the relocated rank oracle --long-counts and both growth oracles under -Xint;
#   (e) mutants.py variants of the relocated source: both fault variants PASS their growth oracles, the five mutants
#       (eager-publish, double-compare, root-backoff, stale-child, unbalanced) FAIL with the donor's messages;
#   (f) M3BackoffWordNgramsDifferentialDump: donor, relocated, donor -Xint, relocated -Xint byte-identical;
#   (g) javap -p of every donor owner class (nested included), PACKAGE and TYPE rows applied, equals the relocated one.
# Prints DIFFERENTIAL PASS|FAIL. usage: differential.sh <donor-git-dir> <scratch-dir>   (JAVA_HOME = JDK 21; after package)
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
rm -rf "$2"; mkdir -p "$2/donor-src" "$2/donor" "$2/before" "$2/mutants" "$2/out"
SCRATCH=$(winpath "$2")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
PY=${PYTHON:-python}
status=PASS
fail() { echo "FAIL $*"; status=FAIL; }
pin=$(awk -F'\t' '$1=="REPO" {print $3; exit}' "$MAP")
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
before_path=$(awk -F'\t' '$1=="SKIP" && $2 ~ /before\/[A-Za-z]+[.]java[.]template$/ && $2 !~ /module-info/ {print $2; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" && $2!="-" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" && $2!="-" {print $3; exit}' "$MAP")
target_of() { awk -F'\t' -v t="$1" '$1=="TYPE" && $2==t {print $3; exit}' "$MAP"; }
donor_owner=$(basename "$owner_path" .java.template)
target_owner=$(target_of "$donor_owner")
mapfile -t oracle_paths < <(awk -F'\t' '$1=="FILE" && $3 ~ /src\/test\/java\// {print $2}' "$MAP")
pkg_dir="$SCRATCH/donor-src/$(echo "$donor_package" | tr . /)"
mkdir -p "$pkg_dir"
git -C "$DONOR_GIT" show "$pin:$owner_path" > "$pkg_dir/$donor_owner.java"
sources=("$pkg_dir/$donor_owner.java")
for path in "${oracle_paths[@]}"; do
    name=$(basename "$path")
    if git -C "$DONOR_GIT" show "$pin:$path" | grep -q '^package '; then
        git -C "$DONOR_GIT" show "$pin:$path" > "$pkg_dir/$name"; sources+=("$pkg_dir/$name")
    else
        git -C "$DONOR_GIT" show "$pin:$path" > "$SCRATCH/donor-src/$name"; sources+=("$SCRATCH/donor-src/$name")
    fi
done
# The fault oracles and helpers belong to the fault variants (e); the donor lane compiles owner + behavior oracles.
behavior=(); for s in "${sources[@]}"; do case "$s" in *Fault.java|*GrowthOracle.java) ;; *) behavior+=("$s") ;; esac; done
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/donor" "${behavior[@]}" || fail "donor compile"
echo "DONOR $pin $owner_path + ${#behavior[@]} sources"

# (a)+(b) oracle modes: donor class (default package) vs relocated class (target package)
modes=("WordRankOracle:" "WordRankOracle:--long-counts" "WordRankOracle:--allocation" "WordContextOracle:"
       "WordContextOracle:--allocation" "WordContextOracle:--layout" "WordTrainingOracle:--behavior"
       "WordTrainingOracle:--layout" "WordTrainingOracle:--overflow" "WordTrainingOracle:--allocation"
       "WordTrainingOracle:--public-allocation" "TrainingContextIndexOracle:" "TrainingContextIndexOracle:--allocation"
       "TrainingContextIndexOracle:--layout")
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
for entry in "${modes[@]}"; do
    oracle=${entry%%:*}; mode=${entry#*:}; tag="$oracle${mode:-:main}"
    "$JAVA_BIN/java" -Xmx768m -cp "$SCRATCH/donor" "$oracle" $mode > "$SCRATCH/out/donor-$tag.txt" 2>&1 || fail "donor $tag"
    "$JAVA_BIN/java" -Xmx768m -cp "$RELOCATED_CP" "$target_package.$(target_of "$oracle")" $mode > "$SCRATCH/out/relocated-$tag.txt" 2>&1 || fail "relocated $tag"
    d=$(tail -n 1 "$SCRATCH/out/donor-$tag.txt"); r=$(tail -n 1 "$SCRATCH/out/relocated-$tag.txt")
    case "$mode" in
        --allocation|--public-allocation) echo "ORACLE $tag donor=[$d] relocated=[$r]" ;;
        *) [ "$d" = "$r" ] && echo "ORACLE $tag IDENTICAL [$r]" || fail "ORACLE $tag donor=[$d] relocated=[$r]" ;;
    esac
done

echo "PHASE_END seconds=$SECONDS"
# (c) red witness of the frozen preimage
git -C "$DONOR_GIT" show "$pin:$before_path" > "$SCRATCH/before/$donor_owner.java"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/before" "$SCRATCH/before/$donor_owner.java" \
    "$SCRATCH/donor-src/WordRankOracle.java" || fail "preimage compile"
if "$JAVA_BIN/java" -Xmx768m -cp "$SCRATCH/before" WordRankOracle --allocation > "$SCRATCH/out/before-allocation.txt" 2>&1; then
    fail "preimage allocation witness passed"
else
    echo "RED_WITNESS preimage $(grep -o 'WORD_RANK_ALLOCATION.*' "$SCRATCH/out/before-allocation.txt") $(grep -o 'QUERY_SORT_ALLOCATION: [0-9]*' "$SCRATCH/out/before-allocation.txt")"
fi

echo "PHASE_END seconds=$SECONDS"
# (d) interpreter-only runs
"$JAVA_BIN/java" -Xint -Xmx768m -cp "$RELOCATED_CP" "$target_package.$(target_of WordRankOracle)" --long-counts > "$SCRATCH/out/xint-long.txt" 2>&1 \
    && echo "XINT long-counts $(tail -n 1 "$SCRATCH/out/xint-long.txt")" || fail "xint long-counts"

echo "PHASE_END seconds=$SECONDS"
# (e) fault variants and mutants of the relocated source
"$PY" -I "$RECIPE_DIR/mutants.py" "$MODULE/src/main/java/$(echo "$target_package" | tr . /)/$target_owner.java" "$SCRATCH/mutants" > "$SCRATCH/out/mutants.txt" || fail "mutants.py"
test_src="$MODULE/src/test/java/$(echo "$target_package" | tr . /)"
variant() {  # variant <name> <oracle donor type> <args> <expect PASS|FAIL> [needle]
    local dir="$SCRATCH/mutants/$1" oracle; oracle=$(target_of "$2")
    "$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$dir/classes" "$dir/$target_owner.java" \
        "$test_src/$oracle.java" "$test_src/$(target_of TrainingArrayFault).java" "$test_src/$(target_of TrainingContextArrayFault).java" \
        || { fail "compile $1"; return; }
    local verdict=PASS
    "$JAVA_BIN/java" ${XINT:-} -Xmx768m -cp "$dir/classes" "$target_package.$oracle" $3 > "$SCRATCH/out/mutant-$1${XINT:-}.txt" 2>&1 || verdict=FAIL
    if [ "$verdict" != "$4" ]; then fail "VARIANT $1 ${XINT:-} expected $4 got $verdict"; return; fi
    if [ -n "${5:-}" ] && ! grep -q -- "$5" "$SCRATCH/out/mutant-$1${XINT:-}.txt"; then fail "VARIANT $1 message '$5' missing"; return; fi
    echo "VARIANT $1 ${XINT:-} $verdict as expected ${5:+(\"$5\")} $(grep -m1 -o -E '[A-Z_]+_PASS.*|AssertionError: .{0,80}' "$SCRATCH/out/mutant-$1${XINT:-}.txt")"
}
variant fault-rows TrainingGrowthOracle "" PASS
variant fault-context TrainingContextGrowthOracle "" PASS
XINT=-Xint variant fault-rows TrainingGrowthOracle "" PASS
XINT=-Xint variant fault-context TrainingContextGrowthOracle "" PASS
variant eager-publish TrainingContextGrowthOracle "" FAIL "array reference unchanged labels failure=2"
variant double-compare WordRankOracle --long-counts FAIL "exact long ranking"
variant root-backoff WordContextOracle "" FAIL "reverse suffix / depth / ordering parity"
variant stale-child WordTrainingOracle --layout FAIL "reachable node 4"
variant unbalanced WordTrainingOracle --layout FAIL "AVL_BALANCE"

echo "PHASE_END seconds=$SECONDS"
# (f) differential dump
dump() { "$JAVA_BIN/java" ${3:-} -cp "$1$SEP$MODULE/target/test-classes" "$target_package.M3BackoffWordNgramsDifferentialDump" "$2"; }
dump "$SCRATCH/donor" "$donor_package.$donor_owner" > "$SCRATCH/out/dump-donor.txt"
dump "$MODULE/target/classes" "$target_package.$target_owner" > "$SCRATCH/out/dump-relocated.txt"
dump "$SCRATCH/donor" "$donor_package.$donor_owner" -Xint > "$SCRATCH/out/dump-donor-xint.txt"
dump "$MODULE/target/classes" "$target_package.$target_owner" -Xint > "$SCRATCH/out/dump-relocated-xint.txt"
for side in donor relocated donor-xint relocated-xint; do
    f="$SCRATCH/out/dump-$side.txt"
    echo "DUMP $side sha256=$(sha256sum "$f" | cut -d' ' -f1) bytes=$(wc -c < "$f") lines=$(wc -l < "$f")"
    cmp -s "$SCRATCH/out/dump-donor.txt" "$f" || fail "dump $side differs"
done
tail -n 1 "$SCRATCH/out/dump-relocated.txt"

echo "PHASE_END seconds=$SECONDS"
# (g) javap -p parity of every owner class
sed_map=$(awk -F'\t' '$1=="PACKAGE" && $2!="-" {gsub(/[.]/, "[.]", $2); printf "s/\\b%s\\b/%s/g;", $2, $3}
    $1=="TYPE" {printf "s/\\b%s\\b/%s/g;", $2, $3}' "$MAP")
members=0
for class in "$SCRATCH/donor/$(echo "$donor_package" | tr . /)/$donor_owner"*.class; do
    name=$(basename "$class" .class); target_name=$target_owner${name#"$donor_owner"}
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/donor" "$donor_package.$name" | tr -d '\r' | sed -E "$sed_map" > "$SCRATCH/out/javap-donor-$name.txt"
    "$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.$target_name" | tr -d '\r' > "$SCRATCH/out/javap-relocated-$name.txt"
    diff -q "$SCRATCH/out/javap-donor-$name.txt" "$SCRATCH/out/javap-relocated-$name.txt" > /dev/null || fail "javap $name"
    members=$((members + $(grep -c ';' "$SCRATCH/out/javap-relocated-$name.txt")))
done
echo "JAVAP classes=$(ls "$SCRATCH/donor/$(echo "$donor_package" | tr . /)/$donor_owner"*.class | wc -l) members=$members"
echo "PHASE_END seconds=$SECONDS"
echo "DIFFERENTIAL $status dump_sha256=$(sha256sum "$SCRATCH/out/dump-relocated.txt" | cut -d' ' -f1)"
[ "$status" = PASS ]
