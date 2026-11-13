#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# jit_probe.sh <java_bin_dir> <test_classes> <out_dir> <runs> <label>=<patched_classes> [<label>=<patched_classes>...]
# Gate JIT_PROBE v1: runs M3NumberParseJitPerf (test/jdk/jdk/internal/math; a warm Double.parseDouble /
# Float.parseFloat loop over decimal, hex, short, long and subnormal literals) <runs> times per variant with
#   -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining -XX:+LogVMOutput -XX:-DisplayVMOutput
# so each run leaves <out_dir>/<label>-<run>.xml (the VM log keeps every compiler thread's PrintCompilation and
# PrintInlining text, marked by writer thread). Runs of the variants alternate (ABBA) as in perf_ab.sh. Then
# jit_inline.py summarize sums each variant's runs into <out_dir>/<label>.tsv. Timings of these runs are not used.
set -u
java_bin=$1 test_classes=$2 out=$3 runs=$4
shift 4
here=$(cd "$(dirname "$0")" && pwd)
mkdir -p "$out"
for ((r = 1; r <= runs; r++)); do
  order=("$@")
  if ((r % 2 == 0)); then
    order=(); for ((k = $#; k >= 1; k--)); do order+=("${!k}"); done
  fi
  for spec in "${order[@]}"; do
    label=${spec%%=*} classes=${spec#*=}
    timeout 600 "$java_bin/java" -Xmx1g --patch-module "java.base=$classes" \
      --add-opens java.base/jdk.internal.math=ALL-UNNAMED \
      -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining \
      -XX:+LogVMOutput -XX:-DisplayVMOutput "-XX:LogFile=$out/$label-$r.xml" \
      -cp "$test_classes" M3NumberParseJitPerf > "$out/$label-$r.out" 2>&1
    echo "run $r $label exit $?"
  done
done
for spec in "$@"; do
  label=${spec%%=*}
  python -I -B "$here/jit_inline.py" summarize "$out/$label.tsv" "$out/$label"-[0-9]*.xml
done
