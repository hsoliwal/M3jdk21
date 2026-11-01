#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$ROOT"
echo "M3JDK21 integrated java.base/libjava + primitive index + filtered owner verification"

# Verify each independent, canonical-source-sealed product atom against its
# original baseline and run the same Java21 oracle in normal and -Xint modes.
bash m3/collections/qualification/natural-run-20261021/verify.sh
bash m3/collections/qualification/filtered-isempty-20261021/verify.sh

JAVA="src/java.base/share/classes/jdk/internal/misc/M3ObjectMemory.java"
NATIVE="src/java.base/share/native/libjava/M3ObjectMemory.c"
TEST="test/jdk/jdk/internal/misc/M3ObjectMemoryTest.java"
for SOURCE in "$JAVA" "$NATIVE" "$TEST"; do
  test -s "$SOURCE" || { echo "missing first-party M3JDK21 JVM-native source: $SOURCE" >&2; exit 1; }
done
echo "0d3e2d044f8caa26dbcf488992d69e8a29562358330ec4265a65f20fc8dce7b7  $JAVA" | sha256sum -c -
echo "ff317b645e6b497f40b7b517a1f11a16fb10896ab4e61325dfc3601434b63c03  $NATIVE" | sha256sum -c -
echo "a484bff774dc11e266ee97513ad78e0ea9588809553f3ec707d8f28bbaaa31eb  $TEST" | sha256sum -c -

JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classes" "$WORK/src/jdk/internal/misc"
cp "$JAVA" "$WORK/src/jdk/internal/misc/M3ObjectMemory.java"

# Compile byte-identical JDK source against one isolated java.base patch path,
# without implicitly compiling unrelated unqualified JDK sources or loading the
# unbuilt libjava exports.
javac -source 21 -target 21 -proc:none -Xlint:all -Werror \
  --patch-module java.base="$WORK/src" \
  -d "$WORK/classes" "$WORK/src/jdk/internal/misc/M3ObjectMemory.java"
javac -source 21 -target 21 -proc:none -Xlint:all -Werror \
  --patch-module java.base="$WORK/classes" \
  --add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
  -cp "$WORK/classes" -d "$WORK/classes" "$TEST"
cc -std=c11 -Wall -Wextra -Werror -fPIC \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -c "$NATIVE" -o "$WORK/M3ObjectMemory.o"

echo "PASS focused M3JDK21 exact Java21 collection differential and native M3ObjectMemory source compilation"
echo "NOT_RUN: complete M3JDK21 make images and native libjava symbol linking, jtreg/JCK/runtime GC/JIT/Android matrix"
echo "NOT_RUN: canonical Synexia Maven/OpenRewrite JUnit until its independent CI records a pass"
