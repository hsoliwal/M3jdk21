#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# This bounded tool builds patched Java21 class sources; it does NOT build the full M3JDK21 image.
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
src="$root/src/java.base/share/classes"
test="$root/test/jdk"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

if [[ "$(javac -version 2>&1)" != javac\ 21* ]]; then
  echo "BLOCKED: javac 21 is required" >&2
  exit 2
fi
if [[ "$(java -version 2>&1 | head -1)" != *\"21\"* ]]; then
  echo "BLOCKED: java 21 is required" >&2
  exit 2
fi
if [[ "$(git -C "$root" hash-object "$src/jdk/internal/mindex/M3CollectionMaskCompactor.java")" \
       != "f85e7359010bd25e0a909a145e283007cff25d7c" ]]; then
  echo "HOLD: M3JDK21 mask compactor source drift" >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$test/jdk/internal/mindex/M3CollectionMaskCompactorPrefixTest.java")" \
       != "39875249ce1b1adf5203bcf2074db45848bc5eeb" ]]; then
  echo "HOLD: original jtreg oracle source drift" >&2
  exit 1
fi

mkdir -p "$tmp/src/jdk/internal/mindex" "$tmp/src/java/util" "$tmp/classes" "$tmp/tests"
cp "$src/jdk/internal/mindex/M3CollectionMaskCompactor.java" "$tmp/src/jdk/internal/mindex/"
cp "$src/java/util/CollectionViews.java" "$tmp/src/java/util/"

# Explicit source files: never invoke a repo-wide patch-module compilation.
javac -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$tmp/src" -d "$tmp/classes" \
  "$tmp/src/jdk/internal/mindex/M3CollectionMaskCompactor.java" \
  "$tmp/src/java/util/CollectionViews.java"

javac -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$tmp/classes" \
  --add-exports "java.base/jdk.internal.mindex=ALL-UNNAMED" \
  -d "$tmp/tests" \
  "$test/jdk/internal/mindex/M3CollectionMaskCompactorPrefixTest.java" \
  "$test/java/util/CollectionViews/RunSearchJni.java" \
  "$test/java/util/CollectionViews/BootstrapViewsTest.java" \
  "$test/java/util/CollectionViews/CollectionViewsContract.java"

java --patch-module "java.base=$tmp/classes" \
  --add-exports "java.base/jdk.internal.mindex=ALL-UNNAMED" \
  -cp "$tmp/tests" M3CollectionMaskCompactorPrefixTest

java --patch-module "java.base=$tmp/classes" -Xss256k \
  -cp "$tmp/tests" BootstrapViewsTest

# Existing backed/live CollectionViews contract is reused, not copied or replaced.
# No JNI library is loaded here; native parity/full installed image remain separate gates.
java --patch-module "java.base=$tmp/classes" -Xss256k \
  --add-opens "java.base/java.util=ALL-UNNAMED" --add-modules jdk.management \
  -cp "$tmp/tests" CollectionViewsContract

echo "PASS: JDK21 patch-module compaction, bootstrap filtered views and sorted contract oracles"
