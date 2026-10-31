#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$root"

sha256sum --check <<'HASHES'
ed76ad01f7b89f064846d0dc35a1a705f461abe17772694c1be8223dbed2156c  m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java
18fb1aa21aec7ce0ed9c3e9e46b4f5a86413a786574ca5a66ac548ac9c65b31f  m3/collections/src/main/java/com/m3/collections/M3SignalDimensions.java
b7573258fca1fbc19fa2726032cb58a04b4582359e3505defbc41f886a5356a0  m3/collections/src/main/native/collections/m3_index_mask_compactor.c
cb7211d70e551452c1724f186bfbd88a3e459ca0ac64a96f14dfda92b996e15c  m3/collections/src/test/java/com/m3/collections/M3RequiredExcludedMaskSelectorContract.java
89c35701322381e4d958bf6344e2357988c4ad97ce1a0dd42a9a3f698a205c7f  m3/collections/src/test/java/com/m3/collections/M3RequiredExcludedMaskSelectorNativeContract.java
7f6a0956478d0ce3bed280cd214723fc6832c7f5ea164fb068a593206a92c835  m3/collections/src/test/java/com/m3/collections/M3RequiredExcludedMaskSelectorTest.java
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

"$javac" --release 21 -Xlint:all -Werror -d "$work/classes"   m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java   m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java   m3/collections/src/main/java/com/m3/collections/M3SignalOrderNative.java   m3/collections/src/main/java/com/m3/collections/M3SignalDimensions.java   m3/collections/src/test/java/com/m3/collections/M3SignalDimensionsContract.java   m3/collections/src/test/java/com/m3/collections/M3RequiredExcludedMaskSelectorContract.java   m3/collections/src/test/java/com/m3/collections/M3RequiredExcludedMaskSelectorNativeContract.java

"$java" -Xmx64m -cp "$work/classes"   com.m3.collections.M3SignalDimensionsContract
"$java" -Xmx64m -cp "$work/classes"   com.m3.collections.M3RequiredExcludedMaskSelectorContract

gcc -std=c17 -fPIC -shared -Wall -Wextra -Werror   -fsanitize=undefined -fno-sanitize-recover=all   -I"$jdk/include" -I"$jdk/include/linux"   m3/collections/src/main/native/collections/m3_index_mask_compactor.c   -o "$work/libm3_collections_mask.so"

"$java" -Xcheck:jni -Xmx64m   -Djava.library.path="$work" -cp "$work/classes"   com.m3.collections.M3RequiredExcludedMaskSelectorNativeContract

echo 'PASS required/excluded exact selector Java21 + JNI parity'
