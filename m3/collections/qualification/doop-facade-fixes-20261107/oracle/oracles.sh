#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Oracle driver of recipe qualification/doop-facade-fixes-20261107:
#   1. the two donor reduced-state oracles (rows role=donor_oracle of ../PROVENANCE.tsv, read by git cat-file from the
#      pinned donor heads, default package) versus their relocated copies in this directory (package jdk.m3.collections),
#      each run on the default JIT, -Xint and -XX:TieredStopAtLevel=1; donor and relocated stdout must be byte-identical
#      (the relocation changes no name the output prints);
#   2. DoopFacadeFixesDifferential compiled once against MASTER classes and run on MASTER and HEAD classes (JIT, -Xint),
#      classified by classify_diff.py (SAME / FIX_A / UNCLASSIFIED);
#   3. the module's M3TransientFacadeDump (9 families) on MASTER and HEAD classes (JIT, -Xint): every family digest must
#      be identical (no family reaches either fixed path).
# usage: oracles.sh <donor-git> <master classes> <master test-classes> <head classes> <head test-classes> <out dir>
#                   [episodes=20000] [ops=40] [dump-ops=100000]
# env: JAVA_HOME = a JDK 21; JTMP = java.io.tmpdir for every JVM.
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; HERE=$(cd "$(dirname "$0")" && pwd -W) ;; *) SEP=':'; HERE=$(cd "$(dirname "$0")" && pwd) ;; esac
DONOR=$1; MC=$2; MT=$3; HC=$4; HT=$5; OUT=$6; EP=${7:-20000}; OPS=${8:-40}; DOPS=${9:-100000}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
T="-Djava.io.tmpdir=${JTMP:?JTMP}"
status=PASS
rm -rf "$OUT"; mkdir -p "$OUT/donor-src" "$OUT/donor" "$OUT/relocated" "$OUT/diff"
same() { if cmp -s "$2" "$3"; then echo "$1 IDENTICAL sha256=$(sha256sum < "$2" | cut -c1-16)"; else echo "$1 DIFFERENT"; status=FAIL; fi; }
awk -F'\t' '$10=="donor_oracle" {print $6 "\t" $7}' "$HERE/../PROVENANCE.tsv" > "$OUT/donor-oracles.tsv"
while IFS=$'\t' read -r head path; do
    git -C "$DONOR" cat-file blob "$(git -C "$DONOR" rev-parse "$head:$path")" > "$OUT/donor-src/$(basename "$path")"
done < "$OUT/donor-oracles.tsv"
"$J/javac" "-J$T" --release 21 -Xlint:all -Werror -d "$OUT/donor" "$OUT"/donor-src/*.java || { echo "ORACLE donor javac FAIL"; exit 2; }
"$J/javac" "-J$T" --release 21 -Xlint:all -Werror -d "$OUT/relocated" \
    "$HERE/jdk/m3/collections/DuplicateAdmissionOracle.java" "$HERE/jdk/m3/collections/IdleArrayGrowthOracle.java" \
    || { echo "ORACLE relocated javac FAIL"; exit 2; }
for main in DuplicateAdmissionOracle IdleArrayGrowthOracle; do
    for mode in jit xint c1; do
        X=""; [ $mode = xint ] && X="-Xint"; [ $mode = c1 ] && X="-XX:TieredStopAtLevel=1"
        "$J/java" "$T" $X -cp "$OUT/donor" "$main" > "$OUT/donor-$main-$mode.txt" 2>&1 || status=FAIL
        "$J/java" "$T" $X -cp "$OUT/relocated" "jdk.m3.collections.$main" > "$OUT/relocated-$main-$mode.txt" 2>&1 || status=FAIL
        same "oracle $main $mode donor-vs-relocated" "$OUT/donor-$main-$mode.txt" "$OUT/relocated-$main-$mode.txt"
    done
    same "oracle $main relocated jit-vs-xint" "$OUT/relocated-$main-jit.txt" "$OUT/relocated-$main-xint.txt"
    echo "  $(head -1 "$OUT/relocated-$main-jit.txt")"
done
"$J/javac" "-J$T" --release 21 -Xlint:all -Werror -cp "$MC" -d "$OUT/diff" \
    "$HERE/jdk/m3/collections/DoopFacadeFixesDifferential.java" || { echo "DIFFERENTIAL javac FAIL"; exit 2; }
for side in master head; do
    C=$MC; [ $side = head ] && C=$HC
    for mode in jit xint; do
        X=""; [ $mode = xint ] && X="-Xint"
        "$J/java" "$T" $X -cp "$OUT/diff$SEP$C" jdk.m3.collections.DoopFacadeFixesDifferential \
            "$OUT/trace-$side-$mode.txt" "$EP" "$OPS" || status=FAIL
    done
    same "differential $side jit-vs-xint" "$OUT/trace-$side-jit.txt" "$OUT/trace-$side-xint.txt"
done
python -I "$HERE/classify_diff.py" "$OUT/trace-master-jit.txt" "$OUT/trace-head-jit.txt" "$OUT/classify.tsv" || status=FAIL
for side in master head; do
    C="$MC$SEP$MT"; [ $side = head ] && C="$HC$SEP$HT"
    for mode in jit xint; do
        X=""; [ $mode = xint ] && X="-Xint"
        "$J/java" "$T" $X -cp "$C" jdk.m3.collections.M3TransientFacadeDump "$DOPS" 20261106 > "$OUT/dump-$side-$mode.txt" || status=FAIL
    done
done
for mode in jit xint; do same "dump $mode master-vs-head" "$OUT/dump-master-$mode.txt" "$OUT/dump-head-$mode.txt"; done
same "dump head jit-vs-xint" "$OUT/dump-head-jit.txt" "$OUT/dump-head-xint.txt"
echo "ORACLES $status"
[ "$status" = PASS ]
