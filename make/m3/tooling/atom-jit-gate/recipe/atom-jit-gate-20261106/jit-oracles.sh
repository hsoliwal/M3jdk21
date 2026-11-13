#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Oracles 1 and 2 of the leaf, donor and relocated side by side on one JDK 21 (child JVMs, each bounded by timeout):
# (1) hotLoop: M3OpenJdkOrderedPasses --hot (80,000 calls of the fixed arithmetic oracle) under -Xint, the donor's
#     forced compilation (-Xbatch -XX:-TieredCompilation -XX:CompileThreshold=100 -XX:+PrintCompilation) and -Xcomp
#     (-XX:+PrintCompilation): the M3_JIT_DIGEST line must be equal in every mode and on both sides (donor receipt
#     cc78391bd00cc9c9), and every compiled run must print a PrintCompilation line for ::hotLoop.
# (2) long-set add atom: the relocated long-set proof (longset-proof/: the single-probe oracle, the derived baseline and
#     the JIT warmup) compiled with the M3JDK owner jdk.m3.collections.M3LongHashSet / M3LongCollection / M3LongIterator,
#     and the donor set (the donor postimage of the atom with its two interfaces, the oracle, the baseline derived by
#     the donor rule, the warmup) under the donor tier options: -Xint; C1 tier 1 (-Xbatch -XX:+TieredCompilation
#     -XX:TieredStopAtLevel=1 -XX:CompileThreshold=1000 -XX:+PrintCompilation); C2 tier 4 (TieredStopAtLevel=4). Each
#     run must print PASS operations=284500 checks=745022 finalSize=166 and JIT_WARMUP_PASS entries=266240; C1 and C2
#     must show M3LongHashSet::add compiled at tier 1 and tier 4; donor and relocated oracle lines must be identical.
# usage: JAVA_HOME=<JDK 21> jit-oracles.sh <donor-git-dir> <donor-rev> <scratch-dir>   (after mvn package of the module)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
ROOT=$(winpath "$(dirname "$0")/../../../../../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
TIMEOUT=${M3_ATOMJIT_CHILD_TIMEOUT:-300}
status=PASS
fail() { echo "FAIL $*"; status=FAIL; }
row() { awk -F'\t' -v k="$1" -v s="$2" '$1==k && index($2, s) {print $2; exit}' "$MAP"; }
show() { git -C "$DONOR_GIT" show "$REV:$1"; }
rm -rf "$SCRATCH/hot" "$SCRATCH/longset"
mkdir -p "$SCRATCH/hot/src" "$SCRATCH/hot/classes" "$SCRATCH/longset/donor/src" "$SCRATCH/longset/donor/classes" \
    "$SCRATCH/longset/relocated/src" "$SCRATCH/longset/relocated/classes"

# (1) donor ordered passes: its closure (the atomizer and pattern index, NEC rows) compiled from the donor tree
passes=$(row FILE /M3OpenJdkOrderedPasses.java)
donor_pkg_dir=$(dirname "$passes" | sed 's#.*/src/main/java/##')
mkdir -p "$SCRATCH/hot/src/$donor_pkg_dir"
for path in "$passes" $(awk -F'\t' '($1=="NEC" || $1=="OMIT") && $2 ~ /\/M3[A-Za-z]+\.java$/ && $2 !~ /UniversalRecipe/ {print $2}' "$MAP"); do
    show "$path" > "$SCRATCH/hot/src/$donor_pkg_dir/$(basename "$path")"
done
"$JAVA_BIN/javac" --release 21 -proc:none -nowarn -d "$SCRATCH/hot/classes" -sourcepath "$SCRATCH/hot/src" \
    "$SCRATCH/hot/src/$donor_pkg_dir/M3OpenJdkOrderedPasses.java"
donor_main="${donor_pkg_dir//\//.}.M3OpenJdkOrderedPasses"
hot() { # side classpath main mode flags...
    local side=$1 cp=$2 main=$3 mode=$4; shift 4
    timeout "$TIMEOUT" "$JAVA_BIN/java" "$@" -cp "$cp" "$main" --hot > "$SCRATCH/hot/$side-$mode.log" 2>&1
    local digest; digest=$(grep -E '^M3_JIT_DIGEST=' "$SCRATCH/hot/$side-$mode.log" | cut -d= -f2)
    local witness; witness=$(grep -m1 -E '::hotLoop' "$SCRATCH/hot/$side-$mode.log" | tr -s ' ' || true)
    echo "hot $side $mode digest=$digest hotLoop_compiled=${witness:+yes} ${witness:-}"
    echo "$digest" >> "$SCRATCH/hot/digests.txt"
    if [ "$mode" != xint ] && [ -z "$witness" ]; then fail "hot $side $mode: no PrintCompilation line for ::hotLoop"; fi
}
for side in donor relocated; do
    if [ "$side" = donor ]; then cp="$SCRATCH/hot/classes"; main=$donor_main; else cp="$MODULE/target/classes"; main=jdk.m3.tooling.atomjit.M3OpenJdkOrderedPasses; fi
    hot "$side" "$cp" "$main" xint -Xint
    hot "$side" "$cp" "$main" forced -Xbatch -XX:-TieredCompilation -XX:CompileThreshold=100 -XX:+PrintCompilation
    hot "$side" "$cp" "$main" xcomp -Xcomp -XX:+PrintCompilation
done
distinct=$(sort -u "$SCRATCH/hot/digests.txt" | wc -l)
if [ "$distinct" = 1 ]; then echo "ORACLE1 PASS digest=$(head -1 "$SCRATCH/hot/digests.txt") runs=$(wc -l < "$SCRATCH/hot/digests.txt") (donor receipt cc78391bd00cc9c9)"; else fail "ORACLE1 digests differ: $(sort -u "$SCRATCH/hot/digests.txt" | tr '\n' ' ')"; fi

# (2) long-set add atom
rel="$SCRATCH/longset/relocated/src/jdk/m3/collections"
mkdir -p "$rel"
cp "$ROOT"/m3/collections/src/main/java/jdk/m3/collections/{M3LongHashSet,M3LongCollection,M3LongIterator}.java "$rel/"
cp "$MODULE"/longset-proof/*.java "$rel/"
don="$SCRATCH/longset/donor/src/donor"
mkdir -p "$don"
show "$(row OMIT /M3LongHashSet.java.txt)" > "$don/M3LongHashSet.java"
show "$(row OMIT /M3LongCollection.java.txt)" > "$don/M3LongCollection.java"
show "$(row OMIT /M3LongIterator.java.txt)" > "$don/M3LongIterator.java"
show "$(row FILE /M3LongHashSetSingleProbeTest.java.txt)" > "$don/M3LongHashSetSingleProbeTest.java"
show "$(row FILE /M3LongHashSetJitWarmup.java)" > "$don/M3LongHashSetJitWarmup.java"
show "$(row FILE /inputs/M3LongHashSet.java.txt)" | sed 's/M3LongHashSet/M3LongHashSetBaseline/g' > "$don/M3LongHashSetBaseline.java"
donor_longset_pkg=$(sed -n 's/^package \([a-z0-9.]*\);.*/\1/p' "$don/M3LongHashSetJitWarmup.java" | head -1)
for side in donor relocated; do
    "$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/longset/$side/classes" $(find "$SCRATCH/longset/$side/src" -name '*.java')
done
declare -A TIER=([xint]="-Xint"
                 [c1]="-Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:CompileThreshold=1000 -XX:+PrintCompilation"
                 [c2]="-Xbatch -XX:+TieredCompilation -XX:TieredStopAtLevel=4 -XX:CompileThreshold=1000 -XX:+PrintCompilation")
declare -A LEVEL=([c1]=1 [c2]=4)
for side in donor relocated; do
    if [ "$side" = donor ]; then main="$donor_longset_pkg.M3LongHashSetJitWarmup"; else main=jdk.m3.collections.M3LongHashSetJitWarmup; fi
    for tier in xint c1 c2; do
        log="$SCRATCH/longset/$side-$tier.log"
        # shellcheck disable=SC2086
        timeout "$TIMEOUT" "$JAVA_BIN/java" ${TIER[$tier]} -cp "$SCRATCH/longset/$side/classes" "$main" > "$log" 2>&1 || fail "longset $side $tier exit $?"
        oracle=$(grep -E '^PASS operations=' "$log" || true)
        warm=$(grep -E '^JIT_WARMUP_PASS ' "$log" || true)
        grep -E '^(PASS operations=|JIT_WARMUP_PASS )' "$log" > "$SCRATCH/longset/$side-$tier.oracle" || true
        witness=""
        if [ "$tier" != xint ]; then
            witness=$(grep -m1 -E "\bb +${LEVEL[$tier]} +.*M3LongHashSet::add " "$log" | tr -s ' ' || true)
            [ -n "$witness" ] || fail "longset $side $tier: M3LongHashSet::add not compiled at tier ${LEVEL[$tier]}"
        fi
        [ "$oracle" = "PASS operations=284500 checks=745022 finalSize=166" ] || fail "longset $side $tier oracle: $oracle"
        [ "$warm" = "JIT_WARMUP_PASS entries=266240" ] || fail "longset $side $tier warmup: $warm"
        echo "longset $side $tier $oracle | $warm | ${witness:-interpreter}"
    done
done
for tier in xint c1 c2; do
    if cmp -s "$SCRATCH/longset/donor-$tier.oracle" "$SCRATCH/longset/relocated-$tier.oracle"; then
        echo "longset donor-vs-relocated $tier IDENTICAL sha256=$(sha256sum "$SCRATCH/longset/relocated-$tier.oracle" | cut -c1-16)"
    else fail "longset donor-vs-relocated $tier DIFFERENT"; fi
done
echo "ORACLE2 $status"
echo "JIT_ORACLES $status java=$("$JAVA_BIN/java" -version 2>&1 | head -1)"
[ "$status" = PASS ]
