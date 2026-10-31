#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
: "${M3_JDK:?Set the matching built M3jdk21 image}"
: "${M3_BOOT_JDK:?Set the boot JDK for the jtreg harness}"
: "${M3_JTREG:?Set jtreg.jar}"
out=${M3_RESULTS:-$root/m3/tooling/cb/target/image}
mkdir -p "$out/classes" "$out/source"
"$M3_JDK/bin/java" -version > "$out/version.log" 2>&1
cc -shared -fPIC -Wall -Wextra -Werror -I"$M3_JDK/include" -I"$M3_JDK/include/linux" \
  "$root/test/jdk/jdk/internal/mindex/cb/libM3JNINull.c" -o "$out/libM3JNINull.so"
"$M3_BOOT_JDK/bin/java" -jar "$M3_JTREG" -jdk:"$M3_JDK" -conc:1 -timeoutFactor:4 \
  -nativepath:"$out" -w:"$out/jtreg-work" -r:"$out/jtreg-report" \
  "$root/test/jdk/jdk/internal/mindex/cb" \
  "$root/test/jdk/java/lang/String/MIndexMappedStringBackingTest.java" \
  > "$out/jtreg.log" 2>&1

# Reuse the existing JNI/String semantic oracle; no alternate probe framework.
"$M3_JDK/bin/javac" -d "$out/classes" \
  "$root/m3/runtime-integration/tests/StringApiProbe.java"
cc -shared -fPIC -Wall -Wextra -Werror -I"$M3_JDK/include" -I"$M3_JDK/include/linux" \
  "$root/m3/runtime-integration/tests/string_api_probe.c" -o "$out/libstring_api_probe.so"
"$M3_JDK/bin/java" -Xmixed -Xcheck:jni --add-opens java.base/java.lang=ALL-UNNAMED \
  -Dprobe.native="$out/libstring_api_probe.so" -cp "$out/classes" StringApiProbe \
  > "$out/jni-default.log" 2>&1
"$M3_JDK/bin/java" -Xint -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage \
  --add-opens java.base/java.lang=ALL-UNNAMED -Dprobe.native="$out/libstring_api_probe.so" \
  -cp "$out/classes" StringApiProbe > "$out/jni-enabled.log" 2>&1

# Execute the retained VI oracle on this image, without patching java.base.
cp "$root/m3/tooling/vi/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-vi/VIProbe.java.txt" \
  "$out/source/VIProbe.java"
"$M3_JDK/bin/javac" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -d "$out/classes" "$out/source/VIProbe.java"
for mode in int mixed; do
  "$M3_JDK/bin/java" "-X$mode" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    -cp "$out/classes" com.m3.vi.VIProbe > "$out/vi-$mode.log" 2>&1
done

"$M3_JDK/bin/javac" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$out/classes" \
  "$root/test/jdk/jdk/internal/mindex/cb/com/m3/cb/CBProbe.java" \
  "$root/test/jdk/jdk/internal/mindex/cb/com/m3/cb/VmFixture.java"
"$M3_JDK/bin/java" -Xbatch -XX:TieredStopAtLevel=1 -XX:CompileThreshold=100 -XX:+PrintCompilation \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$out/classes" com.m3.cb.CBProbe \
  > "$out/c1.log" 2>&1
"$M3_JDK/bin/java" -Xbatch -XX:-TieredCompilation -XX:CompileThreshold=100 -XX:+PrintCompilation \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$out/classes" com.m3.cb.CBProbe \
  > "$out/c2.log" 2>&1
cat "$out/jtreg.log" "$out/jni-default.log" "$out/jni-enabled.log" \
  "$out/vi-int.log" "$out/vi-mixed.log"
