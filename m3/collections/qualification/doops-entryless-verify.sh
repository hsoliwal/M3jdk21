#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Read-only receiver qualification: never rewrites or merges product source.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

python3 - <<'PY'
import csv
import hashlib
from pathlib import Path

root = Path('m3/collections/qualification/doops-entryless-receiver.tsv')
assert root.is_file(), 'missing receiving provenance ledger'
with root.open(encoding='utf-8', newline='') as stream:
    rows = list(csv.DictReader(stream, delimiter='\t'))
expected = {
    'm3/collections/src/main/java/com/m3/collections/M3FilteredViews.java',
    'm3/collections/src/main/java/com/m3/collections/M3TreeMap.java',
    'm3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java',
    'm3/collections/src/main/native/collections/m3_index_mask_compactor.c',
    'm3/collections/src/test/java/com/m3/collections/M3FilteredTreeSlotScanProof.java',
    'm3/collections/src/test/java/com/m3/collections/M3FilteredTreeSlotScanTest.java',
    'm3/collections/src/test/java/com/m3/collections/M3EntrylessSortedBulkProof.java',
    'm3/collections/src/test/java/com/m3/collections/M3EntrylessSortedBulkTest.java',
    'm3/collections/src/test/java/com/m3/collections/M3IndexMaskPositionPageProof.java',
    'm3/collections/src/test/java/com/m3/collections/M3IndexMaskPositionPageTest.java',
}
assert len(rows) == len(expected), 'receiving manifest length mismatch'
assert {r['path'] for r in rows} == expected, 'unexpected or repeated receiver target'
assert {r['product_baseline'] for r in rows} == {'ce005d25b29fb6246e013bffb462afd5ed070362'}
for row in rows:
    path = Path(row['path'])
    assert not path.is_symlink() and path.is_file(), f'unsafe or absent source: {path}'
    assert row['acceptance'] == 'HOLD_PRODUCT_GATES', f'gate status changed: {path}'
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    assert actual == row['post_sha256'], f'source-seal mismatch: {path}: {actual}'
print(f'PASS DOOPS receiver SHA-256 source seals: {len(rows)} exact postimages')
PY

export LANG=C.UTF-8
OUT="${RUNNER_TEMP:-$(mktemp -d)}/m3-doops-entryless"
mkdir -p "$OUT/classes" "$OUT/native"

SRC=m3/collections/src/main/java
ARRAYS=m3/arrays/src/main/java
TEST=m3/collections/src/test/java/com/m3/collections

# Compile the REAL M3 owners reached by the product-side independent oracles.
# The sourcepath resolves first-party dependencies without introducing a facade.
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  -sourcepath "$SRC:$ARRAYS" -d "$OUT/classes" \
  "$TEST/M3FilteredTreeSlotScanProof.java" \
  "$TEST/M3EntrylessSortedBulkProof.java" \
  "$TEST/M3IndexMaskPositionPageProof.java" \
  "$TEST/M3IndexMaskCompactorSelfTest.java"

java -ea -cp "$OUT/classes" com.m3.collections.M3FilteredTreeSlotScanProof
java -ea -cp "$OUT/classes" com.m3.collections.M3EntrylessSortedBulkProof
java -ea -cp "$OUT/classes" com.m3.collections.M3IndexMaskPositionPageProof
java -ea -cp "$OUT/classes" com.m3.collections.M3IndexMaskCompactorSelfTest

# Compile the EXISTING M3 JNI owner, not a new native allocator or runtime.
cc -std=c17 -O2 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  "$SRC/../native/collections/m3_index_mask_compactor.c" \
  -o "$OUT/native/libm3_collections_mask.so"
test -s "$OUT/native/libm3_collections_mask.so"
nm -D "$OUT/native/libm3_collections_mask.so" | grep -Fq Java_com_m3_collections_M3IndexMaskCompactor_positionsPageAbiNative
nm -D "$OUT/native/libm3_collections_mask.so" | grep -Fq Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsPageNative

# Explicit NATIVE_REQUIRED selection must not silently fall back to Java.
java -ea -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" \
  com.m3.collections.M3IndexMaskPositionPageProof native
java -ea -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" \
  com.m3.collections.M3IndexMaskCompactorSelfTest --native

echo 'PASS DOOPS Java21 + C17 JNI receiver source qualification; Maven/OpenRewrite/fleet gates remain separate'
