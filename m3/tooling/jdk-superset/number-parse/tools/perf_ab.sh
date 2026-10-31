#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# perf_ab.sh <java_bin_dir> <test_classes> <out.tsv> <rounds> <label>=<patched_classes> [<label>=<patched_classes>...]
# Gate PERF_AB v1: runs M3NumberParseAtomsPerf (test/jdk/jdk/internal/math) once per variant per round, variants
# interleaved inside each round (ABBA order) so host-load drift and run-order bias hit every variant alike.
# Appends one row per measured atom: round variant atom ns_per_op_atom ns_per_op_reference speedup (failed runs: atom = RUN_FAILED).
set -u
java_bin=$1 test_classes=$2 out=$3 rounds=$4
shift 4
printf 'round\tvariant\tatom\tns_per_op_atom\tns_per_op_reference\tspeedup\n' > "$out"
for ((r = 1; r <= rounds; r++)); do
  order=("$@")
  if ((r % 2 == 0)); then  # ABBA: even rounds run the variants in reverse order
    order=(); for ((k = $#; k >= 1; k--)); do order+=("${!k}"); done
  fi
  for spec in "${order[@]}"; do
    label=${spec%%=*} classes=${spec#*=}
    rows=$("$java_bin/java" -Xmx1g --patch-module "java.base=$classes" \
      --add-opens java.base/jdk.internal.math=ALL-UNNAMED -cp "$test_classes" M3NumberParseAtomsPerf 2>&1 \
      | awk -F'\t' 'NF == 4 && $1 != "atom"')
    if [ -z "$rows" ]; then
      printf '%d\t%s\tRUN_FAILED\t\t\t\n' "$r" "$label" >> "$out"
    else
      printf '%s\n' "$rows" | awk -v r="$r" -v l="$label" -F'\t' '{print r "\t" l "\t" $0}' >> "$out"
    fi
    echo "round $r $label done"
  done
done
