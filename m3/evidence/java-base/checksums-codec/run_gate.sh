#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# run_gate.sh <master tree> <branch tree> <out> [count] : CHECKSUMS_TO_JDK v1 gate (JDK 21 on PATH).
#  1. javac -Xlint:all: commons-codec 1.17.1 and 1.22.1 (--release 8, as their poms) and the relocated module
#     jdk.m3.codec (--release 21), each on master and on the branch.
#  2. CrcDifferential, 3 seeds: master vs branch must print identical lines for valid calls.
#  3. CrcSpeed: commons-codec 1.22.1 on master and on the branch.
set -u
A=$1; B=$2; O=$3; N=${4:-200000}; H=$(cd "$(dirname "$0")" && pwd); rm -rf "$O"; mkdir -p "$O"
V=m3/vendor/third_party; R=m3/vendor/third_party-m3/relocated/commons-codec-1.22.1
java -version 2>&1 | head -1
compile() { local out=$1; shift; mkdir -p "$O/$out"
  javac -proc:none -Xlint:all,-options "$@" -d "$O/$out" > "$O/javac_$out.log" 2>&1
  echo "javac $out rc=$? classes=$(find "$O/$out" -name '*.class' | wc -l)" \
       "errors=$(grep -c ': error: ' "$O/javac_$out.log") warnings=$(grep -c ': warning: ' "$O/javac_$out.log")"; }
mkdir -p "$O/rel"; sed 's/org\.apache\.commons\.codec/jdk.m3.lib.codec/g' "$H/CrcDifferential.java" > "$O/rel/CrcDifferential.java"
for side in master branch; do
  T=$A; [ $side = branch ] && T=$B
  for v in 1.17.1 1.22.1; do
    compile "$side-$v" --release 8 $(find "$T/$V/commons-codec-$v/src/main/java" -name '*.java')
    compile "harness-$side-$v" -cp "$O/$side-$v" "$H/CrcDifferential.java" "$H/CrcSpeed.java"
  done
  compile "$side-relocated" --release 21 $(find "$T/$R/src/main/java" -name '*.java')
  compile "harness-$side-relocated" -cp "$O/$side-relocated" "$O/rel/CrcDifferential.java"
done
for v in 1.17.1 1.22.1 relocated; do
  for seed in 1 2 3; do
    for side in master branch; do
      java -cp "$O/harness-$side-$v:$O/$side-$v" CrcDifferential $seed $N > "$O/diff_${side}_${v}_$seed.txt"
    done
    m="$O/diff_master_${v}_$seed.txt"; b="$O/diff_branch_${v}_$seed.txt"
    bad=$(grep -c 'bad(' "$b"); same=$(diff "$m" "$b" | grep -c '^<')
    echo "$v seed $seed: $N samples, $bad with one invalid call; lines differing master/branch=$same;" \
         "master jdk=DIFFERENT $(grep -c 'jdk=DIFFERENT' "$m"), branch jdk=DIFFERENT $(grep -c 'jdk=DIFFERENT' "$b")"
    diff "$m" "$b" | grep '^[<>]' | head -6 > "$O/diff_lines_${v}_$seed.txt"
  done
done
for side in master branch; do
  java -cp "$O/harness-$side-1.22.1:$O/$side-1.22.1" CrcSpeed | sed "s/^/$side /"
done
