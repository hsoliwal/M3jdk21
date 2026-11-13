#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Merge-loss phase-2 leaf 50 native gate (WSL, gcc 13.3, JDK 21). Compares the incumbent tree (BASE) with the
# apply.py candidate (CAND) in every compile/link combination the master workflows and full-java-jni verify.py
# use for the four leaf-50 C owners, then repeats the candidate compile inside a sparse closure (no src/java.base)
# as the eight sparse workflow jobs in CLOSURES.tsv check it out. Writes GATE_RESULTS.tsv under OUT.
#
# usage: native-gate.sh <BASE root> <CAND root> <OUT> <classes> <javac -h headers>
#FLLDCIM: MRL - mergeloss2 leaf 50: every link combination of the four C owners, incumbent vs candidate.
set -uo pipefail
BASE="$1"; CAND="$2"; OUT="$3"; CLS="$4"; HDR="$5"
J=/usr/lib/jvm/java-21-openjdk-amd64
N=m3/collections/src/main/native/collections
T=m3/collections/src/test/native
INC=(-I"$J/include" -I"$J/include/linux")
RES="$OUT/GATE_RESULTS.tsv"
mkdir -p "$OUT"
printf 'gate\ttree\tcombo\trc\tdiagnostics\tdetail\n' > "$RES"

# combo name | flags | sources (relative to the tree) | kind (shared, syntax, exe, objlink)
COMBOS=(
  "mask-c17-shared|-std=c17 -O2 -Wall -Wextra -Werror -pedantic -fPIC -shared|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|shared|libm3_collections_mask.so"
  "mask-c11-shared|-std=c11 -O2 -Wall -Wextra -Werror -fPIC -shared|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|shared|libm3_collections_mask.so"
  "mask-c11-strict-shared|-std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -fPIC -shared|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|shared|libm3_collections_mask.so"
  "mask-c11-syntax|-std=c11 -fsyntax-only -Wall -Wextra -Werror|$N/m3_index_mask_compactor.c $N/m3_index_mask_window.c|syntax|-"
  "mask-c17-ubsan-hdr|-std=c17 -O1 -g -Wall -Wextra -Werror -pedantic -fsanitize=undefined -fno-sanitize-recover=all -fPIC -shared -include $HDR/jdk_m3_collections_M3IndexMaskWindow.h|$N/m3_index_mask_window.c $N/m3_index_mask_compactor.c|shared|libm3_collections_mask.so"
  "window-contract-exe|-std=c17 -O2 -Wall -Wextra -Werror|$N/m3_index_mask_window.c $T/m3_mask_window_current_master_contract.c|exe|window-contract"
  "window-contract-ubsan|-std=c17 -O1 -g -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=all|$N/m3_index_mask_window.c $T/m3_mask_window_current_master_contract.c|exe|window-contract-ubsan"
  "window-spans-exe|-std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined|$N/m3_index_mask_window.c $T/m3_mask_window_spans.c|exe|window-spans"
  "window-regions-exe|-std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined|$N/m3_index_mask_window.c $T/m3_mask_window_regions.c|exe|window-regions"
  "longpath-strict|-std=c17 -fPIC -shared -O2 -Wall -Wextra -Wconversion -Wshadow -Werror -pedantic|$N/m3_long_index_path_masks_jni.c|shared|libm3_collections_long_path_masks.so"
  "longpath-ubsan|-std=c17 -fPIC -shared -O1 -Wall -Wextra -Wconversion -Wshadow -Werror -pedantic -fsanitize=undefined -fno-sanitize-recover=undefined|$N/m3_long_index_path_masks_jni.c|shared|libm3_collections_long_path_masks.so"
  "statehist-hdr|-std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined -fPIC -shared -include $HDR/jdk_m3_collections_M3StateMaskHistogram.h|$N/m3_state_mask_histogram.c|shared|libm3_collections_statehist.so"
  "statehist-strict|-std=c17 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -Werror -fPIC -shared|$N/m3_state_mask_histogram.c|shared|libm3_collections_statehist.so"
  "bitlane-hdr|-std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined -fPIC -shared -include $HDR/jdk_m3_collections_M3BitLane28.h|$N/segmented_bit_lane.c|shared|libm3bitlane.so"
  "bitlane-strict|-std=c17 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -Werror -fPIC -shared|$N/segmented_bit_lane.c|shared|libm3bitlane.so"
  "proto-window|-std=c17 -fsyntax-only -Wall -Wextra -Werror -pedantic -include $HDR/jdk_m3_collections_M3IndexMaskWindow.h|$N/m3_index_mask_window.c|syntax|-"
  "proto-compactor|-std=c17 -fsyntax-only -Wall -Wextra -Werror -pedantic -include $HDR/jdk_m3_collections_M3IndexMaskCompactor.h|$N/m3_index_mask_compactor.c|syntax|-"
  "proto-longpath|-std=c17 -fsyntax-only -Wall -Wextra -Werror -pedantic -include $HDR/jdk_m3_collections_M3LongIndexPathMasksJNI.h|$N/m3_long_index_path_masks_jni.c|syntax|-"
  "proto-statehist|-std=c17 -fsyntax-only -Wall -Wextra -Werror -pedantic -include $HDR/jdk_m3_collections_M3StateMaskHistogram.h|$N/m3_state_mask_histogram.c|syntax|-"
  "proto-bitlane|-std=c17 -fsyntax-only -Wall -Wextra -Werror -pedantic -include $HDR/jdk_m3_collections_M3BitLane28.h|$N/segmented_bit_lane.c|syntax|-"
)

