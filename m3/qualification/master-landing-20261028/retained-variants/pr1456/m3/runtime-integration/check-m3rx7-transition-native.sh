#!/usr/bin/env bash
# Copyright (c) 2026, Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

# Usage: M3_JAVA_HOME=/absolute/jdk21 bash check-m3rx7-transition-native.sh [source-root]
# Source root may be a recipe postimages directory before canonical replay.
# Use a stock Java 21 host without RX7 symbols in libjava for the absence proof;
# source-built M3JDK21 runs the jtreg native-boundary test directly.
RX7_SOURCE_ROOT="${1:-$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)}"
: "${M3_JAVA_HOME:?set M3_JAVA_HOME to the Java 21 toolchain used for qualification}"
RX7_CC="${CC:-cc}"
RX7_WORK="$(mktemp -d "${TMPDIR:-/tmp}/m3rx7-native.XXXXXX")"
trap 'rm -rf -- "$RX7_WORK"' EXIT
mkdir -p "$RX7_WORK/source/jdk/internal/mindex" "$RX7_WORK/patch" "$RX7_WORK/headers" "$RX7_WORK/tests"

case "$(uname -s)" in
    Linux) RX7_JNI_OS=linux; RX7_LIBRARY="$RX7_WORK/libm3rx7qualification.so"; RX7_LINK=(-shared) ;;
    Darwin) RX7_JNI_OS=darwin; RX7_LIBRARY="$RX7_WORK/libm3rx7qualification.dylib"; RX7_LINK=(-dynamiclib) ;;
    *) printf '%s\n' 'This host proof supports Linux and macOS only; use source-built jtreg elsewhere.' >&2; exit 2 ;;
esac

"$M3_JAVA_HOME/bin/java" -version
"$M3_JAVA_HOME/bin/javac" -version
"$RX7_CC" --version
cp "$RX7_SOURCE_ROOT/src/java.base/share/classes/jdk/internal/mindex/M3RX7.java" "$RX7_WORK/source/jdk/internal/mindex/"
cp "$RX7_SOURCE_ROOT/src/java.base/share/classes/jdk/internal/mindex/M3RX7Transitions.java" "$RX7_WORK/source/jdk/internal/mindex/"
cp "$RX7_SOURCE_ROOT/test/jdk/jdk/internal/mindex/M3RX7NativeBootstrap.java" "$RX7_WORK/source/jdk/internal/mindex/"
"$M3_JAVA_HOME/bin/javac" -Xlint:all -Werror --patch-module "java.base=$RX7_WORK/source" \
    -d "$RX7_WORK/patch" -h "$RX7_WORK/headers" \
    "$RX7_WORK/source/jdk/internal/mindex/M3RX7.java" \
    "$RX7_WORK/source/jdk/internal/mindex/M3RX7Transitions.java" \
    "$RX7_WORK/source/jdk/internal/mindex/M3RX7NativeBootstrap.java"
"$RX7_CC" -std=c17 -O2 -Wall -Wextra -Werror -fPIC "${RX7_LINK[@]}" \
    -I"$M3_JAVA_HOME/include" -I"$M3_JAVA_HOME/include/$RX7_JNI_OS" -I"$RX7_WORK/headers" \
    "$RX7_SOURCE_ROOT/src/java.base/share/native/libjava/M3RX7.c" -o "$RX7_LIBRARY"

if [[ "$RX7_JNI_OS" == linux ]]; then
    nm -D --defined-only "$RX7_LIBRARY" | awk '/Java_jdk_internal_mindex_M3RX7/ { print $3 }'
else
    nm -gU "$RX7_LIBRARY" | awk '/Java_jdk_internal_mindex_M3RX7/ { print $3 }'
fi

"$M3_JAVA_HOME/bin/javac" -Xlint:all -Werror --patch-module "java.base=$RX7_WORK/patch" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$RX7_WORK/tests" \
    "$RX7_SOURCE_ROOT/test/jdk/jdk/internal/mindex/M3RX7TransitionCompositionContract.java" \
    "$RX7_SOURCE_ROOT/test/jdk/jdk/internal/mindex/M3RX7NativeBoundaryContract.java"
RX7_JAVA_ARGS=(--patch-module "java.base=$RX7_WORK/patch" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED -Xcheck:jni -Xmx512m -cp "$RX7_WORK/tests")
"$M3_JAVA_HOME/bin/java" "${RX7_JAVA_ARGS[@]}" M3RX7TransitionCompositionContract
"$M3_JAVA_HOME/bin/java" "${RX7_JAVA_ARGS[@]}" M3RX7NativeBoundaryContract java-default
"$M3_JAVA_HOME/bin/java" "${RX7_JAVA_ARGS[@]}" \
    "-Dm3.rx7.native.path=$RX7_LIBRARY" M3RX7NativeBoundaryContract
printf '%s\n' 'PASS: host Java21 transfer semantics, Java default without new JNI symbols, and direct JNI boundary parity.'
printf '%s\n' 'Scope: component qualification; source-built M3JDK21 libjava/jtreg and other platforms require their own gates.'
