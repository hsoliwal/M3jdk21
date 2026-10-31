#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set matched runtime image}"
: "${M3_BOOT_JDK:?Set private original Java21 compiler}"
result=${M3_RESULTS:-$root/m3/build/runtime-segments}
mkdir -p "$result/classes"
"$M3_BOOT_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "$root/m3/runtime-segments/tests/SegmentedJvmti.java"
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -shared -fPIC \
 -I"$M3_BOOT_JDK/include" -I"$M3_BOOT_JDK/include/linux" \
 "$root/m3/runtime-segments/tests/segmented_jvmti.c" -o "$result/libsegmentsjvmti.so"
"$M3_TEST_JDK/bin/java" -XX:+UnlockExperimentalVMOptions -XX:+UseM3SegmentedStrings \
 -XX:-CreateCoredumpOnCrash -Xcheck:jni --add-opens java.base/java.lang=ALL-UNNAMED \
 -agentpath:"$result/libsegmentsjvmti.so" -Dm3.jvmti="$result/libsegmentsjvmti.so" \
 -cp "$result/classes" SegmentedJvmti > "$result/jvmti.log" 2>&1
cat "$result/jvmti.log"
