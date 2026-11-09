#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# run_write_gate.sh <v1 tree> <v2 tree> <out> [count] : JACKSON_NUMBERS_TO_JDK v2 gate.
#  1. javac --release 8: jackson-core 2.19.2 before v2 ("v1") and after; 2.15.3, 2.16.1 and the relocated module after.
#  2. SchubfachVsJdk on the v1 classes: Jackson's Schubfach copy vs Double/Float.toString, 3 seeds.
#  3. JacksonNumberWriteDifferential, 3 seeds, v1 vs v2: the outputs must be identical.
set -u
A=$1; B=$2; O=$3; N=${4:-1000000}; H=$(dirname "$0"); rm -rf "$O"; mkdir -p "$O"
V=m3/vendor/third_party
compile() { local out=$1; shift; mkdir -p "$O/$out"
  javac -nowarn -proc:none "$@" -d "$O/$out" > "$O/javac_$out.log" 2>&1
  echo "javac $out rc=$? classes=$(find "$O/$out" -name '*.class' | wc -l) errors=$(grep -c 'error:' "$O/javac_$out.log")"; }
compile v1 --release 8 $(find "$A/$V/jackson-core-2.19.2/src/main/java" -name '*.java')
for v in 2.15.3 2.16.1 2.19.2; do compile "v2-$v" --release 8 $(find "$B/$V/jackson-core-$v/src/main/java" -name '*.java'); done
compile v2-relocated --release 21 $(find "$B/m3/vendor/third_party-m3/relocated/jackson-core-2.19.2/src/main/java" -name '*.java')
for side in v1 v2-2.19.2; do compile "harness-$side" -cp "$O/$side" "$H/JacksonNumberWriteDifferential.java"; done
compile harness-schubfach -cp "$O/v1" "$H/SchubfachVsJdk.java"
for seed in 1 2 3; do
  java -cp "$O/harness-schubfach:$O/v1" SchubfachVsJdk 10000000 $seed | tail -3 | sed "s/^/seed $seed schubfach-vs-jdk /"
  for side in v1 v2-2.19.2; do
    java -cp "$O/harness-$side:$O/$side" JacksonNumberWriteDifferential $seed $N > "$O/write_${side}_$seed.txt"
  done
  if cmp -s "$O/write_v1_$seed.txt" "$O/write_v2-2.19.2_$seed.txt"; then
    echo "seed $seed: $N samples IDENTICAL (sha256 $(sha256sum < "$O/write_v1_$seed.txt" | cut -c1-16))"
  else
    echo "seed $seed: DIFFERENT $(diff "$O/write_v1_$seed.txt" "$O/write_v2-2.19.2_$seed.txt" | grep -c '^<') lines"
  fi
done
