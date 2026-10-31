#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SOURCE="${1:?source root}"
OUT="${2:?output root}"
mkdir -p "$OUT"/{patch,classes,headers,native}
JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
INTERNAL="$SOURCE/src/java.base/share/classes/jdk/internal/mindex"
COLLECTIONS="$SOURCE/m3/collections/src/main/java/com/m3/collections"
NATIVE="$SOURCE/src/java.base/share/native/libjava"
javac -Xlint:all -Werror --patch-module java.base="$INTERNAL:$ROOT/tests/jdk/internal/mindex" \
  -d "$OUT/patch" -h "$OUT/headers" "$INTERNAL/M3StateRelation.java" \
  "$INTERNAL/M3CollectionMaskCompactor.java" "$ROOT/tests/jdk/internal/mindex/M3CompositeFactAtomsTest.java"
javac --release 21 -Xlint:all -Werror -d "$OUT/classes" -h "$OUT/headers" \
  "$COLLECTIONS/M3SlotArrays.java" "$COLLECTIONS/M3SpanIndex.java" \
  "$ROOT/tests/com/m3/collections/M3SpanIndexOracleTest.java"
CFLAGS=(-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$NATIVE")
gcc "${CFLAGS[@]}" -shared "$NATIVE/M3StateRelation.c" -o "$OUT/native/libm3_relation_test.so"
gcc "${CFLAGS[@]}" -shared "$SOURCE/m3/collections/src/main/native/collections/m3_span_index_jni.c" -o "$OUT/native/libm3_collections_span.so"
gcc "${CFLAGS[@]}" -fsanitize=undefined -fno-sanitize-recover=all "$ROOT/tests/relation_native_test.c" "$NATIVE/M3StateRelation.c" -o "$OUT/native/relation_native_test"
"$OUT/native/relation_native_test"
java -Xcheck:jni --patch-module java.base="$OUT/patch" \
  jdk.internal.mindex.M3CompositeFactAtomsTest "$OUT/native/libm3_relation_test.so"
java -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" com.m3.collections.M3SpanIndexOracleTest
for f in "$OUT/native/"*.so; do nm -D --defined-only "$f" | awk '{print $3}' | sort; done > "$OUT/exports.txt"
