#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_BASE_JDK:?Set M3_BASE_JDK}"
: "${M3_TEST_JDK:?Set M3_TEST_JDK}"
result=${M3_RESULTS:-$root/m3/build/runtime-stage1}
mkdir -p "$result/classes"
"$M3_BASE_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "$root/m3/runtime-stage1/tests/JoinAllocation.java"
for length in 1 53 1024; do
  for wide in false true; do
    for fork in 1 2 3; do
      for lane in baseline patched; do
        jdk=$M3_BASE_JDK
        if [[ $lane == patched ]]; then jdk=$M3_TEST_JDK; fi
        "$jdk/bin/java" -Xms512m -Xmx512m -XX:+UseG1GC -XX:-UseStringDeduplication \
          --add-opens java.base/java.lang=ALL-UNNAMED -cp "$result/classes" \
          JoinAllocation "$length" "$wide" > "$result/allocation-$lane-$length-$wide-$fork.json" 2>&1
        cat "$result/allocation-$lane-$length-$wide-$fork.json"
      done
    done
  done
done
