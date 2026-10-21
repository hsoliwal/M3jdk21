#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
RECIPE="$ROOT/m3/collections/recipe/state-mask-histogram-20261017"
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
python3 "$ROOT/m3/collections/qualification/array-mask-successor-20261019/verify_successor.py" --root "$ROOT" --family histogram
mkdir -p "$OUT/classes" "$OUT/native" "$OUT/headers"
JH=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
SRC="$ROOT/m3/collections/src/main/java/com/m3/collections"
TEST="$ROOT/m3/collections/src/test/java/com/m3/collections"
C="$ROOT/m3/collections/src/main/native/collections/m3_state_mask_histogram.c"
javac --release 21 -proc:none -Xlint:all -Werror -h "$OUT/headers" \
  -d "$OUT/classes" "$SRC/M3StateMaskHistogram.java" "$TEST/M3StateMaskHistogramSelfTest.java"
java -Djava.library.path="$OUT/native" -cp "$OUT/classes" \
  com.m3.collections.M3StateMaskHistogramSelfTest
"${CC:-gcc}" -std=c17 -Wall -Wextra -Wpedantic -Werror -Wconversion -Wsign-conversion \
  -I"$JH/include" -I"$JH/include/linux" -I"$OUT/headers" \
  -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=all "$C" \
  -o "$OUT/native/libm3_collections_statehist.so"
java -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" \
  com.m3.collections.M3StateMaskHistogramSelfTest
printf '%s\n' 'BOUNDED_JAVA21_C17_JNI_PASS; owning module/JDK/Maven qualification separate'
