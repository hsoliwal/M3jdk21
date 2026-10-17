#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
# Real target owner, Java21, and both original/new native symbols; no local test shims.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
TARGET="${M3_MASK_WINDOW_BUILD:-$(mktemp -d)}"
mkdir -p "$TARGET/classes" "$TARGET/native"
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
java -version
javac --release 21 -proc:none -Xlint:all -Werror -d "$TARGET/classes" \
  "$ROOT/m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java" \
  "$ROOT/m3/collections/src/main/java/com/m3/collections/M3IndexMaskWindow.java" \
  "$ROOT/m3/collections/src/test/java/com/m3/collections/M3IndexMaskWindowSelfTest.java"
java -cp "$TARGET/classes" com.m3.collections.M3IndexMaskWindowSelfTest
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic \
  -fPIC -shared -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_compactor.c" \
  "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_window.c" \
  -o "$TARGET/native/libm3_collections_mask.so"
java -Djava.library.path="$TARGET/native" -cp "$TARGET/classes" \
  com.m3.collections.M3IndexMaskWindowSelfTest --native
python3 "$ROOT/m3/collections/qualification/m3-index-mask-window-20261017/verify.py"
