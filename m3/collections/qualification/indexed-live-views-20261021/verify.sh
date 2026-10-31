#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
Q="$ROOT/m3/collections/qualification/indexed-live-views-20261021"
MAIN="$ROOT/m3/collections/src/main/java"
TEST="$ROOT/m3/collections/src/test/java"
OUTPUT="$(mktemp -d)"
trap 'rm -rf "$OUTPUT"' EXIT
python3 "$Q/verify_recipe.py"
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  -sourcepath "$MAIN:$TEST" -d "$OUTPUT" \
  "$MAIN/com/m3/collections/M3IndexedListSnapshot.java" \
  "$TEST/com/m3/collections/M3IndexedLiveViewContract.java"
java -ea -cp "$OUTPUT" com.m3.collections.M3IndexedLiveViewContract
java -ea -Xint -cp "$OUTPUT" com.m3.collections.M3IndexedLiveViewContract
echo "M3_INDEXED_LIVE_JAVA21_PASS source=actual-receiver"
