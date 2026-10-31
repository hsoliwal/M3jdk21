#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate=$(cd "$(dirname "$0")" && pwd)
resources="$crate/../../src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-bitlane-runtime-count-skip-20261019"
generated="$crate/target/generated-receiver/src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java"
echo "e87553ae33e4d05f07c3b57e081a88d076e871499fe98a0f69df8ddb9d0d2fb2  $generated" | sha256sum -c -
echo "025556c17d0ce7879f63b75d04c30a3b16b6338ff73fa33f4ee49fdb1e196bb8  $resources/M3BitLane28.before.java.txt" | sha256sum -c -
echo "dd14fe5384ff92db5c0232ef1fbc68da731ee305dda5b7c22777a3fa4c5e90fa  $resources/M3Address28.java.txt" | sha256sum -c -
echo "c5d9d2797966a56b9c2ec97908f6b76a0ad370a72cb05edc2a55b62775ba22ab  $resources/M3Bits.java.txt" | sha256sum -c -
echo "7cce5d9e56e5308f130e4dfaa191c663c27afb3e35ed206347447cc925dec89f  $crate/proof/M3BitLane28.c" | sha256sum -c -
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
jdk=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
for variant in baseline generated; do
  dir="$work/$variant"
  mkdir -p "$dir/src/jdk/internal/mindex" "$dir/classes" "$dir/headers"
  for name in M3Address28 M3Bits; do cp "$resources/$name.java.txt" "$dir/src/jdk/internal/mindex/$name.java"; done
  if [[ $variant == baseline ]]; then cp "$resources/M3BitLane28.before.java.txt" "$dir/src/jdk/internal/mindex/M3BitLane28.java"; else cp "$generated" "$dir/src/jdk/internal/mindex/M3BitLane28.java"; fi
  cp "$crate/proof/BitLaneNavigationProof.java" "$dir/src/jdk/internal/mindex/"
  javac --release 21 -proc:none -Xlint:all -Werror --patch-module java.base="$dir/src" -h "$dir/headers" -d "$dir/classes" "$dir/src/jdk/internal/mindex/"*.java
  gcc -std=c17 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -fsanitize=undefined -fno-sanitize-recover=undefined -O2 -fPIC -shared -I"$jdk/include" -I"$jdk/include/linux" -I"$dir/headers" "$crate/proof/M3BitLane28.c" -o "$dir/libbitlane-proof.so"
  java -ea -Xmx256m -Xcheck:jni --patch-module java.base="$dir/classes" jdk.internal.mindex.BitLaneNavigationProof | tee "$dir/java.log"
  java -ea -Xmx256m -Xcheck:jni --patch-module java.base="$dir/classes" jdk.internal.mindex.BitLaneNavigationProof "$dir/libbitlane-proof.so" | tee "$dir/native.log"
  java -ea --patch-module java.base="$dir/classes" jdk.internal.mindex.BitLaneNavigationProof --missing-native
  javap -public -s "$dir/classes/jdk/internal/mindex/M3BitLane28.class" > "$dir/api.txt"
done
diff -u "$work/baseline/java.log" "$work/generated/java.log"
diff -u "$work/baseline/native.log" "$work/generated/native.log"
diff -u "$work/baseline/api.txt" "$work/generated/api.txt"
diff -u "$work/baseline/headers/jdk_internal_mindex_M3BitLane28.h" "$work/generated/headers/jdk_internal_mindex_M3BitLane28.h"
echo 'CANONICAL_GENERATED_JAVA_BASE_JNI_PARITY_PASS'
