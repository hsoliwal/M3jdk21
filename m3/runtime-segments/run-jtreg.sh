#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set matched runtime image}"
: "${M3_BOOT_JDK:?Set private original Java21}"
: "${JTREG_JAR:?Set jtreg.jar path}"
result=${M3_RESULTS:-$root/m3/build/runtime-segments}
native=${M3_NATIVE_DIR:-$root/build/linux-x86_64-server-fastdebug/support/test/jdk/jtreg/native/lib}
mkdir -p "$result"
[[ -f "$native/libstringPlatformChars.so" ]]
"$M3_BOOT_JDK/bin/java" -jar "$JTREG_JAR" -jdk:"$M3_TEST_JDK" -nativepath:"$native" -conc:3 -timeoutFactor:4 \
 -w:"$result/jtreg-final-default-work" -r:"$result/jtreg-final-default-report" \
 "$root/test/jdk/java/lang/String" "$root/test/jdk/java/util/StringJoiner" > "$result/jtreg-final-default.log" 2>&1
paths=()
for name in StringJoinTest StringRepeat HashCode ContentEquals Supplementary RegionMatches Encodings Regex SBConstructor Exceptions;do
 paths+=("$root/test/jdk/java/lang/String/$name.java")
done
for name in SubString Concat Intern;do paths+=("$root/test/jdk/java/lang/String/CompactString/$name.java");done
paths+=("$root/test/jdk/java/lang/String/nativeEncoding/StringPlatformChars.java" "$root/test/jdk/java/util/StringJoiner")
"$M3_BOOT_JDK/bin/java" -jar "$JTREG_JAR" -jdk:"$M3_TEST_JDK" -nativepath:"$native" \
 '-javaoptions:-XX:+UnlockExperimentalVMOptions -XX:+UseM3SegmentedStrings -XX:-CreateCoredumpOnCrash' \
 -conc:2 -timeoutFactor:4 -w:"$result/jtreg-enabled-work" -r:"$result/jtreg-enabled-report" \
 "${paths[@]}" > "$result/jtreg-enabled.log" 2>&1
"$M3_BOOT_JDK/bin/java" -jar "$JTREG_JAR" -jdk:"$M3_TEST_JDK" -conc:1 -timeoutFactor:4 \
 -w:"$result/jtreg-representation-work" -r:"$result/jtreg-representation-report" \
 "$root/m3/runtime-segments/tests/SegmentedStringProbe.java" > "$result/jtreg-representation.log" 2>&1
cat "$result/jtreg-final-default.log" "$result/jtreg-enabled.log" "$result/jtreg-representation.log"
