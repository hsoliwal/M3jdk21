#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo 'usage: run-ginq-join-installed.sh /absolute/path/to/source-built-jdk /unused/output/dir' >&2
  exit 64
fi
jdk="$1"
out="$2"
if [[ ! -x "$jdk/bin/java" || ! -x "$jdk/bin/javac" || ! -x "$jdk/bin/javap" ]]; then
  echo 'missing installed Java, javac, or javap executable' >&2
  exit 65
fi
if [[ -e "$out" || -L "$out" ]]; then
  echo 'output directory must not exist (no stale receipts)' >&2
  exit 66
fi
modules="$("$jdk/bin/java" --list-modules)"
if ! grep -Eq '^jdk\.m3\.collections(@|$)' <<< "$modules"; then
  echo 'candidate JVM does not contain installed jdk.m3.collections' >&2
  exit 67
fi
if [[ ! -s "$jdk/lib/modules" || ! -s "$jdk/jmods/jdk.m3.collections.jmod" ]]; then
  echo 'candidate missing packaged M3 runtime module image or collections jmod' >&2
  exit 71
fi
string_api="$("$jdk/bin/javap" --module java.base java.lang.String)"
for required in 'public java.lang.String capitalize();' \
                'public java.lang.String uncapitalize();' \
                'public java.lang.String center(int, java.lang.String);'; do
  if ! grep -Fq "$required" <<< "$string_api"; then
    echo "candidate java.lang.String missing M3 member: $required" >&2
    exit 72
  fi
done
runtime_properties="$("$jdk/bin/java" -XshowSettings:properties -version 2>&1)"
runtime_home="$(sed -n 's/^[[:space:]]*java[.]home = //p' <<< "$runtime_properties" | head -n 1)"
if [[ -z "$runtime_home" || ! -d "$runtime_home" || \
      "$(cd "$runtime_home" && pwd -P)" != "$(cd "$jdk" && pwd -P)" ]]; then
  echo 'candidate java.home differs from the supplied packaged JDK directory' >&2
  exit 73
fi
native="${M3_ARRAYS_NATIVE_PATH:-}"
if [[ -z "$native" || ! -f "$native" || -L "$native" || ! -s "$native" ]]; then
  echo 'source-built M3 arrays JNI path is missing, symlinked or empty' >&2
  exit 74
fi
if [[ "$native" != /* ]]; then
  echo 'M3 arrays JNI path must be absolute' >&2
  exit 75
fi
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test="$repo/test/jdk/jdk/m3/collections/GinqHashJoinTest.java"
if [[ ! -f "$test" ]]; then echo "missing owning jtreg source: $test" >&2; exit 68; fi
if ! command -v sha256sum >/dev/null 2>&1; then
  echo 'sha256sum required for immutable receipt fingerprints' >&2
  exit 69
fi
mkdir -p "$out"
printf '%s\n' "$modules" > "$out/installed-modules.txt"
printf '%s\n' "$string_api" > "$out/m3-string-api.txt"
printf '%s\n' "$runtime_properties" > "$out/runtime-properties.txt"
sha256sum "$test" "$jdk/bin/java" "$jdk/bin/javac" "$jdk/bin/javap" "$jdk/lib/modules" "$jdk/jmods/jdk.m3.collections.jmod" "$native" "${BASH_SOURCE[0]}" > "$out/input-sha256.txt"
"$jdk/bin/java" -version 2> "$out/java-version.txt"
"$jdk/bin/javac" -source 21 -target 21 -Xlint:all -Werror --add-modules jdk.m3.collections -d "$out" "$test" 2>&1 | tee "$out/compile.txt"
"$jdk/bin/java" "-Dm3.arrays.native.path=$native" --add-modules jdk.m3.collections -cp "$out" GinqHashJoinTest | tee "$out/mixed.txt"
"$jdk/bin/java" -Xint "-Dm3.arrays.native.path=$native" --add-modules jdk.m3.collections -cp "$out" GinqHashJoinTest | tee "$out/interpreted.txt"
if ! cmp -s "$out/mixed.txt" "$out/interpreted.txt"; then
  echo 'mixed and interpreted stdout differ' >&2
  exit 70
fi
printf '%s\n' 'M3_GINQ_INSTALLED_SOURCE_BOUND_TESTS_PASS' | tee "$out/status.txt"
