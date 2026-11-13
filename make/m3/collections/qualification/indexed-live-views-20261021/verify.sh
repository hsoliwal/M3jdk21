#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../../.." && pwd)"
Q="$ROOT/make/m3/collections/qualification/indexed-live-views-20261021"
MAIN="$ROOT/src/jdk.m3.collections/share/classes"
TEST="$ROOT/test/jdk"
OUTPUT="$(mktemp -d)"
trap 'rm -rf "$OUTPUT"' EXIT
python3 "$Q/verify_recipe.py"
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  -sourcepath "$MAIN:$TEST" -d "$OUTPUT" \
  "$MAIN/jdk/m3/collections/M3IndexedListSnapshot.java" \
  "$TEST/jdk/m3/collections/M3IndexedLiveViewContract.java"
java -ea -cp "$OUTPUT" jdk.m3.collections.M3IndexedLiveViewContract
java -ea -Xint -cp "$OUTPUT" jdk.m3.collections.M3IndexedLiveViewContract
echo "M3_INDEXED_LIVE_JAVA21_PASS source=actual-receiver"
