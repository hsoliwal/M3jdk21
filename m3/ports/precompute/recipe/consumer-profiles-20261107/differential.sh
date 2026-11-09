#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor closure (the main-scope FILE rows of RELOCATION_MAP.tsv and the oracle main,
# compiled from the donor git tree with javac --release 21 -Xlint:all -Werror) and the relocated classes (target/classes
# and target/test-classes of m3/ports/precompute) replay the same inputs.
# (a) The oracle main prints the same report on both sides once the donor output is mapped by the TYPE rows (the
#     report names its own class); carriage returns are removed on both sides.
# (b) The reflective dump driver M3IndexPrecomputeConsumerProfilesDump (test scope) replays CASES seeded blends through
#     the donor classes and the relocated classes on the default JIT, and XINT_CASES blends under -Xint; each pair of
#     dumps must be byte-identical.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests. Donor package and type names come from the map.
# Derived from m3/arrays/recipe/int-ring-buffer-20261106/differential.sh: three owners reached by one dump, the JUnit
# bridge row is not compiled on the donor side (it only calls the oracle main), and the oracle report is TYPE-mapped.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [CASES=6000] [XINT_CASES=1000]
#        (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CASES=${4:-6000}
XINT_CASES=${5:-1000}
mkdir -p "$3/donor-classes" "$3/donor-src"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
target_of() { awk -F'\t' -v d="$1" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP"; }
donor_of() { awk -F'\t' -v t="$1" '$1=="TYPE" && $3==t {print $2; exit}' "$MAP"; }
rm -f "$SCRATCH"/donor-src/*.java
awk -F'\t' '$1=="FILE" && ($3 ~ /^src\/main\/java\// || $3 ~ /Oracle\.java$/) {print $2}' "$MAP" | while read -r path; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/donor-src/$(basename "$path" .txt)"
    echo "DONOR $donor_commit $path blob=$(git -C "$DONOR_GIT" rev-parse "$REV:$path" | cut -c1-12)"
done
"$JAVA_BIN/javac" -J-Djava.io.tmpdir="$SCRATCH" --release 21 -encoding UTF-8 -proc:none -Xlint:all -Werror \
    -d "$SCRATCH/donor-classes" "$SCRATCH"/donor-src/*.java
RELOCATED="$MODULE/target/classes$SEP$MODULE/target/test-classes"
DONOR_CP="$SCRATCH/donor-classes$SEP$MODULE/target/test-classes"
status=PASS
compare() {
    local left right verdict
    left=$(sha256sum "$SCRATCH/$1.txt" | cut -d' ' -f1)
    right=$(sha256sum "$SCRATCH/$2.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/$1.txt" "$SCRATCH/$2.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 vs $2 $verdict sha256=$left/$right bytes=$(wc -c < "$SCRATCH/$1.txt") lines=$(wc -l < "$SCRATCH/$1.txt")"
}
normalize="$SCRATCH/type-normalize.sed"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" > "$normalize"
# (a) the oracle main
target_oracle=$(awk -F'\t' '$1=="FILE" && $3 ~ /Oracle\.java$/ {print $3; exit}' "$MAP")
target_oracle=$(basename "$target_oracle" .java)
"$JAVA_BIN/java" -Djava.io.tmpdir="$SCRATCH" -cp "$SCRATCH/donor-classes" "$donor_package.$(donor_of "$target_oracle")" \
    | tr -d '\r' | sed -f "$normalize" > "$SCRATCH/donor-oracle.txt"
"$JAVA_BIN/java" -Djava.io.tmpdir="$SCRATCH" -cp "$RELOCATED" "$target_package.$target_oracle" | tr -d '\r' \
    > "$SCRATCH/relocated-oracle.txt"
compare donor-oracle relocated-oracle
cat "$SCRATCH/relocated-oracle.txt"
# (b) the dump: profiles, capability and workload classes in the order of their main-scope FILE rows' TYPE rows
owners=$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {n=split($3, p, "/"); sub(/\.java$/, "", p[n]); print p[n]}' "$MAP")
capability=$(echo "$owners" | grep 'Capability$')
workload=$(echo "$owners" | grep 'Workload$')
profiles=$(echo "$owners" | grep 'Profiles$')
DUMP="$target_package.M3IndexPrecomputeConsumerProfilesDump"
dump() { # dump <name> <class path> <package> <use donor names: yes|no> <cases> [jvm flag]
    local p=$profiles c=$capability w=$workload
    if [ "$4" = yes ]; then p=$(donor_of "$p"); c=$(donor_of "$c"); w=$(donor_of "$w"); fi
    "$JAVA_BIN/java" -Djava.io.tmpdir="$SCRATCH" ${6:-} -cp "$2" "$DUMP" "$3.$p" "$3.$c" "$3.$w" "$5" > "$SCRATCH/$1.txt"
}
dump donor-dump "$DONOR_CP" "$donor_package" yes "$CASES"
dump relocated-dump "$RELOCATED" "$target_package" no "$CASES"
compare donor-dump relocated-dump
dump donor-dump-xint "$DONOR_CP" "$donor_package" yes "$XINT_CASES" -Xint
dump relocated-dump-xint "$RELOCATED" "$target_package" no "$XINT_CASES" -Xint
compare donor-dump-xint relocated-dump-xint
tail -n 1 "$SCRATCH/relocated-dump.txt"
tail -n 1 "$SCRATCH/relocated-dump-xint.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
