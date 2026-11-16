#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# POSITION_KERNEL_PROOF v1 (2026-11-16). usage: run_proof.sh <base-java-lang-dir> <head-java-lang-dir> <work-dir> <classes-dir>
# Copies linearIndexOf and linearLastIndexOf (and, when present, windowFor and the window constants) out of the base
# and the head M3StringPositionPrecompute.java into PositionKernelBase and PositionKernel (awk by declaration line;
# the copied text is not edited; two forwarding methods are added). The kernels, the M3String proof stub and
# PositionKernelProof are compiled INTO java.lang of a stock JDK 21 with --patch-module, so the copied text calls the
# stock StringLatin1 and StringUTF16 helpers, and the proof runs on that patched JVM. Exit 0 = PASS.
set -u
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
base=$1; head=$2; work=$3; classes=$4
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
gen=$work/gen/java/lang
rm -rf "$work/gen"; mkdir -p "$gen" "$classes"
kernel() {
  local name=$1 src=$2/M3StringPositionPrecompute.java
  local method
  method() { awk -v sig="$1" 'index($0, sig) { on = 1 } on { print } on && /^    }$/ { exit }' "$src"; }
  {
    echo 'package java.lang;'
    echo "final class $name {"
    grep -F -e 'private static final int WINDOW_UNITS = 4096;' -e 'private static final int FIRST_WINDOW_UNITS = 32;' \
      -e 'private static final byte[] NO_WINDOW = new byte[0];' "$src"
    method 'private static int linearIndexOf(M3String source, char unit, int from, int end)'
    method 'private static int linearLastIndexOf(M3String source, char unit, int from)'
    method 'private static byte[] windowFor(byte[] window, int count, byte coder)'
    echo '    static int first(M3String s, char u, int from, int end) { return linearIndexOf(s, u, from, end); }'
    echo '    static int last(M3String s, char u, int from) { return linearLastIndexOf(s, u, from); }'
    echo '}'
  } > "$gen/$name.java"
  echo "$name	copied_lines=$(wc -l < "$gen/$name.java")	windowFor=$(grep -c 'windowFor(byte' "$gen/$name.java")"
}
kernel PositionKernelBase "$base"
kernel PositionKernel "$head"
cp "$here/M3String.stub.java.txt" "$gen/M3String.java"; cp "$here/PositionKernelProof.java" "$gen/"
MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -nowarn -XDignore.symbol.file -implicit:none -d "$classes" \
  --patch-module "java.base=$work/gen" "$gen/M3String.java" "$gen/PositionKernelBase.java" "$gen/PositionKernel.java" \
  "$gen/PositionKernelProof.java" || { echo "COMPILE_FAIL"; exit 3; }
MSYS_NO_PATHCONV=1 "$java_home/bin/java" --patch-module "java.base=$classes" java.lang.PositionKernelProof
