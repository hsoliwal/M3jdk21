#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Exact original-owner Java21/JNI host probe; not a source-built full M3JDK21 image.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
if [[ "$(uname -s)" != Linux ]]; then
  echo 'BLOCKED: this focused libjava JNI host probe requires Linux' >&2
  exit 2
fi
: "${CC:=cc}"
if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
fi
if [[ "$("$JAVA_HOME/bin/javac" -version 2>&1)" != javac\ 21* ]]; then
  echo 'BLOCKED: Java 21 compiler required' >&2
  exit 2
fi

python3 "$root/m3/qualification/superset-native-filtered-20261021/verify_received_blobs.py"
# Do not replay atop drifted product files or replace more recent class owners.
if [[ "$(git -C "$root" hash-object "$root/src/java.base/share/classes/jdk/internal/m3/M3IndexMaskWindow.java")" != b9c942d354189c26ed0848d7dd73f21754838049 ]]; then
  echo 'HOLD: JDK-owned Java mask receiver drift' >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$root/src/java.base/share/native/libjava/M3IndexMaskWindow.c")" != 59b2130ca8e897e75e24afbd45e8fd885c55b0e9 ]]; then
  echo 'HOLD: JDK-owned native mask receiver drift' >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$root/test/jdk/jdk/internal/m3/M3MaskCollectionViewIntegrationTest.java")" != a5a7d4508513b829d1ac14b9562aa108998a0e3f ]]; then
  echo 'HOLD: cross-contract oracle source drift' >&2
  exit 1
fi
if [[ "$(sha256sum "$root/src/java.base/share/classes/jdk/internal/m3/M3IndexedArray.java" | cut -d' ' -f1)" != "10c40ed918edd6b756db3cd17c46afeb54490d7a74efcbd67d3b22022accef56" ]]; then
  echo 'HOLD: fused indexed-array owner drift' >&2
  exit 1
fi
if [[ "$(sha256sum "$root/test/jdk/jdk/internal/m3/M3FusedMaskedFilteredSortedViewTest.java" | cut -d' ' -f1)" != "1c599c9f7a8dd98dd1c6b067c7703105923914fa9a86082899a133993ed1bf97" ]]; then
  echo 'HOLD: fused view contract drift' >&2
  exit 1
fi

scratch="$(mktemp -d)"
trap 'rm -rf -- "$scratch"' EXIT
mkdir -p "$scratch/classes" "$scratch/headers" "$scratch/contracts"
base="$root/src/java.base/share/classes"
libjava="$root/src/java.base/share/native/libjava"
tests="$root/test/jdk"

# Compile the actual M3JDK21 product owners, with no synthetic replacement classes.
"$JAVA_HOME/bin/javac" -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$base" -h "$scratch/headers" -d "$scratch/classes" \
  "$base/jdk/internal/m3/M3IndexMaskWindow.java" \
  "$base/jdk/internal/m3/M3IndexedArray.java" \
  "$base/jdk/internal/m3/M3IndexedView.java" \
  "$base/java/util/CollectionViews.java"

# Real libjava JNI implementation; original javac-generated symbol declarations.
"$CC" -std=c17 -O2 -Wall -Wextra -Werror -fPIC -shared \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$scratch/headers" \
  "$libjava/M3IndexMaskWindow.c" -o "$scratch/libm3_mask_original.so"

"$JAVA_HOME/bin/javac" -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$scratch/classes:$tests/jdk/internal/m3" \
  -d "$scratch/classes" \
  "$tests/jdk/internal/m3/M3MaskCountTest.java" \
  "$tests/jdk/internal/m3/M3MaskPreparedPredicateTest.java" \
  "$tests/jdk/internal/m3/M3MaskCollectionViewIntegrationTest.java" \
  "$tests/jdk/internal/m3/M3FusedMaskedFilteredSortedViewTest.java"

"$JAVA_HOME/bin/java" -Xcheck:jni --patch-module "java.base=$scratch/classes" \
  jdk.internal.m3.M3MaskCollectionViewIntegrationTest java
"$JAVA_HOME/bin/java" --patch-module "java.base=$scratch/classes" \
  jdk.internal.m3.M3FusedMaskedFilteredSortedViewTest
"$JAVA_HOME/bin/java" -Xint --patch-module "java.base=$scratch/classes" \
  jdk.internal.m3.M3FusedMaskedFilteredSortedViewTest
"$JAVA_HOME/bin/java" -Xcheck:jni --patch-module "java.base=$scratch/classes" \
  jdk.internal.m3.M3MaskCollectionViewIntegrationTest native "$scratch/libm3_mask_original.so"
"$JAVA_HOME/bin/java" -Xint -Xcheck:jni --patch-module "java.base=$scratch/classes" \
  jdk.internal.m3.M3MaskCountTest native-quick "$scratch/libm3_mask_original.so"
"$JAVA_HOME/bin/java" -Xcheck:jni --patch-module "java.base=$scratch/classes" \
  jdk.internal.m3.M3MaskPreparedPredicateTest native "$scratch/libm3_mask_original.so"

# Preserve the existing java.util live/sorted contract test; no alternate wrapper.
"$JAVA_HOME/bin/javac" -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$scratch/classes" --add-modules jdk.management \
  -d "$scratch/contracts" \
  "$tests/java/util/CollectionViews/RunSearchJni.java" \
  "$tests/java/util/CollectionViews/BootstrapViewsTest.java" \
  "$tests/java/util/CollectionViews/CollectionViewsContract.java"
"$JAVA_HOME/bin/java" --patch-module "java.base=$scratch/classes" -Xss256k \
  -cp "$scratch/contracts" BootstrapViewsTest
"$JAVA_HOME/bin/java" --patch-module "java.base=$scratch/classes" -Xss256k \
  --add-opens java.base/java.util=ALL-UNNAMED --add-modules jdk.management \
  -cp "$scratch/contracts" CollectionViewsContract --baseline

echo 'PASS: exact integrated M3JDK21 Java21 array/mask fused views, JNI scan/count and filtered/sorted contracts'
