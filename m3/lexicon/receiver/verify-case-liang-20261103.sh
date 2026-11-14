#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Strict source pin -> Java21 compilation -> C17 compilation -> Java -> JNI checked/interpreted.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN="$ROOT/src/main/java/jdk/m3/lexicon"
TEST="$ROOT/src/test/java/jdk/m3/lexicon"
OUT="$ROOT/target/m3-case-liang-precompute-20261103"
if [[ -e "$OUT" ]]; then echo 'REFUSE occupied build output' >&2; exit 3; fi
pins=(
'68a6e2f20e9d0f348b03cabac6d1684c14c517c7de7b2f3dca446fb55372c8f6  src/main/java/jdk/m3/lexicon/M3CaseBoundaryPrecompute.java'
'914bcc0a257f533dd6d593152e4f2940a10c6ca10f946f23bab8f604cf60a026  src/main/java/jdk/m3/lexicon/M3LiangPatternPrecompute.java'
'5f4bc3061d4e6854b6b0780f1055fb6993f9060f102e5772638ae1e43a6aea5c  src/main/java/jdk/m3/lexicon/M3WindowBoundarySummary.java'
'2e1ae2090077dd7e8867add7b89648ef9e0ffda57081c815e16555d871690ffc  src/test/java/jdk/m3/lexicon/M3CaseLiangParityProof.java'
'5f5b4c9b8df707e0d4e4c9880997dc3912ec6de54a1e0a60bdc329f75b2933e4  src/test/java/jdk/m3/lexicon/M3CaseLiangJniParityProof.java'
'067457f444d8c57ceccb030774d485b892b9fb14c38d0a5a0138e5788ad7717e  src/test/native/M3CaseLiangJniOracle.c'
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
java -ea -Xmx512m -cp "$OUT/classes" jdk.m3.lexicon.M3CaseLiangParityProof \
  > "$OUT/logs/03-java-proof.log" 2>&1
java -ea -Xcheck:jni -Xmx512m -cp "$OUT/classes" jdk.m3.lexicon.M3CaseLiangJniParityProof \
  "$OUT/libm3-case-liang-jni.so" > "$OUT/logs/04-jni-checked.log" 2>&1
java -ea -Xint -Xcheck:jni -Xmx512m -cp "$OUT/classes" jdk.m3.lexicon.M3CaseLiangJniParityProof \
  "$OUT/libm3-case-liang-jni.so" > "$OUT/logs/05-jni-interpreted.log" 2>&1
sha256sum "$OUT"/logs/*.log > "$OUT/logs/SHA256SUMS.txt"
for f in 03-java-proof 04-jni-checked 05-jni-interpreted; do tail -1 "$OUT/logs/$f.log"; done
echo 'M3_CASE_LIANG_ORDERED_PASS'
