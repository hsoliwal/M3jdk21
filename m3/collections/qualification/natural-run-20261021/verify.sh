#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
MAIN="m3/collections/src/main/java/com/m3/collections"
TEST="m3/collections/src/test/java/com/m3/collections"
BASELINE_BLOB="d18055008f6eabbd6603e080ff32f8d5a1da8b60"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/baseline" "$TMP/baseline-classes" "$TMP/candidate-classes"

# This refuses a re-targeted/newer owner without a new canonical recipe admission.
echo '3569c0a8ffd94c6e91b87b9316bbaf20583f7dd56810a7bee57a81c8a9975a8d  m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java' | sha256sum -c -
echo 'c25dc83ff3bb07886056d9d4443ef95671396fa41358bb40b0b89a9db6108776  m3/collections/src/test/java/com/m3/collections/M3LongIndexNaturalRunContract.java' | sha256sum -c -
echo 'b507ca68d127620603b59018622cc1015de066f90a86cfc8060a27e5f59c58be  m3/collections/src/test/java/com/m3/collections/M3LongIndexNaturalRunTest.java' | sha256sum -c -
git cat-file blob "$BASELINE_BLOB" > "$TMP/baseline/M3LongIndexProjection.java"
echo '7bf939ea374ce6c0b13c403f5e154c0f8d376f9cddb8e623cf00ef299183a77e  '"$TMP/baseline/M3LongIndexProjection.java" | sha256sum -c -

SOURCES=("$MAIN/M3IndexMaskCompactor.java" "$MAIN/M3LongAddressKernel.java")
CONTRACT="$TEST/M3LongIndexNaturalRunContract.java"
javac --release 21 -proc:none -Xlint:all -Werror -d "$TMP/baseline-classes" \
    "${SOURCES[@]}" "$TMP/baseline/M3LongIndexProjection.java" "$CONTRACT"
javac --release 21 -proc:none -Xlint:all -Werror -d "$TMP/candidate-classes" \
    "${SOURCES[@]}" "$MAIN/M3LongIndexProjection.java" "$CONTRACT"

javap -public -classpath "$TMP/baseline-classes" com.m3.collections.M3LongIndexProjection \
    > "$TMP/baseline-public.txt"
javap -public -classpath "$TMP/candidate-classes" com.m3.collections.M3LongIndexProjection \
    > "$TMP/candidate-public.txt"
diff -u "$TMP/baseline-public.txt" "$TMP/candidate-public.txt"

for KIND in baseline candidate; do
    for MODE in mixed interpreted; do
        if [[ "$KIND" == baseline ]]; then OUTPUT="$TMP/baseline-classes";
        else OUTPUT="$TMP/candidate-classes"; fi
        OPTIONS=()
        if [[ "$MODE" == interpreted ]]; then OPTIONS=(-Xint); fi
        java "${OPTIONS[@]}" -Xcheck:jni -cp "$OUTPUT" \
            com.m3.collections.M3LongIndexNaturalRunContract \
            | tee "$TMP/$KIND-$MODE.log"
    done
done
echo 'PASS focused M3JDK21 owner: SHA-256, public API, Java21 oracle mixed/-Xint; JNI symbols unchanged.'
echo 'NOT_RUN: Maven/OpenRewrite recipe, full owning reactor, source-built HotSpot, native JNI parity, allocation benchmark.'
