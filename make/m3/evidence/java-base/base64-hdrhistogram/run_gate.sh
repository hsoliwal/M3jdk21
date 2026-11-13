#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# run_gate.sh <master tree> <branch tree> <out> [count] : BASE64_TO_JDK v1 gate (JDK 21 on PATH).
#  1. javac -Xlint:all: HdrHistogram (--release 8, as its pom) and the relocated module jdk.m3.hdrhistogram
#     (--release 21), each on master and on the branch.
#  2. Base64HelperDifferential, 3 seeds, on both builds: master vs branch line by line.
#  3. Base64HelperSpeed on HdrHistogram, master and branch.
set -u
A=$1; B=$2; O=$3; N=${4:-100000}; H=$(cd "$(dirname "$0")" && pwd); rm -rf "$O"; mkdir -p "$O/rel"
D=m3/vendor/third_party/HdrHistogram; R=m3/vendor/third_party-m3/relocated/HdrHistogram
java -version 2>&1 | head -1
compile() { local out=$1; shift; mkdir -p "$O/$out"
  javac -proc:none -Xlint:all,-options "$@" -d "$O/$out" > "$O/javac_$out.log" 2>&1
  echo "javac $out rc=$? classes=$(find "$O/$out" -name '*.class' | wc -l)" \
       "errors=$(grep -c ': error: ' "$O/javac_$out.log") warnings=$(grep -c ': warning: ' "$O/javac_$out.log")"; }
for f in Base64HelperDifferential Base64HelperSpeed; do
  sed 's/org\.HdrHistogram/jdk.m3.lib.hdrhistogram/g' "$H/$f.java" > "$O/rel/$f.java"
done
for side in master branch; do
  T=$A; [ $side = branch ] && T=$B
  compile "$side-donor" --release 8 $(find "$T/$D/src/main/java" -name '*.java')
  compile "harness-$side-donor" -cp "$O/$side-donor" "$H/Base64HelperDifferential.java" "$H/Base64HelperSpeed.java"
  compile "$side-relocated" --release 21 $(find "$T/$R/src/main/java" -name '*.java')
  compile "harness-$side-relocated" -cp "$O/$side-relocated" "$O/rel/Base64HelperDifferential.java"
done
for build in donor relocated; do
  pkg=org.HdrHistogram; [ $build = relocated ] && pkg=jdk.m3.lib.hdrhistogram
  for seed in 1 2 3; do
    for side in master branch; do
      java -cp "$O/harness-$side-$build:$O/$side-$build" $pkg.Base64HelperDifferential $seed $N \
        > "$O/diff_${side}_${build}_$seed.txt" 2>&1
    done
    m="$O/diff_master_${build}_$seed.txt"; b="$O/diff_branch_${build}_$seed.txt"
    echo "$build seed $seed: $N samples; lines $(wc -l < "$m")/$(wc -l < "$b"); differing $(diff "$m" "$b" | grep -c '^<');" \
         "round=false $(grep -c 'round=false' "$b"); log lines with a false $(grep 'log=' "$b" | grep -c ':false')"
    paste -d'\t' "$m" "$b" | awk -F'\t' '$1 != $2' > "$O/pairs_${build}_$seed.tsv"
  done
done
echo "# master -> branch outcome of the differing parse calls (count master branch):"
cat "$O"/pairs_*_*.tsv | sed -E 's/.*corrupt=([A-Za-z]+)[^\t]*\t.*corrupt=([A-Za-z]+).*/\1 \2/; s/^null=([A-Za-z]+)\tnull=([A-Za-z]+)$/\1 \2 (null input)/' \
  | sort | uniq -c
echo "# differing lines whose round trip or histogram log differs: $(cat "$O"/pairs_*_*.tsv | awk -F'\t' '$1 !~ /^null=/ {a = $1; b = $2; gsub(/ corrupt=[^ ]*/, "", a); gsub(/ corrupt=[^ ]*/, "", b); if (a != b) n++} END {print n+0}')"
for side in master branch; do
  java -cp "$O/harness-$side-donor:$O/$side-donor" org.HdrHistogram.Base64HelperSpeed | sed "s/^/$side /"
done
