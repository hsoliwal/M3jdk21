#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Paired get/put microbench of M3HashMap, base vs head (qualification/hash-reentrant-guard-20261107). HashBench loads
# the base and the head classes through two class loaders in one JVM and alternates timed rounds between them; this
# script runs FORKS such JVMs and prints, per (key type, operation), the median over forks of each side's median
# ns/op and of the median head/base ratio, plus the widest 10th-90th percentile ratio band of any fork (the noise).
# Run it once with the base classes on both sides (A/A) to see the noise of the machine.
# usage: bench.sh <base classpath> <head classpath> <out dir>
# env:   JAVA_HOME = JDK 21; JVM_OPTS; FORKS (default 5), WARMUP (default 20), ROUNDS (default 60)
set -euo pipefail
export LC_ALL=C
BASE=$1; HEAD=$2; OUT=$3
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
FORKS=${FORKS:-5}; WARMUP=${WARMUP:-20}; ROUNDS=${ROUNDS:-60}
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
rm -rf "$OUT"; mkdir -p "$OUT/bench"
"$J/javac" $JAVAC_J --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$BASE" -d "$OUT/bench" "$HERE/HashBench.java"
for fork in $(seq 1 "$FORKS"); do
  "$J/java" ${JVM_OPTS:-} -cp "$OUT/bench" HashBench "$BASE" "$HEAD" "$WARMUP" "$ROUNDS" | tr -d '\r' \
      | sed "s/^/$fork\t/" >> "$OUT/raw.tsv"
done
python -I - "$OUT/raw.tsv" <<'EOF'
import sys
from statistics import median
cells = {}
for line in open(sys.argv[1], encoding="utf-8"):
    fork, kind, op, *fields = line.rstrip("\n").split("\t")
    values = dict(field.split("=") for field in fields)
    cells.setdefault((kind, op), []).append({k: float(v) for k, v in values.items()})
print("keys\toperation\tbase_ns_op\thead_ns_op\thead/base\tratio_p10\tratio_p90\tforks")
for (kind, op), forks in cells.items():
    print("%s\t%s\t%.3f\t%.3f\t%.4f\t%.4f\t%.4f\t%d" % (kind, op, median(f["base"] for f in forks),
          median(f["head"] for f in forks), median(f["ratio"] for f in forks), min(f["p10"] for f in forks),
          max(f["p90"] for f in forks), len(forks)))
EOF
