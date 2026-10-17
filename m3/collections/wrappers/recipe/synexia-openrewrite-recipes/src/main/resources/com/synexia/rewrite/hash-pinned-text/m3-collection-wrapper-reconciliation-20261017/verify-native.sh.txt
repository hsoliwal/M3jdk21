#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Maven test compilation supplies real generated JNI headers and installed owner classes.
set -euo pipefail
if [[ $# != 1 ]]; then exit 2; fi
NATIVE_JAVA_HOME=$1
cd -- "${BASH_SOURCE[0]%/*}"
NATIVE_ROOT=$PWD/target/native
mkdir -p "$NATIVE_ROOT"
NATIVE_CLASSES="$PWD/target/classes:$PWD/target/test-classes"
COMMON_C=(-std=c17 -O2 -Wall -Wextra -Werror -pedantic -fPIC -shared
  -fsanitize=undefined -fno-sanitize-recover=undefined
  "-I$NATIVE_JAVA_HOME/include" "-I$NATIVE_JAVA_HOME/include/linux"
  "-I$PWD/target/jni-headers")
gcc "${COMMON_C[@]}" -include target/jni-headers/RunSearchJni.h \
  src/test/native/run_search_jni.c -o "$NATIVE_ROOT/librun_search_jni.so"
gcc "${COMMON_C[@]}" -include target/jni-headers/com_m3_collections_M3MapCursorNativeProof.h \
  src/test/native/m3_map_cursor_jni.c -o "$NATIVE_ROOT/libm3_map_cursor_jni.so"
gcc "${COMMON_C[@]}" -include target/jni-headers/com_m3_collections_M3IndexMaskCompactor.h \
  ../src/main/native/collections/m3_index_mask_compactor.c -o "$NATIVE_ROOT/libm3_collections_mask.so"
run_checked() {
  local proof_name=$1
  shift
  "$NATIVE_JAVA_HOME/bin/java" -ea -Xmx384m -Xcheck:jni -cp "$NATIVE_CLASSES" "$@" \
    > "$NATIVE_ROOT/$proof_name.log" 2>&1
  cat "$NATIVE_ROOT/$proof_name.log"
  if grep -E 'WARNING in native method|runtime error:' "$NATIVE_ROOT/$proof_name.log"; then return 1; fi
}
run_checked map-parity com.m3.collections.M3FilteredMapsNativeSelfTest "$NATIVE_ROOT/librun_search_jni.so"
run_checked run-boundaries com.m3.collections.M3FilteredWrapperSelfTest "$NATIVE_ROOT/librun_search_jni.so"
run_checked map-cursor "-Dm3.cursor.native.library=$NATIVE_ROOT/libm3_map_cursor_jni.so" com.m3.collections.M3MapCursorNativeProof
run_checked primitive-mask "-Djava.library.path=$NATIVE_ROOT" com.m3.collections.M3PrimitiveSortedSnapshotOracleSelfTest native
nm -D --defined-only "$NATIVE_ROOT/librun_search_jni.so" "$NATIVE_ROOT/libm3_map_cursor_jni.so" \
  "$NATIVE_ROOT/libm3_collections_mask.so" > "$NATIVE_ROOT/exports.txt"
printf '%s\n' 'M3_WRAPPER_JNI_PASS generated-header ABI, C17, fatal UBSan, checked JNI'
