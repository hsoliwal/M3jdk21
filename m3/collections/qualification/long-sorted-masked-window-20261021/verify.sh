#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")/../../../.." && pwd)"
src="$root/m3/collections/src/main/java/com/m3/collections"
native="$root/m3/collections/src/main/native/collections"
test="$root/m3/collections/src/test/java/com/m3/collections"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes" "$work/native"

# Compile the original parent-owned collection owners, not replacement stubs.
javac --release 21 -Xlint:all -Werror -d "$work/classes" \
  "$src/M3LongIterator.java" \
  "$src/M3LongCollection.java" \
  "$src/M3LongSequence.java" \
  "$src/M3LongList.java" \
  "$src/M3LongStack.java" \
  "$src/M3LongListSpliterator.java" \
  "$src/M3LongArrayList.java" \
  "$src/M3LongSortedSet.java" \
  "$src/M3LaneSearch.java" \
  "$src/M3IndexMaskCompactor.java" \
  "$src/M3LongSortedSnapshot.java" \
  "$test/M3LongSortedMaskedWindowContract.java"

java -cp "$work/classes" com.m3.collections.M3LongSortedMaskedWindowContract

jdk_home="$(cd "$(dirname "$(readlink -f "$(command -v javac)")")/.." && pwd)"
case "$(uname -s)" in
  Linux) os_include=linux; suffix=so ;;
  Darwin) os_include=darwin; suffix=dylib ;;
  *) echo "Explicit JNI gate unsupported on this OS" >&2; exit 2 ;;
esac

cc -std=c11 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$jdk_home/include" -I"$jdk_home/include/$os_include" \
  -o "$work/native/libm3_collections_mask.$suffix" \
  "$native/m3_index_mask_compactor.c"

java -Xcheck:jni -Djava.library.path="$work/native" -cp "$work/classes" \
  com.m3.collections.M3LongSortedMaskedWindowContract --native

echo "M3_SORTED_MASKED_WINDOW_JAVA_JNI_PASS"
