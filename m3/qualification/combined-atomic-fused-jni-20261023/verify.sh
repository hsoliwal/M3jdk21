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

# The donor verifier's 12-file #1878 receipt targets a different, older
# java.util.CollectionViews. Bind the exact present M3JDK21 combined owner,
# retaining every later List/Set stored-occurrence and callback repair.
python3 - "$root" <<'PY'
import hashlib
import pathlib
import sys
root = pathlib.Path(sys.argv[1])
expected = {
    "src/java.base/share/classes/jdk/internal/m3/M3IndexMaskWindow.java": "91c7a99d394fe76a47af0c1eaaa35ffd6d070ee6e2497a63668db8301dd86329",
    "src/java.base/share/native/libjava/M3IndexMaskWindow.c": "4a9c0ab3e8d7a0aac0027e8fb4789f768ee5a48b2db18b7a54e2fd543c5c7f91",
    "src/java.base/share/classes/jdk/internal/m3/M3IndexedArray.java": "0c79f59b111bc1669a626c4a45598b7205319522b0c00c56ef009a8b9ea660e5",
    "src/java.base/share/classes/jdk/internal/m3/M3IndexedView.java": "6eaccec5b5ccc41b39ba165c2b5c57768e84d964430a7c28fba0c5061d41ba04",
    "src/java.base/share/classes/java/util/CollectionViews.java": "c8820e5bbd7b7c7b4c683f5081ec74b99a4aac6b854f6c8dc79d4e0ee4767ffb",
    "test/jdk/jdk/internal/m3/M3MaskCountTest.java": "988ab3de69205aebd2c42b26c1be06ae098d149da110ebb5dee861515482b898",
    "test/jdk/jdk/internal/m3/M3MaskPreparedPredicateTest.java": "650969a480f12a861a34252dee5512d1f53c4da9f32370fd546faacc1ef8d572",
    "test/jdk/jdk/internal/m3/M3MaskCollectionViewIntegrationTest.java": "4d3961f38b9f0776f6a70c4845a9b24b3e73fc5e21292aea6c7c2e547e0b11c5",
    "test/jdk/jdk/internal/m3/M3FusedMaskedFilteredSortedViewTest.java": "1c599c9f7a8dd98dd1c6b067c7703105923914fa9a86082899a133993ed1bf97",
    "test/jdk/java/util/CollectionViews/BootstrapViewsTest.java": "a36b48eee0924abd57ce1a356d26ce4e8c6516170e65478cd42cb2d09fbd8a75",
    "test/jdk/java/util/CollectionViews/CollectionViewsContract.java": "9177fd1b1e69f5b0ef05188e4c24aad73ac286a32d7e0abdbeff8db730a94067",
}
for name, expected_sha in sorted(expected.items()):
    source = root / name
    if not source.is_file() or source.is_symlink():
        raise SystemExit("M3JDK21_EXACT_SOURCE_MISSING: " + name)
    observed = hashlib.sha256(source.read_bytes()).hexdigest()
    if observed != expected_sha:
        raise SystemExit("M3JDK21_EXACT_SOURCE_DRIFT: " + name + " expected=" + expected_sha + " actual=" + observed)
    print("M3JDK21_SOURCE_SEAL_PASS " + name)
PY

scratch="$(mktemp -d)"
trap 'rm -rf -- "$scratch"' EXIT
mkdir -p "$scratch/classes" "$scratch/headers" "$scratch/contracts"
base="$root/src/java.base/share/classes"
libjava="$root/src/java.base/share/native/libjava"
tests="$root/test/jdk"

# Compile only the original, exact M3JDK21 product owners. Passing the
# entire OpenJDK source tree to --patch-module can implicitly compile unrelated
# WeakHashMap.java and fail on its upstream this-escape warning under -Werror.
# Keep ALL lint categories enabled and source-stage only these four JDK owners.
patch_sources="$scratch/patch-java-base"
mkdir -p "$patch_sources/jdk/internal/m3" "$patch_sources/java/util"
cp -- "$base/jdk/internal/m3/M3IndexMaskWindow.java" "$patch_sources/jdk/internal/m3/M3IndexMaskWindow.java"
cp -- "$base/jdk/internal/m3/M3IndexedArray.java" "$patch_sources/jdk/internal/m3/M3IndexedArray.java"
cp -- "$base/jdk/internal/m3/M3IndexedView.java" "$patch_sources/jdk/internal/m3/M3IndexedView.java"
cp -- "$base/java/util/CollectionViews.java" "$patch_sources/java/util/CollectionViews.java"
"$JAVA_HOME/bin/javac" -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$patch_sources" -h "$scratch/headers" -d "$scratch/classes" \
  "$patch_sources/jdk/internal/m3/M3IndexMaskWindow.java" \
  "$patch_sources/jdk/internal/m3/M3IndexedArray.java" \
  "$patch_sources/jdk/internal/m3/M3IndexedView.java" \
  "$patch_sources/java/util/CollectionViews.java"

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

echo 'PASS: M3JDK21 current atomic-fused Java/C JNI scan-count and stored-representative collection contracts'
