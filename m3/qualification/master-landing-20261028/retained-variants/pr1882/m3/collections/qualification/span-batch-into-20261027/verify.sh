#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Original product Java/JNI source owner; no Synexia runtime dependency.
set -euo pipefail
if [ "$#" -ne 1 ]; then echo 'usage: verify.sh M3JDK21_ROOT' >&2; exit 2; fi
root="$(cd "$1" && pwd -P)"
owner="$root/m3/collections"
source="$owner/src/main/java/jdk/m3/collections"
tests="$owner/src/test/java/jdk/m3/collections"
native="$owner/src/main/native/collections/m3_span_index_jni.c"
cd "$root"
test "$(git hash-object "$source/M3SpanIndex.java")" = 'e6758c1789394ccc867bddc8fd299b92a4a49cb4'
test "$(git hash-object "$native")" = '05d13d490d6bc039a3549420fc92b5d12fd38520'
test "$(git hash-object "$source/M3SlotArrays.java")" = 'f6fdf4a8211885a5355a37b0f3927f54cd68dba8'
test "$(git hash-object "$tests/M3SpanBatchIntoContract.java")" = '3797d53282f8127b11f824845773607728abcfa3'
test "$(git hash-object "$tests/M3SpanBatchIntoTest.java")" = '83a9ce7363e3a212d9b5a04448a016149bfff6dd'
test "$(git hash-object "$tests/M3SpanIndexSelfTest.java")" = '44cc8f8fc4fa14cec8a149c0b7f4fc36547cf890'
: "${JAVA_HOME:?JAVA_HOME must select JDK 21}"
test "$("$JAVA_HOME/bin/javac" -version 2>&1)" = 'javac 21.0.12.1' ||
  "$JAVA_HOME/bin/javac" -version 2>&1 | grep -Eq '^javac 21([.]|$)'
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/classes" "$tmp/headers" "$tmp/native" "$tmp/missing"
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all -Werror \
  -h "$tmp/headers" -d "$tmp/classes" \
  "$source/M3SlotArrays.java" "$source/M3SpanIndex.java" \
  "$tests/M3SpanBatchIntoContract.java" "$tests/M3SpanIndexSelfTest.java"
grep -Fq 'Java_jdk_m3_collections_M3SpanIndex_batchNative' \
  "$tmp/headers/jdk_m3_collections_M3SpanIndex.h"
grep -Fq 'Java_jdk_m3_collections_M3SpanIndex_batchIntoNative' \
  "$tmp/headers/jdk_m3_collections_M3SpanIndex.h"
"${CC:-gcc}" -std=c17 -O2 -Wall -Wextra -Werror -fPIC -shared \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -include "$tmp/headers/jdk_m3_collections_M3SpanIndex.h" \
  "$native" -o "$tmp/native/libm3_collections_span.so"
for name in Java_jdk_m3_collections_M3SpanIndex_batchNative \
            Java_jdk_m3_collections_M3SpanIndex_batchIntoNative; do
  nm -D "$tmp/native/libm3_collections_span.so" | grep -Fq "$name"
done
"$JAVA_HOME/bin/java" -Xcheck:jni -Djava.library.path="$tmp/native" \
  -cp "$tmp/classes" jdk.m3.collections.M3SpanBatchIntoContract \
  | tee "$tmp/native-normal.log"
"$JAVA_HOME/bin/java" -Xint -Xcheck:jni -Djava.library.path="$tmp/native" \
  -cp "$tmp/classes" jdk.m3.collections.M3SpanBatchIntoContract \
  | tee "$tmp/native-interpreted.log"
"$JAVA_HOME/bin/java" -Djava.library.path="$tmp/missing" \
  -cp "$tmp/classes" jdk.m3.collections.M3SpanBatchIntoContract \
  | tee "$tmp/java-fallback.log"
grep -Fxq 'PASS M3_SPAN_BATCH_INTO checks=161307 native=true' "$tmp/native-normal.log"
cmp "$tmp/native-normal.log" "$tmp/native-interpreted.log"
grep -Fxq 'PASS M3_SPAN_BATCH_INTO checks=160947 native=false' "$tmp/java-fallback.log"
"$JAVA_HOME/bin/java" -Xcheck:jni -Djava.library.path="$tmp/native" \
  -cp "$tmp/classes" jdk.m3.collections.M3SpanIndexSelfTest \
  | tee "$tmp/legacy-span-native.log"
grep -Fq 'M3SpanIndex Java21/JNI oracle: PASS' "$tmp/legacy-span-native.log"
if [ -n "${M3_SPAN_REPORT_DIR:-}" ]; then
  mkdir -p "$M3_SPAN_REPORT_DIR"
  cp "$tmp/native-normal.log" "$tmp/native-interpreted.log" \
     "$tmp/java-fallback.log" "$tmp/legacy-span-native.log" \
     "$M3_SPAN_REPORT_DIR/"
fi
echo 'PASS M3_SPAN_BATCH_JNI_ORIGINAL_OWNER normal + interpreter + fallback + legacy'
