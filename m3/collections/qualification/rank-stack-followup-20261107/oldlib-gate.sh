#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Old-library gate of qualification/rank-stack-followup-20261107 (Linux; 2026-11-07 run: WSL Ubuntu 24.04, gcc 13.3,
# Temurin 21.0.13). Proves that the restored Java owner falls back to Java when master's 7-export libm3rankbatch is
# loaded (-Dm3.collections.rank.native=true), the case #2693 left open for boundBatchInto0, intersectionCount0 and
# rangeCountInto0:
#   1. gcc -Werror of master's m3_long_rank_batch_jni.c (blob 97f1b4387f2c, the THREE_WAY ours of #2693) -> old library
#      (7 exports) and of the head C -> new library (23 exports);
#   2. javac -Xlint:all,-module -Werror of the owner, the restored contracts and the #2693 digest driver;
#   3. RankStackRestoredDigest (#2693 oracle) with the old library, -Drankstack.trace=true, default and -Xint, two seeds:
#      digest must equal the Java digest and no UnsatisfiedLinkError may be digested;
#   4. the contracts of the guarded callers on the old library (library opted in): PASS, except a final reflective
#      direct-export probe, accepted only at that probe (DIRECT_PROBE);
#   5. one mutant per new fallback (its catch rethrows instead of setting the flag): the old-library digest must fail.
# usage: oldlib-gate.sh <out-dir> <master m3_long_rank_batch_jni.c>
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$HERE/../.." && pwd)
RESTORE=$MODULE/qualification/rank-stack-restore-20261107
OUT=$1
MASTER_C=$2
JDK=${JAVA_HOME:-$(ls -d /root/jdk/jdk-21* "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
rm -rf "$OUT"; mkdir -p "$OUT"/{classes,old,new,mut,src/jdk/m3,tmp}
status=PASS
fail() { echo "FAIL: $*"; status=FAIL; }
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux"
T="$MODULE/src/test/java/jdk/m3/collections"
OWNER="$MODULE/src/main/java/jdk/m3/collections/M3LongRankBatchNative.java"
# contracts whose owner-API checks reach the guarded callers; M3RankIntervalBatchContract ends with a reflective
# rangeCountInto0 probe (direct export, not the owner API) that can only fail on a library without that export;
# M3LongRankIntersectionContract asserts the library state of its mode, so intersectionCount is judged by step 3.
CONTRACTS="M3LongLowerBoundWindowContract M3LongRankUpperCountSweepContract M3RankIntervalBatchContract"
DIRECT_PROBE="M3RankIntervalBatchContract|nativeInvalid|rangeCountInto0"
java -version 2>&1 | head -1; gcc --version | head -1
echo "== 1. libraries"
gcc $FLAGS $INC "$MASTER_C" -o "$OUT/old/libm3rankbatch.so" || fail "gcc master"
gcc $FLAGS $INC "$MODULE/src/main/native/collections/m3_long_rank_batch_jni.c" -o "$OUT/new/libm3rankbatch.so" || fail "gcc head"
echo "exports old=$(nm -D --defined-only "$OUT/old/libm3rankbatch.so" | grep -c ' Java_') new=$(nm -D --defined-only "$OUT/new/libm3rankbatch.so" | grep -c ' Java_')"
echo "old library lacks: $(for s in boundBatchInto0 intersectionCount0 rangeCountInto0; do nm -D "$OUT/old/libm3rankbatch.so" | grep -q "_$s\$" || printf '%s ' $s; done)"
echo "== 2. javac"
ln -s "$MODULE/src/main/java/jdk/m3/collections" "$OUT/src/jdk/m3/collections"
ln -s "$MODULE/../arrays/src/main/java/jdk/m3/arrays" "$OUT/src/jdk/m3/arrays"
compile() { # out-classes owner-source
    javac --release 21 -proc:none -Xlint:all,-module -Werror -implicit:class -sourcepath "$OUT/src" -d "$1" "$2" \
        $(for c in $CONTRACTS M3RankBatchIntoContract; do printf '%s ' "$T/$c.java"; done) \
        "$RESTORE/oracle/jdk/m3/collections/RankStackRestoredDigest.java"
}
compile "$OUT/classes" "$OWNER" || fail "javac"
J="java -ea -Djava.io.tmpdir=$OUT/tmp"
digest() { # classes vm lib seed -> sha (stderr trace to $OUT/trace.err)
    if [ -z "$3" ]; then $J $2 -cp "$1" jdk.m3.collections.RankStackRestoredDigest 400 $4 2> "$OUT/trace.err" | grep -o 'sha256=.*'
    else $J $2 -Xcheck:jni -Drankstack.trace=true -Dm3.collections.rank.native=true -Djava.library.path="$3" -cp "$1" \
        jdk.m3.collections.RankStackRestoredDigest 400 $4 2> "$OUT/trace.err" | grep -o 'sha256=.*'; fi
}
echo "== 3. restored-API digest: Java vs master library vs restored library"
for seed in 20261107 1882; do
    for vm in "" "-Xint"; do
        a=$(digest "$OUT/classes" "$vm" "" $seed)
        b=$(digest "$OUT/classes" "$vm" "$OUT/old" $seed); ule=$(grep -c UnsatisfiedLinkError "$OUT/trace.err"); w=$(grep -ci 'warning' "$OUT/trace.err")
        c=$(digest "$OUT/classes" "$vm" "$OUT/new" $seed)
        echo "seed=$seed vm=${vm:-default} java $a master-library $b (UnsatisfiedLinkError digested: $ule, JNI warnings: $w) restored-library $c"
        [ "$a" = "$b" ] && [ "$a" = "$c" ] && [ "$ule" = 0 ] && [ "$w" = 0 ] || fail "digest seed=$seed vm=$vm"
    done
done
echo "== 4. contracts of the fallback callers on the master library (Java-path arguments)"
for c in $CONTRACTS; do
    arg=""
    for vm in "" "-Xint"; do
        $J $vm -Xcheck:jni -Dm3.collections.rank.native=true -Djava.library.path="$OUT/old" -cp "$OUT/classes" jdk.m3.collections.$c $arg \
            > "$OUT/run.out" 2> "$OUT/run.err"; rc=$?
        echo "master-library ${vm:-default} $c rc=$rc jni_warnings=$(grep -ci warning "$OUT/run.err") $(grep -E '^PASS' "$OUT/run.out" | cut -c1-110)"
        if [ $rc != 0 ]; then
            IFS='|' read -r pc pm px <<< "$DIRECT_PROBE"
            if [ "$c" = "$pc" ] && grep -q "\.$pm(" "$OUT/run.err" && grep -q "$px" "$OUT/run.err"; then
                echo "  DIRECT_EXPORT_PROBE $c: every owner-API check passed; the final reflective $px probe needs that export"
            else fail "$c on master library $vm"; fi
        fi
    done
done
echo "== 5. mutants: each new fallback rethrowing (pre-fix behaviour) must make the master-library digest fail"
n=0; caught=0
for flag in BOUND_BATCH_ABI_MISSING RANGE_COUNT_ABI_MISSING INTERSECTION_COUNT_ABI_MISSING; do
    n=$((n + 1)); mkdir -p "$OUT/mut/$flag/src/jdk/m3" "$OUT/mut/$flag/classes"
    cp -r "$MODULE/src/main/java/jdk/m3/collections" "$OUT/mut/$flag/src/jdk/m3/collections"
    ln -s "$MODULE/../arrays/src/main/java/jdk/m3/arrays" "$OUT/mut/$flag/src/jdk/m3/arrays"
    sed -i "s/$flag = true;/throw olderNativeLibrary;/" "$OUT/mut/$flag/src/jdk/m3/collections/M3LongRankBatchNative.java"
    if ! javac --release 21 -proc:none -Xlint:all,-module -implicit:class -nowarn -sourcepath "$OUT/mut/$flag/src" -d "$OUT/mut/$flag/classes" \
        "$OUT/mut/$flag/src/jdk/m3/collections/M3LongRankBatchNative.java" "$RESTORE/oracle/jdk/m3/collections/RankStackRestoredDigest.java" \
        2> "$OUT/mut/$flag.javac"; then fail "mutant $flag javac"; continue; fi
    a=$(digest "$OUT/classes" "" "" 20261107)
    b=$(digest "$OUT/mut/$flag/classes" "" "$OUT/old" 20261107)
    if [ "$a" != "$b" ]; then caught=$((caught + 1)); echo "mutant $flag caught: master-library digest $b != java $a ($(grep -o "UnsatisfiedLinkError: '[^(]*" "$OUT/trace.err" | sort -u | sed 's/.*\.//' | tr '\n' ' '))"
    else fail "mutant $flag survived"; fi
done
echo "mutants caught=$caught/$n"
echo "OLDLIB_GATE $status"
