#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# EDGE_SCAN_PROOF v1 (2026-11-16). usage: run_proof.sh <head-java-lang-dir> <work-dir> [<classes-dir>]
# Copies the four edge kernels and the EDGE_UNDECIDED constant out of M3StringPrecomputeBridge.java (awk by
# declaration line; the copied text is not edited), compiles them with EdgeScanProof.java on a stock JDK 21 and runs
# the differential. The class output goes to <classes-dir> (default <work-dir>/classes). Exit 0 = PASS.
set -u
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
head=$1; work=$2; classes=${3:-$2/classes}
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
mkdir -p "$work/gen" "$classes"
method() { awk -v sig="$1" 'index($0, sig) { on = 1 } on { print } on && /^    }$/ { exit }' "$head/M3StringPrecomputeBridge.java"; }
{
  echo 'final class EdgeKernel {'
  grep -F 'static final int EDGE_UNDECIDED = -1;' "$head/M3StringPrecomputeBridge.java"
  method 'static int leadingEdge(CharSequence units, boolean strip, int bound)'
  method 'static int trailingEdge(CharSequence units, boolean strip, int bound)'
  method 'private static int stripStepForward(CharSequence units, int index, int length)'
  method 'private static int stripStepBackward(CharSequence units, int end)'
  echo '}'
} > "$work/gen/EdgeKernel.java"
echo "kernel_methods	$(grep -cE '^    (private )?static int ' "$work/gen/EdgeKernel.java")"
"$java_home/bin/javac" -Xlint:all -Werror -sourcepath "$work/gen" -d "$classes" "$work/gen/EdgeKernel.java" \
  "$here/EdgeScanProof.java" || { echo "COMPILE_FAIL"; exit 3; }
"$java_home/bin/java" -cp "$classes" EdgeScanProof
