#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
SRC="m3/collections/src/main/java/jdk/m3/collections/M3CollectionViews.java"
CONTRACT="m3/collections/src/test/java/jdk/m3/collections/M3FilteredIsEmptyContract.java"
TEST="m3/collections/src/test/java/jdk/m3/collections/M3FilteredIsEmptyTest.java"
BASELINE_BLOB="a8470a26113a3f55e407a80bef95953146f3ce72"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/base" "$TMP/base-classes" "$TMP/new-classes"

echo "34c5ebc2cfde388f02dfee4fcaa64d1cb81058210d5dcb261bb76ef01adaf0c7  $SRC" | sha256sum -c -
echo "01e03f8ae3bf3b1e205637f7f07e72f1961b68d7728517e37357434270c2c168  $CONTRACT" | sha256sum -c -
echo "d56817916713863dcb99ef3f08127063fc14f43e3e7cdba5a25c0ba685b65453  $TEST" | sha256sum -c -

git cat-file blob "$BASELINE_BLOB" > "$TMP/base/M3CollectionViews.java"
echo "90b0ba9467d6cb6741de9a3e7b39f1a6016eef2401da3b3eadfab3cdb0e975c8  $TMP/base/M3CollectionViews.java" | sha256sum -c -

javac --release 21 -proc:none -Xlint:all -Werror \
  -d "$TMP/base-classes" "$TMP/base/M3CollectionViews.java" "$CONTRACT"
javac --release 21 -proc:none -Xlint:all -Werror \
  -d "$TMP/new-classes" "$SRC" "$CONTRACT"

javap -public -classpath "$TMP/base-classes" jdk.m3.collections.M3CollectionViews \
  > "$TMP/base-public.txt"
javap -public -classpath "$TMP/new-classes" jdk.m3.collections.M3CollectionViews \
  > "$TMP/new-public.txt"
diff -u "$TMP/base-public.txt" "$TMP/new-public.txt"

for MODE in mixed interpreted; do
  OPTS=()
  if [[ "$MODE" == "interpreted" ]]; then OPTS=(-Xint); fi
  for CANDIDATE in base new; do
    TARGET="$TMP/$CANDIDATE-classes"
    java "${OPTS[@]}" -Xcheck:jni -cp "$TARGET" \
      jdk.m3.collections.M3FilteredIsEmptyContract \
      | tee "$TMP/$CANDIDATE-$MODE.txt"
  done
  diff -u "$TMP/base-$MODE.txt" "$TMP/new-$MODE.txt"
done
echo "PASS Java21 source-sealed filtered isEmpty: baseline/candidate same trace oracle mixed/-Xint; public API identical."
echo "NOT_RUN: canonical Maven/OpenRewrite/release reactor, native ABI matrix, allocation microbenchmarks."
