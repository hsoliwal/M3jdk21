#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
# COMPARE_BENCH v1: usage run_bench.sh <work-dir with classes/ and harness/> <donor-jars>. One JVM per kernel and side,
# 3 forks, median; writes bench.tsv and prints the table (string vs naive vs donor).
set -u
work=$1; donor=$2
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
jh=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
export MSYS_NO_PATHCONV=1
"$jh/bin/javac" -nowarn -d "$work/harness" --patch-module "java.base=$work/classes" -cp "$donor" "$here/CompareBench.java" || exit 3
run() { "$jh/bin/java" --patch-module "java.base=$work/classes" -cp "$work/harness;$donor" CompareBench "$@"; }
kernels=$(run | wc -l)
: > "$work/bench.tsv"
for fork in 1 2 3; do
  for ((k = 0; k < kernels; k++)); do
    for side in string naive donor; do run "$k" "$side" | sed "s/^/$fork	/" >> "$work/bench.tsv"; done
  done
done
python -I - "$work/bench.tsv" <<'PY'
import sys, statistics, collections
cells = collections.defaultdict(list)
for line in open(sys.argv[1], encoding="utf-8"):
    fork, label, side, ns, _ = line.rstrip("\n").split("\t")
    cells[(label, side)].append(float(ns))
print("kernel\tstring ns\tnaive ns\tdonor ns\tvs naive\tvs donor")
for label in dict.fromkeys(l for l, _ in cells):
    s, n, d = (statistics.median(cells[(label, x)]) for x in ("string", "naive", "donor"))
    print(f"{label}\t{s:.0f}\t{n:.0f}\t{d:.0f}\t{n / s:.2f}x\t{d / s:.2f}x")
PY
