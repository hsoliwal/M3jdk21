#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")/../../../.." && pwd)"
src="$root/m3/collections/src/main/java/com/m3/collections"
test_src="$root/m3/collections/src/test/java/com/m3/collections"
native_src="$root/m3/collections/src/main/native/collections"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes" "$work/headers" "$work/current" "$work/legacy"
javac --release 21 -Xlint:all -Werror -h "$work/headers" -d "$work/classes" \
  "$src/M3LaneSearch.java" "$src/M3LongRankBatchNative.java" \
  "$test_src/M3LongRankUpperBoundsContract.java"
java -Xcheck:jni -cp "$work/classes" com.m3.collections.M3LongRankUpperBoundsContract
jdk_home="$(cd "$(dirname "$(readlink -f "$(command -v javac)")")/.." && pwd)"
cc="cc"
"$cc" -std=c11 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$jdk_home/include" -I"$jdk_home/include/linux" \
  -o "$work/current/libm3rankbatch.so" "$native_src/m3_long_rank_batch_jni.c"
test "$(nm -D "$work/current/libm3rankbatch.so" | grep -c ' T Java_com_m3_collections_M3LongRankBatchNative_')" -eq 3
java -Xcheck:jni -Dm3.collections.rank.native=true \
  -Djava.library.path="$work/current" -cp "$work/classes" \
  com.m3.collections.M3LongRankUpperBoundsContract --native
# Symbol-absent legacy ABI simulation from the identical first-party lower-bound implementation.
awk '/^JNIEXPORT jint JNICALL/{exit} {print}' \
  "$native_src/m3_long_rank_batch_jni.c" > "$work/legacy.c"
"$cc" -std=c11 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$jdk_home/include" -I"$jdk_home/include/linux" \
  -o "$work/legacy/libm3rankbatch.so" "$work/legacy.c"
test "$(nm -D "$work/legacy/libm3rankbatch.so" | grep -c ' T Java_com_m3_collections_M3LongRankBatchNative_')" -eq 1
java -Xcheck:jni -Dm3.collections.rank.native=true \
  -Djava.library.path="$work/legacy" -cp "$work/classes" \
  com.m3.collections.M3LongRankUpperBoundsContract --legacy
echo "M3_RANK_UPPER_JNI_PASS"
