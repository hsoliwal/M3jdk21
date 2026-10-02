#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_BASE_JDK:?Set M3_BASE_JDK}"
: "${M3_TEST_JDK:?Set M3_TEST_JDK}"
: "${JTREG_JAR:?Set JTREG_JAR to the isolated jtreg.jar}"
result=${M3_RESULTS:-$root/m3/build/runtime-stage1}
mkdir -p "$result"
native=${M3_NATIVE_DIR:-$root/build/linux-x86_64-server-fastdebug/support/test/jdk/jtreg/native/lib}
[[ -f "$native/libstringPlatformChars.so" ]] || { echo "Build the upstream native test library; see README." >&2; exit 2; }
for lane in base patched; do
  jdk=$M3_BASE_JDK
  if [[ $lane == patched ]]; then jdk=$M3_TEST_JDK; fi
  for suite in jdk hotspot; do
    case $suite in
      jdk) paths=("$root/test/jdk/java/lang/String" "$root/test/jdk/java/util/StringJoiner");;
      hotspot) paths=("$root/test/hotspot/jtreg/compiler/intrinsics/string" "$root/test/hotspot/jtreg/compiler/stringopts");;
    esac
    "$M3_BASE_JDK/bin/java" -jar "$JTREG_JAR" -jdk:"$jdk" -nativepath:"$native" -conc:2 -timeoutFactor:4 \
      -w:"$result/jtreg-$lane-$suite-work" -r:"$result/jtreg-$lane-$suite-report" \
      "${paths[@]}" > "$result/jtreg-$lane-$suite.log" 2>&1
    cat "$result/jtreg-$lane-$suite.log"
  done
done
"$M3_BASE_JDK/bin/java" -jar "$JTREG_JAR" -jdk:"$M3_TEST_JDK" -conc:2 -timeoutFactor:4 \
  -w:"$result/jtreg-focused-work" -r:"$result/jtreg-focused-report" \
  "$root/m3/runtime-stage1/tests/SingletonJoin.java" > "$result/jtreg-focused.log" 2>&1
cat "$result/jtreg-focused.log"
