#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Source-only Java21 plus existing test-only C17 JNI differential gate.
set -euo pipefail
MODULE="$(cd "$(dirname "$0")/.." && pwd)"
MAIN="$MODULE/src/main/java/jdk/m3/regexcursor"
TEST="$MODULE/src/test/java/jdk/m3/regexcursor"
NATIVE="$MODULE/src/test/native/M3StaticGrammarLexiconJni.c"
OUTPUT="${1:-$(mktemp -d -t m3-grammar-token-proof-XXXXXXXX)}"
mkdir -p "$OUTPUT/classes" "$OUTPUT/logs"
# All prior owners are *exact* parent source identities; fail closed before compile.
for spec in \
  "$MAIN/M3StaticGrammarLexicon.java:651090ed378332e7041d880c64a2c1092b23ec16" \
  "$TEST/M3StaticGrammarLexiconNativeProof.java:cf2fcd17843ce43fe6bcae98f200cf0880a250a4" \
  "$TEST/M3StaticGrammarLexiconProof.java:7477997c39deeedd3c188cd2d1a87c2cadd0f59c" \
  "$NATIVE:52c0f1ba57bfba5df678a8db32363a0fa929bdf4"; do
  path="${spec%:*}"; expected="${spec##*:}"
  current="$(git hash-object "$path")"
  if [[ "$current" != "$expected" ]]; then
    echo "REFUSED: existing source owner drift: $path" >&2
    exit 2
  fi
done
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
cat > "$OUTPUT/logs/00-context.log" <<CONTEXT
parent_static_git_blob=651090ed378332e7041d880c64a2c1092b23ec16
parent_native_proof_git_blob=cf2fcd17843ce43fe6bcae98f200cf0880a250a4
parent_java_proof_git_blob=7477997c39deeedd3c188cd2d1a87c2cadd0f59c
parent_c17_oracle_git_blob=52c0f1ba57bfba5df678a8db32363a0fa929bdf4
CONTEXT
# Compilation includes the source-locked parent's Java proof helper, not a shadow class.
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$OUTPUT/classes" \
  "$MAIN/M3StaticGrammarLexicon.java" "$MAIN/M3GrammarTokenProjection.java" \
  "$TEST/M3StaticGrammarLexiconProof.java" "$TEST/M3StaticGrammarLexiconNativeProof.java" \
  "$TEST/M3GrammarTokenProjectionProof.java" "$TEST/M3GrammarTokenProjectionNativeProof.java" \
  > "$OUTPUT/logs/01-java-compile.log" 2>&1
java -ea -cp "$OUTPUT/classes" jdk.m3.regexcursor.M3GrammarTokenProjectionProof \
  | tee "$OUTPUT/logs/02-java-oracle.log"
case "$(uname -s)" in
  Linux) osinc=linux; extension=so ;;
  *) echo 'REFUSED: native JNI platform not explicitly supported by this script' >&2; exit 2 ;;
esac
gcc -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
  -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/$osinc" "$NATIVE" \
  -o "$OUTPUT/libm3_static_grammar_proof.$extension" \
  > "$OUTPUT/logs/03-native-compile.log" 2>&1
java -ea -Xcheck:jni -cp "$OUTPUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUTPUT/libm3_static_grammar_proof.$extension" \
  | tee "$OUTPUT/logs/04-jni-checked.log"
java -ea -Xint -Xcheck:jni -cp "$OUTPUT/classes" \
  jdk.m3.regexcursor.M3GrammarTokenProjectionNativeProof "$OUTPUT/libm3_static_grammar_proof.$extension" \
  | tee "$OUTPUT/logs/05-jni-interpreted.log"
sha256sum "$MAIN/M3GrammarTokenProjection.java" "$TEST/M3GrammarTokenProjection"* \
  "$MODULE/M3_GRAMMAR_TOKEN_PROJECTION.md" > "$OUTPUT/logs/06-source-hashes.txt"
printf 'M3_TOKEN_PROJECTION_ISOLATED_GATES_PASS output=%s\n' "$OUTPUT"
