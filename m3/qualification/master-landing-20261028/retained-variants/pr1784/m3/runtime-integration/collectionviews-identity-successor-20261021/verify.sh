#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Compare exact parent source against the original java.base owner-local repair.
set -euo pipefail
[[ "$#" == 1 ]] || { echo 'usage: verify.sh <new-absolute-output-dir>' >&2; exit 2; }
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
OUT="$1"
[[ "$OUT" == /* && ! -e "$OUT" ]] || {
  echo 'output must be a new absolute directory' >&2; exit 2;
}
mkdir -p "$OUT"/{parent-src,successor-src,parent-classes,successor-classes,test-classes}
mkdir -p "$OUT"/{parent-src,successor-src}/java/util

PARENT=ef24da4c5224ca0f728db3c3c30fa412592c7765
PARENT_BLOB=1df992f3258e3b8573003172d9b7ea7083ac5e18
SUCCESSOR_BLOB=3a4258b2846c311d812883314d7c178f32feeabb
SOURCE=src/java.base/share/classes/java/util/CollectionViews.java
git -C "$ROOT" show "$PARENT:$SOURCE" > "$OUT/parent-src/java/util/CollectionViews.java"
cp "$ROOT/$SOURCE" "$OUT/successor-src/java/util/CollectionViews.java"
test "$(git -C "$ROOT" hash-object "$OUT/parent-src/java/util/CollectionViews.java")" = "$PARENT_BLOB"
test "$(git -C "$ROOT" hash-object "$OUT/successor-src/java/util/CollectionViews.java")" = "$SUCCESSOR_BLOB"
printf '%s  %s\n' "b739779bddf9a89ec1c0266fbbe70923101cd139974e76c917ed2f45648315fc" \
    "$OUT/successor-src/java/util/CollectionViews.java" | sha256sum -c - | tee "$OUT/seals.log"
java -version > "$OUT/java-version.log" 2>&1
javac -version > "$OUT/javac-version.log" 2>&1

javac --patch-module java.base="$OUT/parent-src" -Xlint:all -Werror \
    -d "$OUT/parent-classes" "$OUT/parent-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/successor-src" -Xlint:all -Werror \
    -d "$OUT/successor-classes" "$OUT/successor-src/java/util/CollectionViews.java"
javap -public -s "$OUT/parent-classes/java/util/CollectionViews.class" > "$OUT/abi-parent.txt"
javap -public -s "$OUT/successor-classes/java/util/CollectionViews.class" > "$OUT/abi-successor.txt"
diff -u "$OUT/abi-parent.txt" "$OUT/abi-successor.txt" > "$OUT/abi-diff.txt"

TESTDIR="$ROOT/test/jdk/java/util/CollectionViews"
javac --patch-module java.base="$OUT/successor-classes" --add-modules jdk.management \
    -Xlint:all -Werror -d "$OUT/test-classes" \
    "$TESTDIR/FilteredRepresentativeContractTest.java" \
    "$TESTDIR/FilteredSortedSpliteratorMetadataTest.java" \
    "$TESTDIR/FilteredSpliteratorReentrantTest.java" \
    "$TESTDIR/StoredRepresentativeOracle.java" \
    "$TESTDIR/BootstrapViewsTest.java" \
    "$TESTDIR/CollectionViewsContract.java"

set +e
java -Xint --patch-module java.base="$OUT/parent-classes" -cp "$OUT/test-classes" \
    FilteredRepresentativeContractTest > "$OUT/parent-red.log" 2>&1
RED=$?
set -e
if [[ "$RED" == 0 ]] || ! grep -q 'identity set rejects external equal' "$OUT/parent-red.log"; then
  echo 'EXPECTED_PARENT_IDENTITY_FAILURE_NOT_REPRODUCED' >&2
  cat "$OUT/parent-red.log" >&2
  exit 1
fi

for mode in default interpreter c1 c2; do
  flags=()
  case "$mode" in
    interpreter) flags=(-Xint) ;;
    c1) flags=(-XX:TieredStopAtLevel=1) ;;
    c2) flags=(-XX:-TieredCompilation -Xbatch) ;;
  esac
  java "${flags[@]}" --patch-module java.base="$OUT/successor-classes" \
      -cp "$OUT/test-classes" FilteredRepresentativeContractTest \
      > "$OUT/representative-$mode.log" 2>&1
  grep -q '^PASS FilteredRepresentativeContractTest' "$OUT/representative-$mode.log"
  java "${flags[@]}" --patch-module java.base="$OUT/successor-classes" \
      -cp "$OUT/test-classes" StoredRepresentativeOracle \
      > "$OUT/stored-$mode.log" 2>&1
  grep -q 'StoredRepresentativeOracle PASS checks=' "$OUT/stored-$mode.log"
done

for suite in FilteredSpliteratorReentrantTest FilteredSortedSpliteratorMetadataTest BootstrapViewsTest; do
  java --patch-module java.base="$OUT/successor-classes" \
      -cp "$OUT/test-classes" "$suite" > "$OUT/$suite.log" 2>&1
done
java -Xss256k --add-opens java.base/java.util=ALL-UNNAMED --add-modules jdk.management \
    --patch-module java.base="$OUT/successor-classes" -cp "$OUT/test-classes" \
    CollectionViewsContract > "$OUT/CollectionViewsContract.log" 2>&1

echo 'PASS exact-parent-red successor-green + ABI + all focused suites; full image/JNI/jtreg remains pending' | tee "$OUT/summary.log"
