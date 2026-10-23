#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Selected in-java.base source and jtreg-oracle smoke gate, not an M3JDK image build.
set -euo pipefail
cd "$(dirname "$0")/../../../.."
src=src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java
test=test/jdk/java/util/ArrayList/M3CollectionMaskCompactorContract.java
source_blob=$(git hash-object "$src")
test_blob=$(git hash-object "$test")
[[ "$source_blob" == e6ca59d8ae7fba700765e670dd5ec7d8dd84bc8d ]] || {
  echo "Unexpected compactor source revision: $source_blob" >&2
  exit 1
}
[[ "$test_blob" == 066eb1b6a636b38c56bc007d1a83eeb6793ddf40 ]] || {
  echo "Unexpected jtreg oracle revision: $test_blob" >&2
  exit 1
}
# Independent public JDK collection oracle; stock host semantics, not M3 dispatch.
public_test=test/jdk/java/util/ArrayList/M3ArrayListVectorRemoveIfContract.java
[[ "$(git hash-object "$public_test")" == c760bb2ba03f6ad2b587834abd9abbb602ac1ae9 ]] || {
  echo "Unexpected public contract oracle source" >&2
  exit 1
}
build=$(mktemp -d)
trap 'rm -rf -- "$build"' EXIT
mkdir -p "$build/source/jdk/internal/mindex" "$build/java.base" "$build/tests"
cp -- "$src" "$build/source/jdk/internal/mindex/M3CollectionMaskCompactor.java"
javac -Xlint:all -Werror \
  --patch-module java.base="$build/source" \
  -d "$build/java.base" "$build/source/jdk/internal/mindex/M3CollectionMaskCompactor.java"
javac -Xlint:all -Werror \
  --patch-module java.base="$build/java.base" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -d "$build/tests" "$test" "$public_test"
java --patch-module java.base="$build/java.base" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -cp "$build/tests" M3CollectionMaskCompactorContract
java -cp "$build/tests" M3ArrayListVectorRemoveIfContract
echo "PASS strict Java21 kernel oracle and stock-runtime ArrayList/Vector reference contract"
echo "NOTE public test has NOT exercised the M3JDK21 java.util integration: requires a product image"
