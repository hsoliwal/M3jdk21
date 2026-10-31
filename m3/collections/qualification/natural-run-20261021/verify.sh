#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
MAIN="m3/collections/src/main/java/jdk/m3/collections"
TEST="m3/collections/src/test/java/jdk/m3/collections"
BASELINE_BLOB="d98d8a45725f102292a63180a44e6054bf9b57c8"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/baseline" "$TMP/baseline-classes" "$TMP/candidate-classes"

# This refuses a re-targeted/newer owner without a new canonical recipe admission.
echo '0019e28bcff8d7dc80bcd82731c7b7f610a3471bf464e8f81edb4224a659c0dc  m3/collections/src/main/java/jdk/m3/collections/M3LongIndexProjection.java' | sha256sum -c -
echo '8507af2a701bcfa85f30d5a7c71baa6e4fc3c2e84c69b2b5a48610c55e100136  m3/collections/src/test/java/jdk/m3/collections/M3LongIndexNaturalRunContract.java' | sha256sum -c -
echo 'e7b4729a9ea509e42117ffb8d5f542488a2dddb196ad69e9c9173048958ee78a  m3/collections/src/test/java/jdk/m3/collections/M3LongIndexNaturalRunTest.java' | sha256sum -c -
git cat-file blob "$BASELINE_BLOB" > "$TMP/baseline/M3LongIndexProjection.java"
echo 'bf601d711667e1078ca30246da6fa89b43d7ed951a3179e0a161365526581959  '"$TMP/baseline/M3LongIndexProjection.java" | sha256sum -c -

SOURCES=("$MAIN/M3IndexMaskCompactor.java" "$MAIN/M3LongAddressKernel.java")
CONTRACT="$TEST/M3LongIndexNaturalRunContract.java"
javac --release 21 -proc:none -Xlint:all -Werror -d "$TMP/baseline-classes" \
    "${SOURCES[@]}" "$TMP/baseline/M3LongIndexProjection.java" "$CONTRACT"
javac --release 21 -proc:none -Xlint:all -Werror -d "$TMP/candidate-classes" \
    "${SOURCES[@]}" "$MAIN/M3LongIndexProjection.java" "$CONTRACT"

javap -public -classpath "$TMP/baseline-classes" jdk.m3.collections.M3LongIndexProjection \
    > "$TMP/baseline-public.txt"
javap -public -classpath "$TMP/candidate-classes" jdk.m3.collections.M3LongIndexProjection \
    > "$TMP/candidate-public.txt"
diff -u "$TMP/baseline-public.txt" "$TMP/candidate-public.txt"

for KIND in baseline candidate; do
    for MODE in mixed interpreted; do
        if [[ "$KIND" == baseline ]]; then OUTPUT="$TMP/baseline-classes";
        else OUTPUT="$TMP/candidate-classes"; fi
        OPTIONS=()
        if [[ "$MODE" == interpreted ]]; then OPTIONS=(-Xint); fi
        java "${OPTIONS[@]}" -Xcheck:jni -cp "$OUTPUT" \
            jdk.m3.collections.M3LongIndexNaturalRunContract \
            | tee "$TMP/$KIND-$MODE.log"
    done
done
echo 'PASS focused M3JDK21 owner: SHA-256, public API, Java21 oracle mixed/-Xint; JNI symbols unchanged.'
echo 'NOT_RUN: Maven/OpenRewrite recipe, full owning reactor, source-built HotSpot, native JNI parity, allocation benchmark.'
