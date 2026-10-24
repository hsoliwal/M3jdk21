#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Compile the exact java.base source candidates; no claims about complete SDK builds.
set -euo pipefail
[[ $# == 1 ]] || { echo 'usage: verify.sh <new-absolute-evidence-dir>' >&2; exit 2; }
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
OUT="$1"
[[ "$OUT" == /* && ! -e "$OUT" ]] || { echo 'require a new absolute output directory' >&2; exit 2; }
mkdir -p "$OUT"
(cd "$ROOT" && sha256sum -c <<'SEALS'
e09db07fb113233e52a67960411608d8b18dc0eedfc5a7f861cdaecc7c1cc1e6  src/java.base/share/classes/java/util/CollectionViews.java
a6ea3101c0573d33808b5a25262e4bf26d72d2a8882ee544cfbbe3e693f7145d  m3/compatibility/collection-views/preimage/CollectionViews.java.txt
f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6  test/jdk/java/util/CollectionViews/StoredRepresentativeOracle.java
SEALS
) | tee "$OUT/seals.log"
java -version > "$OUT/java-version.log" 2>&1
javac -version > "$OUT/javac-version.log" 2>&1
mkdir -p "$OUT"/{before-src,after-src,before-classes,after-classes,test-classes}
mkdir -p "$OUT"/{before-src,after-src}/java/util
cp "$ROOT/m3/compatibility/collection-views/preimage/CollectionViews.java.txt" "$OUT/before-src/java/util/CollectionViews.java"
cp "$ROOT/src/java.base/share/classes/java/util/CollectionViews.java" "$OUT/after-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/before-src" -Xlint:all -Werror -d "$OUT/before-classes" "$OUT/before-src/java/util/CollectionViews.java"
javac --patch-module java.base="$OUT/after-src" -Xlint:all -Werror -d "$OUT/after-classes" "$OUT/after-src/java/util/CollectionViews.java"
javap -public -s "$OUT/before-classes/java/util/CollectionViews.class" > "$OUT/abi-before.txt"
javap -public -s "$OUT/after-classes/java/util/CollectionViews.class" > "$OUT/abi-after.txt"
diff -u "$OUT/abi-before.txt" "$OUT/abi-after.txt" > "$OUT/abi-diff.txt"
javac --patch-module java.base="$OUT/after-classes" --add-modules jdk.management -Xlint:all -Werror -d "$OUT/test-classes" \
    "$ROOT/test/jdk/java/util/CollectionViews/"*.java
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
java --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" BootstrapViewsTest | tee "$OUT/bootstrap.log"
java -Xss256k --add-opens java.base/java.util=ALL-UNNAMED --add-modules jdk.management \
    --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" CollectionViewsContract | tee "$OUT/contract.log"
for test in FilteredBatchAdmissionTest FilteredCopyOnWriteBulkTest FilteredIsEmptyDirectTest FilteredListRemoveContractTest \
    FilteredOwnerMembershipTest FilteredRepresentativeContractTest FilteredSortedSpliteratorMetadataTest \
    FilteredSetMutationDispatchTest FilteredSpliteratorReentrantTest M3CollectionViewsCursorMaskContract; do
    java --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" "$test" | tee "$OUT/$test.log"
done
java -Xint --patch-module java.base="$OUT/after-classes" -cp "$OUT/test-classes" FilteredBatchAdmissionTest | tee "$OUT/FilteredBatchAdmissionTest-interpreter.log"
echo 'SOURCE_SEALED_COLLECTIONVIEWS_PATCH_MODULE_PASS; full JDK and native runtime still pending' | tee "$OUT/summary.log"
