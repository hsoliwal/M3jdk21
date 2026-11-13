#!/bin/bash
# run_gate.sh <java.base classes dir> <out> [count] : compile the wired java.math and every jdk.internal.math.M3* class
# as a java.base patch (stock JDK 21, -implicit:none), then run BigParseDifferential on the stock JDK and on the
# patched one for 3 seeds; the outputs must be identical.
set -u
B=$1; O=$2; N=${3:-200000}; rm -rf "$O"; mkdir -p "$O/patch" "$O/harness"
javac --patch-module java.base="$B" -implicit:none -Xlint:all,-serial,-deprecation -d "$O/patch" \
  "$B/java/math/BigInteger.java" "$B/java/math/BigDecimal.java" $(ls "$B"/jdk/internal/math/M3*.java) > "$O/javac_patch.log" 2>&1
echo "javac patch rc=$? classes=$(find "$O/patch" -name '*.class' | wc -l) warnings=$(grep -c 'warning:' "$O/javac_patch.log")"
javac -d "$O/harness" "$(dirname "$0")/BigParseDifferential.java" > "$O/javac_harness.log" 2>&1 || { cat "$O/javac_harness.log"; exit 1; }
for seed in 1 2 3; do
  java -cp "$O/harness" BigParseDifferential $seed $N > "$O/stock_$seed.txt"
  java --patch-module java.base="$O/patch" -Xlog:class+load=info:file="$O/load_$seed.log" -cp "$O/harness" BigParseDifferential $seed $N > "$O/m3_$seed.txt"
  echo "seed $seed patched classes: $(grep -cE "java.math.BigInteger source: $O/patch|jdk.internal.math.M3BigNumbers source: $O/patch" "$O/load_$seed.log") of 2 loaded from the patch"
  if cmp -s "$O/stock_$seed.txt" "$O/m3_$seed.txt"; then
    echo "seed $seed: $N literals IDENTICAL (sha256 $(sha256sum < "$O/m3_$seed.txt" | cut -c1-16))"
  else
    echo "seed $seed: DIFFERENT $(diff "$O/stock_$seed.txt" "$O/m3_$seed.txt" | grep -c '^<') lines"; diff "$O/stock_$seed.txt" "$O/m3_$seed.txt" | head -6 | cut -c1-240
  fi
done
