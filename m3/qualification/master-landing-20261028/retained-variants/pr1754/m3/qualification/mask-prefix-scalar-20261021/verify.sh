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
if [[ "$(java -version 2>&1 | head -1)" != *\"21.* ]]; then
  echo "BLOCKED: java 21 is required" >&2
  exit 2
fi
if [[ "$(git -C "$root" hash-object "$src/jdk/internal/mindex/M3CollectionMaskCompactor.java")" \
       != "f85e7359010bd25e0a909a145e283007cff25d7c" ]]; then
  echo "HOLD: M3JDK21 mask compactor source drift" >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$test/jdk/internal/mindex/M3CollectionMaskCompactorPrefixTest.java")" \
       != "d720912f0f57bcde9fc07a05f31869a19f3452eb" ]]; then
  echo "HOLD: original jtreg oracle source drift" >&2
  exit 1
fi
# Confirm the actual original JDK callers, not only the stand-alone kernel.
if [[ "$(git -C "$root" hash-object "$src/java/util/ArrayList.java")" \
       != "885d891b0f817080824622d592691a27cf8ffa26" ]]; then
  echo "HOLD: original M3JDK21 java.util.ArrayList source drift" >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$src/java/util/Vector.java")" \
       != "a79a77ce8700dc494d02d906b2a7765670629eee" ]]; then
  echo "HOLD: original M3JDK21 java.util.Vector source drift" >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$test/java/util/ArrayList/M3ArrayMaskCallersContract.java")" \
       != "81fcd00c088dd9ae8ed203b71a59a1dae6fdee7e" ]]; then
  echo "HOLD: original JDK ArrayList/Vector behavior oracle source drift" >&2
  exit 1
fi
# Existing, previously qualified original JDK array-mask and subList/Vector donor tests.
if [[ "$(git -C "$root" hash-object "$test/java/util/ArrayList/M3MaskCompactionDifferential.java")" \
       != "c505b5604e978f215e4aeb93eadfd9f933ffc743" ]]; then
  echo "HOLD: original JDK compaction differential changed" >&2
  exit 1
fi
if [[ "$(git -C "$root" hash-object "$test/java/util/ArrayList/M3JdkRemoveIfMaskContracts.java")" \
       != "2dc2fb6494f008fb7166dabfb97000d282a5f9af" ]]; then
  echo "HOLD: original JDK removeIf/sublist contract changed" >&2
  exit 1
fi

mkdir -p "$tmp/src/jdk/internal/mindex" "$tmp/src/java/util" "$tmp/classes" "$tmp/tests"
cp "$src/jdk/internal/mindex/M3CollectionMaskCompactor.java" "$tmp/src/jdk/internal/mindex/"
cp "$src/java/util/CollectionViews.java" "$tmp/src/java/util/"
cp "$src/java/util/ArrayList.java" "$tmp/src/java/util/"
cp "$src/java/util/Vector.java" "$tmp/src/java/util/"

# Explicit source files: never invoke a repo-wide patch-module compilation.
javac -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$tmp/src" -d "$tmp/classes" \
  "$tmp/src/jdk/internal/mindex/M3CollectionMaskCompactor.java" \
  "$tmp/src/java/util/ArrayList.java" \
  "$tmp/src/java/util/Vector.java" \
  "$tmp/src/java/util/CollectionViews.java"

javac -source 21 -target 21 -Xlint:all -Werror -proc:none \
  --patch-module "java.base=$tmp/classes" \
  --add-exports "java.base/jdk.internal.mindex=ALL-UNNAMED" \
  -d "$tmp/tests" \
  "$test/jdk/internal/mindex/M3CollectionMaskCompactorPrefixTest.java" \
  "$test/java/util/ArrayList/M3ArrayMaskCallersContract.java" \
  "$test/java/util/ArrayList/M3MaskCompactionDifferential.java" \
  "$test/java/util/ArrayList/M3JdkRemoveIfMaskContracts.java" \
  "$test/java/util/CollectionViews/RunSearchJni.java" \
  "$test/java/util/CollectionViews/BootstrapViewsTest.java" \
  "$test/java/util/CollectionViews/CollectionViewsContract.java"

