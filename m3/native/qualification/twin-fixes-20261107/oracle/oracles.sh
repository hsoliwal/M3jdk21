#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Oracle driver of recipe qualification/twin-fixes-20261107 (m3/native), host side (any OS with a JDK 21):
#   1. the donor oracle of each leaf (rows role donor_oracle* of ../PROVENANCE.tsv, read by git cat-file from the pinned
#      donor merge commit) compiled with the donor's own owner closure at that commit (the production M3NativeLeaseScan,
#      M3ScopedLeaseTable, M3JniArrayReferences and M3RuntimeHeap of the donor package) versus its relocated copy in
#      this directory (package jdk.m3.jni) against HEAD classes, each run under -Xcheck:jni on the default JIT, -Xint and
#      -XX:TieredStopAtLevel=1; donor and relocated stdout must be byte-identical. Leaf N needs the missing-export
#      fixture library (oracle/native/MissingFullSymbolLibrary.c built as m3_scoped_leases, <fixture dir>): without it
#      the oracle refuses ("JNI stub must load successfully");
#   2. the relocated oracles against MASTER classes: leaf N must end in UnsatisfiedLinkError (collect has no fallback),
#      leaf J must fail its observation ("half pair"), both recorded with their exit code;
#   3. the existing-API differential: the module's M3ScopedLeaseDump (1,025 concat parity cases, deterministic scans,
#      <dump-ops> seeded random lease/pin/pool/JNI-borrow/scan/concat/scope operations) on MASTER and HEAD classes,
#      JIT and -Xint, byte-identical.
# usage: oracles.sh <donor-git> <master classes> <head classes> <head test-classes> <heap jar> <fixture dir> <out dir>
#        [dump-ops=100000]
# env: JAVA_HOME = a JDK 21; JTMP = java.io.tmpdir for every JVM.
set -uo pipefail
# MSYS rewrites a mixed class path (relative;S:/...) as a Unix path list: no conversion of any argument here
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; HERE=$(cd "$(dirname "$0")" && pwd -W); export MSYS_NO_PATHCONV=1 ;; *) SEP=':'; HERE=$(cd "$(dirname "$0")" && pwd) ;; esac
DONOR=$1; MC=$2; HC=$3; HT=$4; HEAP=$5; FIX=$6; OUT=$7; DOPS=${8:-100000}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
T="-Djava.io.tmpdir=${JTMP:?JTMP}"
JVM=("$T" -XX:-UsePerfData -Xcheck:jni)
status=PASS
rm -rf "$OUT"; mkdir -p "$OUT/donor-src" "$OUT/donor" "$OUT/relocated" "$OUT/relocated-master"
same() { if cmp -s "$2" "$3"; then echo "$1 IDENTICAL sha256=$(sha256sum < "$2" | cut -c1-16)"; else echo "$1 DIFFERENT"; status=FAIL; fi; }
PROV="$HERE/../PROVENANCE.tsv"
# donor names come from the relocation map and the provenance rows, never from this file (JDK-naming rename tokens)
MAP="$HERE/../../../recipe/scoped-leases-20261106/RELOCATION_MAP.tsv"
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
donor_src_dir=$(dirname "$(awk -F'\t' '$1=="N.src" {print $7}' "$PROV")")
donor_pkg_dir="$OUT/donor-src/${donor_pkg//./\/}"
mkdir -p "$donor_pkg_dir"
# the donor owner closure at the leaf-N merge commit (#16771, the last of the three merges: its first parent is the
# leaf-J merge #16775, whose first parent is the leaf-F merge #16772, so both JNI fixes are in its tree)
JHEAD=$(awk -F'\t' '$1=="N.src" {print $6}' "$PROV")
for owner in M3RuntimeHeap M3NativeLeaseScan M3ScopedLeaseTable M3JniArrayReferences; do
    git -C "$DONOR" show "$JHEAD:$donor_src_dir/$owner.java" > "$donor_pkg_dir/$owner.java" || status=FAIL
