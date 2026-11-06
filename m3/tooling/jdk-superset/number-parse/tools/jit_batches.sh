#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# jit_batches.sh <java_bin_dir> <test_classes> <out_dir> <batches> <runs> <pre_classes> <post_classes>
#                <size_pre.tsv> <size_post.tsv> <BATCHES.tsv> <mode> [<JVM flag>...]
# Gate JIT_BATCHES v1: the spread of JIT_PROBE v1. Runs <batches> independent jit_probe.sh batches (<runs> ABBA runs
# of the pre and post images each), merges each batch (jit_inline.py merge, as for JIT.tsv) and appends one row per
# batch to <BATCHES.tsv> (header written when the file is new):
#   mode batch runs atoms gated_before gated_after reasons_before reasons_after runs_exit_0
# <mode> labels the rows; the JVM flags (for example -Xbatch: synchronous compiles, a reproducible inlining log)
# reach every probe run through JAVA_TOOL_OPTIONS. Each batch's VM logs are deleted once its summaries exist; the
# per-batch summaries and JIT.tsv stay in <out_dir>/<mode>-<batch>. Prints one progress line per batch.
set -u
java_bin=$1 test_classes=$2 out=$3 batches=$4 runs=$5 pre=$6 post=$7 size_pre=$8 size_post=$9 table=${10} mode=${11}
shift 11
here=$(cd "$(dirname "$0")" && pwd)
flags="$*"
variants=2  # the pre and post images of every run
header="mode\tbatch\truns\tatoms\tgated_before\tgated_after\treasons_before\treasons_after\truns_exit_0"
[ -f "$table" ] || printf "%b\n" "$header" > "$table"
for ((b = 1; b <= batches; b++)); do
  dir=$out/$mode-$b
  JAVA_TOOL_OPTIONS="$flags" bash "$here/jit_probe.sh" "$java_bin" "$test_classes" "$dir" "$runs" \
    pre="$pre" post="$post" > "$dir.log" 2>&1
  atoms=$(python -I -B "$here/jit_inline.py" merge "$dir/pre.tsv" "$dir/post.tsv" "$size_pre" "$size_post" \
    "$dir/JIT.tsv" | cut -f2)
  reasons=$(python -I -B "$here/jit_reasons.py" "$dir/JIT.tsv")
  printf "%s\t%d\t%d\t%s\t%s\t%d/%d\n" "$mode" "$b" "$runs" "$atoms" "$reasons" "$(grep -c ' exit 0$' "$dir.log")" \
    "$((variants * runs))" >> "$table"
  rm -f "$dir"/*.xml
  echo "batch $mode $b: $(tail -1 "$table" | cut -f4-9 | tr '\t' ' ') $(date +%H:%M:%S)"
done
