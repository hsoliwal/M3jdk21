#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 (Java differential) of recipe entryless-lane-kernel-20261106: the donor lane kernel compiled from the donor tree
# (the MERGE row's Java owner + the proof of the first FILE row, fetched with targeted git show from a blobless clone) versus
# the relocated members of jdk.m3.collections.M3IndexMaskCompactor (compiled by the module's Maven package), replayed over
#   1. the proof main (its fixed, rejection, 3,200 seeded random and 140,000-slot cases; output PASS cases=N native=false),
#   2. the reflective dump driver M3IndexMaskLaneKernelDump over <ops> seeded random key/value lanes, windows and signed
#      ranges (one in eight full-span), every Java-path result digested into one sha256.
# Both outputs must be byte-identical between the donor and the relocated class. The donor C kernel and the develop tree's
# retained variants are also dumped into <scratch>/native for the Linux native gate (native-gate.sh needs no git). Every
# donor name (paths, package, class) is read from RELOCATION_MAP.tsv, so this script carries no donor token.
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
donor_java=$(awk -F'\t' '$1=="MERGE" {print $2; exit}' "$MAP")
donor_proof=$(awk -F'\t' '$1=="FILE" && $3 ~ /KernelProof\.java$/ {print $2; exit}' "$MAP")
donor_c=$(awk -F'\t' '$1=="APPEND" {print $2; exit}' "$MAP")
retained=$(awk -F'\t' '$1=="SKIP" && $2 ~ /retained-variants/ {print $2; exit}' "$MAP")
kernel_simple=$(basename "$donor_java" .java); proof_simple=$(basename "$donor_proof" .java)
pkg_dir="$SCRATCH/donor-src/${donor_pkg//./\/}"
rm -rf "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/native" "$SCRATCH/run"
mkdir -p "$pkg_dir" "$SCRATCH/donor-classes" "$SCRATCH/native" "$SCRATCH/run"
COMMIT=$(git -C "$DONOR" rev-parse "$REV^{commit}")
git -C "$DONOR" show "$COMMIT:$donor_java" > "$pkg_dir/$kernel_simple.java"
git -C "$DONOR" show "$COMMIT:$donor_proof" > "$pkg_dir/$proof_simple.java"
git -C "$DONOR" show "$COMMIT:$donor_c" > "$SCRATCH/native/kernel.c"
for pr in $(echo "$retained" | sed 's/.*{\(.*\)}.*/\1/' | tr ',' ' '); do
    git -C "$DONOR" show "$COMMIT:${retained%%\{*}$pr${retained#*\}}" > "$SCRATCH/native/retained-$pr.c" 2>/dev/null || true
done
cp "$MODULE/src/test/java/jdk/m3/collections/M3IndexMaskLaneKernelDump.java" "$SCRATCH/donor-src/"
printf '%s\n%s\n' "$donor_pkg.$kernel_simple" "$donor_pkg.$proof_simple" > "$SCRATCH/donor-names.txt"
echo "DONOR commit=$COMMIT kernel_sha256=$(sha256sum "$SCRATCH/native/kernel.c" | cut -c1-64) java_sha256=$(sha256sum "$pkg_dir/$kernel_simple.java" | cut -c1-64)"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/donor-classes" \
    "$pkg_dir/$kernel_simple.java" "$pkg_dir/$proof_simple.java" "$SCRATCH/donor-src/M3IndexMaskLaneKernelDump.java"
echo "donor closure compiled (javac --release 21 -Xlint:all -Werror)"
CP_M3="$MODULE/target/classes$SEP$MODULE/target/test-classes"
status=PASS
compare() { # label donor-file relocated-file
    if cmp -s "$2" "$3"; then v=IDENTICAL; else v=DIFFERENT; status=FAIL; fi
    echo "$1 $v bytes=$(wc -c < "$3") donor_sha256=$(sha256sum "$2" | cut -c1-64) relocated_sha256=$(sha256sum "$3" | cut -c1-64)"
    tr '\n' ' ' < "$3"; echo
}
"$JAVA_BIN/java" -ea -cp "$SCRATCH/donor-classes" "$donor_pkg.$proof_simple" > "$SCRATCH/run/donor-proof.txt"
"$JAVA_BIN/java" -ea -cp "$CP_M3" jdk.m3.collections.M3IndexMaskLaneKernelProof > "$SCRATCH/run/m3-proof.txt"
compare proof "$SCRATCH/run/donor-proof.txt" "$SCRATCH/run/m3-proof.txt"
"$JAVA_BIN/java" -ea -cp "$SCRATCH/donor-classes" jdk.m3.collections.M3IndexMaskLaneKernelDump "$donor_pkg.$kernel_simple" "$OPS" "$SEED" > "$SCRATCH/run/donor-dump.txt"
"$JAVA_BIN/java" -ea -cp "$CP_M3" jdk.m3.collections.M3IndexMaskLaneKernelDump jdk.m3.collections.M3IndexMaskCompactor "$OPS" "$SEED" > "$SCRATCH/run/m3-dump.txt"
compare dump "$SCRATCH/run/donor-dump.txt" "$SCRATCH/run/m3-dump.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
