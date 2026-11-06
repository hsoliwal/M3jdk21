#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Strict source pin -> Java21 compilation -> C17 compilation -> Java -> JNI checked/interpreted.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN="$ROOT/src/main/java/com/m3/lexicon"
TEST="$ROOT/src/test/java/com/m3/lexicon"
OUT="$ROOT/target/m3-case-liang-precompute-20261103"
if [[ -e "$OUT" ]]; then echo 'REFUSE occupied build output' >&2; exit 3; fi
pins=(
'330de9d8f8752974316411bc8c8b75b97a3edb0b20a9fd17b44255410d033073  src/main/java/com/m3/lexicon/M3CaseBoundaryPrecompute.java'
'fa7030872dd2f0614e1a69168b9c38588bebfbf89a34c917b058fb2984b7a72d  src/main/java/com/m3/lexicon/M3LiangPatternPrecompute.java'
'a10b0086cf3e6134296f8a4a53a369252bc8c5c138ba82ac6f3b163652836847  src/main/java/com/m3/lexicon/M3WindowBoundarySummary.java'
'6cf2d81afb0f3b1b399df98c6df2788f2031124ce748c0c1a266d3300ea09f3d  src/test/java/com/m3/lexicon/M3CaseLiangParityProof.java'
'bb82d5a35085ea6c9e1b04b2db573e9376369835ba7a217b61dd5d4fbf636eca  src/test/java/com/m3/lexicon/M3CaseLiangJniParityProof.java'
'e129675e7462a8f02eef929011860a13a6fefd08eb54c0339ef4b1f158813b1e  src/test/native/M3CaseLiangJniOracle.c'
)
for line in "${pins[@]}"; do (cd "$ROOT" && echo "$line" | sha256sum --check --status) || { echo 'REFUSE source hash drift' >&2; exit 4; }; done
mkdir -p "$OUT/classes" "$OUT/logs"
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$OUT/classes" "$MAIN"/*.java \
  "$TEST/M3CaseLiangParityProof.java" "$TEST/M3CaseLiangJniParityProof.java" \
  > "$OUT/logs/01-java21-compile.log" 2>&1
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}" 
gcc -std=c17 -O2 -fPIC -shared -Wall -Wextra -Wpedantic -Wconversion -Wshadow -Werror \
  -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
  "$ROOT/src/test/native/M3CaseLiangJniOracle.c" -o "$OUT/libm3-case-liang-jni.so" \
  > "$OUT/logs/02-c17-jni-compile.log" 2>&1
java -ea -Xmx512m -cp "$OUT/classes" com.m3.lexicon.M3CaseLiangParityProof \
  > "$OUT/logs/03-java-proof.log" 2>&1
java -ea -Xcheck:jni -Xmx512m -cp "$OUT/classes" com.m3.lexicon.M3CaseLiangJniParityProof \
  "$OUT/libm3-case-liang-jni.so" > "$OUT/logs/04-jni-checked.log" 2>&1
java -ea -Xint -Xcheck:jni -Xmx512m -cp "$OUT/classes" com.m3.lexicon.M3CaseLiangJniParityProof \
  "$OUT/libm3-case-liang-jni.so" > "$OUT/logs/05-jni-interpreted.log" 2>&1
sha256sum "$OUT"/logs/*.log > "$OUT/logs/SHA256SUMS.txt"
for f in 03-java-proof 04-jni-checked 05-jni-interpreted; do tail -1 "$OUT/logs/$f.log"; done
echo 'M3_CASE_LIANG_ORDERED_PASS'
