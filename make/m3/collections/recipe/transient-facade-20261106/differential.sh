#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe transient-facade-20261106: the donor family compiled from the donor tree (every FILE row
# under src/main, every ALREADY row whose owner is already on master, and the OWNER-resolved dependency, fetched with
# targeted git show from a blobless clone) versus the relocated and already-present classes of jdk.m3.collections
# (compiled by the module's Maven package), replayed over
#   1. the five self-test mains of the FILE and ALREADY rows (recycler serial/lazy/close-boundary, top-k planner, idle
#      differential),
#   2. the M3JDK-authored dump driver M3TransientFacadeDump: 8 scenario families x <ops> seeded operations
#      (lifecycle-recreate, lifecycle-retain, recycle, reentrant-close, pressure, topk, recycler, growth), each digested
#      into one sha256, on the default JIT and again under -Xint.
# Every pair of outputs must be byte-identical. The donor copy of the dump driver is the same source with the package
# line and the TYPE rows mapped back to the donor names; every donor name is read from RELOCATION_MAP.tsv, so this
# script carries no donor token. Each java run is bounded by timeout(1) at 115 s.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops=100000] [seed=20261106]
# env:   JAVA_HOME (a JDK 21); the module must be packaged (target/classes, target/test-classes)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR=$1; REV=$2; mkdir -p "$3"; SCRATCH=$(winpath "$3"); OPS=${4:-100000}; SEED=${5:-20261106}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
pkg_dir="$SCRATCH/donor-src/${donor_pkg//./\/}"
rm -rf "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/run"
mkdir -p "$pkg_dir" "$SCRATCH/donor-classes" "$SCRATCH/run"
COMMIT=$(git -C "$DONOR" rev-parse "$REV^{commit}")
awk -F'\t' '{t=$3; k=$1; if (k=="SKIP" && t ~ /^ALREADY /) {k="FILE"; sub(/^ALREADY /,"",t)}}
    (k=="FILE" && (t ~ /^src\/main\// || t ~ /(SelfTest|Differential)\.java$/)) || ($1=="SKIP" && $3 ~ /^OWNER /) {print $2}' "$MAP" > "$SCRATCH/donor-paths.txt"
while read -r path; do
    git -C "$DONOR" show "$COMMIT:$path" > "$pkg_dir/$(basename "$path")"
done < "$SCRATCH/donor-paths.txt"
unmap=$(awk -F'\t' '$1=="TYPE" {printf "s/\\<%s\\>/%s/g;", $3, $2}' "$MAP")
sed -e "s/^package jdk\.m3\.collections;/package $donor_pkg;/" -e "$unmap" \
    "$MODULE/src/test/java/jdk/m3/collections/M3TransientFacadeDump.java" > "$pkg_dir/M3TransientFacadeDump.java"
echo "DONOR commit=$COMMIT files=$(wc -l < "$SCRATCH/donor-paths.txt") (src/main FILE rows, self-test/differential mains, OWNER dependency) + dump driver"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/donor-classes" $(find "$SCRATCH/donor-src" -name '*.java')
echo "donor closure compiled (javac --release 21 -Xlint:all -Werror)"
CP_M3="$MODULE/target/classes$SEP$MODULE/target/test-classes"
status=PASS
compare() { # label donor-file relocated-file
    if cmp -s "$2" "$3"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
    echo "$1 $v lines=$(wc -l < "$3") donor_sha256=$(sha256sum "$2" | cut -c1-64) relocated_sha256=$(sha256sum "$3" | cut -c1-64)"
}
for main in $(awk -F'\t' '($1=="FILE" || ($1=="SKIP" && $3 ~ /^ALREADY /)) && $3 ~ /(SelfTest|Differential)\.java$/ {n=split($3,a,"/"); sub(/\.java$/,"",a[n]); print a[n]}' "$MAP"); do
    timeout 115 "$JAVA_BIN/java" -ea -cp "$SCRATCH/donor-classes" "$donor_pkg.$main" > "$SCRATCH/run/donor-$main.txt"
    timeout 115 "$JAVA_BIN/java" -ea -cp "$CP_M3" "jdk.m3.collections.$main" > "$SCRATCH/run/m3-$main.txt"
    compare "$main" "$SCRATCH/run/donor-$main.txt" "$SCRATCH/run/m3-$main.txt"
    tail -n 1 "$SCRATCH/run/m3-$main.txt" | cut -c1-160
done
for mode in jit xint; do
    flags=""; [ "$mode" = xint ] && flags="-Xint"
    timeout 115 "$JAVA_BIN/java" $flags -cp "$SCRATCH/donor-classes" "$donor_pkg.M3TransientFacadeDump" "$OPS" "$SEED" > "$SCRATCH/run/donor-dump-$mode.txt"
    timeout 115 "$JAVA_BIN/java" $flags -cp "$CP_M3" jdk.m3.collections.M3TransientFacadeDump "$OPS" "$SEED" > "$SCRATCH/run/m3-dump-$mode.txt"
    compare "dump-$mode" "$SCRATCH/run/donor-dump-$mode.txt" "$SCRATCH/run/m3-dump-$mode.txt"
    cut -d' ' -f1,4,5 "$SCRATCH/run/m3-dump-$mode.txt" | sed 's/sha256=\(.\{16\}\).*/sha256=\1/' | tr '\n' ';'; echo
done
compare "dump-jit-vs-xint" "$SCRATCH/run/m3-dump-jit.txt" "$SCRATCH/run/m3-dump-xint.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
