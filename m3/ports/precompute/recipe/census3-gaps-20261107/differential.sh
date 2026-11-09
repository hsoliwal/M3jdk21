#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# census3 m3/ports/precompute differential (P3-010): donor classes vs owner classes, default JIT and -Xint, byte-identical.
# usage: differential.sh <donor-classes> <owner-classes> <out-dir> [episodes]
#   donor-classes: javac output of the develop 78cf2ad4c7e5 donor named in oracle/SIDE_MAP.tsv with its source closure
#                  (the donor's indexstring, common, job and native-interop modules)
#   owner-classes: m3/ports/precompute target/classes after apply.py (mvn package)
#   the donor side is generated: mkdonor.py maps oracle/census3/OwnerFrontier.java through oracle/SIDE_MAP.tsv
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
donor=$1; owner=$2; out=$3; episodes=${4:-20000}
JAVA_HOME=${JDK21_HOME:-"C:/Program Files/Java/jdk-21"}
tmp=${M3_TMPDIR:-S:/m3-scratch/eb-conv/c3g/tmp}
sep=";"; [ "$(uname -s | cut -c1-5)" = "Linux" ] && sep=":"
mkdir -p "$out/donor" "$out/owner" "$out/gen"
python -I "$here/mkdonor.py" "$out/gen" > /dev/null
"$JAVA_HOME/bin/javac" -J-Djava.io.tmpdir="$tmp" -d "$out/donor" -cp "$donor" \
  "$here/oracle/census3/Census3FrontierDifferential.java" "$out/gen/census3/DonorFrontier.java"
"$JAVA_HOME/bin/javac" -J-Djava.io.tmpdir="$tmp" -d "$out/owner" -cp "$owner" \
  "$here/oracle/census3/Census3FrontierDifferential.java" "$here/oracle/census3/OwnerFrontier.java"
for mode in jit xint; do
  flag=""; [ "$mode" = xint ] && flag="-Xint"
  for s in donor owner; do
    cp="$out/$s$sep"; [ "$s" = donor ] && cp="$cp$donor" || cp="$cp$owner"
    "$JAVA_HOME/bin/java" -Djava.io.tmpdir="$tmp" $flag -cp "$cp" census3.Census3FrontierDifferential "$s" "$episodes" \
      > "$out/$s-$mode.txt"
  done
  if cmp -s "$out/donor-$mode.txt" "$out/owner-$mode.txt"; then echo "IDENTICAL $mode $(cat "$out/owner-$mode.txt")";
  else echo "DIFFER $mode"; diff "$out/donor-$mode.txt" "$out/owner-$mode.txt" | head; exit 1; fi
done
cmp -s "$out/owner-jit.txt" "$out/owner-xint.txt" && echo "JIT == Xint"
