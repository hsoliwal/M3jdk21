#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set matched runtime image}"
: "${M3_BOOT_JDK:?Set private original Java21 compiler}"
result=${M3_RESULTS:-$root/m3/build/runtime-integration}
mkdir -p "$result/classes"
"$M3_BOOT_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "$root/m3/runtime-integration/tests/SegmentedAllocation.java"
for fork in 1 2 3;do
 for enabled in false true;do
  flags=();if [[ $enabled == true ]];then flags=(-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage);fi
  "$M3_TEST_JDK/bin/java" -Xint -Xms256m -Xmx256m -XX:+UseG1GC -XX:-UseStringDeduplication \
   "${flags[@]}" --add-opens java.base/java.lang=ALL-UNNAMED -cp "$result/classes" \
   SegmentedAllocation "$enabled" > "$result/allocation-$enabled-$fork.json" 2>&1
  cat "$result/allocation-$enabled-$fork.json"
 done
done
