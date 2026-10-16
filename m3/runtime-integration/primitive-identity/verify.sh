#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
# Target-side qualification orchestration, not a transformation engine.
set -euo pipefail
[[ $# == 2 ]] || { echo 'usage: verify.sh <pinned Synexia recipe directory> <new evidence directory>' >&2; exit 2; }
TARGET=$(cd "$(dirname "$0")/../../.." && pwd)
RECIPE=$(cd "$1" && pwd)
# Canonical source: hsoliwal/com.synexia@c90964099bdb0a752d29033d3c7723842ad6b2dc.
# Exact executable inputs permit verified source exports as well as Git checkouts.
(cd "$RECIPE"; sha256sum --check <<'SEALED_INPUTS'
2922c5dbb51f078e468c0aa5e267e8d110cfdb0df2f41956b71ab2fc4c78fd4b  src/main/java/com/synexia/m3/recipe/M3PrimitiveIdentityReplay.java
1c51f04fc6bd0c77c53e4b6e0c362fca78a464ebd9202f0acc1a299d0d6658b0  src/test/java/com/synexia/m3/recipe/M3PrimitiveIdentityReplaySelfTest.java
779524510bfaca1f36671bd23ddae28e97b39bd9030569edfcc6c18e3e0ea040  prepare-inputs.py
19f7b949d794d85de9b821f0d8b2bdc20e66d7ce21e271461abf21e46a8975c2  targets.tsv
SEALED_INPUTS
)
[[ $(uname -s) == Linux && $(getconf LONG_BIT) == 64 ]] || { echo 'Linux64 proof only' >&2; exit 1; }
mkdir "$2"
OUT=$(cd "$2" && pwd)
cp -R "$RECIPE" "$OUT/recipe"
python3 "$OUT/recipe/prepare-inputs.py" "$TARGET"
JHOME=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
java -version > "$OUT/java-version.txt" 2>&1
gcc --version > "$OUT/gcc-version.txt"
mkdir -p "$OUT"/{recipeclasses,generated,base,compiler,tests,normal,lowered}
javac -d "$OUT/recipeclasses" "$OUT/recipe/src/main/java/com/synexia/m3/recipe/M3PrimitiveIdentityReplay.java" "$OUT/recipe/src/test/java/com/synexia/m3/recipe/M3PrimitiveIdentityReplaySelfTest.java"
java -cp "$OUT/recipeclasses" com.synexia.m3.recipe.M3PrimitiveIdentityReplaySelfTest "$OUT/recipe" | tee "$OUT/recipe-tests.log"
for pass in apply fixed-point; do
  java -Dm3.recipe.root="$OUT/recipe" -cp "$OUT/recipeclasses" com.synexia.m3.recipe.M3PrimitiveIdentityReplay --apply-all "$OUT/generated" > "$OUT/recipe-$pass.log"
done
! grep -q 'changed=true' "$OUT/recipe-fixed-point.log"
SRC="$OUT/generated"
javac --patch-module java.base="$SRC/src/java.base/share/classes" -d "$OUT/base" "$SRC"/src/java.base/share/classes/jdk/internal/mindex/*.java
javac --patch-module jdk.compiler="$SRC/src/jdk.compiler/share/classes" -d "$OUT/compiler" "$SRC/src/jdk.compiler/share/classes/com/sun/tools/javac/comp/M3PrimitiveIdentityLowering.java"
javac --patch-module java.base="$OUT/base" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$OUT/tests" "$SRC"/test/jdk/jdk/internal/mindex/PrimitiveIdentity*Test.java
gcc -DM3_PATCHED_TEST -std=c11 -O2 -Wall -Wextra -Werror -fPIC -shared -I"$JHOME/include" -I"$JHOME/include/linux" "$SRC/src/java.base/linux/native/libjava/M3PrimitiveImage.c" "$SRC/test/jdk/jdk/internal/mindex/libPrimitiveIdentityTest.c" -o "$OUT/libPrimitiveIdentityTest.so"
for mode in default interpreter c1 c2; do
  flags=()
  case "$mode" in interpreter) flags=(-Xint);; c1) flags=(-XX:TieredStopAtLevel=1);; c2) flags=(-XX:-TieredCompilation -Xbatch);; esac
  java "${flags[@]}" --patch-module java.base="$OUT/base" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -Dm3.test.patch="$OUT/base" -cp "$OUT/tests" PrimitiveIdentityTest "$OUT/libPrimitiveIdentityTest.so" > "$OUT/contracts-$mode.log"
  grep 'assertions=311476 JNI=true PASS' "$OUT/contracts-$mode.log"
done
java --patch-module java.base="$OUT/base" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -Dm3.test.patch="$OUT/base" -cp "$OUT/tests" PrimitiveIdentityFixedTest "$OUT/libPrimitiveIdentityTest.so" > "$OUT/fixed-address.log" 2>&1
grep 'independentJVMs=3 PASS' "$OUT/fixed-address.log"
FIXTURE="$SRC/test/langtools/tools/javac/m3/PrimitiveIdentityFixture.java"
javac --patch-module java.base="$OUT/base" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$OUT/normal" "$FIXTURE"
java --patch-module jdk.compiler="$OUT/compiler" -m jdk.compiler/com.sun.tools.javac.comp.M3PrimitiveIdentityLowering --patch-module java.base="$OUT/base" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$OUT/lowered" "$FIXTURE" > "$OUT/compiler-lowering.log" 2>&1
grep '^m3.identity.lowered=17$' "$OUT/compiler-lowering.log"
for flavor in normal lowered; do
  java --patch-module java.base="$OUT/base" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$OUT/$flavor" PrimitiveIdentityFixture | tee "$OUT/compiler-$flavor.log"
  javap -c -p -classpath "$OUT/$flavor" PrimitiveIdentityFixture > "$OUT/compiler-$flavor-bytecode.txt"
done
printf 'Focused patched-Java21/JNI proof passed. Not a matched-image or performance qualification.\nEvidence: %s\n' "$OUT"
