#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set M3_TEST_JDK to matched segmented runtime image}"
: "${M3_BOOT_JDK:?Set M3_BOOT_JDK to a private original Java21 compiler}"
result=${M3_RESULTS:-$root/m3/build/runtime-segments}
mkdir -p "$result/classes"
"$M3_BOOT_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" \
  "$root"/m3/runtime-segments/tests/*.java "$root/m3/runtime-stage1/tests/JoinJni.java"
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -shared -fPIC \
  -I"$M3_BOOT_JDK/include" -I"$M3_BOOT_JDK/include/linux" \
  "$root/m3/runtime-stage1/tests/join_jni.c" -o "$result/libjoinprobe.so"
flags=(-ea -esa -XX:+UnlockExperimentalVMOptions -XX:+UseM3SegmentedStrings -XX:-CreateCoredumpOnCrash \
  --add-opens java.base/java.lang=ALL-UNNAMED -cp "$result/classes")
for compact in + -;do
  for probe in SegmentedStringProbe SegmentedFallbackProbe SegmentedConcurrent;do
    timeout 180 "$M3_TEST_JDK/bin/java" "${flags[@]}" -XX:"$compact"CompactStrings \
      "$probe" > "$result/$probe-compact$compact.log" 2>&1
    cat "$result/$probe-compact$compact.log"
  done
  timeout 180 "$M3_TEST_JDK/bin/java" "${flags[@]}" -XX:"$compact"CompactStrings -Xcheck:jni \
    -Dm3.jni="$result/libjoinprobe.so" SegmentedJniProbe > "$result/jni-compact$compact.log" 2>&1
  cat "$result/jni-compact$compact.log"
done
for gc in UseG1GC UseSerialGC UseZGC UseShenandoahGC;do
  timeout 180 "$M3_TEST_JDK/bin/java" "${flags[@]}" -Xmx256m -XX:+UnlockDiagnosticVMOptions \
    -XX:+VerifyBeforeGC -XX:+VerifyAfterGC -XX:+"$gc" SegmentedConcurrent > "$result/gc-$gc.log" 2>&1
  cat "$result/gc-$gc.log"
done