# Check the original JDK21 caller behavior before patching java.base.
java --add-opens "java.base/java.util=ALL-UNNAMED" \
  -cp "$tmp/tests" M3ArrayMaskCallersContract
java -Xint --add-opens "java.base/java.util=ALL-UNNAMED" \
  -cp "$tmp/tests" M3ArrayMaskCallersContract

# Verify both original collection classes directly call the product-owned M3 atom.
javap -p -c "$tmp/classes/java/util/ArrayList.class" > "$tmp/arraylist-bytecode.txt"
javap -p -c "$tmp/classes/java/util/Vector.class" > "$tmp/vector-bytecode.txt"
grep -Fq "jdk/internal/mindex/M3CollectionMaskCompactor.compact" "$tmp/arraylist-bytecode.txt"
grep -Fq "jdk/internal/mindex/M3CollectionMaskCompactor.compact" "$tmp/vector-bytecode.txt"

# Execute actual M3JDK21 java.util.ArrayList and Vector under mixed and interpreted VM modes.
java --patch-module "java.base=$tmp/classes" \
  --add-opens "java.base/java.util=ALL-UNNAMED" \
  -cp "$tmp/tests" M3ArrayMaskCallersContract
java -Xint --patch-module "java.base=$tmp/classes" \
  --add-opens "java.base/java.util=ALL-UNNAMED" \
  -cp "$tmp/tests" M3ArrayMaskCallersContract

# Source-built java.base classes with three stock Java21 collectors: stores,
# reference identity and post-removal tail clearing must remain correct.
# This is NOT a full custom HotSpot/GC image qualification.
for collector in UseSerialGC UseParallelGC UseG1GC; do
  java "-XX:+$collector" --patch-module "java.base=$tmp/classes" \
    --add-opens "java.base/java.util=ALL-UNNAMED" \
    -cp "$tmp/tests" M3ArrayMaskCallersContract
done

# Reuse original JDK integration regression rather than inventing another collection engine.
java -ea -Xcheck:jni --patch-module "java.base=$tmp/classes" \
  --add-exports "java.base/jdk.internal.mindex=ALL-UNNAMED" \
  -cp "$tmp/tests" M3MaskCompactionDifferential
java -ea -Xcheck:jni --patch-module "java.base=$tmp/classes" \
  -cp "$tmp/tests" M3JdkRemoveIfMaskContracts

# Compare the actual patched class files against the Java 21 public APIs.
# javap -public on a classname alone would silently read the host bootstrap JDK.
javap -public "$tmp/classes/java/util/ArrayList.class" > "$tmp/public-after-arraylist.txt"
javap -public java.util.ArrayList > "$tmp/public-before-arraylist.txt"
diff -u "$tmp/public-before-arraylist.txt" "$tmp/public-after-arraylist.txt"
javap -public "$tmp/classes/java/util/Vector.class" > "$tmp/public-after-vector.txt"
javap -public java.util.Vector > "$tmp/public-before-vector.txt"
diff -u "$tmp/public-before-vector.txt" "$tmp/public-after-vector.txt"

java --patch-module "java.base=$tmp/classes" \
  --add-exports "java.base/jdk.internal.mindex=ALL-UNNAMED" \
  -cp "$tmp/tests" M3CollectionMaskCompactorPrefixTest

java -Xint --patch-module "java.base=$tmp/classes" \
  --add-exports "java.base/jdk.internal.mindex=ALL-UNNAMED" \
  -cp "$tmp/tests" M3CollectionMaskCompactorPrefixTest

java --patch-module "java.base=$tmp/classes" -Xss256k \
  -cp "$tmp/tests" BootstrapViewsTest

# Existing backed/live CollectionViews contract is reused, not copied or replaced.
# No JNI library is loaded here; native parity/full installed image remain separate gates.
java --patch-module "java.base=$tmp/classes" -Xss256k \
  --add-opens "java.base/java.util=ALL-UNNAMED" --add-modules jdk.management \
  -cp "$tmp/tests" CollectionViewsContract

echo "PASS: original Java21 API, native M3JDK ArrayList/Vector and mask differentials, filtered and sorted owners"
