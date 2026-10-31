#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

root="$(cd "$(dirname "$0")/../../../.." && pwd)"
owner="$root/m3/collections"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir -p "$work/classes" "$work/native"
javac --release 21 -Xlint:all -Werror -d "$work/classes" \
  "$owner/src/main/java/com/m3/collections/M3IndexMaskCompactor.java" \
  "$owner/src/test/java/com/m3/collections/M3RequiredExcludedWindowContract.java"

# Same verified class bytes: Java backend and actual region-based JNI.
java -cp "$work/classes" \
  com.m3.collections.M3RequiredExcludedWindowContract

jdk_home="$(cd "$(dirname "$(readlink -f "$(command -v javac)")")/.." && pwd)"
case "$(uname -s)" in
  Linux) platform_include=linux ;;
  Darwin) platform_include=darwin ;;
  *) echo "Explicit native gate unsupported on this platform" >&2; exit 2 ;;
esac
cc -std=c11 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$jdk_home/include" -I"$jdk_home/include/$platform_include" \
  -o "$work/native/libm3_collections_mask.$(test "$platform_include" = darwin && echo dylib || echo so)" \
  "$owner/src/main/native/collections/m3_index_mask_compactor.c"

java -Xcheck:jni -Djava.library.path="$work/native" -cp "$work/classes" \
  com.m3.collections.M3RequiredExcludedWindowContract --native

echo "M3_TWO_MASK_WINDOW_JAVA_JNI_SOURCE_PASS"
