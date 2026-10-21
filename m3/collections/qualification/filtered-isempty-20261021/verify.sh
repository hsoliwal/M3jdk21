#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
SRC="m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java"
CONTRACT="m3/collections/src/test/java/com/m3/collections/M3FilteredIsEmptyContract.java"
TEST="m3/collections/src/test/java/com/m3/collections/M3FilteredIsEmptyTest.java"
BASELINE_BLOB="7b777fcabcf7c47ab75ad0df1095663b11dcc31c"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/base" "$TMP/base-classes" "$TMP/new-classes"

echo "34c5ebc2cfde388f02dfee4fcaa64d1cb81058210d5dcb261bb76ef01adaf0c7  $SRC" | sha256sum -c -
echo "b6981ffa45a771462986a5ad3ebc2475904873a868d06ee6c957a07c1be299c2  $CONTRACT" | sha256sum -c -
echo "3096f6368c75fc702f2bc9e075053efa9f349526130779337a5bb3496d2fc4a9  $TEST" | sha256sum -c -

git cat-file blob "$BASELINE_BLOB" > "$TMP/base/M3CollectionViews.java"
echo "511800d4d79c7d8838ddb497f031e13827decd44c9c2790093f89ea01cdaa349  $TMP/base/M3CollectionViews.java" | sha256sum -c -

javac --release 21 -proc:none -Xlint:all -Werror \
  -d "$TMP/base-classes" "$TMP/base/M3CollectionViews.java" "$CONTRACT"
javac --release 21 -proc:none -Xlint:all -Werror \
  -d "$TMP/new-classes" "$SRC" "$CONTRACT"

javap -public -classpath "$TMP/base-classes" com.m3.collections.M3CollectionViews \
  > "$TMP/base-public.txt"
javap -public -classpath "$TMP/new-classes" com.m3.collections.M3CollectionViews \
  > "$TMP/new-public.txt"
diff -u "$TMP/base-public.txt" "$TMP/new-public.txt"

for MODE in mixed interpreted; do
  OPTS=()
  if [[ "$MODE" == "interpreted" ]]; then OPTS=(-Xint); fi
  for CANDIDATE in base new; do
    TARGET="$TMP/$CANDIDATE-classes"
    java "${OPTS[@]}" -Xcheck:jni -cp "$TARGET" \
      com.m3.collections.M3FilteredIsEmptyContract \
      | tee "$TMP/$CANDIDATE-$MODE.txt"
  done
  diff -u "$TMP/base-$MODE.txt" "$TMP/new-$MODE.txt"
done
echo "PASS Java21 source-sealed filtered isEmpty: baseline/candidate same trace oracle mixed/-Xint; public API identical."
echo "NOT_RUN: canonical Maven/OpenRewrite/release reactor, native ABI matrix, allocation microbenchmarks."
