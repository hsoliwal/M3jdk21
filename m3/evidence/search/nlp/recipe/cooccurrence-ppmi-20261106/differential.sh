#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2 (figures, witnesses, Java path), 3 and 4 of the cooccurrence-ppmi Java leaf. Donor files come from the donor
# git tree at the REPO pin of RELOCATION_MAP.tsv (package and type names from the map only):
#   (a) donor lane: the owner template (FILE row 1) + the donor oracles (other FILE rows), javac --release 21 -proc:none
#       -Xlint:all -Werror; each oracle mode in its own JVM;
#   (b) relocated lane: the same modes on target/classes + target/test-classes; PASS lines (checks, digests) of behavior
#       modes must be identical; allocation modes are printed side by side;
#   (c) red witnesses of the three predecessor templates (SKIP rows naming before/*.java.template): the full-sort
#       preimage with the ranking oracle --allocation (BOUND_ALLOCATION), topk-v1 with the sparse oracle --allocation
#       (SPARSE_ALLOCATION_EXCEEDED), sparse-v1 with the positive oracle (POSITIVE_SCRATCH_EXCEEDED); the preimage also
#       runs the ranking and sparse oracles' behavior modes, whose digests must equal the relocated ones;
#   (d) Java path: no gate sets the opt-in property and no selector library exists, so every ranking ran the Java
#       PositiveTopK path; requesting the native path (property true) without the library must fail visibly;
#   (e) donor-side manifest atom: the donor recipe engine and ManifestAtomProbe (SKIP rows) over the donor resource
#       directory reproduce the donor's MANIFEST_ATOM_PASS figures (nothing of it is relocated);
#   (f) M3CooccurrencePpmiDifferentialDump: donor, relocated, donor -Xint, relocated -Xint byte-identical; the relocated
#       positive oracle --behavior also under -Xint;
#   (g) javap -p of every donor owner class (nested included), PACKAGE/TYPE rows applied, equals the relocated one.
# Prints DIFFERENTIAL PASS|FAIL. usage: differential.sh <donor-git-dir> <scratch-dir>   (JAVA_HOME = JDK 21; after package)
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
rm -rf "$2"; mkdir -p "$2/donor-src" "$2/donor" "$2/out" "$2/manifest"
SCRATCH=$(winpath "$2")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
status=PASS
fail() { echo "FAIL $*"; status=FAIL; }
pin=$(awk -F'\t' '$1=="REPO" {print $3; exit}' "$MAP")
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" && $2!="-" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" && $2!="-" {print $3; exit}' "$MAP")
target_of() { awk -F'\t' -v t="$1" '$1=="TYPE" && $2==t {print $3; exit}' "$MAP"; }
skip_path() { awk -F'\t' -v m="$1" '$1=="SKIP" && index($2, m) {print $2; exit}' "$MAP"; }
donor_owner=$(basename "$owner_path" .java.template)
target_owner=$(target_of "$donor_owner")
pkg_dir="$SCRATCH/donor-src/$(echo "$donor_package" | tr . /)"
mkdir -p "$pkg_dir"
git -C "$DONOR_GIT" show "$pin:$owner_path" > "$pkg_dir/$donor_owner.java"
oracles=()
while read -r path; do
    git -C "$DONOR_GIT" show "$pin:$path" > "$SCRATCH/donor-src/$(basename "$path")"; oracles+=("$SCRATCH/donor-src/$(basename "$path")")
done < <(awk -F'\t' '$1=="FILE" && $3 ~ /src\/test\/java\// {print $2}' "$MAP")
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/donor" "$pkg_dir/$donor_owner.java" "${oracles[@]}" || fail "donor compile"
echo "DONOR $pin $owner_path + ${#oracles[@]} oracles"

# (a)+(b)
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
modes=("PpmiRankingOracle:" "PpmiRankingOracle:--allocation" "PpmiSparseOracle:" "PpmiSparseOracle:--allocation"
       "PpmiPositiveOracle:--behavior" "PpmiPositiveOracle:")
