#!/bin/bash
# Gate for the #15117 union on m3/tooling/search: module verify scripts on the union stage, then the three
# com.synexia step probes (empty keys, empty range, lower-bound reuse) against the union Java + C, plus two
# mutants that each break one #15117 feature and must be rejected.
set -u
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8
export PATH=$JAVA_HOME/bin:$PATH
U=stage_u/m3/tooling/search
C=stage/m3/tooling/search
R=synroot/synexia-openrewrite-recipes/recipe-crates
K=$R/m3jdk-sorted-empty-keys-20261101/verification
G=$R/m3jdk-sorted-empty-range-20261101/verification
L=$R/m3jdk-sorted-lower-reuse-20261102/verification
P=jdk/m3/tooling/search
W=/tmp/union15117; rm -rf $W; mkdir -p $W/logs
row() { printf 'GATE\t%s\t%s\n' "$1" "$2"; }
run() { local name=$1; shift; "$@" > "$W/logs/$name.log" 2>&1; row "$name" "rc=$? $(tail -1 "$W/logs/$name.log" | cut -c1-150)"; }
run module-int-verify bash $U/verify.sh
run module-long-verify bash $U/verify-long-bounds.sh
build() {  # <java-src> <c-src> <out>
  local j=$1 c=$2 o=$3; mkdir -p $o/classes
  javac --release 21 -proc:none -Xlint:all -Werror -h $o/classes -d $o/classes "$j" $U/src/main/java/$P/M3NativeSortedIntBounds.java \
    $U/src/test/java/$P/M3SortedIntBoundsSelfTest.java $K/java/$P/M3NativeEmptyBatchProbe.java \
    $G/java/$P/M3NativeEmptyRangeProbe.java $L/java/$P/M3SortedUpperReuseProbe.java || return 1
  local cc=(gcc -std=c17 -O2 -Wall -Wextra -Werror -Wpedantic -fPIC -shared -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -I $o/classes)
  "${cc[@]}" "$c" -o $o/n.so && "${cc[@]}" -Dmalloc=m3_test_malloc "$c" $K/native/fail_malloc.c -o $o/k.so \
    && "${cc[@]}" -Dmalloc=m3_fail_malloc "$c" $L/native/fail_malloc.c -o $o/l.so
}
jrun() { local o=$1 so=$2; shift 2; java -Xcheck:jni -Dm3.native.sorted.search.library=$o/$so.so -cp $o/classes "$@" 2>&1; }
probes() {  # <out> : prints PASS rows; returns nonzero on the first failed expectation
  local o=$1
  jrun $o k jdk.m3.tooling.search.M3NativeEmptyBatchProbe empty | grep -Fxq 'M3_NATIVE_EMPTY_KEYS_PASS|result=0' || { echo FAIL empty-keys; return 1; }
  jrun $o k jdk.m3.tooling.search.M3NativeEmptyBatchProbe nonempty | grep -Fxq 'M3_NATIVE_NONEMPTY_OOM_EXPECTED' || { echo FAIL nonempty-oom; return 1; }
  jrun $o n jdk.m3.tooling.search.M3NativeEmptyBatchProbe spectrum | grep -Fxq 'M3_NATIVE_SORTED_SPECTRUM_PASS|checks=153912' || { echo FAIL spectrum; return 1; }
  jrun $o n jdk.m3.tooling.search.M3NativeEmptyRangeProbe | grep -Fxq 'M3_NATIVE_EMPTY_RANGE_PASS|cases=6732|result_ints=7616136|validation=8' || { echo FAIL empty-range; return 1; }
  jrun $o n jdk.m3.tooling.search.M3SortedIntBoundsSelfTest | grep -Eq 'M3_SORTED_INT_BOUNDS_TEST.*java=PASS.*native_jni=PASS' || { echo FAIL selftest; return 1; }
  for mode in "-Xint" "-Xbatch -XX:TieredStopAtLevel=1" "-Xbatch -XX:-TieredCompilation"; do
    JAVA_TOOL_OPTIONS="$mode" jrun $o n jdk.m3.tooling.search.M3SortedUpperReuseProbe \
      | grep -Fq 'M3_UPPER_REUSE_PASS checks=3530681 queries=2995645 cases=107004 digest=eb18d80247d35a3a' || { echo "FAIL upper-reuse $mode"; return 1; }
  done
  jrun $o l jdk.m3.tooling.search.M3SortedUpperReuseProbe failing-malloc | grep -Fq 'M3_UPPER_REUSE_OOME_PASS checks=3' || { echo FAIL oome; return 1; }
  echo "PASS empty-keys nonempty-oom spectrum=153912 empty-range=6732 selftest upper-reuse=3530681x3modes oome"
}
build $U/src/main/java/$P/M3SortedIntBounds.java $U/src/main/native/m3_native_sorted_bounds.c $W/union && run union-probes probes $W/union
build $C/src/main/java/$P/M3SortedIntBounds.java $C/src/main/native/m3_native_sorted_bounds.c $W/chain
javap -public -s -cp $W/chain/classes jdk.m3.tooling.search.M3SortedIntBounds > $W/chain.api
javap -public -s -cp $W/union/classes jdk.m3.tooling.search.M3SortedIntBounds > $W/union.api
cmp -s $W/chain.api $W/union.api && row public-api "identical to master" || row public-api "CHANGED"
# Mutant 1 (C): reuse the memo for every later key, not only an equal adjacent key.
sed 's/if (!has_previous || needle != previous) {/if (!has_previous) {/' $U/src/main/native/m3_native_sorted_bounds.c > $W/mc.c
cmp -s $W/mc.c $U/src/main/native/m3_native_sorted_bounds.c && row mutant-c "NOT_APPLIED"
build $U/src/main/java/$P/M3SortedIntBounds.java $W/mc.c $W/mut-c && { probes $W/mut-c > $W/logs/mut-c.log 2>&1 && row mutant-c "SURVIVED" || row mutant-c "rejected: $(tail -1 $W/logs/mut-c.log)"; }
# Mutant 2 (Java): reuse the previous bounds for any key not below its predecessor.
sed 's/needle == keys\[i - 1\]/needle >= keys[i - 1]/' $U/src/main/java/$P/M3SortedIntBounds.java > $W/M3SortedIntBounds.java
cmp -s $W/M3SortedIntBounds.java $U/src/main/java/$P/M3SortedIntBounds.java && row mutant-java "NOT_APPLIED"
build $W/M3SortedIntBounds.java $U/src/main/native/m3_native_sorted_bounds.c $W/mut-j && { probes $W/mut-j > $W/logs/mut-j.log 2>&1 && row mutant-java "SURVIVED" || row mutant-java "rejected: $(tail -1 $W/logs/mut-j.log)"; }
cp -r $W/logs logs_union 2>/dev/null
