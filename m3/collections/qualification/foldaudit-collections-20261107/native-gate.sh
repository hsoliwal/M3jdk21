#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of qualification/foldaudit-collections-20261107 for a Linux host (2026-11-07 run: WSL Ubuntu 24.04, gcc 13.3,
# Temurin 21). Library m3counts = m3_count_lanes.c + m3_count_totals.c (the restored count atoms of PR #1084).
#   1. javac --release 21 -Xlint:all,-module -Werror -h of the restored owners (sourcepath, no module-info), the origin tests
#      and the digest oracle; every Java_* prototype of the two headers must equal its C definition.
#   2. gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared through -include of both headers; nm -D exports = 2.
#   3. FoldauditCountsDigest: Java vs native (-Xcheck:jni, default and -Xint), two seeds: digests must be equal.
#   4. origin tests (JUnit console): Java path without a library; native-required (-Dm3.test.native.required=true) with the
#      library, default and -Xint; native-required WITHOUT a library must refuse (the origin's never-skip contract).
#   5. mutants: one per restored C export (inside the named function) and four Java mutants of the restored owners; every
#      mutant must fail the origin tests or the digest.
# usage: native-gate.sh <out-dir> [junit-console-standalone.jar] [junit-jupiter-api.jar] [opentest4j.jar] [apiguardian.jar]
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$HERE/../.." && pwd)
OUT=$1
M2=${M2:-/mnt/s/m3-scratch/eb-conv/m2}
CONSOLE=${2:-$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar}
JUPITER=${3:-$M2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar}
OPENTEST=${4:-$(ls $M2/org/opentest4j/opentest4j/*/*.jar | head -1)}
APIGUARD=${5:-$(ls $M2/org/apiguardian/apiguardian-api/*/*.jar | head -1)}
JDK=${JAVA_HOME:-$(ls -d /root/jdk/jdk-21* "$HOME"/jdk/jdk-21* 2>/dev/null | head -1)}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
rm -rf "$OUT"; mkdir -p "$OUT"/{classes,tests,hdr,lib,mut,src/jdk/m3,tmp}
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
S="$MODULE/src/main/java/jdk/m3/collections"
T="$MODULE/src/test/java/jdk/m3/collections"
N="$MODULE/src/main/native/collections"
FLAGS="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared"
INC="-I$JDK/include -I$JDK/include/linux -I$OUT/hdr"
HDR="-include jdk_m3_collections_M3CountLanes.h -include jdk_m3_collections_M3CountTotals.h"
J="java -ea -Djava.io.tmpdir=$OUT/tmp -XX:-UsePerfData"
java -version 2>&1 | head -1; gcc --version | head -1
echo "lanes.c sha256=$(sha256sum "$N/m3_count_lanes.c" | cut -c1-16) totals.c sha256=$(sha256sum "$N/m3_count_totals.c" | cut -c1-16)"

step "1. javac -h (owners via -sourcepath; origin tests; digest oracle)"
ln -s "$S" "$OUT/src/jdk/m3/collections"
ln -s "$MODULE/../arrays/src/main/java/jdk/m3/arrays" "$OUT/src/jdk/m3/arrays"
javac --release 21 -proc:none -Xlint:all,-module -Werror -implicit:class -sourcepath "$OUT/src" -h "$OUT/hdr" -d "$OUT/classes" \
    "$S/M3CountLanes.java" "$S/M3CountTotals.java" "$S/M3LongBag.java" "$HERE/oracle/jdk/m3/collections/FoldauditCountsDigest.java" || fail "javac owners"
javac --release 21 -proc:none -Xlint:all,-module -Werror -d "$OUT/tests" -cp "$OUT/classes:$JUPITER:$OPENTEST:$APIGUARD" \
    "$T/M3CountLanesTest.java" "$T/M3CountTotalsTest.java" || fail "javac tests"
python3 - "$OUT/hdr" "$N/m3_count_lanes.c" "$N/m3_count_totals.c" > "$OUT/prototypes.txt" <<'PY' || fail "prototype mismatch"
import glob, re, sys
heads = ''.join(open(p).read() for p in glob.glob(sys.argv[1] + '/*.h'))
src = ''.join(open(p).read() for p in sys.argv[2:])
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
sys.exit(1 if bad or len(hs) != 2 else 0)
PY
tail -1 "$OUT/prototypes.txt"

