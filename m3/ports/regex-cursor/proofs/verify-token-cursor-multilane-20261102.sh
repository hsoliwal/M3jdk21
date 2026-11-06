#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# M3 primitive multi-lane receipt. Output only to one target under current crate.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN="$ROOT/src/main/java/jdk/m3/regexcursor"
TEST="$ROOT/src/test/java/jdk/m3/regexcursor"
OUT="$ROOT/target/m3-token-cursor-multilane-20261102"
if [ -e "$OUT" ]; then echo 'REFUSE occupied output' >&2; exit 3; fi
read -r sha _ < <(sha256sum "$MAIN/M3TokenCursor.java")
if [ "$sha" != 'ea73610277b30819cda3bbc5a8a92aff5080bf202eb4d513a6774a791d42b04d' ]; then echo 'REFUSE source drift' >&2; exit 4; fi
mkdir -p "$OUT/classes" "$OUT/logs"
# STRICT: lint/compile before any runtime/proof tests.
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$OUT/classes" \
  "$MAIN"/*.java \
  "$TEST/M3TokenCursorMultiLaneProof.java" \
  "$TEST/M3TokenCursorMultiLaneBench.java" \
  "$TEST/M3TokenCursorSingleLaneProof.java" \
  "$TEST/M3GrammarCompleteCursorProof.java" \
  "$TEST/M3GrammarTokenProjectionProof.java" \
  "$TEST/M3CompleteTokenProjectionProof.java" \
  "$TEST/M3StaticGrammarLexiconProof.java" \
  "$TEST/M3StaticGrammarLexiconNativeProof.java" \
  "$TEST/M3GrammarTokenProjectionNativeProof.java" \
  > "$OUT/logs/01-javac21.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3TokenCursorMultiLaneProof > "$OUT/logs/02-multilane-proof.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3TokenCursorSingleLaneProof > "$OUT/logs/03-single-lane-proof.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3GrammarCompleteCursorProof > "$OUT/logs/04-complete-cursor.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3GrammarTokenProjectionProof > "$OUT/logs/05-projection.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3CompleteTokenProjectionProof > "$OUT/logs/06-complete-admission.log" 2>&1
java -ea -Xmx768m -cp "$OUT/classes" jdk.m3.regexcursor.M3StaticGrammarLexiconProof > "$OUT/logs/07-lexicon.log" 2>&1
# Unchanged native C17 oracle. Compile after Java; runtime only after compile.
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
gcc -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
  -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/src/test/native/M3StaticGrammarLexiconJni.c" \
  -o "$OUT/libm3grammar.so" > "$OUT/logs/08-gcc-c17.log" 2>&1
java -ea -Xcheck:jni -Xmx768m -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUT/libm3grammar.so" > "$OUT/logs/09-jni-checked.log" 2>&1
java -ea -Xint -Xcheck:jni -Xmx768m -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUT/libm3grammar.so" > "$OUT/logs/10-jni-interpreted.log" 2>&1
sha256sum "$OUT"/logs/*.log > "$OUT/logs/SHA256SUMS.txt"
for f in "$OUT"/logs/{02-multilane-proof,03-single-lane-proof,04-complete-cursor,05-projection,06-complete-admission,07-lexicon,09-jni-checked,10-jni-interpreted}.log; do tail -1 "$f"; done
echo 'M3_MULTILANE_ISOLATED_PASS'
