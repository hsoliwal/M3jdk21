#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Tests of java.base's jdk.internal.m3.compression on a host JDK 21 (Linux): the product sources are compiled into a
# java.base patch (-Xlint:all,-serial -Werror); the tests share the package (they use package-private types), so they
# are patched in too, with JUnit on the boot class path. Runs every JUnit class and every *SelfTest main without and
# with the opt-in JNI library (built from src/java.base/share/native/libm3compression with the CMake warning set).
# CompressionInteropSelfTest needs an external corpus and runs only when M3_LZ_CORPUS names one.
# usage: test-java-base.sh <junit-platform-console-standalone.jar> [work-dir]
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
junit=$1; work=${2:-$(mktemp -d)}
pkg=jdk.internal.m3.compression
main_src=$root/src/java.base/share/classes/jdk/internal/m3/compression
test_src=$root/test/jdk/jdk/internal/m3/compression
rm -rf "$work"; mkdir -p "$work/main" "$work/test" "$work/native"
reads=(--add-reads java.base=ALL-UNNAMED,java.management,jdk.management --add-modules java.management,jdk.management)
javac -Xlint:all,-serial -Werror --patch-module java.base="$root/src/java.base/share/classes" -d "$work/main" "$main_src"/*.java
javac --patch-module java.base="$work/main:$root/test/jdk" "${reads[@]}" -cp "$junit" -d "$work/test" "$test_src"/*.java
java_home=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c11 -O2 -fPIC -shared -Wall -Wextra -Wconversion -Wsign-conversion -Wshadow -Wpedantic -Werror \
  -I"$java_home/include" -I"$java_home/include/linux" -o "$work/native/libm3lz.so" \
  "$root/src/java.base/share/native/libm3compression/m3_lz_jni.c"
run=(java -Xbootclasspath/a:"$junit" --patch-module java.base="$work/main:$work/test" "${reads[@]}"
     --add-opens java.base/$pkg=ALL-UNNAMED -cp "$junit")
junit_classes=$(cd "$test_src" && grep -l "@Test" *.java | sed "s/\.java$//; s/^/--select-class=$pkg./")
mains=$(cd "$test_src" && grep -l "static void main" *SelfTest.java | sed 's/\.java$//')
for lib in none native; do
  props=(); [ $lib = native ] && props=(-Dm3.lz.library="$work/native/libm3lz.so")
  # shellcheck disable=SC2086
  "${run[@]}" "${props[@]}" org.junit.platform.console.ConsoleLauncher execute $junit_classes \
    --disable-banner --details=summary --fail-if-no-tests
  for m in $mains; do
    case $m in
      CompressionInteropSelfTest) [ -n "${M3_LZ_CORPUS:-}" ] || continue; args=("$M3_LZ_CORPUS") ;;
      LzNativeArenaSelfTest) args=("$work/native/libm3lz.so") ;;
      *) args=() ;;
    esac
    "${run[@]}" "${props[@]}" $pkg.$m "${args[@]}" > "$work/$lib-$m.log"
    echo "M3_COMPRESSION_MAIN $lib $m PASS $(tail -1 "$work/$lib-$m.log" | cut -c1-100)"
  done
done
echo "M3_COMPRESSION_JAVA_BASE_TESTS PASS"
