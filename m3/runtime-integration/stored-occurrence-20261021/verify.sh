#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Compile the exact java.base source candidates; no claims about complete SDK builds.
set -euo pipefail
[[ $# == 1 ]] || { echo 'usage: verify.sh <new-absolute-evidence-dir>' >&2; exit 2; }
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
OUT="$1"
[[ "$OUT" == /* && ! -e "$OUT" ]] || { echo 'require a new absolute output directory' >&2; exit 2; }
mkdir -p "$OUT"
mkdir -p "$OUT"/{before-src,after-src,before-classes,after-classes,test-classes}
mkdir -p "$OUT"/{before-src,after-src}/java/util
(cd "$ROOT" && git show 7f657d6ca05760af7bee7d4d2d9e14ae91ddd942:src/java.base/share/classes/java/util/CollectionViews.java) > "$OUT/before-src/java/util/CollectionViews.java"
cp "$ROOT/src/java.base/share/classes/java/util/CollectionViews.java" "$OUT/after-src/java/util/CollectionViews.java"
{
  test "$(git -C "$ROOT" hash-object "$OUT/before-src/java/util/CollectionViews.java")" = "2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66"
  test "$(git -C "$ROOT" hash-object "$OUT/after-src/java/util/CollectionViews.java")" = "540ebf535672a27a90f2a547f2d21aeebebe88aa"
  printf '%s  %s\n' "a6ea3101c0573d33808b5a25262e4bf26d72d2a8882ee544cfbbe3e693f7145d" "$OUT/before-src/java/util/CollectionViews.java" | sha256sum -c -
  printf '%s  %s\n' "f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6" "$ROOT/test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java" | sha256sum -c -
  printf '%s  %s\n' "736b9f6df1d50d76dde9cf4333a47ffe7a98b1fe5ac48029bcbcd30e304bd3fa" "$OUT/after-src/java/util/CollectionViews.java" | sha256sum -c -
} | tee "$OUT/seals.log"
java -version > "$OUT/java-version.log" 2>&1
javac -version > "$OUT/javac-version.log" 2>&1
javac --patch-module java.base="$OUT/before-src" -Xlint:all -Werror -d "$OUT/before-classes" "$OUT/before-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/after-src" -Xlint:all -Werror -d "$OUT/after-classes" "$OUT/after-src/java/util/CollectionViews.java"
javap -public -s "$OUT/before-classes/java/util/CollectionViews.class" > "$OUT/abi-before.txt"
javap -public -s "$OUT/after-classes/java/util/CollectionViews.class" > "$OUT/abi-after.txt"
diff -u "$OUT/abi-before.txt" "$OUT/abi-after.txt" > "$OUT/abi-diff.txt"
javac --patch-module java.base="$OUT/after-classes" --add-modules jdk.management -Xlint:all -Werror -d "$OUT/test-classes" \
    "$ROOT/test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java" \
    "$ROOT/test/jdk/java/util/CollectionViews/BootstrapViewsTest.java" \
    "$ROOT/test/jdk/java/util/CollectionViews/RunSearchJni.java" \
    "$ROOT/test/jdk/java/util/CollectionViews/CollectionViewsContract.java" \
    "$ROOT/test/jdk/java/util/CollectionViews/FilteredSpliteratorReentrantTest.java"
set +e
java -Xint --patch-module java.base="$OUT/before-classes" -cp "$OUT/test-classes" StoredRepresentativeOracle > "$OUT/old-red.log" 2>&1
RED=$?
set -e
[[ "$RED" != 0 ]] && grep -q 'AssertionError: check=1' "$OUT/old-red.log" || {
    echo 'RED_ORACLE_NOT_REPRODUCED' >&2
    cat "$OUT/old-red.log" >&2
    exit 1
}
for mode in default interpreter c1 c2; do
    flags=()
    case "$mode" in
        interpreter) flags=(-Xint) ;;
        c1) flags=(-XX:TieredStopAtLevel=1) ;;
        c2) flags=(-XX:-TieredCompilation -Xbatch) ;;
    esac
    java "${flags[@]}" --patch-module java.base="$OUT/after-classes" \
        -cp "$OUT/test-classes" StoredRepresentativeOracle > "$OUT/oracle-$mode.log" 2>&1
    grep -q 'StoredRepresentativeOracle PASS checks=' "$OUT/oracle-$mode.log"
    cat "$OUT/oracle-$mode.log"
done
java --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" FilteredSpliteratorReentrantTest | tee "$OUT/reentrant.log"
java --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" BootstrapViewsTest | tee "$OUT/bootstrap.log"
java -Xss256k --add-opens java.base/java.util=ALL-UNNAMED --add-modules jdk.management \
    --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" CollectionViewsContract | tee "$OUT/contract.log"
echo 'SOURCE_SEALED_COMPOSITE_COLLECTIONVIEWS_PATCH_MODULE_PASS; full JDK and native runtime still pending' | tee "$OUT/summary.log"
