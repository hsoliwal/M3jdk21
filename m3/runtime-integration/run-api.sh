#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set matched runtime image}"
: "${M3_BOOT_JDK:?Set original private Java21}"
result=${M3_RESULTS:-$root/m3/build/runtime-integration}
mkdir -p "$result/classes"
"$M3_BOOT_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "$root/m3/runtime-integration/tests/StringApiProbe.java"
gcc -shared -fPIC -I"$M3_BOOT_JDK/include" -I"$M3_BOOT_JDK/include/linux" \
 "$root/m3/runtime-integration/tests/string_api_probe.c" -o "$result/libapiprobe.so"
for mode in default int c2 enabled enabled-nocompact;do
 flags=();case $mode in int)flags=(-Xint);;c2)flags=(-Xbatch -XX:-TieredCompilation);;
 enabled)flags=(-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage);;
 enabled-nocompact)flags=(-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings);;esac
 timeout 240 "$M3_TEST_JDK/bin/java" -ea -esa -Xcheck:jni -XX:-CreateCoredumpOnCrash "${flags[@]}" \
   --add-opens java.base/java.lang=ALL-UNNAMED -Dprobe.native="$result/libapiprobe.so" \
   -cp "$result/classes" StringApiProbe > "$result/api-$mode.log" 2>&1
 grep -F 'SEMANTICS checks=808685 digest=30e36ec204f5aec8e0d672ff5be675d3f8ac1677164481ec82a335b2540da499' "$result/api-$mode.log"
done
