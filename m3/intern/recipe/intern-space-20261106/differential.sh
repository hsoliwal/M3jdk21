#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2-4 of recipe intern-space-20261106 against the donor tree. The donor family (the four main FILE rows) and its
# seven proof mains (the Proof FILE rows) are compiled from the donor git tree with javac --release 21 -Xlint:all
# -Werror; M3InternSpaceDump (test scope of m3/intern) is compiled a second time with its package line set to the
# donor package of the map, so both sides replay one source. Then:
#   proofs   each donor proof main and its relocation print the same line (PASS checks=N), default JIT and -Xint
#   dump     the seeded dump (100,000 operations by default) of the donor and of the relocated classes is
#            byte-identical, default JIT and -Xint (four runs, each bounded by RUN_LIMIT seconds)
#   javap    javap -p of every donor main class (nested classes too) equals the relocated one after the PACKAGE map
# Prints DIFFERENTIAL PASS|FAIL with counts and the sha256 of every compared output. The donor package comes from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names it literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [operations]   (JAVA_HOME = JDK 21; after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
OPERATIONS=${4:-100000}
RUN_LIMIT=120
rm -rf "$3/src" "$3/classes" "$3/out" "$3/javap"
mkdir -p "$3/src" "$3/classes" "$3/out" "$3/javap"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
mapfile -t main_files < <(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\// {print $2}' "$MAP")
mapfile -t proof_files < <(awk -F'\t' '$1=="FILE" && $2 ~ /Proof\.java$/ {print $2}' "$MAP")
package_dir="$SCRATCH/src/${donor_package//.//}"
mkdir -p "$package_dir"
for path in "${main_files[@]}" "${proof_files[@]}"; do
    git -C "$DONOR_GIT" show "$donor_commit:$path" > "$package_dir/$(basename "$path")"
done
sed "s/^package ${target_package//./\\.};/package $donor_package;/" "$MODULE/src/test/java/${target_package//.//}/M3InternSpaceDump.java" \
    > "$package_dir/M3InternSpaceDump.java"
echo "DONOR $donor_commit main=${#main_files[@]} proofs=${#proof_files[@]} operations=$OPERATIONS"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" $(find "$SCRATCH/src" -name '*.java')
DONOR_CP="$SCRATCH/classes"
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
status=PASS
compare() { # label
    local label=$1 verdict donor_sha relocated_sha
    donor_sha=$(sha256sum "$SCRATCH/out/donor-$label.txt" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$SCRATCH/out/relocated-$label.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/out/donor-$label.txt" "$SCRATCH/out/relocated-$label.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$label $verdict sha256=$relocated_sha donor_sha256=$donor_sha bytes=$(wc -c < "$SCRATCH/out/relocated-$label.txt") lines=$(wc -l < "$SCRATCH/out/relocated-$label.txt")"
}
timed() { # side label classpath java-args...
    local side=$1 label=$2 classpath=$3 started elapsed; shift 3
    started=$(date +%s)
    timeout "$RUN_LIMIT" "$JAVA_BIN/java" -cp "$classpath" "$@" > "$SCRATCH/out/$side-$label.txt"
    elapsed=$(( $(date +%s) - started ))
    echo "RUN $side $label ${elapsed}s"
}
for path in "${proof_files[@]}"; do
    proof=$(basename "$path" .java)
    for mode in default xint; do
        flags=(); [ "$mode" = xint ] && flags=(-Xint -Xmx128m)
        timed donor "$proof-$mode" "$DONOR_CP" "${flags[@]}" "$donor_package.$proof" > /dev/null
        timed relocated "$proof-$mode" "$RELOCATED_CP" "${flags[@]}" "$target_package.$proof" > /dev/null
        compare "$proof-$mode"
        echo "  $(tail -n 1 "$SCRATCH/out/relocated-$proof-$mode.txt")"
    done
done
for mode in default xint; do
    flags=(); [ "$mode" = xint ] && flags=(-Xint)
    timed donor "dump-$mode" "$DONOR_CP" "${flags[@]}" "$donor_package.M3InternSpaceDump" "$OPERATIONS"
    timed relocated "dump-$mode" "$RELOCATED_CP" "${flags[@]}" "$target_package.M3InternSpaceDump" "$OPERATIONS"
    compare "dump-$mode"
done
if cmp -s "$SCRATCH/out/relocated-dump-default.txt" "$SCRATCH/out/relocated-dump-xint.txt"; then echo "dump default-vs-xint IDENTICAL"; else echo "dump default-vs-xint DIFFERENT"; status=FAIL; fi
for section in B T G S P L F R; do
    printf '%s=%s ' "$section" "$(grep -c "^$section " "$SCRATCH/out/relocated-dump-default.txt")"
done
echo
grep -E '^(BYTES|TUPLES|GRAPH|A |TOTAL)' "$SCRATCH/out/relocated-dump-default.txt" | cut -c1-200
retarget() { # the PACKAGE map as a literal (not regex) substitution on every line
    local line
    while IFS= read -r line; do printf '%s\n' "${line//"$donor_package"/$target_package}"; done
}
classes=0
mapfile -t main_types < <(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\// {n=split($3, p, "/"); sub(/\.java$/, "", p[n]); print p[n]}' "$MAP")
donor_main=0
for type in "${main_types[@]}"; do
    donor_main=$((donor_main + $(find "$SCRATCH/classes/${donor_package//.//}" \( -name "$type.class" -o -name "$type\$*.class" \) | wc -l)))
done
while IFS= read -r class_file; do
    simple=$(basename "$class_file" .class)
    "$JAVA_BIN/javap" -p -cp "$DONOR_CP" "$donor_package.$simple" | retarget > "$SCRATCH/javap/donor-$simple.txt"
    "$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.$simple" > "$SCRATCH/javap/relocated-$simple.txt"
    if ! diff --strip-trailing-cr -q "$SCRATCH/javap/donor-$simple.txt" "$SCRATCH/javap/relocated-$simple.txt" > /dev/null; then
        echo "javap $simple DIFFERENT"; status=FAIL
    fi
    classes=$((classes + 1))
done < <(for type in "${main_types[@]}"; do
    find "$MODULE/target/classes/${target_package//.//}" \( -name "$type.class" -o -name "$type\$*.class" \)
done | sort)   # the family's main FILE types only: later leaves add classes to the package (RECIPE_MEMORY.tsv)
[ "$classes" = "$donor_main" ] || { echo "javap class count relocated=$classes donor=$donor_main DIFFERENT"; status=FAIL; }
echo "javap classes=$classes lines=$(cat "$SCRATCH"/javap/relocated-*.txt | wc -l) $([ "$status" = PASS ] && echo IDENTICAL || echo CHECK)"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
