#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
MODULE="$ROOT/m3/ports/parallel-bulk"
PARALLEL="$ROOT/src/m3.parallel/share/classes"
BUILD="${BUILD_DIR:-$MODULE/target/verify-standalone}"
rm -rf "$BUILD"
mkdir -p "$BUILD/patch-src/jdk/internal/vm/parallel" "$BUILD/patch" "$BUILD/classes"
for name in BulkTask BulkExecutor BulkExecution BulkExecutorProvider BulkScheduler; do
  cp "$ROOT/src/java.base/share/classes/jdk/internal/vm/parallel/$name.java" "$BUILD/patch-src/jdk/internal/vm/parallel/"
done
javac -source 21 -target 21 -proc:none -Xlint:all,-module -Werror \
  --patch-module "java.base=$BUILD/patch-src" -d "$BUILD/patch" \
  "$BUILD"/patch-src/jdk/internal/vm/parallel/*.java
mapfile -d '' SOURCES < <(find "$PARALLEL/jdk" -name '*.java' -print0 | sort -z)
javac --release 21 -proc:none -Xlint:all,-module -Werror -d "$BUILD/classes" "${SOURCES[@]}"
javac -source 21 -target 21 -proc:none -Xlint:all,-module -Werror \
  --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" -d "$BUILD/classes" \
  "$ROOT/src/m3.parallel.bulk.provider/share/classes/jdk/m3/parallel/bulk/M3ParallelIntAddProvider.java" \
  "$MODULE/verification/M3ParallelIntAddProof.java"
java --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" M3ParallelIntAddProof
java -Xint --patch-module "java.base=$BUILD/patch" \
  --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED \
  -cp "$BUILD/classes" M3ParallelIntAddProof

# Execute the actual M3 parallel provider SPI regression against the modules
# compiled above, not a mock provider layer. jtreg's @build dependency is
# explicit, and every @run scenario receives a fresh JVM/class initialization.
PROVIDER_TEST="$ROOT/test/jdk/jdk/m3/parallel/BackendProviderFailureTest.java"
SWAP_TEST="$ROOT/test/jdk/jdk/m3/parallel/BackendSwapTest.java"
for test_source in "$SWAP_TEST" "$PROVIDER_TEST"; do
  if [[ ! -f "$test_source" ]]; then
    echo "Missing required M3 parallel provider regression: $test_source" >&2
    exit 1
  fi
done
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  -cp "$BUILD/classes" -d "$BUILD/classes" \
  "$SWAP_TEST" "$PROVIDER_TEST"

for vm in default interpreter; do
  options=()
  if [[ "$vm" == interpreter ]]; then
    options=(-Xint)
  fi
  for scenario in id capabilities score probe supports-service supports-runtime \
      supports-linkage supports-assertion reference-error; do
    actual=$(java "${options[@]}" -cp "$BUILD/classes" \
      BackendProviderFailureTest "$scenario")
    expected="BackendProviderFailureTest passed $scenario"
    if [[ "$actual" != "$expected" ]]; then
      echo "M3 parallel provider case failed ($vm, $scenario): $actual" >&2
      exit 1
    fi
    printf '%s [%s]\n' "$actual" "$vm"
  done
done
printf 'M3_PARALLEL_PROVIDER_SPI_PASS configurations=2 scenarios=9 checks=18\n'
