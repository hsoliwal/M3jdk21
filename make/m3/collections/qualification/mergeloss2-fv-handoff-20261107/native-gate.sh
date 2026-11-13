#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Native gate of qualification/mergeloss2-fv-handoff-20261107 (WSL, gcc 13.3, JDK 21), derived from
# ../mergeloss2-50-20261107/native-gate.sh: the compactor owner of the incumbent tree (BASE) and of the candidate
# (CAND) in every compile/link combination those gates use for m3_index_mask_compactor.c, the JNI function-table
# harnesses (the two origin harnesses of rows 35/27, the row-17 origin harness, the existing two-mask harness),
# nm -D export identity, javac -h prototype checks, and -Xcheck:jni JVM runs of the existing compactor/window
# self-tests and contracts plus the Java/JNI parity contracts this recipe relocates. Writes GATE_RESULTS.tsv.
#
# usage: native-gate.sh <BASE root> <CAND root> <OUT> <classes:test-classes:m3-arrays.jar> <javac -h headers>
#FLLDCIM: MRL - mergeloss2 fv-handoff: every link combination of the compactor owner, incumbent vs candidate.
set -uo pipefail
BASE="$1"; CAND="$2"; OUT="$3"; CLS="$4"; HDR="$5"
J=/usr/lib/jvm/java-21-openjdk-amd64
N=m3/collections/src/main/native/collections
T=m3/collections/src/test/native
INC=(-I"$J/include" -I"$J/include/linux")
RES="$OUT/GATE_RESULTS.tsv"
mkdir -p "$OUT"
printf 'gate\ttree\tcombo\trc\tdiagnostics\tdetail\n' > "$RES"

# name | flags | sources (relative to the tree; the origin harnesses come from CAND) | kind | product
COMBOS=(
  "mask-c17-shared|-std=c17 -O2 -Wall -Wextra -Werror -pedantic -fPIC -shared|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|shared|libm3_collections_mask.so"
  "mask-c11-shared|-std=c11 -O2 -Wall -Wextra -Werror -fPIC -shared|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|shared|libm3_collections_mask.so"
  "mask-c11-strict-shared|-std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -fPIC -shared|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|shared|libm3_collections_mask.so"
  "mask-c11-syntax|-std=c11 -fsyntax-only -Wall -Wextra -Werror|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|syntax|-"
  "mask-c17-ubsan-hdr|-std=c17 -O1 -g -Wall -Wextra -Werror -pedantic -fsanitize=undefined -fno-sanitize-recover=all -fPIC -shared -include $HDR/jdk_m3_collections_M3IndexMaskWindow.h|$N/m3_index_mask_window.c $N/m3_index_mask_compactor.c|shared|libm3_collections_mask.so"
  "proto-compactor|-std=c17 -fsyntax-only -Wall -Wextra -Werror -pedantic -include $HDR/jdk_m3_collections_M3IndexMaskCompactor.h|$N/m3_index_mask_compactor.c|syntax|-"
  "proto-compactor-strict|-std=c11 -fsyntax-only -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -include $HDR/jdk_m3_collections_M3IndexMaskCompactor.h|$N/m3_index_mask_compactor.c|syntax|-"
  "h35-sparse-reads|-std=c17 -O2 -Wall -Wextra -Werror|@$T/m3_sparse_mask_reads.c $N/m3_index_mask_compactor.c|exe|-"
  "h35-sparse-reads-ubsan|-std=c17 -O1 -g -Wall -Wextra -Werror -fsanitize=undefined,address -fno-sanitize-recover=all|@$T/m3_sparse_mask_reads.c $N/m3_index_mask_compactor.c|exe|-"
  "h27-compactor-spans|-std=c17 -O2 -Wall -Wextra -Werror|@$T/m3_mask_compactor_spans.c $N/m3_index_mask_compactor.c|exe|-"
  "h27-compactor-spans-ubsan|-std=c17 -O1 -g -Wall -Wextra -Werror -fsanitize=undefined,address -fno-sanitize-recover=all|@$T/m3_mask_compactor_spans.c $N/m3_index_mask_compactor.c|exe|-"
  "h17-empty-regions|-std=c17 -O2 -Wall -Wextra -Werror|@ORIGIN/m3_mask_empty_regions.c $N/m3_index_mask_compactor.c|exe|candidate"
  "two-mask-refinement-baseline|-std=c17 -O2 -Wall -Wextra -Werror|$T/m3_required_excluded_window_refinement.c $N/m3_index_mask_compactor.c|exe|--baseline"
  "two-mask-refinement-skip-prefix|-std=c17 -O2 -Wall -Wextra -Werror|$T/m3_required_excluded_window_refinement.c $N/m3_index_mask_compactor.c|exe|--skip-prefix"
  "window-contract-exe|-std=c17 -O2 -Wall -Wextra -Werror|$N/m3_index_mask_window.c $T/m3_mask_window_current_master_contract.c|exe|-"
)

