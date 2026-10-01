#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set M3_TEST_JDK to the stage-2 patched image}"
result=${M3_RESULTS:-$root/m3/build/runtime-stage2}
mkdir -p "$result/classes"

"$M3_TEST_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "$root"/m3/runtime-stage2/tests/*.java
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -shared -fPIC \
  -I"$M3_TEST_JDK/include" -I"$M3_TEST_JDK/include/linux" \
  "$root/m3/runtime-stage2/tests/m3_joined_jni.c" -o "$result/libm3joinedprobe.so"

for storage in flat joined; do
  storage_opts=()
  enabled=false
  if [[ $storage == joined ]]; then
    storage_opts=(-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage)
    enabled=true
  fi
  for mode in jit int nocompact c2; do
    mode_opts=()
    case $mode in
      int) mode_opts=(-Xint);;
      nocompact) mode_opts=(-XX:-CompactStrings);;
      c2) mode_opts=(-Xbatch -XX:-TieredCompilation);;
    esac
    common=(-ea "${storage_opts[@]}" "${mode_opts[@]}" --add-opens java.base/java.lang=ALL-UNNAMED -Dm3.enabled=$enabled)
    "$M3_TEST_JDK/bin/java" "${common[@]}" -cp "$result/classes" M3JoinedStringSemantics \
      > "$result/$storage-$mode-semantics.log" 2>&1
    "$M3_TEST_JDK/bin/java" -Xcheck:jni "${common[@]}" -Dm3.jni="$result/libm3joinedprobe.so" \
      -cp "$result/classes" M3JoinedStringJni > "$result/$storage-$mode-jni.log" 2>&1
    cat "$result/$storage-$mode-semantics.log" "$result/$storage-$mode-jni.log"
  done

  "$M3_TEST_JDK/bin/java" -ea "${storage_opts[@]}" -Dm3.enabled=$enabled \
    -cp "$result/classes" M3JoinedAllocation > "$result/$storage-allocation.log" 2>&1
  cat "$result/$storage-allocation.log"
done

for gc in g1 serial zgc shenandoah; do
  case $gc in
    g1) gc_opts=(-XX:+UseG1GC -XX:+UseStringDeduplication);;
    serial) gc_opts=(-XX:+UseSerialGC);;
    zgc) gc_opts=(-XX:+UseZGC);;
    shenandoah) gc_opts=(-XX:+UseShenandoahGC);;
  esac
  "$M3_TEST_JDK/bin/java" -ea -Xmx256m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage \
    -XX:+UnlockDiagnosticVMOptions -XX:+VerifyBeforeGC -XX:+VerifyAfterGC "${gc_opts[@]}" \
    --add-opens java.base/java.lang=ALL-UNNAMED -Dm3.enabled=true \
    -cp "$result/classes" M3JoinedStringSemantics > "$result/joined-$gc.log" 2>&1
  cat "$result/joined-$gc.log"
done
