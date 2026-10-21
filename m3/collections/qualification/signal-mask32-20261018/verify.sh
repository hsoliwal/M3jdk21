#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
MAIN="$ROOT/m3/collections/src/main/java/com/m3/collections"
TEST="$ROOT/m3/collections/src/test/java/com/m3/collections"
NATIVE="$ROOT/m3/collections/src/main/native/collections"
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT
mkdir -p "$BUILD/classes"

# Standalone source-only Java21 compile: no placeholder SDK or fabricated dependencies.
javac --release 21 -Xlint:all -Werror -d "$BUILD/classes" \
  "$MAIN/M3SignalOrderNative.java" \
  "$MAIN/M3SignalOrder.java" \
  "$MAIN/M3SignalDimensions.java" \
  "$TEST/M3SignalMask32Contract.java"

java -cp "$BUILD/classes" com.m3.collections.M3SignalMask32Contract
echo "JAVA_21_DIFFERENTIAL=PASS"

if [[ "$(uname -s)" == "Linux" ]] && command -v cc >/dev/null; then
  JAVA_BIN="$(readlink -f "$(command -v javac)")"
  JDK_HOME="$(cd "$(dirname "$JAVA_BIN")/.." && pwd)"
  cc -std=c11 -Wall -Wextra -Werror -O2 -fPIC -shared \
    -I"$JDK_HOME/include" -I"$JDK_HOME/include/linux" \
    -o "$BUILD/libm3signalorder.so" \
    "$NATIVE/m3_signal_order_jni.c"
  java -Dm3.collections.signal.native=true \
    -Djava.library.path="$BUILD" -cp "$BUILD/classes" \
    com.m3.collections.M3SignalMask32Contract --require-jni
  echo "JAVA_C_JNI_PARITY=PASS"
else
  echo "JAVA_C_JNI_PARITY=NOT_RUN_PLATFORM_OR_COMPILER_UNAVAILABLE"
fi