for entry in "${modes[@]}"; do
    oracle=${entry%%:*}; mode=${entry#*:}; tag="$oracle${mode:-:main}"
    "$JAVA_BIN/java" -Xmx768m -cp "$SCRATCH/donor" "$oracle" $mode > "$SCRATCH/out/donor-$tag.txt" 2>&1 || fail "donor $tag"
    "$JAVA_BIN/java" -Xmx768m -cp "$RELOCATED_CP" "$target_package.$(target_of "$oracle")" $mode > "$SCRATCH/out/relocated-$tag.txt" 2>&1 || fail "relocated $tag"
    d=$(grep -E '_PASS|ALLOCATION' "$SCRATCH/out/donor-$tag.txt" | tr '\n' ' '); r=$(grep -E '_PASS|ALLOCATION' "$SCRATCH/out/relocated-$tag.txt" | tr '\n' ' ')
    case "$tag" in
        *--allocation|PpmiPositiveOracle:main) echo "ORACLE $tag donor=[$d] relocated=[$r]" ;;
        *) [ "$d" = "$r" ] && echo "ORACLE $tag IDENTICAL [$r]" || fail "ORACLE $tag donor=[$d] relocated=[$r]" ;;
    esac
done
echo "PHASE_END seconds=$SECONDS"

# (c) predecessor witnesses
witness() {  # witness <before-file-name> <donor oracle> <mode> <expected marker>
    local dir="$SCRATCH/before-$1" path; path=$(skip_path "before/$1"); mkdir -p "$dir"
    git -C "$DONOR_GIT" show "$pin:$path" > "$dir/$donor_owner.java"
    "$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$dir" "$dir/$donor_owner.java" "$SCRATCH/donor-src/$2.java" \
        || { fail "compile $1"; return; }
    if "$JAVA_BIN/java" -Xmx768m -cp "$dir" "$2" $3 > "$SCRATCH/out/red-$1-$2.txt" 2>&1; then
        fail "RED_WITNESS $1 $2 $3 passed"
    else
        grep -q "$4" "$SCRATCH/out/red-$1-$2.txt" && echo "RED_WITNESS $1 $2 $3 $(grep -o -E "$4[^)]*|[A-Z_]*ALLOCATION bytesPerCall=[0-9]+" "$SCRATCH/out/red-$1-$2.txt" | tr '\n' ' ')" \
            || fail "RED_WITNESS $1 marker $4 missing"
    fi
}
witness CooccurrencePpmi.java.template PpmiRankingOracle --allocation BOUND_ALLOCATION
witness topk-v1.java.template PpmiSparseOracle --allocation SPARSE_ALLOCATION_EXCEEDED
witness sparse-v1.java.template PpmiPositiveOracle "" POSITIVE_SCRATCH_EXCEEDED
pre="$SCRATCH/before-CooccurrencePpmi.java.template"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$pre" "$pre/$donor_owner.java" "$SCRATCH/donor-src/PpmiSparseOracle.java" || fail "preimage sparse compile"
for oracle in PpmiRankingOracle PpmiSparseOracle; do
    p=$("$JAVA_BIN/java" -Xmx768m -cp "$pre" "$oracle" 2>&1 | grep '_PASS'); r=$(grep '_PASS' "$SCRATCH/out/relocated-$oracle:main.txt")
    [ "$p" = "$r" ] && echo "PREIMAGE_DIGEST $oracle IDENTICAL [$p]" || fail "PREIMAGE_DIGEST $oracle preimage=[$p] relocated=[$r]"
done
echo "PHASE_END seconds=$SECONDS"

# (d) Java path and the visible failure of a requested but missing native selector
# the target side of the two LITERAL rows: the dotted one is the opt-in property, the other the library name
property=$(awk -F'\t' '$1=="LITERAL" && $3 ~ /[.]/ {print $3; exit}' "$MAP"); library=$(awk -F'\t' '$1=="LITERAL" && $3 !~ /[.]/ {print $3; exit}' "$MAP")
if "$JAVA_BIN/java" "-D$property=true" -cp "$RELOCATED_CP" "$target_package.$(target_of PpmiPositiveOracle)" --behavior > "$SCRATCH/out/native-requested.txt" 2>&1; then
    fail "native path requested without a library did not fail"