done
echo "donor closure at $JHEAD: $(cd "$donor_pkg_dir" && ls | tr '\n' ' ')"
awk -F'\t' '$10 ~ /^donor_oracle\(/ {print $1 "\t" $6 "\t" $7}' "$PROV" > "$OUT/donor-oracles.tsv"
while IFS=$'\t' read -r id head path; do
    git -C "$DONOR" cat-file blob "$(git -C "$DONOR" rev-parse "$head:$path")" > "$donor_pkg_dir/$(basename "$path")"
done < "$OUT/donor-oracles.tsv"
"$J/javac" "-J$T" --release 21 -proc:none -Xlint:all -Werror -d "$OUT/donor" "$donor_pkg_dir"/*.java || { echo "ORACLE donor javac FAIL"; exit 2; }
"$J/javac" "-J$T" --release 21 -proc:none -Xlint:all -Werror -cp "$HC$SEP$HEAP" -d "$OUT/relocated" "$HERE"/jdk/m3/jni/*.java || { echo "ORACLE relocated javac FAIL"; exit 2; }
run_pair() { # <donor main> <relocated main> <extra jvm flags...>
    local dmain=$1 rmain=$2; shift 2
    for mode in jit xint c1; do
        X=""; [ $mode = xint ] && X="-Xint"; [ $mode = c1 ] && X="-XX:TieredStopAtLevel=1"
        "$J/java" "${JVM[@]}" $X "$@" -cp "$OUT/donor" "$donor_pkg.$dmain" > "$OUT/donor-$rmain-$mode.txt" 2>&1 || { echo "donor $dmain $mode exit=$?"; status=FAIL; }
        "$J/java" "${JVM[@]}" $X "$@" -cp "$OUT/relocated$SEP$HC$SEP$HEAP" "jdk.m3.jni.$rmain" > "$OUT/relocated-$rmain-$mode.txt" 2>&1 || { echo "relocated $rmain $mode exit=$?"; status=FAIL; }
        same "oracle $rmain $mode donor-vs-relocated" "$OUT/donor-$rmain-$mode.txt" "$OUT/relocated-$rmain-$mode.txt"
    done
    same "oracle $rmain relocated jit-vs-xint" "$OUT/relocated-$rmain-jit.txt" "$OUT/relocated-$rmain-xint.txt"
    echo "  $(tail -1 "$OUT/relocated-$rmain-jit.txt")"
    "$J/java" "${JVM[@]}" "$@" -cp "$OUT/relocated$SEP$MC$SEP$HEAP" "jdk.m3.jni.$rmain" > "$OUT/master-$rmain.txt" 2>&1
    echo "  $rmain on MASTER classes: exit=$? $(grep -m1 -E 'Exception|Error|PASS' "$OUT/master-$rmain.txt" | cut -c1-120)"
}
run_pair NativeFullSymbolFallbackOracle M3NativeFullSymbolFallbackOracle "-Djava.library.path=$FIX"
run_pair M3JniPairMonitorOracle M3JniPairMonitorOracle
for side in master head; do
    C=$MC; [ $side = head ] && C=$HC
    for mode in jit xint; do
        X=""; [ $mode = xint ] && X="-Xint"
        "$J/java" "$T" -XX:-UsePerfData $X -cp "$C$SEP$HEAP$SEP$HT" jdk.m3.jni.M3ScopedLeaseDump jdk.m3.jni "$DOPS" > "$OUT/dump-$side-$mode.txt" 2>&1 || status=FAIL
    done
done
for mode in jit xint; do same "dump $mode master-vs-head" "$OUT/dump-master-$mode.txt" "$OUT/dump-head-$mode.txt"; done
same "dump head jit-vs-xint" "$OUT/dump-head-jit.txt" "$OUT/dump-head-xint.txt"
grep -E '^(CONCAT_PARITY|RANDOM|TOTAL) ' "$OUT/dump-head-jit.txt" | sed 's/^/  /'
echo "ORACLES $status"
[ "$status" = PASS ]
