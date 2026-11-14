#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Read-only source, compiled isolated proof. Output only under module/target.
set -euo pipefail
MODULE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$MODULE/target/m3-ascii-root-qualification"
if [ -e "$OUT" ]; then
  echo "REFUSE existing output: $OUT" >&2
  exit 3
fi
mkdir -p "$OUT/classes" "$OUT/logs"
FILE="$MODULE/src/main/java/jdk/m3/regexcursor/M3StaticGrammarLexicon.java"
SOURCE_DIR="$MODULE/src/test/java/jdk/m3/regexcursor"
if [ ! -f "$FILE" ]; then echo "missing lexicon source" >&2; exit 2; fi
# Reject unexpected source owner before compiling this exact source-sealed candidate.
expected='23c9aedeed6fc3383007cdde6fd15bb1d2a3016800021f67f361cefffee19160'
actual="$(sha256sum "$FILE" | awk '{print $1}')"
if [ "$actual" != "$expected" ]; then
  echo "source drift: $FILE SHA256=$actual" >&2
  exit 4
fi
printf 'SOURCE_SHA256_PASS %s\n' "$actual" > "$OUT/logs/01-source-seal.log"
javac --release 21 -Xlint:all -Werror -d "$OUT/classes" \
  "$FILE" "$SOURCE_DIR/M3AsciiRootAllocationProof.java" \
  "$SOURCE_DIR/M3AsciiRootMicrobench.java" \
  > "$OUT/logs/02-java21-lint-compile.log" 2>&1
printf 'STRICT_JAVA21_COMPILE_PASS\n' > "$OUT/logs/03-compile-pass.log"
java -ea -Xms256m -Xmx256m -XX:+UseSerialGC -cp "$OUT/classes" \
  jdk.m3.regexcursor.M3AsciiRootAllocationProof \
  > "$OUT/logs/04-allocation-and-concurrency.log"
# Representative deterministic cases; full before/after data remains in Synexia evidence.
for roots in 7 8 26; do
  for mode in hit miss; do
    for search in exact longest; do
      java -ea -Xms256m -Xmx256m -XX:+UseSerialGC -cp "$OUT/classes" \
        jdk.m3.regexcursor.M3AsciiRootMicrobench "$roots" "$mode" "$search" \
        >> "$OUT/logs/05-runtime-samples.log"
    done
  done
done
printf 'M3_ASCII_ROOT_QUALIFICATION_PASS source_sealed=true\n'
