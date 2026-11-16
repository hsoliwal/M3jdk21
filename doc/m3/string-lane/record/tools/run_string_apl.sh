#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# STRING_APL v2 (2026-11-16). String lane run: hash planes of the indexstring donor family, joined against the planes
# of the M3JDK string owners, then one converge route per donor type. Reuses the atomize kit
# (atomize.sh, atom_planes.py, converge_plan.py); reads only. v1 called apl.sh, which fails on Git Bash: its
# `here=` line yields two lines (`a && pwd -W || b && pwd` runs both pwd) and its step() reports status 0 for a
# failed step (PIPESTATUS is read after `local last=$(...)`). v2 runs the same steps directly with real statuses.
# usage: run_string_apl.sh [owner]   (owner: also rebuild the owner planes)
out=I:/m3-scratch/string-converge-20261116
kit=C:/m3-scratch-sup/lanes/atomize
failed=0
step() {
  local name=$1; shift
  "$@" > "$out/apl/$name.out" 2>&1
  local status=$?
  printf '%s\t%s\t%s\t%s\n' "$name" "$status" "$(date -Is)" "$(grep -v '^SLF4J' "$out/apl/$name.out" | tail -1 | tr '\t' ' ' | cut -c1-200)"
  [ "$status" -eq 0 ] || failed=1
}
mkdir -p "$out/apl"
{
  echo "START $(date -Is)"
  if [ "${1:-}" = owner ]; then
    step owner_atomize bash "$kit/atomize.sh" com.synexia.rewrite.M3AtomizeInventory "$out/owner/src" "$out/owner/az"
    step owner_planes python -I "$kit/atom_planes.py" planes "$out/owner/az" "$out/owner/planes"
  fi
  step atomize bash "$kit/atomize.sh" com.synexia.rewrite.M3AtomizeInventory "$out/donor/src" "$out/apl/az"
  step planes python -I "$kit/atom_planes.py" planes "$out/apl/az" "$out/apl/planes"
  step join python -I "$kit/atom_planes.py" join "$out/apl/planes/ATOM_HASHES.tsv" "$out/owner/planes/ATOM_HASHES.tsv" "$out/apl/TO_OWNER.tsv"
  step converge python -I "$kit/converge_plan.py" "$out/apl/az" "$out/apl/planes/ATOM_HASHES.tsv" "$out/owner/planes/ATOM_HASHES.tsv" "$out/apl/converge"
  echo "END failed=$failed $(date -Is)"
} > "$out/run.log" 2>&1
exit $failed
