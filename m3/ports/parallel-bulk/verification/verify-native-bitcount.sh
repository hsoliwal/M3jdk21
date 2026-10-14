#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
MODULE="$ROOT/m3/ports/parallel-bulk"
BUILD="${BUILD_DIR:-$MODULE/target/verify-native-bitcount}"
JHOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
rm -rf "$BUILD"
mkdir -p "$BUILD/patch-src/jdk/internal/vm/parallel" "$BUILD/patch" "$BUILD/classes"
for name in BulkTask BulkExecutor BulkExecution BulkExecutorProvider BulkScheduler; do
  cp "$ROOT/src/java.base/share/classes/jdk/internal/vm/parallel/$name.java" \
     "$BUILD/patch-src/jdk/internal/vm/parallel/"
done
javac -source 21 -target 21 -proc:none -Xlint:all,-module -Werror \
  --patch-module "java.base=$BUILD/patch-src" -d "$BUILD/patch" \
  "$BUILD"/patch-src/jdk/internal/vm/parallel/*.java
javac -source 21 -target 21 -proc:none -Xlint:all,-module -Werror \
  --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -h "$BUILD" -d "$BUILD/classes" \
  "$MODULE/src/main/java/com/m3/parallel/bulk/M3ParallelNativeBitCountProvider.java" \
  "$MODULE/src/test/java/com/m3/parallel/bulk/M3ParallelNativeBitCountProof.java"
cc -shared -fPIC -std=c11 -Wall -Wextra -Wconversion -Wshadow -Wpedantic -Werror \
  -fsanitize=undefined -I"$JHOME/include" -I"$JHOME/include/linux" \
  -o "$BUILD/libm3bitcount.so" "$MODULE/native/m3_native_bitcount_jni.c"
java -ea -Xcheck:jni -D"m3.parallel.bitcount.native.path=$BUILD/libm3bitcount.so" \
  --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" com.m3.parallel.bulk.M3ParallelNativeBitCountProof
java -ea -Xint -Xcheck:jni -D"m3.parallel.bitcount.native.path=$BUILD/libm3bitcount.so" \
  --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" com.m3.parallel.bulk.M3ParallelNativeBitCountProof
java --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" com.m3.parallel.bulk.M3ParallelNativeBitCountProof missing
