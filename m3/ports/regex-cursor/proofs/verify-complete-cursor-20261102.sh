#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Isolated owner proof. No repository writes outside target/, no Maven/PR mutation.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN="$ROOT/src/main/java/jdk/m3/regexcursor"
TEST="$ROOT/src/test/java/jdk/m3/regexcursor"
OUT="$ROOT/target/m3-complete-cursor-20261102"
if [ -e "$OUT" ]; then echo 'REFUSE occupied proof output' >&2; exit 3; fi
sha_lock() {
  local path="$1" expected="$2" actual
  actual="$(sha256sum "$path" | awk '{print $1}')"
  if [ "$actual" != "$expected" ]; then
    echo "REFUSE changed source: $path" >&2
    exit 4
  fi
}
sha_lock "$MAIN/M3GrammarTokenProjection.java" 'ce512c462e9c27d33e2619f2b627d303d09b73aa7b69e31e3198246db96f72a2'
sha_lock "$TEST/M3GrammarCompleteCursorProof.java" '348cbd429b63411cfddd7315f5435df4d8e02db3c9611c8d18e71ac6553cfa14'
mkdir -p "$OUT/classes" "$OUT/logs"
# Stage 1 — strict JDK21 lint + compilation. No test may execute before this gate.
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$OUT/classes" \
  "$MAIN"/*.java \
  "$TEST/M3GrammarCompleteCursorProof.java" \
  "$TEST/M3GrammarTokenProjectionProof.java" \
  "$TEST/M3GrammarTokenProjectionNativeProof.java" \
  "$TEST/M3StaticGrammarLexiconNativeProof.java" \
  "$TEST/M3StaticGrammarLexiconProof.java" \
  > "$OUT/logs/01-java21-lint-compile.log" 2>&1
# Stage 2 — standalone Java oracle then former owner regression.
java -ea -Xmx512m -cp "$OUT/classes" jdk.m3.regexcursor.M3GrammarCompleteCursorProof \
  > "$OUT/logs/02-owned-cursor-proof.log" 2>&1
java -ea -Xmx512m -cp "$OUT/classes" jdk.m3.regexcursor.M3GrammarTokenProjectionProof \
  > "$OUT/logs/03-legacy-projection.log" 2>&1
# Stage 3 — C17/JNI unchanged original native oracle.
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
gcc -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
  -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/src/test/native/M3StaticGrammarLexiconJni.c" \
  -o "$OUT/libm3grammar.so" > "$OUT/logs/04-c17-compile.log" 2>&1
# Stage 4 — only after JNI compilation, checked and interpreted native runtime.
java -ea -Xcheck:jni -Xmx512m -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUT/libm3grammar.so" \
  > "$OUT/logs/05-jni-checked.log" 2>&1
java -ea -Xint -Xcheck:jni -Xmx512m -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUT/libm3grammar.so" \
  > "$OUT/logs/06-jni-interpreted.log" 2>&1
for f in "$OUT"/logs/{02-owned-cursor-proof,03-legacy-projection,05-jni-checked,06-jni-interpreted}.log; do tail -1 "$f"; done
sha256sum "$OUT"/logs/*.log > "$OUT/logs/SHA256SUMS.txt"
echo 'M3_COMPLETE_CURSOR_ISOLATED_PASS'
