#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${M3_JDK:?Set M3_JDK to an isolated JDK 21 directory}"
rm -rf build
mkdir -p build/classes build/tests build/logs
mapfile -t sources < <(find src -name '*.java' | sort)
"$M3_JDK/bin/javac" --release 21 -Xlint:all,-module -Werror -d build/classes "${sources[@]}" \
  > build/logs/compile.log 2>&1
"$M3_JDK/bin/jar" --create --date=2026-10-02T00:00:00Z --file build/com.m3.collections.jar -C build/classes .
"$M3_JDK/bin/javac" --release 21 -Xlint:all -Werror \
  --module-path build/com.m3.collections.jar --add-modules com.m3.collections \
  -d build/tests test/M3CollectionsFoundationTest.java > build/logs/test-compile.log 2>&1
for mode in jit int; do
  flags=()
  if [[ "$mode" == int ]]; then flags=(-Xint); fi
  "$M3_JDK/bin/java" -ea -esa "${flags[@]}" \
    --module-path build/com.m3.collections.jar --add-modules com.m3.collections \
    -cp build/tests M3CollectionsFoundationTest | tee "build/logs/test-$mode.log"
done
sha256sum build/com.m3.collections.jar | tee build/logs/artifacts.sha256