build() {  # tree label, combo spec, output dir -> one GATE_RESULTS row
  local root="$1" label="$2" spec="$3" dir="$4"
  IFS='|' read -r name flags srcs kind product <<< "$spec"
  local files=() log="$dir/$name.log"
  for s in $srcs; do files+=("$root/$s"); done
  mkdir -p "$dir"
  ( cd "$root/$N" && case "$kind" in
      syntax) gcc $flags "${INC[@]}" "${files[@]}" ;;
      *) gcc $flags "${INC[@]}" "${files[@]}" -o "$dir/$name.$kind" ;;
    esac ) > "$log" 2>&1
  local rc=$? diag
  diag=$(grep -cE ': (warning|error|note|fatal error):' "$log"); diag=${diag:-0}
  local detail="-"
  if [ "$rc" = 0 ] && [ "$kind" = exe ]; then
    "$dir/$name.exe" > "$dir/$name.run" 2>&1; detail="run_rc=$?"
  elif [ "$rc" != 0 ]; then
    detail=$(grep -m1 -E 'error' "$log" | sed 's/^.*error: //' | cut -c1-120)
  fi
  printf 'gcc\t%s\t%s\t%s\t%s\t%s\n' "$label" "$name" "$rc" "$diag" "$detail" >> "$RES"
}

# Sparse closure: the candidate tree as a sparse job checks it out (m3/collections + m3/native/include only).
SPARSE="$OUT/cand-sparse"
mkdir -p "$SPARSE"
( cd "$CAND" && tar cf - m3/collections m3/native/include ) | ( cd "$SPARSE" && tar xf - )
SPARSE_BASE="$OUT/base-sparse"
mkdir -p "$SPARSE_BASE"
( cd "$BASE" && tar cf - m3/collections m3/native/include ) | ( cd "$SPARSE_BASE" && tar xf - )

for spec in "${COMBOS[@]}"; do
  build "$BASE" base "$spec" "$OUT/base"
  build "$CAND" cand "$spec" "$OUT/cand"
  build "$SPARSE_BASE" base-sparse "$spec" "$OUT/base-sparse-out"
  build "$SPARSE" cand-sparse "$spec" "$OUT/cand-sparse-out"
done

# Exports: nm -D --defined-only per shared library and combo, incumbent vs candidate.
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

# JVM: -Xcheck:jni throw differential + existing self-tests/contracts, against each tree's libraries.
for label in base cand; do
  lib="$OUT/jvm-$label"; mkdir -p "$lib"
  for spec in "${COMBOS[@]}"; do
    IFS='|' read -r name flags srcs kind product <<< "$spec"
    case "$name" in
      mask-c17-shared|longpath-strict|statehist-strict|bitlane-strict) cp "$OUT/$label/$name.shared" "$lib/$product";;
    esac
  done
  java -Xcheck:jni -Xmx512m -cp "$CLS" jdk.m3.collections.M3Mergeloss50JniThrowDifferential "$lib" \
    "$OUT/throw-$label.txt" > "$OUT/throw-$label.log" 2>&1
  rc=$?
  printf 'jvm\t%s\tthrow-differential\t%s\t%s\tlines=%s throws=%s\n' "$label" "$rc" \
    "$(grep -ci 'warning' "$OUT/throw-$label.log")" "$(wc -l < "$OUT/throw-$label.txt")" \
    "$(grep -c ' THROW ' "$OUT/throw-$label.txt")" >> "$RES"
  for test in "M3IndexMaskWindowSelfTest --native" "M3IndexMaskWindowRecoverySelfTest --native" \
      "M3IndexMaskCompactorSelfTest --native" "M3BudgetedMaskWindowContract --native" \
      "M3MaskNativeParityProof --native" "M3LongIndexPathMasksJNISelfTest --native" \
      "M3StateMaskHistogramSelfTest --native" "M3StateMaskHistogramIndexedNativeContract --native"; do
    set -- $test
    java -ea -Xcheck:jni -Xmx512m -Djava.library.path="$lib" -cp "$CLS" "jdk.m3.collections.$1" ${2:-} \
      > "$OUT/$label-$1.log" 2>&1
    rc=$?
    printf 'jvm\t%s\t%s\t%s\t%s\t%s\n' "$label" "$1" "$rc" "$(grep -ci 'warning' "$OUT/$label-$1.log")" \
      "$(tail -1 "$OUT/$label-$1.log" | tr '\t' ' ' | cut -c1-100)" >> "$RES"
  done
done
cmp -s "$OUT/throw-base.txt" "$OUT/throw-cand.txt"
printf 'jvm\tbase-vs-cand\tthrow-transcript\t%s\t-\tdistinct_throws=%s\n' "$?" \
  "$(grep -o ' THROW [^]]*' "$OUT/throw-base.txt" | sed 's/ H:.*//' | sort -u | wc -l)" >> "$RES"
column -t -s "$(printf '\t')" "$RES"
