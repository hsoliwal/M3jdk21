#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# STRING_OWNER2 v1 (2026-11-16). Widens the owner side of the string convergence join: the first owner planes held
# only java.lang string owners, jdk.internal.mindex, jdk.internal.m3 and java.util.regex (132 files), so a donor
# type already received elsewhere in M3JDK (m3/ports/indexstring, the vendored mindex, lexicon, precompute,
# compile-time, regex-cursor, runtime-integration, core, indexdb) was routed ABSORB. This run adds those trees at the
# same pin, rebuilds the owner planes and repeats join and converge for the donor planes that already exist.
# Sources go to a 4 KB-cluster drive (the scratch volume costs about 1 MB per file). Reads only; exit 0 = every step ran.
out=I:/m3-scratch/string-converge-20261116
kit=C:/m3-scratch-sup/lanes/atomize
clone=C:/m3-superset/conv/m3jdk21
pin=e3c8bb1d0e3
src=C:/tmp/string-owner2/src
dirs='m3/ports/indexstring m3/vendor/m3/m3-mindex m3/lexicon m3/ports/precompute m3/ports/compiletime m3/ports/regex-cursor m3/runtime-integration m3/core m3/indexdb'
failed=0
step() {
  local name=$1; shift
  "$@" > "$out/owner2/$name.out" 2>&1
  local status=$?
  printf '%s\t%s\t%s\t%s\n' "$name" "$status" "$(date -Is)" "$(grep -v '^SLF4J' "$out/owner2/$name.out" | tail -1 | tr '\t' ' ' | cut -c1-220)"
  [ "$status" -eq 0 ] || failed=1
}
materialize() {
  export GIT_OPTIONAL_LOCKS=0 GIT_TERMINAL_PROMPT=0
  git -C "$clone" ls-tree -r "$pin" -- $dirs | awk -F'\t' '$2 ~ /\.java$/ && $2 !~ /\/src\/test\// && $2 !~ /\/test\//' > "$out/owner2/OWNER2_BLOBS.tsv"
  awk '{print $3}' "$out/owner2/OWNER2_BLOBS.tsv" | sort -u | git -C "$clone" fetch --quiet --no-tags --filter=blob:none origin --stdin
  while read -r mode type oid path; do
    mkdir -p "$src/$(dirname "$path")" && git -C "$clone" cat-file blob "$oid" > "$src/$path" || return 1
  done < "$out/owner2/OWNER2_BLOBS.tsv"
  cp -r "$out/owner/src/src" "$src/" || return 1
  echo "owner2 files $(find "$src" -name '*.java' | wc -l) (added $(wc -l < "$out/owner2/OWNER2_BLOBS.tsv"))"
}
mkdir -p "$out/owner2" "$src"
{
  echo "START $(date -Is)"
  step materialize materialize
  step atomize bash "$kit/atomize.sh" com.synexia.rewrite.M3AtomizeInventory "$src" "$out/owner2/az"
  step planes python -I "$kit/atom_planes.py" planes "$out/owner2/az" "$out/owner2/planes"
  step join python -I "$kit/atom_planes.py" join "$out/apl/planes/ATOM_HASHES.tsv" "$out/owner2/planes/ATOM_HASHES.tsv" "$out/owner2/TO_OWNER2.tsv"
  step converge python -I "$kit/converge_plan.py" "$out/apl/az" "$out/apl/planes/ATOM_HASHES.tsv" "$out/owner2/planes/ATOM_HASHES.tsv" "$out/owner2/converge"
  echo "END failed=$failed $(date -Is)"
} > "$out/owner2/run.log" 2>&1
exit $failed
