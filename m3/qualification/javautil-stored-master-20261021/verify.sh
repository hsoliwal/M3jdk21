#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
[[ $# == 1 ]] || { echo 'usage: verify.sh <new absolute output directory>' >&2; exit 2; }
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
OUT="$1"
[[ "$OUT" == /* && ! -e "$OUT" ]] || { echo 'output must be a new absolute directory' >&2; exit 2; }
mkdir -p "$OUT"/{before-src/java/util,after-src/java/util,before-classes,after-classes,test-classes}
SOURCE=src/java.base/share/classes/java/util/CollectionViews.java
TEST=test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java
BLOB=2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66
git -C "$ROOT" cat-file blob "$BLOB" > "$OUT/before-src/java/util/CollectionViews.java"
cp "$ROOT/$SOURCE" "$OUT/after-src/java/util/CollectionViews.java"
(cd "$ROOT" && sha256sum --check <<'SEALS'
9be6954fb7e4e5261a3f9932bbdd6aedf46c945fb6bb9e15e5c21727e8436d7c  src/java.base/share/classes/java/util/CollectionViews.java
f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6  test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java
f35b8547a5163aa79cee603e0e8bdcbf7361515d435914ac205feb9a037990e2  test/jdk/java/util/CollectionViews/FilteredSpliteratorReentrantTest.java
e3d8f4208b2f8d3f0b05f512d39e30e276ad4496632f917376f3ebb8e37957ad  test/jdk/java/util/CollectionViews/FilteredSortedSpliteratorMetadataTest.java
SEALS
) | tee "$OUT/seals.log"
echo "a6ea3101c0573d33808b5a25262e4bf26d72d2a8882ee544cfbbe3e693f7145d  $OUT/before-src/java/util/CollectionViews.java" | sha256sum -c - | tee -a "$OUT/seals.log"
java -version > "$OUT/java-version.txt" 2>&1
javac -version > "$OUT/javac-version.txt" 2>&1
javac --patch-module java.base="$OUT/before-src" -Xlint:all -Werror -d "$OUT/before-classes" "$OUT/before-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/after-src" -Xlint:all -Werror -d "$OUT/after-classes" "$OUT/after-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/after-classes" -Xlint:all -Werror -d "$OUT/test-classes" "$ROOT/$TEST" "$ROOT/test/jdk/java/util/CollectionViews/FilteredSpliteratorReentrantTest.java" "$ROOT/test/jdk/java/util/CollectionViews/FilteredSortedSpliteratorMetadataTest.java"
javap -public -s -classpath "$OUT/before-classes" java.util.CollectionViews > "$OUT/abi-before.txt"
javap -public -s -classpath "$OUT/after-classes" java.util.CollectionViews > "$OUT/abi-after.txt"
diff -u "$OUT/abi-before.txt" "$OUT/abi-after.txt" > "$OUT/abi-diff.txt"
set +e
java -Xint --patch-module java.base="$OUT/before-classes" -cp "$OUT/test-classes" StoredRepresentativeOracle > "$OUT/baseline-red.txt" 2>&1
RED=$?
set -e
[[ "$RED" -ne 0 ]] && grep -q 'AssertionError: check=1' "$OUT/baseline-red.txt" || { echo 'baseline defect not reproduced' >&2; exit 1; }
set +e
java -Xint --patch-module java.base="$OUT/before-classes" -cp "$OUT/test-classes" FilteredSpliteratorReentrantTest > "$OUT/baseline-reentrant-red.txt" 2>&1
REENTRANT_RED=$?
set -e
[[ "$REENTRANT_RED" -ne 0 ]] && grep -q 'AssertionError: outer cursor delivered element but returned false' "$OUT/baseline-reentrant-red.txt" || { echo 'baseline reentrancy defect not reproduced' >&2; exit 1; }
set +e
java -Xint --patch-module java.base="$OUT/before-classes" -cp "$OUT/test-classes" FilteredSortedSpliteratorMetadataTest > "$OUT/baseline-sorted-red.txt" 2>&1
SORTED_RED=$?
set -e
[[ "$SORTED_RED" -ne 0 ]] && grep -q 'missing owner sorted characteristics check=1' "$OUT/baseline-sorted-red.txt" || { echo 'baseline sorted metadata defect not reproduced' >&2; exit 1; }
for mode in mixed xint c1 c2; do
  flags=()
  case "$mode" in xint) flags=(-Xint);; c1) flags=(-XX:TieredStopAtLevel=1);; c2) flags=(-XX:-TieredCompilation -Xbatch);; esac
  java "${flags[@]}" --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" StoredRepresentativeOracle > "$OUT/green-$mode.txt" 2>&1
  grep -q 'StoredRepresentativeOracle PASS checks=736018' "$OUT/green-$mode.txt"
  java "${flags[@]}" --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" FilteredSpliteratorReentrantTest > "$OUT/reentrant-green-$mode.txt" 2>&1
  grep -q "PASS FilteredSpliteratorReentrantTest" "$OUT/reentrant-green-$mode.txt"
  java "${flags[@]}" --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" FilteredSortedSpliteratorMetadataTest > "$OUT/sorted-green-$mode.txt" 2>&1
  grep -q 'PASS FilteredSortedSpliteratorMetadataTest checks=' "$OUT/sorted-green-$mode.txt"
  cat "$OUT/green-$mode.txt"
  cat "$OUT/reentrant-green-$mode.txt"
  cat "$OUT/sorted-green-$mode.txt"
done
echo 'PASS: original java.base owner + all three stored occurrence/reentrant/sorted metadata Java21 regressions; JNI and full JDK gates still pending' | tee "$OUT/summary.txt"
