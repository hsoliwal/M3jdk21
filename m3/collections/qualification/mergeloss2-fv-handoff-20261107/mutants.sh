#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Mutants of the restored compactor span (qualification/mergeloss2-fv-handoff-20261107), WSL gcc 13.3 + JDK 21.
# Each mutant is one exact text replacement in the candidate m3_index_mask_compactor.c (refused unless it occurs
# exactly once); the mutated owner is linked into the two origin harnesses and into libm3_collections_mask.so for
# the -Xcheck:jni parity contracts. KILLED = any harness or contract fails. The unmutated candidate must pass first.
#
# usage: mutants.sh <CAND root> <OUT> <classes:test-classes:m3-arrays.jar>
#FLLDCIM: MRL - mergeloss2 fv-handoff: one mutant per restored member of the compactor span.
set -uo pipefail
CAND="$1"; OUT="$2"; CLS="$3"
J=/usr/lib/jvm/java-21-openjdk-amd64
N="$CAND/m3/collections/src/main/native/collections"
T="$CAND/m3/collections/src/test/native"
INC=(-I"$J/include" -I"$J/include/linux")
mkdir -p "$OUT"
printf 'id\tmember\tstate\tevidence\n' > "$OUT/MUTANTS.tsv"
# id | member | old | new   (literal strings; \n is a newline)
MUTANTS=(
  "M01|m3_selected_span (trailing trim)|    while (*end > first|    while (0 && *end > first"
  "M02|m3_selected_span (zero-mask whole page)|    if (mask == 0) return first;|    if (mask == 0) { *end = first; return first; }"
  "M03|m3_select_range (span fetch offset)|first + offset + begin, stop - begin, row_ids)|first + offset, stop - begin, row_ids)"
  "M04|m3_select_range (span compaction index)|row_ids[accepted++] = row_ids[i - begin];|row_ids[accepted++] = row_ids[i];"
  "M05|selectLongWindowNative (span fetch offset)|row_offset + position + begin, stop - begin, values)|row_offset + position, stop - begin, values)"
  "M06|selectLongWindowNative (span compaction index)|values[batch++] = values[i - begin];\n|values[batch++] = values[i];\n"
)
run_all() {  # dir with compactor.c -> rc (0 = every check passes), evidence line
  local d="$1" fail=0 ev=""
  for h in m3_sparse_mask_reads m3_mask_compactor_spans; do
    gcc -std=c17 -O2 -Wall -Wextra "${INC[@]}" "$T/$h.c" "$d/compactor.c" -o "$d/$h" > "$d/$h.cc" 2>&1 || { fail=1; ev+="$h:compile "; continue; }
    "$d/$h" > "$d/$h.run" 2>&1 || { fail=1; ev+="$h:$(tail -1 "$d/$h.run" | cut -c1-60) "; }
  done
  mkdir -p "$d/lib"
  gcc -std=c17 -O2 -Wall -Wextra -fPIC -shared "${INC[@]}" "$d/compactor.c" "$N/m3_index_mask_window.c" \
      -o "$d/lib/libm3_collections_mask.so" > "$d/lib.cc" 2>&1 || { echo "1 lib:compile"; return; }
  for test in "M3MaskCompactorSpanContract native" "M3SparseMaskReadContract" "M3IndexMaskCompactorSelfTest --native" \
      "M3MaskNativeParityProof --native"; do
    set -- $test
    java -ea -Xcheck:jni -Xmx768m -Djava.io.tmpdir="$TMPDIR" -Djava.library.path="$d/lib" -cp "$CLS" \
        "jdk.m3.collections.$1" ${2:-} > "$d/$1.log" 2>&1 || { fail=1; ev+="$1 "; }
  done
  echo "$fail ${ev:-all pass}"
}
mkdir -p "$OUT/HEAD"; cp "$N/m3_index_mask_compactor.c" "$OUT/HEAD/compactor.c"
read -r rc ev <<< "$(run_all "$OUT/HEAD")"
printf 'HEAD\tunmutated candidate\t%s\t%s\n' "$([ "$rc" = 0 ] && echo PASS || echo FAIL)" "$ev" >> "$OUT/MUTANTS.tsv"
[ "$rc" = 0 ] || { echo "unmutated candidate fails: $ev"; exit 1; }
killed=0; total=0
for spec in "${MUTANTS[@]}"; do
  IFS='|' read -r id member old new <<< "$spec"
  d="$OUT/$id"; mkdir -p "$d"
  OLD=$(printf '%b' "$old") NEW=$(printf '%b' "$new") python3 - "$N/m3_index_mask_compactor.c" "$d/compactor.c" <<'PY' || { echo "$id site"; exit 1; }
import os, sys
text = open(sys.argv[1], encoding="utf-8").read()
old, new = os.environ["OLD"], os.environ["NEW"]
if text.count(old) != 1: sys.exit("mutation site occurs %d times: %r" % (text.count(old), old[:50]))
open(sys.argv[2], "w", encoding="utf-8").write(text.replace(old, new))
PY
  read -r rc ev <<< "$(run_all "$d")"
  total=$((total + 1)); state=SURVIVED; [ "$rc" = 0 ] || { state=KILLED; killed=$((killed + 1)); }
  printf '%s\t%s\t%s\t%s\n' "$id" "$member" "$state" "$ev" >> "$OUT/MUTANTS.tsv"
  echo "$id $state $ev"
done
echo "mutants=$total killed=$killed survived=$((total - killed))"
[ "$killed" = "$total" ]
