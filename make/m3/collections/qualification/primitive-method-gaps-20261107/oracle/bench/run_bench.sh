#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# usage: run_bench.sh HEAD_CLASSES BASE_CLASSES WORK_DIR OUT_TSV   (one JVM per row; JDK 21; temp under WORK_DIR)
# mutate rows alternate master/head/eager three times, since one JVM per row carries run-to-run noise
set -eu
HEAD=$1; BASE=$2; WORK=$3; OUT=$4
HERE=$(cd "$(dirname "$0")" && pwd)
JDK="C:/Program Files/Java/jdk-21/bin"
mkdir -p "$WORK/src" "$WORK/classes" "$WORK/tmp"
python -I "$HERE/make_eager_candidate.py" "$HERE/../../../../src/main/java/jdk/m3/collections/M3LongTreeSet.java" "$WORK/src"
"$JDK/javac" -J-Djava.io.tmpdir="$WORK/tmp" --release 21 -Xlint:all,-module -Werror -cp "$HEAD" -d "$WORK/classes" \
  "$WORK/src/M3LongTreeSetEagerPrefixCandidate.java" "$HERE/PrimitiveGapsBench.java"
J=("$JDK/java" -Xms2g -Xmx2g -XX:+UseParallelGC "-Djava.io.tmpdir=$WORK/tmp")
printf 'workload\timpl\tn\tops\tns_per_op\tchecksum\tclasses\n' > "$OUT"
row() { local cls=$1; shift; echo "$("${J[@]}" -cp "$WORK/classes;$cls" jdk.m3.collections.PrimitiveGapsBench "$@")	$( [ "$cls" = "$BASE" ] && echo base || echo head)" | tee -a "$OUT"; }
for n in 100000 1000000; do
  for rep in 1 2 3; do row "$BASE" mutate m3 $n; row "$HEAD" mutate m3 $n; row "$HEAD" mutate eager $n; done
  row "$HEAD" get m3 $n; row "$HEAD" get eager $n; row "$HEAD" iterate m3 $n
  row "$HEAD" interleave m3 $n; row "$HEAD" interleave eager $n
  row "$HEAD" bulk m3 $n; row "$HEAD" bulk m3loop $n
  row "$HEAD" getalloc m3 $n
done
