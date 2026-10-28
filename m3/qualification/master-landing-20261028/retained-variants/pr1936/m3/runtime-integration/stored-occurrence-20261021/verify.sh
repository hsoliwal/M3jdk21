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
# Exact current-owner successor; historical seals below remain passive history only.
# Succession receipt: m3/compatibility/collection-views/current-source-receipt.json
# Superseded owner Git blob: b3d21d61844f070df99b0289e514eff350a3f6e7
# Superseded owner SHA-256: c3da7a7f33b7a90058a8b033b6df65df6b56409495567253e836b50dd985bc40
{
  test "$(git -C "$ROOT" hash-object "$OUT/before-src/java/util/CollectionViews.java")" = "2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66"
  test "$(git -C "$ROOT" hash-object "$OUT/after-src/java/util/CollectionViews.java")" = "3f2c3eecb6a0c239025ac7051b5bd4f3e6953605"
  printf '%s  %s\n' "a6ea3101c0573d33808b5a25262e4bf26d72d2a8882ee544cfbbe3e693f7145d" "$OUT/before-src/java/util/CollectionViews.java" | sha256sum -c -
  printf '%s  %s\n' "f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6" "$ROOT/test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java" | sha256sum -c -
  printf '%s  %s\n' "1c811f4473088c46e39e9621eb27380806e420fe09062072e413d48af78059c0" "$OUT/after-src/java/util/CollectionViews.java" | sha256sum -c -
  printf '%s  %s\n' "96904ca4643c5dff2ff1cb9b51223e6dd21e4c2dde4a73ca9d6d56f372bd2c61" "$ROOT/test/jdk/java/util/CollectionViews/CollectionViewsBatchAdmissionTest.java" | sha256sum -c -
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
    "$ROOT/test/jdk/java/util/CollectionViews/RunSearchJni.java" \
    "$ROOT/test/jdk/java/util/CollectionViews/CollectionViewsBatchAdmissionTest.java" \
    "$ROOT/test/jdk/java/util/CollectionViews/BootstrapViewsTest.java" \
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
java --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" CollectionViewsBatchAdmissionTest | tee "$OUT/batch-admission.log"
java --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" BootstrapViewsTest | tee "$OUT/bootstrap.log"
java -Xss256k --add-opens java.base/java.util=ALL-UNNAMED --add-modules jdk.management \
    --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" CollectionViewsContract | tee "$OUT/contract.log"
echo 'SOURCE_SEALED_COMPOSITE_COLLECTIONVIEWS_PATCH_MODULE_PASS; full JDK and native runtime still pending' | tee "$OUT/summary.log"
