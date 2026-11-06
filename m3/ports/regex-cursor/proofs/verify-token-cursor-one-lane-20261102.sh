#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Verify one original M3TokenCursor owner. Isolated Java/JNI only, not Maven/jtreg.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN="$ROOT/src/main/java/jdk/m3/regexcursor"
TEST="$ROOT/src/test/java/jdk/m3/regexcursor"
OUT="$ROOT/target/m3-token-cursor-one-lane-20261102"
if [ -e "$OUT" ]; then echo 'REFUSE occupied proof output' >&2; exit 3; fi
seal() {
  local path="$1" expected="$2" actual
  actual="$(sha256sum "$path" | awk '{print $1}')"
  if [ "$actual" != "$expected" ]; then
    echo "REFUSE changed source: $path" >&2
    exit 4
  fi
}
seal "$MAIN/M3TokenCursor.java" '3f5b0b31d0150ca0ab750596dbbc3bec12061d5c1cecbc821c8967c329364077'
seal "$TEST/M3TokenCursorSingleLaneProof.java" '6be523bd137bdfa50538d55e7b88ef085081160279be69cdbba65d7613e92829'
mkdir -p "$OUT/classes" "$OUT/logs"
# 1. Lint, then compile before tests. JUnit suite is separate, not part of this execution.
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$OUT/classes" \
  "$MAIN/M3ChunkCursor.java" "$MAIN/M3CursorFacts.java" \
  "$MAIN/M3TokenCursor.java" "$MAIN/M3StaticGrammarLexicon.java" \
  "$MAIN/M3GrammarTokenProjection.java" \
  "$TEST/M3TokenCursorSingleLaneProof.java" \
  "$TEST/M3GrammarCompleteCursorProof.java" \
  "$TEST/M3GrammarTokenProjectionProof.java" \
  "$TEST/M3CompleteTokenProjectionProof.java" \
  "$TEST/M3StaticGrammarLexiconProof.java" \
  "$TEST/M3StaticGrammarLexiconNativeProof.java" \
  "$TEST/M3GrammarTokenProjectionNativeProof.java" \
  > "$OUT/logs/01-javac21.log" 2>&1
# 2. Deterministic comparisons and differential full receiver regressions.
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3TokenCursorSingleLaneProof \
  > "$OUT/logs/02-single-lane-proof.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3GrammarCompleteCursorProof \
  > "$OUT/logs/03-complete-cursor.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3GrammarTokenProjectionProof \
  > "$OUT/logs/04-projection.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3CompleteTokenProjectionProof \
  > "$OUT/logs/05-complete-admission.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3StaticGrammarLexiconProof \
  > "$OUT/logs/06-lexicon.log" 2>&1
# 3. Compile actual unchanged C17 JNI oracle only after Java gates pass.
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
gcc -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
  -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/src/test/native/M3StaticGrammarLexiconJni.c" \
  -o "$OUT/libm3grammar.so" > "$OUT/logs/07-gcc-c17.log" 2>&1
java -ea -Xcheck:jni -Xmx768m -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUT/libm3grammar.so" \
  > "$OUT/logs/08-jni-checked.log" 2>&1
java -ea -Xint -Xcheck:jni -Xmx768m -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUT/libm3grammar.so" \
  > "$OUT/logs/09-jni-interpreted.log" 2>&1
sha256sum "$OUT"/logs/*.log > "$OUT/logs/SHA256SUMS.txt"
for f in "$OUT"/logs/{02-single-lane-proof,03-complete-cursor,04-projection,05-complete-admission,06-lexicon,08-jni-checked,09-jni-interpreted}.log; do tail -1 "$f"; done
echo 'M3_TOKEN_CURSOR_ONE_LANE_ISOLATED_PASS'
