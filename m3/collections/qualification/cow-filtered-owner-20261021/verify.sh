#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
SOURCE="m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java"
COW="m3/collections/src/test/java/com/m3/collections/M3FilteredCowOwnerContract.java"
JUNIT="m3/collections/src/test/java/com/m3/collections/M3FilteredCowOwnerTest.java"
ISEMPTY="m3/collections/src/test/java/com/m3/collections/M3FilteredIsEmptyContract.java"
BASELINE_BLOB="a99cd0355879a2f941079e8401de00b51cd0093e"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/baseline-src" "$WORK/baseline-classes" "$WORK/candidate-classes"

echo "7975e471f1b1b92d2a4bdbecdc3a79049dea4a3c93342da71f8e8d403cd0ca18  $SOURCE" | sha256sum -c -
echo "7de9f44e2a6b5dd753297d7f9792f3cb28095234c8b0c7a495c0a5372b02b074  $COW" | sha256sum -c -
echo "15a856bea0a668e4d719b310972e7a0eb31f9bcd5c802d0bc68689835ae67d3e  $JUNIT" | sha256sum -c -
test -s "$ISEMPTY"

git cat-file blob "$BASELINE_BLOB" > "$WORK/baseline-src/M3CollectionViews.java"
echo "34c5ebc2cfde388f02dfee4fcaa64d1cb81058210d5dcb261bb76ef01adaf0c7  $WORK/baseline-src/M3CollectionViews.java" | sha256sum -c -

# The original combined branch already qualified a private isEmpty atom.
# Repeat its contract oracle on both original and candidate to forbid its loss.
javac --release 21 -proc:none -Xlint:all -Werror \
  -d "$WORK/baseline-classes" "$WORK/baseline-src/M3CollectionViews.java" "$ISEMPTY"
javac --release 21 -proc:none -Xlint:all -Werror \
  -d "$WORK/candidate-classes" "$SOURCE" "$ISEMPTY" "$COW"

# Check every M3CollectionViews class, including private nested wrapper concrete
# types. Only public/protected method declarations must remain unchanged.
for BYTECODE in "$WORK/baseline-classes"/com/m3/collections/M3CollectionViews*.class; do
  TYPE="$(basename "$BYTECODE" .class)"
  javap -public -s -classpath "$WORK/baseline-classes" "com.m3.collections.$TYPE" > "$WORK/baseline-$TYPE.api"
  javap -public -s -classpath "$WORK/candidate-classes" "com.m3.collections.$TYPE" > "$WORK/candidate-$TYPE.api"
  diff -u "$WORK/baseline-$TYPE.api" "$WORK/candidate-$TYPE.api"
done

for MODE in mixed interpreted; do
  OPTIONS=()
  if [[ "$MODE" == interpreted ]]; then OPTIONS=(-Xint); fi
  for KIND in baseline candidate; do
    java "${OPTIONS[@]}" -Xcheck:jni -cp "$WORK/$KIND-classes" \
      com.m3.collections.M3FilteredIsEmptyContract | tee "$WORK/$KIND-$MODE-isempty.txt"
  done
  diff -u "$WORK/baseline-$MODE-isempty.txt" "$WORK/candidate-$MODE-isempty.txt"

  # Baseline COW bulk mutation must throw; the source-owner contract repair
  # intentionally changes that observation. Run the qualified donor oracle
  # only on the candidate, without pretending old behavior was equivalent.
  java "${OPTIONS[@]}" -Xcheck:jni -cp "$WORK/candidate-classes" \
    com.m3.collections.M3FilteredCowOwnerContract | tee "$WORK/candidate-$MODE-cow.txt"
done
echo "PASS M3JDK21 original-owner COW contract; existing isEmpty behavior and public API preserved."
echo "HOLD: owner-supported exact COW mutation intentionally changes an earlier exception to successful bulk removal."
echo "NOT_RUN: Maven/OpenRewrite canonical qualification, full M3JDK21/JNI/HotSpot and native/Android matrix."
