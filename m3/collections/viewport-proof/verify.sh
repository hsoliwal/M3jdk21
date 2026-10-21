#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
proof="$root/viewport-proof/build"
mkdir -p "$proof/classes"
javac -Xlint:all -Werror -d "$proof/classes" \
  "$root/src/main/java/com/m3/collections/M3MaskViewportKernel.java" \
  "$root/src/main/java/com/m3/collections/M3MaskViewportNative.java" \
  "$root/src/test/java/com/m3/collections/M3MaskViewportKernelContract.java"
java -ea -cp "$proof/classes" com.m3.collections.M3MaskViewportKernelContract
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
cc -std=c17 -Wall -Wextra -Werror -O2 -fPIC -shared \
  -I"$java_home/include" -I"$java_home/include/linux" \
  "$root/src/main/c/m3maskviewport.c" -o "$proof/libm3maskviewport.so"
java -ea -Xcheck:jni -Dm3.native.path="$proof/libm3maskviewport.so" \
  -cp "$proof/classes" com.m3.collections.M3MaskViewportKernelContract
