#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set M3_TEST_JDK to the stage-3 patched image}"
result=${M3_RESULTS:-$root/m3/build/runtime-stage3}
mkdir -p "$result/classes"

python3 "$root/m3/runtime-stage3/build-lexicon.py" "$result/test.m3lex"
"$M3_TEST_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes"   "$root/m3/runtime-stage3/tests/MIndexStringInvariant.java"

common=(-ea -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage   --add-opens java.base/java.lang=ALL-UNNAMED)

"$M3_TEST_JDK/bin/java" "${common[@]}"   -Djdk.mindex.lexicon="$result/test.m3lex" -Dm3.expect.lexicon=true   -cp "$result/classes" MIndexStringInvariant | tee "$result/mapped.log"

"$M3_TEST_JDK/bin/java" "${common[@]}"   -Djdk.mindex.lexicon="$result/missing.m3lex" -Dm3.expect.lexicon=false   -cp "$result/classes" MIndexStringInvariant | tee "$result/local-fallback.log"
