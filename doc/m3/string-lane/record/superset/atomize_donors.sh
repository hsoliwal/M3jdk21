#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# DONOR_ATOMIZE v1 (2026-11-17, owner: "inline them in atomized way", "consolidate superset additively"). Atomizes the
# M3JDK string baseline (baseline-src) and every donor under src/<artifact>, read-only, with the atomize kit
# (M3AtomizeInventory on OpenRewrite 8.17.1), builds the hash planes of each, and joins each donor's planes against the
# baseline (EXACT > LOGIC > STRUCTURE > NEAR > ABSENT). Two runs at a time. Writes out/<name>/{atomize,planes,
# TO_BASELINE.tsv,run.log} and ATOMIZE_SUMMARY.tsv (name, files, exit, atoms, join plane counts).
# usage: atomize_donors.sh <donors-dir>
set -u
d=$1
kit=/c/m3-scratch-sup/lanes/atomize
export ATOMIZE_JAVA_OPTS=-Xmx3g
one() {
  name=$1; src=$2; out=$d/out/$name
  rm -rf "$out"; mkdir -p "$out"
  bash "$kit/atomize.sh" com.synexia.rewrite.M3AtomizeInventory "$(cygpath -m "$src")" "$(cygpath -m "$out/atomize")" \
    > "$out/run.log" 2>&1
  echo "atomize_exit=$?" >> "$out/run.log"
  python -I "$kit/atom_planes.py" planes "$(cygpath -m "$out/atomize")" "$(cygpath -m "$out/planes")" >> "$out/run.log" 2>&1
  echo "planes_exit=$?" >> "$out/run.log"
}
export -f one
export d kit
one baseline "$d/baseline-src"
ls "$d/src" | xargs -P 2 -I{} bash -c 'one "$1" "$d/src/$1"' _ {}
base="$d/out/baseline/planes/ATOM_HASHES.tsv"
printf 'name\tjava_files\tatomize_exit\tplanes_exit\tatoms\tEXACT\tLOGIC\tSTRUCTURE\tNEAR\tABSENT\n' > "$d/ATOMIZE_SUMMARY.tsv"
for out in "$d"/out/*/; do
  name=$(basename "$out")
  [ "$name" = baseline ] || python -I "$kit/atom_planes.py" join "$(cygpath -m "$out/planes/ATOM_HASHES.tsv")" \
      "$(cygpath -m "$base")" "$(cygpath -m "$out/TO_BASELINE.tsv")" >> "$out/run.log" 2>&1
  if [ "$name" = baseline ]; then files=$(find "$d/baseline-src" -name '*.java' | wc -l)
  else files=$(find "$d/src/$name" -name '*.java' | wc -l); fi
  ae=$(grep -o 'atomize_exit=[0-9]*' "$out/run.log" | cut -d= -f2); pe=$(grep -o 'planes_exit=[0-9]*' "$out/run.log" | cut -d= -f2)
  atoms=$(($(wc -l < "$out/planes/ATOM_HASHES.tsv" 2>/dev/null || echo 1) - 1))
  counts=$(python -I - "$out/TO_BASELINE.tsv" <<'EOF'
import sys, collections
c = collections.Counter()
try:
    rows = open(sys.argv[1], encoding="utf-8").read().split("\n")
    head = rows[0].split("\t")
    col = next((i for i, h in enumerate(head) if h.lower() in ("plane", "match", "match_plane", "strongest")), None)
    for r in rows[1:]:
        f = r.split("\t")
        if col is not None and len(f) > col:
            c[f[col].rstrip("0123456789")] += 1
except OSError:
    pass
print("\t".join(str(c.get(k, 0)) for k in ("EXACT", "LOGIC", "STRUCTURE", "NEAR", "ABSENT")))
EOF
)
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$name" "$files" "${ae:-?}" "${pe:-?}" "$atoms" "$counts" >> "$d/ATOMIZE_SUMMARY.tsv"
done
cat "$d/ATOMIZE_SUMMARY.tsv"
