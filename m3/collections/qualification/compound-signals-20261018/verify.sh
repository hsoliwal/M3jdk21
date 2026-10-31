#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$root"

# Exact code authority: four Synexia-generated outputs and three unchanged M3 owners.
sha256sum --check <<'HASHES'
da761a6b4f10d9a3c9a61979a4ee238fd6d26492d1ba0e6daacdf371f6561058  m3/collections/src/main/java/jdk/m3/collections/M3SignalOrder.java
2b8b802a75e2a0268db41e732da6ed3530df6a128ea25fe3e3cbe981fe419172  m3/collections/src/main/java/jdk/m3/collections/M3SignalOrderNative.java
e7a5991f5be2e0bad99400ff477a1c03ec9d1d8e1236f9faf366485f94966ed5  m3/collections/src/main/native/collections/m3_signal_order_jni.c
c9bc4dc17e68606be0321e42d1c88c404a4dd7f68d7b90c266e446e19d4fa9ba  m3/collections/src/main/java/jdk/m3/collections/M3SignalDimensions.java
4d7a8a18db0ea2c548cf1a0117295cb89b9bd7e8cda56f03ef57060258ab728f  m3/collections/src/test/java/jdk/m3/collections/M3SignalDimensionsContract.java
60e7c8850d214b485359634fc57f0d5896d362da2eed613bfa0c2a0f901d4d8a  m3/collections/src/test/java/jdk/m3/collections/M3SignalDimensionsNativeContract.java
06bd4926667ec5d37797502694bf92f114a486b2307f595d0c9cb3d48832d85a  m3/collections/src/test/java/jdk/m3/collections/M3SignalDimensionsTest.java
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
  m3/collections/src/main/java/jdk/m3/collections/M3SignalOrder.java \
  m3/collections/src/main/java/jdk/m3/collections/M3SignalOrderNative.java \
  m3/collections/src/main/java/jdk/m3/collections/M3SignalDimensions.java \
  m3/collections/src/test/java/jdk/m3/collections/M3SignalDimensionsContract.java \
  m3/collections/src/test/java/jdk/m3/collections/M3SignalDimensionsNativeContract.java

for heap in 32m 128m; do
  "$java" -Xmx"$heap" -cp "$work/classes" jdk.m3.collections.M3SignalDimensionsContract
done

gcc -std=c17 -fPIC -shared -Wall -Wextra -Werror \
  -fsanitize=undefined -fno-sanitize-recover=all \
  -I"$jdk/include" -I"$jdk/include/linux" \
  m3/collections/src/main/native/collections/m3_signal_order_jni.c \
  -o "$work/libm3signalorder.so"
"$java" -Xcheck:jni -Xmx64m -Dm3.collections.signal.native=true \
  -Djava.library.path="$work" -cp "$work/classes" \
  jdk.m3.collections.M3SignalDimensionsNativeContract

echo 'PASS M3 focused compound signals Java 21 and JNI differential'
