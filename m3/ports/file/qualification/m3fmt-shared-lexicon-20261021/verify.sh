#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
OWNER="$ROOT/m3/ports/file"
SRC="$OWNER/src/main/java"
TESTS="$OWNER/qualification/m3fmt-shared-lexicon-20261021"
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/m3fmt-qualification-XXXXXXXX")"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes" "$WORK/headers" "$WORK/native"
test -f "$SRC/com/m3/file/M3FmtLexiconFrame.java"
test -f "$SRC/com/m3/file/M3FileDictionary.java"
# The original file family declares a named module. Explicit qualification Java
# files live outside its production tree: javac must admit BOTH source roots.
# Suppress only the known numeric module-name lint; preserve other -Werror gates.
javac --release 21 -Xlint:all,-module -Werror \
  -sourcepath "$SRC:$TESTS" -d "$WORK/classes" -h "$WORK/headers" \
  "$SRC/com/m3/file/M3FmtVarints.java" \
  "$SRC/com/m3/file/M3FmtLexiconFrame.java" \
  "$SRC/com/m3/file/M3FmtFrozenDictionary.java" \
  "$TESTS/M3FmtLexiconFrameContract.java" \
  "$TESTS/M3FmtFrozenDictionaryContract.java"
HEADER="$WORK/headers/com_m3_file_M3FmtVarints.h"
grep -q 'Java_com_m3_file_M3FmtVarints_packNative' "$HEADER"
java -ea -Xcheck:jni -cp "$WORK/classes" com.m3.file.M3FmtLexiconFrameContract
java -ea -Xcheck:jni -cp "$WORK/classes" com.m3.file.M3FmtFrozenDictionaryContract
gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  "$OWNER/native/m3fmt_varints_jni.c" -o "$WORK/native/libm3fmt_varints.so"
nm -D "$WORK/native/libm3fmt_varints.so" | grep -q 'Java_com_m3_file_M3FmtVarints_packNative'
cmake -S "$OWNER/native" -B "$WORK/cmake" -DCMAKE_BUILD_TYPE=Release
cmake --build "$WORK/cmake" --parallel 2
nm -D "$WORK/cmake/libm3fmt_varints.so" | grep -q 'Java_com_m3_file_M3FmtVarints_packNative'
java -ea -Xcheck:jni -Djava.library.path="$WORK/native" \
  -Dm3.file.fmt.native=true -Dm3.file.fmt.native.required=true \
  -cp "$WORK/classes" com.m3.file.M3FmtLexiconFrameContract | tee "$WORK/native-result.txt"
grep -q 'native=true' "$WORK/native-result.txt"
java -ea -Xint -Xcheck:jni -Djava.library.path="$WORK/native" \
  -Dm3.file.fmt.native=true -Dm3.file.fmt.native.required=true \
  -cp "$WORK/classes" com.m3.file.M3FmtFrozenDictionaryContract
echo M3FMT_TARGET_ISOLATED_JAVA_JNI_PASS
