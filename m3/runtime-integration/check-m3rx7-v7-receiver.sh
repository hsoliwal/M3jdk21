#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname "$0")/../.." && pwd)
PROD="$ROOT/src/java.base/share/classes/jdk/internal/mindex/M3RX7.java"
TEST="$ROOT/test/jdk/java/util/regex/M3RX7Test.java"
MAP="$ROOT/m3/docs/name-mapping.d/m3-regex-v7-dfa.json"
printf '%s  %s\n' \
  '709ebea61c346c761662713e1cb7cdaf67f02bdfa49222ec23eef2353b44e0d2' "$PROD" \
  '0c7d8ef062693a987d57c8fc36d7a69f015e08b12662d5a5774164276697d634' "$TEST" \
  '9f240084b8b072ae63b076cd5eb7b6b3d34b5035a573f611c7f23000216b6f9d' "$MAP" | sha256sum -c -
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$TMP/classes" "$TMP/testclasses"
javac -Xlint:all -Werror --patch-module java.base="$ROOT/src/java.base/share/classes" \
  -d "$TMP/classes" "$PROD"
javac -Xlint:all -Werror --patch-module java.base="$TMP/classes" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -d "$TMP/testclasses" "$TEST"
java -Xcheck:jni -Xmx64m --patch-module java.base="$TMP/classes" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$TMP/testclasses" M3RX7Test
java -Xint -Xcheck:jni -Xmx64m --patch-module java.base="$TMP/classes" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$TMP/testclasses" M3RX7Test
printf 'M3RX7_RECEIVER_STANDALONE_PASS; full matched M3JDK21 jtreg/HotSpot/JNI/GC NOT_RUN\n'
