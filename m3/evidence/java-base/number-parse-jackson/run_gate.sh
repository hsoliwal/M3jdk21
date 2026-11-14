#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# run_gate.sh <master tree> <branch tree> <M3 java.base classes dir> <out> [count] : JACKSON_NUMBERS_TO_JDK v1 gate.
#  1. javac --release 8: master's jackson-core 2.19.2 + FastDoubleParser 1.0.90 ("old"); the overlaid jackson-core
#     2.15.3, 2.16.1, 2.19.2 alone; the relocated jackson-core module (module-info, no jdk.m3.fastdoubleparser).
#  2. java.base patch: FloatingDecimal, java.math and every jdk.internal.math.M3* class of the M3 tree (-implicit:none).
#  3. JacksonNumbersDifferential for 3 seeds: old and new, on the stock JDK and with the patch. Values must be
#     identical in all four runs; exception messages are compared and reported separately.
set -u
A=$1; B=$2; J=$3; O=$4; N=${5:-20000}; H=$(dirname "$0"); rm -rf "$O"; mkdir -p "$O"
V=m3/vendor/third_party
compile() { local out=$1; shift; mkdir -p "$O/$out"
  javac -nowarn -proc:none "$@" -d "$O/$out" > "$O/javac_$out.log" 2>&1
  echo "javac $out rc=$? classes=$(find "$O/$out" -name '*.class' | wc -l) errors=$(grep -c 'error:' "$O/javac_$out.log")"; }
compile old --release 8 $(find "$A/$V/jackson-core-2.19.2/src/main/java" "$A/$V/fastdoubleparser-1.0.90/src/main/java" -name '*.java')
for v in 2.15.3 2.16.1 2.19.2; do compile "new-$v" --release 8 $(find "$B/$V/jackson-core-$v/src/main/java" -name '*.java'); done
compile relocated --release 21 $(find "$B/m3/vendor/third_party-m3/relocated/jackson-core-2.19.2/src/main/java" -name '*.java')
compile patch --patch-module java.base="$J" -implicit:none "$J/jdk/internal/math/FloatingDecimal.java" \
  "$J/java/math/BigInteger.java" "$J/java/math/BigDecimal.java" $(ls "$J"/jdk/internal/math/M3*.java)
for side in old new-2.19.2; do compile "harness-$side" -cp "$O/$side" "$H/JacksonNumbersDifferential.java"; done
for seed in 1 2 3; do
  for side in old new-2.19.2; do
    java -cp "$O/harness-$side:$O/$side" JacksonNumbersDifferential $seed $N "$O/msg_stock_${side}_$seed.txt" > "$O/stock_${side}_$seed.txt"
    java --patch-module java.base="$O/patch" -Xlog:class+load=info:file="$O/load_${side}_$seed.log" \
      -cp "$O/harness-$side:$O/$side" JacksonNumbersDifferential $seed $N "$O/msg_m3_${side}_$seed.txt" > "$O/m3_${side}_$seed.txt"
    echo "seed $seed $side: patched classes loaded $(grep -cE "(jdk.internal.math.FloatingDecimal|java.math.BigInteger|jdk.internal.math.M3BigNumbers) source: $O/patch" "$O/load_${side}_$seed.log") of 3"
  done
  ref="$O/stock_old_$seed.txt"
  for run in stock_new-2.19.2 m3_old m3_new-2.19.2; do
    if cmp -s "$ref" "$O/${run}_$seed.txt"; then echo "seed $seed $run: $N numbers values IDENTICAL to stock_old (sha256 $(sha256sum < "$ref" | cut -c1-16))"
    else echo "seed $seed $run: values DIFFERENT $(diff "$ref" "$O/${run}_$seed.txt" | grep -c '^<') lines"; diff "$ref" "$O/${run}_$seed.txt" | head -4 | cut -c1-300; fi
  done
  for env in stock m3; do
    echo "seed $seed $env messages: old $(wc -l < "$O/msg_${env}_old_$seed.txt") new $(wc -l < "$O/msg_${env}_new-2.19.2_$seed.txt") differing $(diff "$O/msg_${env}_old_$seed.txt" "$O/msg_${env}_new-2.19.2_$seed.txt" | grep -c '^<')"
  done
done
cat "$O"/msg_stock_old_*.txt | sort | uniq -c | sort -rn | head -12 > "$O/messages_old_top.txt"
cat "$O"/msg_stock_new-2.19.2_*.txt | sort | uniq -c | sort -rn | head -12 > "$O/messages_new_top.txt"
diff <(cat "$O"/msg_stock_old_*.txt) <(cat "$O"/msg_stock_new-2.19.2_*.txt) | grep '^[<>]' | sort | uniq -c | sort -rn | head -20 > "$O/messages_diff_top.txt"
