#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$root"

sha256sum --check <<'HASHES'
226b09d8996bf6b2a952c7341495a1f6dfbec13f8bb605996b1c26b93772e29f  m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java
18fb1aa21aec7ce0ed9c3e9e46b4f5a86413a786574ca5a66ac548ac9c65b31f  m3/collections/src/main/java/jdk/m3/collections/M3SignalDimensions.java
b7573258fca1fbc19fa2726032cb58a04b4582359e3505defbc41f886a5356a0  m3/collections/src/main/native/collections/m3_index_mask_compactor.c
cb7211d70e551452c1724f186bfbd88a3e459ca0ac64a96f14dfda92b996e15c  m3/collections/src/test/java/jdk/m3/collections/M3RequiredExcludedMaskSelectorContract.java
7a286b65a23caa3c5ecf2eca579a6bb7758a142f1042fba874cfba7680d78339  m3/collections/src/test/java/jdk/m3/collections/M3RequiredExcludedMaskSelectorNativeContract.java
06872ae62968b7900fb75921723b03fed4766c7cd5f867cbb08ce0fd2fd18d8f  m3/collections/src/test/java/jdk/m3/collections/M3RequiredExcludedMaskSelectorTest.java
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

"$javac" --release 21 -Xlint:all -Werror -d "$work/classes"   m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java   m3/collections/src/main/java/jdk/m3/collections/M3SignalOrder.java   m3/collections/src/main/java/jdk/m3/collections/M3SignalOrderNative.java   m3/collections/src/main/java/jdk/m3/collections/M3SignalDimensions.java   m3/collections/src/test/java/jdk/m3/collections/M3SignalDimensionsContract.java   m3/collections/src/test/java/jdk/m3/collections/M3RequiredExcludedMaskSelectorContract.java   m3/collections/src/test/java/jdk/m3/collections/M3RequiredExcludedMaskSelectorNativeContract.java

"$java" -Xmx64m -cp "$work/classes"   jdk.m3.collections.M3SignalDimensionsContract
"$java" -Xmx64m -cp "$work/classes"   jdk.m3.collections.M3RequiredExcludedMaskSelectorContract

gcc -std=c17 -fPIC -shared -Wall -Wextra -Werror   -fsanitize=undefined -fno-sanitize-recover=all   -I"$jdk/include" -I"$jdk/include/linux"   m3/collections/src/main/native/collections/m3_index_mask_compactor.c   -o "$work/libm3_collections_mask.so"

"$java" -Xcheck:jni -Xmx64m   -Djava.library.path="$work" -cp "$work/classes"   jdk.m3.collections.M3RequiredExcludedMaskSelectorNativeContract

echo 'PASS required/excluded exact selector Java21 + JNI parity'
