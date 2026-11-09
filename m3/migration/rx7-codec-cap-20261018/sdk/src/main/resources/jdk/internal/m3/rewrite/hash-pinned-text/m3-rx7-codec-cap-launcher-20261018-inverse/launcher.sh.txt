#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname "$0")/../.." && pwd)
PROD="$ROOT/src/java.base/share/classes/jdk/internal/mindex"
TEST="$ROOT/test/jdk/java/util/regex/M3RX7CodecTest.java"
MAP="$ROOT/m3/docs/name-mapping.d/m3-regex-v7-image-codec.json"
printf '%s  %s
'   '709ebea61c346c761662713e1cb7cdaf67f02bdfa49222ec23eef2353b44e0d2' "$PROD/M3RX7.java"   '5c9f80b832a722a9611a718d5c6228aa59721b5fae846267e05ac5c451353ba7' "$PROD/M3RX7Image.java"   'fc6727011d2cc9027d43280ce692c4646c378438c0fb7be9c8f7ae7d0c1102c9' "$PROD/M3RX7Codec.java"   'bd2ed346fb4ba3945b0d7a14c04418f4996b2f11c4945e8fc14bdba24a99bd6a' "$TEST"   '32155be04c0d9635d7be7678609dde08a2b5dd797504a9a93752cf1c5050dfb6' "$MAP" | sha256sum -c -
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$TMP/classes" "$TMP/testclasses"
javac -Xlint:all -Werror   --patch-module java.base="$ROOT/src/java.base/share/classes"   -d "$TMP/classes"   "$PROD/M3RX7.java" "$PROD/M3RX7Image.java" "$PROD/M3RX7Codec.java"
javac -Xlint:all -Werror   --patch-module java.base="$TMP/classes"   --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED   -d "$TMP/testclasses" "$TEST"
java -Xmx96m -Xcheck:jni   --patch-module java.base="$TMP/classes"   --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED   -cp "$TMP/testclasses" M3RX7CodecTest
java -Xmx96m -Xint -Xcheck:jni   --patch-module java.base="$TMP/classes"   --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED   -cp "$TMP/testclasses" M3RX7CodecTest
printf 'M3RX7_IMAGE_CODEC_STANDALONE_PASS; Pattern/Matcher dispatch, NFA executor and native lanes NOT_RUN
'
