#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Interop mutants of qualification/foldaudit-collections-20261107 (Windows JDK 21 after the real-pom interop build): one
# sed mutant per restored interop owner member, compiled with javac against the built classes, judged by the origin test
# M3BagInteropTest through the JUnit console launcher. usage: interop_mutants.sh <out-dir> <worktree> <junit console jar> <guava jar> <eclipse jar> <eclipse api jar> <m3-arrays jar> <jupiter api> <opentest4j> <apiguardian>
set -uo pipefail
OUT=$1; WT=$2; CONSOLE=$3; GUAVA=$4; ECL=$5; ECLAPI=$6; ARRAYS=$7; JUPITER=$8; OPENTEST=$9; APIGUARD=${10}
JDK=${JAVA_HOME:-C:/Program Files/Java/jdk-21}
I="$WT/m3/collections/interop"
S="$I/src/main/java/jdk/m3/collections/interop"
RT="$I/target/classes;$I/target/test-classes;$WT/m3/collections/target/classes;$ARRAYS;$GUAVA;$ECL;$ECLAPI"
rm -rf "$OUT"; mkdir -p "$OUT"
status=PASS; caught=0; total=0
# id | file | member | old | new
MUTANTS="J05|M3GuavaLongMultiset.java|public boolean setCount(Long value, int expected, int desired)|if (count(value) != expected) return false;|if (count(value) == expected) return false;
J06|M3GuavaLongMultiset.java|public int add(Long value, int occurrences)|if (occurrences != 0) { counts.add(key, occurrences); version++; }|if (occurrences != 0) { counts.add(key, occurrences); }
J07|M3LibraryBags.java|public static M3LongBag fromJdk(Collection<Long> source)|result.add(Objects.requireNonNull(key, \"key\"), 1L);|result.add(Objects.requireNonNull(key, \"key\"), 2L);"
while IFS='|' read -r id file member old new; do
    [ -n "$id" ] || continue
    total=$((total + 1)); mkdir -p "$OUT/$id/classes"
    python -I - "$S/$file" "$OUT/$id/$file" "$member" "$old" "$new" <<'PY' || { echo "FAIL: $id not applicable"; status=FAIL; continue; }
import sys
src, dst, member, old, new = sys.argv[1:6]
text = open(src, encoding='utf-8').read()
old = old.replace('\\"', '"'); new = new.replace('\\"', '"')
start = text.index(member)
if old not in text[start:]:
    sys.exit(f'{member}: {old!r} absent')
open(dst, 'w', encoding='utf-8', newline='\n').write(text[:start] + text[start:].replace(old, new, 1))
PY
    "$JDK/bin/javac" --release 21 -proc:none -d "$OUT/$id/classes" -cp "$RT" "$OUT/$id/$file" 2> "$OUT/$id.javac" || { echo "FAIL: $id javac"; status=FAIL; continue; }
    "$JDK/bin/java" -ea -Djava.io.tmpdir="$OUT" -XX:-UsePerfData -jar "$CONSOLE" execute -cp "$OUT/$id/classes;$RT" \
        --disable-banner --details=summary --fail-if-no-tests -c jdk.m3.collections.interop.M3BagInteropTest > "$OUT/$id.out" 2>&1
    if [ $? -ne 0 ]; then caught=$((caught + 1)); echo "$id $member caught: $(grep -E 'tests failed' "$OUT/$id.out" | tr -s ' ')"; else echo "FAIL: $id survived"; status=FAIL; fi
done <<< "$MUTANTS"
"$JDK/bin/java" -ea -Djava.io.tmpdir="$OUT" -XX:-UsePerfData -jar "$CONSOLE" execute -cp "$RT" --disable-banner --details=summary --fail-if-no-tests \
    -c jdk.m3.collections.interop.M3BagInteropTest > "$OUT/unmutated.out" 2>&1 && echo "unmutated: $(grep -E 'tests successful' "$OUT/unmutated.out" | tr -s ' ')" || { echo "FAIL: unmutated run"; status=FAIL; }
echo "interop mutants caught=$caught/$total"
echo "INTEROP_MUTANTS $status"
