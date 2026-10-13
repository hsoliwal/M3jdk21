#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
MODULE="$ROOT/m3/ports/parallel-bulk"
PARALLEL="$ROOT/m3/ports/parallel"
BUILD="${BUILD_DIR:-$MODULE/target/verify-standalone}"
rm -rf "$BUILD"
mkdir -p "$BUILD/patch-src/jdk/internal/vm/parallel" "$BUILD/patch" "$BUILD/classes"
for name in BulkTask BulkExecutor BulkExecution BulkExecutorProvider BulkScheduler; do
  cp "$ROOT/src/java.base/share/classes/jdk/internal/vm/parallel/$name.java" "$BUILD/patch-src/jdk/internal/vm/parallel/"
done
javac -source 21 -target 21 -proc:none -Xlint:all,-module -Werror \
  --patch-module "java.base=$BUILD/patch-src" -d "$BUILD/patch" \
  "$BUILD"/patch-src/jdk/internal/vm/parallel/*.java
mapfile -d '' SOURCES < <(find "$PARALLEL/src/main/java/com" -name '*.java' -print0 | sort -z)
javac --release 21 -proc:none -Xlint:all,-module -Werror -d "$BUILD/classes" "${SOURCES[@]}"
javac -source 21 -target 21 -proc:none -Xlint:all,-module -Werror \
  --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" -d "$BUILD/classes" \
  "$MODULE/src/main/java/com/m3/parallel/bulk/M3ParallelIntAddProvider.java" \
  "$MODULE/verification/M3ParallelIntAddProof.java"
java --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" M3ParallelIntAddProof
java -Xint --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" M3ParallelIntAddProof
