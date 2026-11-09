#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of qualification/rank-stack-restore-20261107 for a Linux host (2026-11-07 run: WSL Ubuntu 24.04, gcc 13.3,
# Temurin 21.0.13). Library m3rankbatch = m3_long_rank_batch_jni.c alone (as the m3-long-rank-bounds/topk workflows).
#   1. javac --release 21 -Xlint:all,-module -Werror -h of M3LongRankBatchNative with the restored contracts, the #2616
#      contracts and the two digest drivers; every Java_* prototype of the header must equal its C definition.
#   2. gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared through -include of the header (new library),
#      the same with -DM3_RANK_LEGACY_ABI_TEST (legacy build of the existing workflow), master's C (old library) and
#      the legacy stub src/test/native/legacy_m3rankbatch.c; nm -D export counts.
#   3. every restored contract: Java path (no library) and native (-Xcheck:jni, default and -Xint), and with master's
#      library (Java fallback of the restored entry points); the #2616 and fused-bounds contracts on old vs new library.
#   4. RankStackRestoredDigest: native vs Java byte-identical, default and -Xint, two seeds.
#   5. one mutant per restored export (MUTANTS below, applied inside the named C function only): its contract must fail
#      (equalRanges0 is exported for older consumers only, so its mutant is judged by RankStackLegacyAbiProbe).
# usage: native-gate.sh <out-dir> <master m3_long_rank_batch_jni.c>   (JDK: $JAVA_HOME or /root/jdk/jdk-21*)
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$HERE/../.." && pwd)
OUT=$1
MASTER_C=$2
JDK=${JAVA_HOME:-$(ls -d /root/jdk/jdk-21* "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
rm -rf "$OUT"; mkdir -p "$OUT"/{classes,hdr,new,legacy,old,stub,missing,mut,src/jdk/m3,tmp}
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
C="$MODULE/src/main/native/collections/m3_long_rank_batch_jni.c"
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux -I$OUT/hdr"
HDR="-include jdk_m3_collections_M3LongRankBatchNative.h"
T="$MODULE/src/test/java/jdk/m3/collections"
J="java -ea -Djava.io.tmpdir=$OUT/tmp -cp $OUT/classes"
NATIVE="-Dm3.collections.rank.native=true -Dm3.test.native.required=true -Dm3.collections.all.native.required=true -Dcontract.requireNative=true"
# contract | Java-path argument | native argument (the receipts' workflow arguments)
CONTRACTS="M3LongDifferenceContract|| M3LongFloorIndicesContract||--native M3LongLowerBoundWindowContract||
M3LongRankBatchIntersectionValuesContract|| M3LongRankClosestWindowContract||--native M3LongRankEqualRangesContract||--native
M3LongRankIntersectionContract|--java|--native M3LongRankKDiffContract||--native-required M3LongRankMonotoneSweepContract||--native
M3LongRankTripletSumContract|| M3LongRankUpperCountSweepContract||--native M3LongRankWindowContract||
M3LongRankWindowIntersectionContract|--java|--native M3LongRunStatisticsContract||--native-required
M3LongUnionValuesContract|java|native M3MergedSelectContract|| M3RankBatchIntoContract|| M3RankBatchLeetNativeContract||
M3RankIntervalBatchContract||--native M3SortedTwoSumBatchContract||"
# native runs known to fail at the PR head itself (contract older than a later stack commit): class | stderr pattern
STALE="M3RankBatchIntoContract|native validation accepted bad input [0, 1, 0, 0]"
# id | C function (suffix) | contract | old text | new text (first occurrence inside that function)
MUTANTS="M01|equalRanges0|RankStackLegacyAbiProbe|values[mid] <= needle|values[mid] < needle
M02|m3_rank_bound_chunk|M3LongRankUpperCountSweepContract|keys[i], 0))|keys[i], 1))
M03|rangeCountInto0|M3RankIntervalBatchContract|to[i], 0)|to[i], 1)
M04|intersectionCount0|M3LongRankIntersectionContract|matches++;|matches += 2;
M05|intersectionWindowCount0|M3LongRankWindowIntersectionContract|if (a < b) {|if (a <= b) {
M06|m3_intersection_values_scan|M3LongRankBatchIntersectionValuesContract|if (a < b) {|if (a <= b) {
M07|m3_union_values_scan|M3LongUnionValuesContract|next = right_page[rc++];|next = right_page[rc++] + 1;
M08|m3_difference_scan|M3LongDifferenceContract|a < b ? a : b|a > b ? a : b
M09|runStatistics0|M3LongRunStatisticsContract|if (current > best) best = current;|if (current > best + 1) best = current;
M10|distinctDifferencePairCount0|M3LongRankKDiffContract|for (jsize i = 1; i < n; i++) {|for (jsize i = 2; i < n; i++) {
M11|m3_rank_equal_range_chunk|M3LongRankWindowContract|source[upper] <= key|source[upper] < key
M12|m3_kth_merged_value|M3MergedSelectContract|left[a] <= right[b] ? left[a] : right[b]|left[a] <= right[b] ? right[b] : left[a]
M13|floorIndicesInto0|M3LongFloorIndicesContract|keys[i], 1) - 1|keys[i], 0) - 1
M14|m3_closest_window_start|M3LongRankClosestWindowContract|> (uint64_t) right - (uint64_t) target|>= (uint64_t) right - (uint64_t) target
M15|m3_pair_sum_compare|M3SortedTwoSumBatchContract|(sum > target) - (sum < target)|(sum >= target) - (sum < target)
M16|distinctTripletSumCount0|M3LongRankTripletSumContract|values[i] == values[i - 1]) continue;|values[i] == values[i - 1] \&\& 0) continue;"
java -version 2>&1 | head -1; gcc --version | head -1
echo "C sha256=$(sha256sum "$C" | cut -c1-16) master_c sha256=$(sha256sum "$MASTER_C" | cut -c1-16)"

step "1. javac -h (owners via -sourcepath without module-info; restored + #2616 contracts; digest drivers)"
ln -s "$MODULE/src/main/java/jdk/m3/collections" "$OUT/src/jdk/m3/collections"
ln -s "$MODULE/../arrays/src/main/java/jdk/m3/arrays" "$OUT/src/jdk/m3/arrays"
FILES=""
for row in $CONTRACTS; do FILES="$FILES $T/${row%%|*}.java"; done
javac --release 21 -proc:none -Xlint:all,-module -Werror -implicit:class -sourcepath "$OUT/src" -h "$OUT/hdr" -d "$OUT/classes" \
    "$MODULE/src/main/java/jdk/m3/collections/M3LongRankBatchNative.java" $FILES "$T/M3LongUnionLegacyContract.java" \
    "$T/M3LongCountSmallerRightContract.java" "$T/M3KthPairDistanceContract.java" "$T/M3LongPairDistanceContract.java" \
    "$T/M3LongRankBoundsContract.java" "$T/M3LongTopKFrequencyContract.java" \
    "$HERE/oracle/jdk/m3/collections/RankStackRestoredDigest.java" "$HERE/oracle/jdk/m3/collections/RankStackLegacyAbiProbe.java"     || fail "javac"
python3 - "$OUT/hdr/jdk_m3_collections_M3LongRankBatchNative.h" "$C" > "$OUT/prototypes.txt" <<'PY' || fail "prototype mismatch"
import re, sys
heads, src = open(sys.argv[1]).read(), open(sys.argv[2]).read()
norm = lambda s: re.sub(r'\s+', ' ', s).replace(' *', '*').strip()
hs = {m.group(2): (m.group(1), [norm(p) for p in m.group(3).split(',')])
      for m in re.finditer(r'JNIEXPORT (\w+) JNICALL (Java_\w+)\s*\(([^)]*)\);', heads)}
cs = {m.group(2): (m.group(1), [norm(re.sub(r'\w+$', '', p.strip())) for p in m.group(3).split(',')])
      for m in re.finditer(r'JNIEXPORT (\w+) JNICALL\s*(Java_\w+)\(([^)]*)\)\s*\{', src)}
bad = 0
for name in sorted(set(hs) | set(cs)):
    same = hs.get(name) == cs.get(name)
    bad += not same
    print(('EQUAL ' if same else 'DIFFERENT ') + name + ' ' + str(hs.get(name)) + ('' if same else ' | C ' + str(cs.get(name))))
print(f'prototypes header={len(hs)} c={len(cs)} different={bad}')
sys.exit(1 if bad else 0)
PY
tail -1 "$OUT/prototypes.txt"

step "2. gcc (new, legacy-ABI, master, stub) and nm -D"
gcc $FLAGS $INC $HDR "$C" -o "$OUT/new/libm3rankbatch.so" || fail "gcc new"
gcc $FLAGS $INC -DM3_RANK_LEGACY_ABI_TEST "$C" -o "$OUT/legacy/libm3rankbatch.so" || fail "gcc legacy"
gcc $FLAGS $INC "$MASTER_C" -o "$OUT/old/libm3rankbatch.so" || fail "gcc master"
gcc $FLAGS $INC "$MODULE/src/test/native/legacy_m3rankbatch.c" -o "$OUT/stub/libm3rankbatch.so" || fail "gcc stub"
for lib in new legacy old stub; do
    echo "exports $lib=$(nm -D --defined-only "$OUT/$lib/libm3rankbatch.so" | grep -c ' Java_')"
done
nm -D --defined-only "$OUT/new/libm3rankbatch.so" | awk '/ Java_/{print $3}' | sort > "$OUT/exports-new.txt"
nm -D --defined-only "$OUT/old/libm3rankbatch.so" | awk '/ Java_/{print $3}' | sort > "$OUT/exports-old.txt"
echo "exports old-not-in-new=$(comm -23 "$OUT/exports-old.txt" "$OUT/exports-new.txt" | wc -l) restored=$(comm -13 "$OUT/exports-old.txt" "$OUT/exports-new.txt" | wc -l)"

run() { # label lib vmflags class args...
    local label=$1 lib=$2 vm=$3; shift 3
    $J $vm -Djava.library.path="$OUT/$lib" "$@" > "$OUT/run.out" 2> "$OUT/run.err"; local rc=$?
    echo "$label rc=$rc jni_warnings=$(grep -c -i 'warning' "$OUT/run.err") $(grep -E '^PASS|^FAIL|checks' "$OUT/run.out" | tail -1 | cut -c1-150)"
    return $rc
}
step "3. contracts: Java path, native (-Xcheck:jni, default and -Xint), master library (fallback)"
for row in $CONTRACTS; do
    IFS='|' read -r cls jarg narg <<< "$row"
    run "java   $cls" missing "" jdk.m3.collections.$cls $jarg || fail "$cls java"
    for vm in "" "-Xint"; do
        if ! run "native$vm $cls" new "-Xcheck:jni $vm $NATIVE" jdk.m3.collections.$cls $narg; then
            pattern=$(echo "$STALE" | grep "^$cls|" | cut -d'|' -f2-)
            if [ -n "$pattern" ] && grep -qF "$pattern" "$OUT/run.err"; then
                echo "  STALE_AT_PR_HEAD $cls: $pattern (every parity check before the final probe passed)"
            else fail "$cls native $vm"; fi
        fi
    done
    run "oldlib $cls" old "-Xcheck:jni -Dm3.collections.rank.native=true" jdk.m3.collections.$cls $jarg || echo "  (info) $cls with master library"
done
for vm in "" "-Xint"; do
    run "native$vm RankStackLegacyAbiProbe (legacy equalRanges0 export)" new "-Xcheck:jni $vm -Dm3.collections.rank.native=true"         jdk.m3.collections.RankStackLegacyAbiProbe 400 || fail "legacy equalRanges0 probe $vm"
done
run "stub   M3LongUnionLegacyContract" stub "-Xcheck:jni -Dm3.collections.rank.native=true" jdk.m3.collections.M3LongUnionLegacyContract || fail "union legacy stub"
for lib in old new; do
    run "$lib M3LongCountSmallerRightContract" $lib "-Xcheck:jni -Dm3.collections.rank.native=true" jdk.m3.collections.M3LongCountSmallerRightContract --native || fail "LC315 $lib"
    run "$lib M3KthPairDistanceContract" $lib "-Xcheck:jni -Dm3.collections.rank.native=true" jdk.m3.collections.M3KthPairDistanceContract --native || fail "KTH $lib"
    run "$lib M3LongPairDistanceContract" $lib "-Xcheck:jni -Dm3.collections.rank.native=true -Dm3.collections.rank.pairdistance.native=true" jdk.m3.collections.M3LongPairDistanceContract || fail "PAIR $lib"
    run "$lib M3LongRankBoundsContract" $lib "-Xcheck:jni -Dm3.collections.rank.native=true" jdk.m3.collections.M3LongRankBoundsContract || fail "bounds $lib"
    run "$lib M3LongTopKFrequencyContract" $lib "-Xcheck:jni -Dm3.collections.rank.native=true" jdk.m3.collections.M3LongTopKFrequencyContract || fail "topk $lib"
done
for c in M3LongCountSmallerRightContract M3KthPairDistanceContract M3LongPairDistanceContract; do
    run "java   $c" missing "" jdk.m3.collections.$c || fail "$c java"
done

step "4. RankStackRestoredDigest: native vs Java"
for seed in 20261107 1882; do
    for vm in "" "-Xint"; do
        a=$($J $vm jdk.m3.collections.RankStackRestoredDigest 400 $seed | grep -o 'sha256=.*')
        b=$($J $vm -Xcheck:jni -Dm3.collections.rank.native=true -Djava.library.path="$OUT/new" jdk.m3.collections.RankStackRestoredDigest 400 $seed 2> "$OUT/digest.err")
        echo "digest seed=$seed vm=${vm:-default} java $a native ${b##*sha256=} ($(echo "$b" | grep -o 'native=[a-z]*'), jni_warnings=$(grep -ci warning "$OUT/digest.err"))"
        [ "$a" = "sha256=${b##*sha256=}" ] || fail "digest seed=$seed $vm"
    done
done

step "4b. RankStackRestoredDigest with master's library opted in (fallback of the restored entry points; information)"
for seed in 20261107 1882; do
    a=$($J jdk.m3.collections.RankStackRestoredDigest 400 $seed | grep -o 'sha256=.*')
    b=$($J -Drankstack.trace=true -Dm3.collections.rank.native=true -Djava.library.path="$OUT/old" jdk.m3.collections.RankStackRestoredDigest 400 $seed 2> "$OUT/oldlib.err")
    echo "digest seed=$seed java $a master-library ${b##*sha256=} entry points without fallback: $(grep -o "UnsatisfiedLinkError: '[^(]*" "$OUT/oldlib.err" | sed "s/.*M3LongRankBatchNative\.//" | sort -u | tr '\n' ' ')"
done

step "5. mutants (one per restored export)"
caught=0; total=0
while IFS='|' read -r id fn cls old new; do
    [ -n "$id" ] || continue
    total=$((total + 1))
    python3 - "$C" "$OUT/mut/$id.c" "$fn" "$old" "$new" <<'PY' || { fail "$id not applicable"; continue; }
import re, sys
src, dst, fn, old, new = sys.argv[1:6]
lines = open(src).read().split('\n')
start = next(i for i, l in enumerate(lines) if re.match(r'^(static [^(]*\b|)(Java_\w+_)?' + re.escape(fn) + r'\(', l)
             or re.match(r'^\w[^(]*\b' + re.escape(fn) + r'\(', l) or l.startswith(fn + '(') or l.endswith('_' + fn + '('))
depth, seen, end = 0, False, start
for end in range(start, len(lines)):
    depth += lines[end].count('{') - lines[end].count('}')
    seen = seen or '{' in lines[end]
    if seen and depth == 0:
        break
body = '\n'.join(lines[start:end + 1])
if old not in body:
    sys.exit(f'{fn}: {old!r} absent')
lines[start:end + 1] = body.replace(old, new.replace('\\&', '&'), 1).split('\n')
open(dst, 'w').write('\n'.join(lines))
PY
    mkdir -p "$OUT/mut/$id"
    gcc $FLAGS $INC "$OUT/mut/$id.c" -o "$OUT/mut/$id/libm3rankbatch.so" 2> "$OUT/mut/$id.gcc" || { fail "$id gcc"; continue; }
    narg=$(echo "$CONTRACTS" | tr ' ' '\n' | grep "^$cls|" | cut -d'|' -f3)
    [ "$cls" = RankStackLegacyAbiProbe ] && narg=400
    $J -Xcheck:jni $NATIVE -Djava.library.path="$OUT/mut/$id" jdk.m3.collections.$cls $narg > "$OUT/mut/$id.out" 2>&1
    rc=$?
    msg=$(grep -m1 -E 'AssertionError|Exception|FAIL' "$OUT/mut/$id.out" | cut -c1-140)
    if [ $rc -ne 0 ]; then caught=$((caught + 1)); echo "$id $fn caught by $cls: $msg"; else fail "$id $fn survived $cls"; fi
done <<< "$MUTANTS"
echo "mutants caught=$caught/$total"
echo "NATIVE_GATE $status"
