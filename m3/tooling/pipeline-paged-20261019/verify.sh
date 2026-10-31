#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Focused Java21 proof of the actual M3JDK21 java.base product source.
set -euo pipefail
cd "$(dirname "$0")/../../.."
src=src/java.base/share/classes/jdk/internal/util/M3PipelinePlan.java
expected=b8be59d0ef28caad202da24866742e016ccb463ee895c8355ae24acf3143723b
actual="$(sha256sum "$src" | cut -d' ' -f1)"
if [[ "$actual" != "$expected" ]]; then
  echo "REFUSING changed product source digest: $actual" >&2
  exit 1
fi
workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT
mkdir -p "$workspace/classes" "$workspace/tests"
javac -Xlint:all --patch-module java.base=src/java.base/share/classes/jdk/internal/util \
  -d "$workspace/classes" "$src"
javac -Xlint:all --add-exports java.base/jdk.internal.util=ALL-UNNAMED \
  --patch-module java.base="$workspace/classes" -d "$workspace/tests" \
  test/jdk/jdk/internal/util/M3PipelinePlanTest.java \
  test/jdk/jdk/internal/util/M3PipelinePlanIterativeTest.java \
  test/jdk/jdk/internal/util/M3PipelinePlanDifferentialTest.java \
  test/jdk/jdk/internal/util/M3PipelinePageTest.java \
  test/jdk/jdk/internal/util/M3PipelinePageDifferentialTest.java
for t in M3PipelinePlanTest M3PipelinePlanIterativeTest \
         M3PipelinePlanDifferentialTest M3PipelinePageTest M3PipelinePageDifferentialTest; do
  java -Xmx512m -Xss256k --patch-module java.base="$workspace/classes" \
    --add-exports java.base/jdk.internal.util=ALL-UNNAMED \
    -cp "$workspace/tests" "$t"
done
echo "M3JDK21_PAGED_PIPELINE_GATES PASS"