step "2. gcc and nm -D"
gcc $FLAGS $INC $HDR "$N/m3_count_lanes.c" "$N/m3_count_totals.c" -o "$OUT/lib/libm3counts.so" || fail "gcc"
nm -D --defined-only "$OUT/lib/libm3counts.so" | awk '/ Java_/{print $3}' | sort | tee "$OUT/exports.txt"
echo "exports=$(wc -l < "$OUT/exports.txt")"
[ "$(wc -l < "$OUT/exports.txt")" = 2 ] || fail "export count"
LIB="$OUT/lib/libm3counts.so"

step "3. FoldauditCountsDigest: Java vs native, default and -Xint, two seeds"
for seed in 20261107 1084; do
    for vm in "" "-Xint"; do
        a=$($J $vm -cp "$OUT/classes" jdk.m3.collections.FoldauditCountsDigest 1500 $seed | grep -o 'sha256=.*')
        b=$($J $vm -Xcheck:jni -Dfoldaudit.native=true -Dfoldaudit.library="$LIB" -cp "$OUT/classes" jdk.m3.collections.FoldauditCountsDigest 1500 $seed 2> "$OUT/digest.err")
        echo "digest seed=$seed vm=${vm:-default} java $a native ${b##*sha256=} ($(echo "$b" | grep -o 'checks=[0-9]*'), jni_warnings=$(grep -ci warning "$OUT/digest.err"))"
        [ "$a" = "sha256=${b##*sha256=}" ] || fail "digest seed=$seed $vm"
    done
done

CP="$OUT/classes:$OUT/tests:$JUPITER:$OPENTEST:$APIGUARD"
junit() { # label vmflags... (classes M3CountLanesTest M3CountTotalsTest)
    local label=$1; shift
    $J "$@" -jar "$CONSOLE" execute -cp "$CP" --disable-banner --details=summary --fail-if-no-tests \
        -c jdk.m3.collections.M3CountLanesTest -c jdk.m3.collections.M3CountTotalsTest > "$OUT/junit.out" 2> "$OUT/junit.err"
    local rc=$?
    echo "$label rc=$rc $(grep -E 'tests (successful|failed)' "$OUT/junit.out" | tr -s ' ' | tr '\n' ' ') jni_warnings=$(grep -ci 'warning' "$OUT/junit.err")"
    return $rc
}
step "4. origin tests: Java path; native-required default and -Xint; refusal without a library"
junit "java-path   " || fail "java path tests"
NATIVE="-Dm3.counted.native=$LIB -Dm3.totals.native=$LIB -Dm3.test.native.required=true -Xcheck:jni"
junit "native      " $NATIVE || fail "native tests"
junit "native -Xint" $NATIVE -Xint || fail "native -Xint tests"
if junit "refusal     " -Dm3.test.native.required=true; then fail "native-required without a library did not refuse"; else echo "  refusal confirmed: $(grep -m1 -o 'never skip[^"]*\|never skipped' "$OUT/junit.out" | head -1)"; fi

