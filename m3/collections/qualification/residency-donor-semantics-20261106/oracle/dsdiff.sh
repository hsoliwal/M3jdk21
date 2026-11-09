#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Driver of ResidencyDonorSemanticsDifferential: compiles the oracle once (against BEFORE), runs it on BEFORE and AFTER
# classes and classifies every differing line. usage: dsdiff.sh <before classes> <after classes> <out dir> <episodes> <ops>
# env: JAVA_HOME = JDK 21; M3_ARRAYS_JAR = the m3-arrays jar the module was built with.
set -u
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
B=$1; A=$2; OUT=$3; EP=$4; OPS=$5
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
rm -rf "$OUT"; mkdir -p "$OUT/oracle"
"$J/javac" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$B$SEP${M3_ARRAYS_JAR:-}" -d "$OUT/oracle" \
    "$HERE/jdk/m3/collections/ResidencyDonorSemanticsDifferential.java" || exit 2
for side in before after; do
  if [ $side = before ]; then C=$B; else C=$A; fi
  "$J/java" -cp "$OUT/oracle$SEP$C$SEP${M3_ARRAYS_JAR:-}" jdk.m3.collections.ResidencyDonorSemanticsDifferential \
      "$OUT/ds-$side.txt" "$EP" "$OPS" || echo "RUN_FAIL $side"
  echo "$side sha256=$(sha256sum < "$OUT/ds-$side.txt" | cut -c1-12) lines=$(wc -l < "$OUT/ds-$side.txt")"
done
python -I "$HERE/classify_donor_semantics.py" "$OUT/ds-before.txt" "$OUT/ds-after.txt" "$OUT/ds-classes.tsv"
