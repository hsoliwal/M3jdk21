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
if [[ ! -x "$jdk/bin/java" || ! -x "$jdk/bin/javac" ]]; then
  echo 'missing installed Java or javac executable' >&2
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
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
test="$repo/test/jdk/jdk/m3/collections/GinqHashJoinTest.java"
if [[ ! -f "$test" ]]; then echo "missing owning jtreg source: $test" >&2; exit 68; fi
mkdir -p "$out"
"$jdk/bin/java" -version 2> "$out/java-version.txt"
"$jdk/bin/javac" -source 21 -target 21 -Xlint:all -Werror --add-modules jdk.m3.collections -d "$out" "$test"
"$jdk/bin/java" --add-modules jdk.m3.collections -cp "$out" GinqHashJoinTest | tee "$out/mixed.txt"
"$jdk/bin/java" -Xint --add-modules jdk.m3.collections -cp "$out" GinqHashJoinTest | tee "$out/interpreted.txt"
echo 'M3_GINQ_INSTALLED_SOURCE_BOUND_TESTS_PASS'