step "5. mutants"
# id | file | function or class | old | new | judged by (junit|digest)
MUTANTS="C01|m3_count_lanes.c|Java_jdk_m3_collections_M3CountLanes_combineNative|if (a[i] > INT64_MAX - b[i]) {|if (a[i] > INT64_MAX - b[i] - 1) {|junit
C02|m3_count_totals.c|Java_jdk_m3_collections_M3CountTotals_combineNative|default: value = a[i] > b[i] ? a[i] - b[i] : 0; break;|default: value = a[i] >= b[i] ? a[i] - b[i] : 1; break;|junit
C03|m3_count_lanes.c|Java_jdk_m3_collections_M3CountLanes_combineNative|case 2: value = a[i] < b[i] ? a[i] : b[i]; break;|case 2: value = a[i] > b[i] ? a[i] : b[i]; break;|digest
J01|M3CountTotals.java|M3CountTotals|total[0] = Math.addExact(total[0], Math.min(amount, lookup.count(key))));|total[0] = Math.addExact(total[0], Math.max(amount, lookup.count(key))));|junit
J02|M3CountTotals.java|M3CountTotals|return Math.addExact(left.totalCount() - overlap, right.totalCount());|return Math.addExact(left.totalCount(), right.totalCount());|junit
J03|M3CountLanes.java|M3CountLanes|case UNION -> Math.max(a, b);|case UNION -> Math.min(a, b);|junit
J04|M3CountTotals.java|M3CountTotals|total = Math.addExact(total, value);|total = total + value;|junit"
caught=0; total=0
while IFS='|' read -r id file fn old new judge; do
    [ -n "$id" ] || continue
    total=$((total + 1)); mkdir -p "$OUT/mut/$id"
    case $file in *.c) src="$N/$file";; *) src="$S/$file";; esac
    python3 - "$src" "$OUT/mut/$id/$file" "$fn" "$old" "$new" <<'PY' || { fail "$id not applicable"; continue; }
import re, sys
src, dst, fn, old, new = sys.argv[1:6]
text = open(src).read()
old = old.replace('\\"', '"').replace('\\&', '&'); new = new.replace('\\"', '"').replace('\\&', '&')
start = text.index(fn)
if old not in text[start:]:
    sys.exit(f'{fn}: {old!r} absent')
open(dst, 'w').write(text[:start] + text[start:].replace(old, new, 1))
PY
    if [ "${file##*.}" = c ]; then
        other=$([ "$file" = m3_count_lanes.c ] && echo "$N/m3_count_totals.c" || echo "$N/m3_count_lanes.c")
        gcc $FLAGS $INC $HDR "$OUT/mut/$id/$file" "$other" -o "$OUT/mut/$id/libm3counts.so" 2> "$OUT/mut/$id.gcc" || { fail "$id gcc"; continue; }
        MLIB="$OUT/mut/$id/libm3counts.so"; MCP="$OUT/classes"
    else
        mkdir -p "$OUT/mut/$id/classes"
        jother=$([ "$file" = M3CountTotals.java ] && echo "$S/M3CountLanes.java" || echo "$S/M3CountTotals.java")
        javac --release 21 -proc:none -implicit:class -sourcepath "$OUT/src" -d "$OUT/mut/$id/classes" "$OUT/mut/$id/$file" "$jother" 2> "$OUT/mut/$id.javac" || { fail "$id javac"; continue; }
        MLIB="$LIB"; MCP="$OUT/mut/$id/classes:$OUT/classes"
    fi
    if [ "$judge" = digest ]; then
        a=$($J -cp "$OUT/classes" jdk.m3.collections.FoldauditCountsDigest 1500 20261107 | grep -o 'sha256=.*')
        b=$($J -Dfoldaudit.native=true -Dfoldaudit.library="$MLIB" -cp "$MCP" jdk.m3.collections.FoldauditCountsDigest 1500 20261107 2>&1 | grep -o 'sha256=.*\|AssertionError.*' | head -1)
        if [ "$a" != "$b" ]; then caught=$((caught + 1)); echo "$id $fn caught by digest: java $a native ${b:0:60}"; else fail "$id survived digest"; fi
    else
        $J -Dm3.counted.native="$MLIB" -Dm3.totals.native="$MLIB" -Dm3.test.native.required=true -Xcheck:jni -jar "$CONSOLE" execute \
            -cp "$MCP:$OUT/tests:$JUPITER:$OPENTEST:$APIGUARD" --disable-banner --details=summary --fail-if-no-tests \
            -c jdk.m3.collections.M3CountLanesTest -c jdk.m3.collections.M3CountTotalsTest > "$OUT/mut/$id.out" 2>&1
        if [ $? -ne 0 ]; then caught=$((caught + 1)); echo "$id $fn caught by tests: $(grep -E 'tests failed' "$OUT/mut/$id.out" | tr -s ' ')"; else fail "$id survived tests"; fi
    fi
done <<< "$MUTANTS"
echo "mutants caught=$caught/$total"
echo "NATIVE_GATE $status"
