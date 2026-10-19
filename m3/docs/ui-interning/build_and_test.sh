#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile m3/collections (main, -Xlint:all -Werror as the pom does), compile the
# ui-interning tests, run them under JaCoCo and write the coverage CSV.
# Usage: build_and_test.sh <repo-root> <out-dir> [test-class-filter]
# Run it under C:/m3-superset/tools/build_lock.sh (one build at a time).
set -euo pipefail
ROOT=$1; OUT=$2; FILTER=${3:-'com.m3.collections.M3(LabelPool|TextExtentCache|TreePathTrie|TreePathRows|LabelLanes).*Test'}
JDK="C:/Program Files/Java/jdk-21/bin"
M2=$(cygpath -m ~/.m2/repository)
CONSOLE=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
AGENT=$M2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
CLI=$M2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
SRC=$ROOT/m3/collections/src
mkdir -p "$OUT/main" "$OUT/test" "$OUT/coverage"
ls "$SRC/main/java/com/m3/collections/"M3{LabelPool,TextExtentCache,TreePathTrie,TreePathRows,LabelLanes}.java 2>/dev/null > "$OUT/main.txt" || true
# The whole module does not compile on master 677437be (M3FilteredViews duplicate reversed/removeFirst), so
# only this leaf's classes are listed and javac pulls their dependencies through -sourcepath.
"$JDK/javac" --release 21 -Xlint:all,-module -Werror -encoding UTF-8 -sourcepath "$SRC/main/java" -d "$OUT/main" @"$OUT/main.txt"
ls "$SRC/test/java/com/m3/collections/"M3{LabelPool,TextExtentCache,TreePathTrie,TreePathRows,LabelLanes,LabelInterning}*.java 2>/dev/null > "$OUT/test.txt" || true
"$JDK/javac" --release 21 -Xlint:all,-module -encoding UTF-8 -cp "$OUT/main;$CONSOLE" -d "$OUT/test" @"$OUT/test.txt"
rm -f "$OUT/coverage/jacoco.exec"
"$JDK/java" -javaagent:"$AGENT=destfile=$OUT/coverage/jacoco.exec,includes=com.m3.collections.M3LabelPool*:com.m3.collections.M3TextExtentCache*:com.m3.collections.M3TreePathTrie*:com.m3.collections.M3TreePathRows*:com.m3.collections.M3LabelLanes*" \
  -jar "$CONSOLE" execute --class-path "$OUT/main;$OUT/test" --include-classname "$FILTER" --scan-class-path "$OUT/test" --details=summary --disable-banner
"$JDK/java" -jar "$CLI" report "$OUT/coverage/jacoco.exec" --classfiles "$OUT/main" --sourcefiles "$SRC/main/java" \
  --csv "$OUT/coverage/coverage.csv" --html "$OUT/coverage/html" --quiet
grep -E 'M3(LabelPool|TextExtentCache|TreePathTrie|TreePathRows|LabelLanes)' "$OUT/coverage/coverage.csv" \
  | awk -F, '{printf "%-40s line %d/%d branch %d/%d\n", $3, $9, $8+$9, $7, $6+$7}'
