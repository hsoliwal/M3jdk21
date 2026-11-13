#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../../.." && pwd)
jdk=${1:-${JAVA_HOME:-}}
out=${2:-"$repo/m3/build/j485"}

test -n "$jdk"
test -x "$jdk/bin/java"
test -x "$jdk/bin/javac"
grep -Eq '^JAVA_VERSION="21([.+-]|")' "$jdk/release"

case "$out" in "$repo/src/"*|"$repo/test/"*|"$repo/make/"*) echo "J485 output may not enter product trees" >&2; exit 2;; esac
test ! -e "$out"
mkdir -p "$out/patch-src/java/util/stream" "$out/patch" "$out/smoke" "$out/logs"

for name in AbstractPipeline.java ReferencePipeline.java Stream.java package-info.java Gatherer.java GathererOp.java Gatherers.java; do
  cp -- "$repo/src/java.base/share/classes/java/util/stream/$name" "$out/patch-src/java/util/stream/$name"
done

"$jdk/bin/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror   --patch-module java.base="$out/patch-src"   -d "$out/patch"   "$out/patch-src/java/util/stream/"*.java   >"$out/logs/patch-compile.log" 2>&1

"$jdk/bin/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror   --patch-module java.base="$out/patch"   -d "$out/smoke"   "$repo/m3/backports/recipes/j485/J485Smoke.java"   >"$out/logs/smoke-compile.log" 2>&1

"$jdk/bin/java" -Xcheck:jni --patch-module java.base="$out/patch"   -cp "$out/smoke" m3.j485.J485Smoke | tee "$out/logs/smoke-jit.log"
"$jdk/bin/java" -Xint -Xcheck:jni --patch-module java.base="$out/patch"   -cp "$out/smoke" m3.j485.J485Smoke | tee "$out/logs/smoke-int.log"

"$jdk/bin/javap" --module java.base java.util.stream.Stream > "$out/logs/baseline-stream-api.txt"
"$jdk/bin/javap" "$out/patch/java/util/stream/Stream.class" > "$out/logs/j485-stream-api.txt"
grep -F 'gather(java.util.stream.Gatherer' "$out/logs/j485-stream-api.txt" >/dev/null
if grep -F 'gather(java.util.stream.Gatherer' "$out/logs/baseline-stream-api.txt" >/dev/null; then
  echo "baseline Java21 unexpectedly contains Stream.gather" >&2
  exit 3
fi

printf 'gate\tstatus\njava21_patch_compile\tPASS\nsmoke_jit\tPASS\nsmoke_int\tPASS\napi_delta\tPASS\n' > "$out/STATUS.tsv"
printf 'J485_JAVA21_PATCH_PASS\n'
