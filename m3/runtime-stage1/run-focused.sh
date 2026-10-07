#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_BASE_JDK:?Set M3_BASE_JDK to the preserved unpatched image}"
: "${M3_TEST_JDK:?Set M3_TEST_JDK to the patched image}"
result=${M3_RESULTS:-$root/m3/build/runtime-stage1}
mkdir -p "$result/classes"
"$M3_BASE_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "$root"/m3/runtime-stage1/tests/*.java
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -shared -fPIC \
  -I"$M3_BASE_JDK/include" -I"$M3_BASE_JDK/include/linux" \
  "$root/m3/runtime-stage1/tests/join_jni.c" -o "$result/libjoinprobe.so"
for lane in baseline patched; do
  jdk=$M3_BASE_JDK; patched=false
  if [[ $lane == patched ]]; then jdk=$M3_TEST_JDK; patched=true; fi
  for mode in jit int nocompact c2; do
    opts=()
    case $mode in int) opts=(-Xint);; nocompact) opts=(-XX:-CompactStrings);; c2) opts=(-Xbatch -XX:-TieredCompilation);; esac
    "$jdk/bin/java" -ea "${opts[@]}" --add-opens java.base/java.lang=ALL-UNNAMED \
      -Dm3.patched="$patched" -cp "$result/classes" SingletonJoin > "$result/$lane-$mode-contract.log" 2>&1
    "$jdk/bin/java" -ea -Xcheck:jni "${opts[@]}" -Dm3.jni="$result/libjoinprobe.so" \
      -cp "$result/classes" JoinJni > "$result/$lane-$mode-jni.log" 2>&1
    cat "$result/$lane-$mode-contract.log" "$result/$lane-$mode-jni.log"
  done
  for gc in g1 serial zgc shenandoah; do
    case $gc in g1) opts=(-XX:+UseG1GC -XX:+UseStringDeduplication);;
      serial) opts=(-XX:+UseSerialGC);; zgc) opts=(-XX:+UseZGC);; shenandoah) opts=(-XX:+UseShenandoahGC);; esac
    "$jdk/bin/java" -ea -Xmx256m -XX:+UnlockDiagnosticVMOptions -XX:+VerifyBeforeGC -XX:+VerifyAfterGC \
      "${opts[@]}" --add-opens java.base/java.lang=ALL-UNNAMED -Dm3.patched="$patched" -Dm3.checkBacking=false \
      -cp "$result/classes" SingletonJoin > "$result/$lane-$gc-contract.log" 2>&1
    cat "$result/$lane-$gc-contract.log"
  done
done
