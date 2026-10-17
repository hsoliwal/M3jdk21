#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
CRATE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$CRATE/../../.." && pwd)"
python3 -B "$CRATE/verify_source_seals.py" "$ROOT"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT

STRINGS="$ROOT/synexia-indexstring/src"
COMPILER="$ROOT/synexia-mindex/compiler/src"

# Exact JDK21 standalone compile: this deliberately uses only the true
# JDK regex semantics, not a miniature mock of javac or JNI objects.
javac --release 21 -Xlint:all -Werror -d "$TEMP" \
  "$STRINGS/main/java/com/synexia/indexstring/MIndexJdkConstantRegexPlan.java" \
  "$STRINGS/test/java/com/synexia/indexstring/MIndexJdkConstantRegexPlanContract.java" \
  "$COMPILER/main/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFacts.java" \
  "$COMPILER/test/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFactsContract.java"
java -ea -cp "$TEMP" com.synexia.indexstring.MIndexJdkConstantRegexPlanContract
java -ea -cp "$TEMP" com.synexia.mindex.compiler.MIndexJavaConstantStringFactsContract
printf 'M3_JAVAC_CONSTANT_REGEX_JDK21=PASS\n'