else
    grep -q "UnsatisfiedLinkError.*$library" "$SCRATCH/out/native-requested.txt" \
        && echo "JAVA_PATH gates ran with $property unset (NativeSelection.ENABLED=false, PositiveTopK); requested without lib$library: $(grep -o -m1 'UnsatisfiedLinkError: [^:]*' "$SCRATCH/out/native-requested.txt")" \
        || fail "native request failure is not an UnsatisfiedLinkError for $library"
fi

# (e) donor-side manifest atom
engine=$(awk -F'\t' '$1=="SKIP" && $2 ~ /ManifestAtomProbe[.]java$/ {print $2; exit}' "$MAP")
resources=$(dirname "$owner_path")
engine_src=$(awk -F'\t' '$1=="SKIP" && $2 ~ /M3GpuToolJoinManifest[.]java$/ {print $2; exit}' "$MAP")
mkdir -p "$SCRATCH/manifest/src" "$SCRATCH/manifest/classes" "$SCRATCH/manifest/res"
git -C "$DONOR_GIT" show "$pin:$engine" > "$SCRATCH/manifest/src/ManifestAtomProbe.java"
git -C "$DONOR_GIT" show "$pin:$engine_src" > "$SCRATCH/manifest/src/M3GpuToolJoinManifest.java"
git -C "$DONOR_GIT" ls-tree -r --name-only "$pin" -- "$resources" | while read -r file; do
    mkdir -p "$SCRATCH/manifest/res/$(dirname "${file#"$resources"/}")"
    git -C "$DONOR_GIT" show "$pin:$file" > "$SCRATCH/manifest/res/${file#"$resources"/}"
done
probe_class=$(git -C "$DONOR_GIT" show "$pin:$engine" | sed -n 's/^package \(.*\);/\1/p').ManifestAtomProbe
if "$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/manifest/classes" "$SCRATCH/manifest/src/"*.java \
    && "$JAVA_BIN/java" -cp "$SCRATCH/manifest/classes" "$probe_class" "$SCRATCH/manifest/res" "$SCRATCH/manifest/emitted" > "$SCRATCH/out/manifest-atom.txt" 2>&1; then
    echo "DONOR_MANIFEST_ATOM $(tail -n 1 "$SCRATCH/out/manifest-atom.txt") files=$(find "$SCRATCH/manifest/res" -type f | wc -l)"
else
    fail "DONOR_MANIFEST_ATOM $(tail -n 2 "$SCRATCH/out/manifest-atom.txt" 2>/dev/null | tr '\n' ' ')"
fi
echo "PHASE_END seconds=$SECONDS"

# (f) differential dump
dump() { "$JAVA_BIN/java" ${3:-} -cp "$1$SEP$MODULE/target/test-classes" "$target_package.M3CooccurrencePpmiDifferentialDump" "$2"; }
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
"$JAVA_BIN/java" -Xint -cp "$RELOCATED_CP" "$target_package.$(target_of PpmiPositiveOracle)" --behavior > "$SCRATCH/out/xint-positive.txt" 2>&1 \
    && echo "XINT positive $(tail -n 1 "$SCRATCH/out/xint-positive.txt")" || fail "xint positive"
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
echo "JAVAP classes=$(ls "$SCRATCH/donor/$(echo "$donor_package" | tr . /)/$donor_owner"*.class | wc -l) members=$members native=$(grep -c ' native ' "$SCRATCH/out/javap-relocated-$donor_owner.txt")"
echo "PHASE_END seconds=$SECONDS"
echo "DIFFERENTIAL $status dump_sha256=$(sha256sum "$SCRATCH/out/dump-relocated.txt" | cut -d' ' -f1)"
[ "$status" = PASS ]
