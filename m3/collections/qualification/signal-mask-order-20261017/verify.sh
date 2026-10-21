#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
TASK="$ROOT/m3/collections/qualification/signal-mask-order-20261017"
MAIN="$ROOT/m3/collections/src/main/java/com/m3/collections"
TEST="$ROOT/m3/collections/src/test/java/com/m3/collections"
NATIVE="$ROOT/m3/collections/src/main/native/collections/m3_signal_order_jni.c"
(cd "$ROOT" && sha256sum -c "$TASK/SOURCE_SHA256.txt")
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes" "$WORK/include" "$WORK/native"
javac --release 21 -proc:none -Xlint:all -Werror -h "$WORK/include" \
  -d "$WORK/classes" \
  "$MAIN/M3SignalOrder.java" "$MAIN/M3SignalOrderNative.java" \
  "$TEST/M3SignalOrderContract.java"
java -cp "$WORK/classes" com.m3.collections.M3SignalOrderContract
JAVA_ROOT="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
"${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic \
  -fPIC -shared -I"$JAVA_ROOT/include" -I"$JAVA_ROOT/include/linux" \
  "$NATIVE" -o "$WORK/native/libm3signalorder.so"
java -Xcheck:jni -Djava.library.path="$WORK/native" \
  -Dm3.collections.signal.native=true -cp "$WORK/classes" \
  com.m3.collections.M3SignalOrderContract native
if [[ "${M3_UBSAN:-0}" == "1" ]]; then
  "${CC:-cc}" -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic \
    -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=all \
    -I"$JAVA_ROOT/include" -I"$JAVA_ROOT/include/linux" \
    "$NATIVE" -o "$WORK/native/libm3signalorder.so"
  java -Xcheck:jni -Djava.library.path="$WORK/native" \
    -Dm3.collections.signal.native=true -cp "$WORK/classes" \
    com.m3.collections.M3SignalOrderContract native
fi
printf '%s\n' 'PASS exact SHA256 source and Java21 + real C17 JNI selected contract'
