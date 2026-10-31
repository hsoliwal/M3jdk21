#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to an actual JDK 21 installation}"
REPO=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)
OUT=${M3_SOURCE_NATIVE_PROOF_OUT:-"$REPO/target/m3-source-facts-native"}
mkdir -p "$OUT/classes"
"${CC:-cc}" -std=c17 -Wall -Wextra -Werror -O2 \
  -fsanitize=undefined -fno-sanitize-recover=undefined -fPIC -shared \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -I"$REPO/m3/arrays/src/main/native/include" \
  "$REPO/m3/arrays/src/main/native/m3_arrays.c" \
  "$REPO/m3/arrays/src/main/native/m3_arrays_jni.c" \
  -o "$OUT/libm3_arrays.so"
SOURCE_PATH="$REPO/m3/ports/compiletime/src/main/java"
SOURCE_PATH+=":$REPO/m3/vendor/synexia/synexia-mindex/compiler/src/main/java"
SOURCE_PATH+=":$REPO/m3/vendor/synexia/synexia-mindex/runtime/src/main/java"
"$JAVA_HOME/bin/javac" --release 21 -proc:none -Xlint:all -Werror \
  -sourcepath "$SOURCE_PATH" -d "$OUT/classes" \
  "$REPO/m3/arrays/src/main/java/com/m3/arrays/M3ArrayNative.java" \
  "$REPO/m3/ports/compiletime/qualification/source-facts-reuse-20261020/src/test/java/M3NativeSourceSnapshotContract.java"
"$JAVA_HOME/bin/java" -Xcheck:jni -cp "$OUT/classes" \
  com.m3.compiletime.ast.M3NativeSourceSnapshotContract "$OUT/libm3_arrays.so"
