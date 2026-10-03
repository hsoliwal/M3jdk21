#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
module="$(cd "$(dirname "$0")" && pwd)"
[[ "$(uname -s)-$(uname -m)" == Linux-x86_64 ]] || { echo 'Unverified image test platform' >&2; exit 2; }
jdk="${M3_JDK:-${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}}"
mkdir -p "$module/target"
work="$(mktemp -d "$module/target/image-tests.XXXXXX")"
printf 'path\tsha256\tmodule\n' > "$work/LOCK.tsv"
bash "$module/assemble.sh" "$work/LOCK.tsv" jdk.jcmd,jdk.jfr,jdk.management,jdk.management.agent "$work/diagnostics"
"$jdk/bin/javac" --release 21 -Xlint:all -Werror -d "$work/probes" "$module/src/it/DiagnosticsSmoke.java"
"$work/diagnostics/image/bin/java" -cp "$work/probes" DiagnosticsSmoke "$work/recording.jfr"
"$work/diagnostics/image/bin/jfr" summary "$work/recording.jfr" > "$work/jfr-summary.txt"
"$work/diagnostics/image/bin/jcmd" -h > "$work/jcmd-help.txt"
mkdir "$work/native" "$work/headers" "$work/legal"
printf 'SPDX-License-Identifier: Apache-2.0\nJNI fixture authored for M3 pack verification.\n' > "$work/legal/NOTICE"
"$jdk/bin/javac" --release 21 -Xlint:all -Werror -h "$work/headers" -d "$work/native-classes" \
  "$module/src/it/native/module-info.java" "$module/src/it/native/com/m3/fixture/NativeProbe.java"
gcc -std=c11 -Wall -Wextra -Werror -fPIC -shared -I"$jdk/include" -I"$jdk/include/linux" -I"$work/headers" \
  "$module/src/it/native/probe.c" -o "$work/native/libm3packprobe.so"
"$jdk/bin/jmod" create --class-path "$work/native-classes" --libs "$work/native" --legal-notices "$work/legal" \
  --module-version 1.0 --main-class com.m3.fixture.NativeProbe --date=2026-10-03T00:00:00Z "$work/native-fixture.jmod"
printf 'path\tsha256\tmodule\nnative-fixture.jmod\t%s\tcom.synexia.pack.fixture\n' "$(sha256sum "$work/native-fixture.jmod" | cut -d' ' -f1)" > "$work/LOCK.tsv"
bash "$module/assemble.sh" "$work/LOCK.tsv" com.synexia.pack.fixture "$work/native-image"
"$work/native-image/image/bin/java" --enable-native-access=com.synexia.pack.fixture -m com.synexia.pack.fixture/com.m3.fixture.NativeProbe
printf 'gate\tstatus\ndiagnostics_jfr\tPASS\ndiagnostics_jcmd\tPASS\nc11_warnings_as_errors\tPASS\njni_jmod_execution\tPASS\n' > "$work/IMAGE_PROOF.tsv"
printf 'PASS: image proof %s\n' "$work"