build() {  # tree root, label, combo spec, output dir -> one GATE_RESULTS row
  local root="$1" label="$2" spec="$3" dir="$4"
  IFS='|' read -r name flags srcs kind product <<< "$spec"
  local files=() log="$dir/$name.log"
  for s in $srcs; do
    case "$s" in
      @ORIGIN/*) files+=("${ORIGIN:?}/${s#@ORIGIN/}") ;;
      @*) files+=("$CAND/${s#@}") ;;
      *) files+=("$root/$s") ;;
    esac
  done
  mkdir -p "$dir"
  ( cd "$root/$N" && case "$kind" in
      syntax) gcc $flags "${INC[@]}" "${files[@]}" ;;
      *) gcc $flags "${INC[@]}" "${files[@]}" -o "$dir/$name.$kind" ;;
    esac ) > "$log" 2>&1
  local rc=$? diag detail="-"
  diag=$(grep -cE ': (warning|error|note|fatal error):' "$log"); diag=${diag:-0}
  if [ "$rc" = 0 ] && [ "$kind" = exe ]; then
    "$dir/$name.exe" $([ "$product" = - ] || echo "$product") > "$dir/$name.run" 2>&1; detail="run_rc=$? $(tail -1 "$dir/$name.run" | tr '\t' ' ' | cut -c1-110)"
  elif [ "$rc" != 0 ]; then
    detail=$(grep -m1 -E 'error' "$log" | sed 's/^.*error: //' | cut -c1-120)
  fi
  printf 'gcc\t%s\t%s\t%s\t%s\t%s\n' "$label" "$name" "$rc" "$diag" "$detail" >> "$RES"
}

for spec in "${COMBOS[@]}"; do
  build "$BASE" base "$spec" "$OUT/base"
  build "$CAND" cand "$spec" "$OUT/cand"
done

# Exports: nm -D --defined-only per shared library, incumbent vs candidate.
for spec in "${COMBOS[@]}"; do
  IFS='|' read -r name flags srcs kind product <<< "$spec"
  [ "$kind" = shared ] || continue
  b="$OUT/base/$name.shared"; c="$OUT/cand/$name.shared"
  [ -f "$b" ] && [ -f "$c" ] || { printf 'nm\tboth\t%s\t1\t-\tmissing library\n' "$name" >> "$RES"; continue; }
  nm -D --defined-only "$b" | awk '{print $2" "$3}' | sort > "$OUT/base/$name.nm"
  nm -D --defined-only "$c" | awk '{print $2" "$3}' | sort > "$OUT/cand/$name.nm"
  cmp -s "$OUT/base/$name.nm" "$OUT/cand/$name.nm"; rc=$?
  printf 'nm\tbase-vs-cand\t%s\t%s\t-\tsymbols=%s jni=%s\n' "$name" "$rc" "$(wc -l < "$OUT/base/$name.nm")" \
    "$(grep -c ' Java_' "$OUT/base/$name.nm")" >> "$RES"
done

# JVM: -Xcheck:jni self-tests/contracts against each tree's mask library (Java classes are the same for both).
for label in base cand; do
  lib="$OUT/jvm-$label"; mkdir -p "$lib"
  cp "$OUT/$label/mask-c17-shared.shared" "$lib/libm3_collections_mask.so"
  for test in "M3IndexMaskCompactorSelfTest --native" "M3MaskNativeParityProof --native" \
      "M3BudgetedMaskWindowContract --native" "M3IndexMaskWindowSelfTest --native" \
      "M3IndexMaskWindowRecoverySelfTest --native" "M3MaskCompactorSpanContract native" "M3SparseMaskReadContract"; do
    set -- $test
    java -ea -Xcheck:jni -Xmx768m -Djava.io.tmpdir="$TMPDIR" -Djava.library.path="$lib" -cp "$CLS" \
      "jdk.m3.collections.$1" ${2:-} > "$OUT/$label-$1.log" 2>&1
    rc=$?
    printf 'jvm\t%s\t%s\t%s\t%s\t%s\n' "$label" "$1" "$rc" "$(grep -ci 'warning' "$OUT/$label-$1.log")" \
      "$(tail -1 "$OUT/$label-$1.log" | tr '\t' ' ' | cut -c1-110)" >> "$RES"
  done
done
for test in M3IndexMaskCompactorSelfTest M3MaskNativeParityProof M3BudgetedMaskWindowContract M3IndexMaskWindowSelfTest \
    M3IndexMaskWindowRecoverySelfTest M3MaskCompactorSpanContract M3SparseMaskReadContract; do
  cmp -s "$OUT/base-$test.log" "$OUT/cand-$test.log"; rc=$?
  printf 'jvm\tbase-vs-cand\t%s-output\t%s\t-\tidentical=%s\n' "$test" "$rc" "$([ "$rc" = 0 ] && echo y || echo n)" >> "$RES"
done
column -t -s "$(printf '\t')" "$RES"
