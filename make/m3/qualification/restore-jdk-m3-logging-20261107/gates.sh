#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates of the jdk.m3.logging restore (GATES.tsv): standalone module compile, the module's tests through the JUnit
# console launcher (jtreg not installed), a fixture differential against the module compiled from the receiver head
# 7c23ff3612ef, and javap over both builds. Nothing here writes into the repository.
#
# usage: bash gates.sh <repo root> <out dir on S:> [JDK21 home] [junit-platform-console-standalone jar]
set -u
ROOT=$1
OUT=$2
JDK=${3:-"/c/Program Files/Java/jdk-21"}
JUNIT=${4:-S:/m3-scratch/m3jdk-release-audit/junit-platform-console-standalone-1.12.2.jar}
TMP="-Djava.io.tmpdir=S:/m3-scratch/eb-conv/m3log/tmp"
RECEIVER=7c23ff3612ef08dc265bd3bab5461e4dcf3ee47c
MOD=src/jdk.m3.logging/share/classes
SOURCES="$MOD/module-info.java $MOD/jdk/m3/logging/M3StructuredFormatter.java $MOD/jdk/m3/logging/package-info.java"
RECIPE=m3/qualification/restore-jdk-m3-logging-20261107
mkdir -p "$OUT/head/mods" "$OUT/base/mods" "$OUT/head/test" "$OUT/src7c23" "$OUT/diff" "$OUT/javap" "$OUT/logs"
WOUT=$(cd "$OUT" && pwd -W 2>/dev/null || pwd)

cd "$ROOT" || exit 9
echo "GATE module_javac_xlint_all (expected: exactly the [module] name warning that Java.gmk disables)"
"$JDK/bin/javac" -J$TMP --release 21 -Xlint:all -Werror -d "$OUT/head/mods/jdk.m3.logging" $SOURCES
echo "module_javac_xlint_all rc=$?"
echo "GATE module_javac (-Xlint:all,-module -Werror, as make/modules/jdk.m3.logging/Java.gmk)"
"$JDK/bin/javac" -J$TMP --release 21 -Xlint:all,-module -Werror -d "$OUT/head/mods/jdk.m3.logging" $SOURCES
echo "module_javac rc=$?"

echo "GATE base_javac (module compiled from the receiver head's source)"
git archive "$RECEIVER" src/jdk.m3.logging | tar -x -C "$OUT/src7c23"
(cd "$OUT/src7c23" && "$JDK/bin/javac" -J$TMP --release 21 -Xlint:all,-module -Werror -d "$OUT/base/mods/jdk.m3.logging" $SOURCES)
echo "base_javac rc=$?"

echo "GATE tests_javac"
"$JDK/bin/javac" -J$TMP --release 21 -Xlint:all -d "$OUT/head/test" -p "$OUT/head/mods" --add-modules jdk.m3.logging \
    -cp "$JUNIT" test/jdk/jdk/m3/logging/*.java
echo "tests_javac rc=$?"

echo "GATE tests_junit"
for c in M3StructuredFormatterTest M3StructuredFormatterPassesTest M3StructuredFormatterPerformanceTest; do
    "$JDK/bin/java" $TMP -p "$WOUT/head/mods" --add-modules jdk.m3.logging -cp "$WOUT/head/test;$JUNIT" \
        org.junit.platform.console.ConsoleLauncher execute --disable-banner --details=tree --select-class "$c"
    echo "tests_junit $c rc=$?"
done

echo "GATE differential (fixture output of head build == receiver-head build)"
"$JDK/bin/javac" -J$TMP --release 21 -Xlint:all -Werror -d "$OUT/diff" -p "$OUT/head/mods" --add-modules jdk.m3.logging \
    "$RECIPE/M3StructuredFormatterDifferential.java"
for side in head base; do
    "$JDK/bin/java" $TMP -p "$WOUT/$side/mods" --add-modules jdk.m3.logging -cp "$WOUT/diff" \
        M3StructuredFormatterDifferential "$WOUT/diff/fixture-$side.txt"
done
sha256sum "$OUT/diff/fixture-head.txt" "$OUT/diff/fixture-base.txt"
cmp "$OUT/diff/fixture-head.txt" "$OUT/diff/fixture-base.txt" && echo "differential IDENTICAL"

echo "GATE javap (-p -c -v; only the Classfile path line may differ)"
for side in head base; do
    for cls in jdk/m3/logging/M3StructuredFormatter module-info; do
        "$JDK/bin/javap" -J$TMP -p -c -v "$OUT/$side/mods/jdk.m3.logging/$cls.class" > "$OUT/javap/$side-$(basename $cls).javap"
    done
done
for n in M3StructuredFormatter module-info; do
    diff "$OUT/javap/head-$n.javap" "$OUT/javap/base-$n.javap" | grep -v -E '^(---|[0-9c0-9]+)$' | grep -v Classfile
    echo "javap $n other_differences=$(diff "$OUT/javap/head-$n.javap" "$OUT/javap/base-$n.javap" | grep -c -v -E '^(---|[0-9]+c[0-9]+|[<>] Classfile)')"
done
sha256sum "$OUT"/head/mods/jdk.m3.logging/jdk/m3/logging/M3StructuredFormatter.class "$OUT"/base/mods/jdk.m3.logging/jdk/m3/logging/M3StructuredFormatter.class

echo "GATE jdk_naming (fixed point on the three prefixes)"
python -I m3/tooling/jdk-naming/jdk_naming.py plan --only src/jdk.m3.logging --only make/modules/jdk.m3.logging \
    --only test/jdk/jdk/m3/logging
echo "GATE recipe (--check writes nothing; exit 0 = fixed point)"
python -I "$RECIPE/apply.py" --check
echo "recipe_check rc=$?"
