#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$root"

# Exact code authority: four Synexia-generated outputs and three unchanged M3 owners.
sha256sum --check <<'HASHES'
da761a6b4f10d9a3c9a61979a4ee238fd6d26492d1ba0e6daacdf371f6561058  m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java
eaa37fee096172a200088d80b2b48955d60d602061fba71d715e25f6d395acc8  m3/collections/src/main/java/com/m3/collections/M3SignalOrderNative.java
e7a5991f5be2e0bad99400ff477a1c03ec9d1d8e1236f9faf366485f94966ed5  m3/collections/src/main/native/collections/m3_signal_order_jni.c
c9bc4dc17e68606be0321e42d1c88c404a4dd7f68d7b90c266e446e19d4fa9ba  m3/collections/src/main/java/com/m3/collections/M3SignalDimensions.java
372a5be9ccd7bba8aae5083e63aaca59805f4b8ed17aca5854f6ea387aa49190  m3/collections/src/test/java/com/m3/collections/M3SignalDimensionsContract.java
c0812f809f09c7ae389c5a9bbf7f6b3904619cb102eae41815f78be34adc58f3  m3/collections/src/test/java/com/m3/collections/M3SignalDimensionsNativeContract.java
32036f5d67de908b70e63ed4294b0ca00dfbbf22bdd5a4b4aaaaef79154375ef  m3/collections/src/test/java/com/m3/collections/M3SignalDimensionsTest.java
HASHES

jdk="${JAVA_HOME:-}"
if [[ -z "$jdk" ]]; then
  jdk="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
fi
javac="$jdk/bin/javac"
java="$jdk/bin/java"
"$javac" -version

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes"

"$javac" --release 21 -Xlint:all -Werror -d "$work/classes" \
  m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java \
  m3/collections/src/main/java/com/m3/collections/M3SignalOrderNative.java \
  m3/collections/src/main/java/com/m3/collections/M3SignalDimensions.java \
  m3/collections/src/test/java/com/m3/collections/M3SignalDimensionsContract.java \
  m3/collections/src/test/java/com/m3/collections/M3SignalDimensionsNativeContract.java

for heap in 32m 128m; do
  "$java" -Xmx"$heap" -cp "$work/classes" com.m3.collections.M3SignalDimensionsContract
done

gcc -std=c17 -fPIC -shared -Wall -Wextra -Werror \
  -fsanitize=undefined -fno-sanitize-recover=all \
  -I"$jdk/include" -I"$jdk/include/linux" \
  m3/collections/src/main/native/collections/m3_signal_order_jni.c \
  -o "$work/libm3signalorder.so"
"$java" -Xcheck:jni -Xmx64m -Dm3.collections.signal.native=true \
  -Djava.library.path="$work" -cp "$work/classes" \
  com.m3.collections.M3SignalDimensionsNativeContract

echo 'PASS M3 focused compound signals Java 21 and JNI differential'
