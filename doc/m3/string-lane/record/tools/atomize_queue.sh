#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
# ATOMIZE_QUEUE v1 (2026-11-17): waits until no AtomizeHost JVM runs, then atomizes the queued sources one at a time
# (M3AtomizeInventoryNoIdeas, planes), pruning each finished run to planes/ + MethodTable/SUMMARY/ERRORS so the scratch
# drive cannot fill again. Reruns any queued run whose planes are missing or failed.
# usage: atomize_queue.sh <donors-dir> <synexia-repo> <kind:name>...   (kind = synexia | donor)
set -u
d=$1; s=$2; shift 2
kit=/c/m3-scratch-sup/lanes/atomize
export ATOMIZE_EXTRA_CP="$(cygpath -m "$kit/extra-noideas")" ATOMIZE_JAVA_OPTS=-Xmx5g
while powershell -NoProfile -Command "if (Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'AtomizeHost' }) { exit 0 } else { exit 1 }"; do
  sleep 60
done
prune() {
  for f in "$1"/atomize/*.tsv "$1"/atomize/CHANGES.patch; do
    [ -f "$f" ] || continue
    case "$f" in *MethodTable.tsv|*SUMMARY.tsv|*ERRORS.tsv) ;; *) rm -f "$f" ;; esac
  done
}
for run in "$d"/out/*/ "$d"/out-synexia/*/; do grep -q 'planes_exit=0' "$run/run.log" 2>/dev/null && prune "$run"; done
for item in "$@"; do
  kind=${item%%:*}; name=${item#*:}
  if [ "$kind" = donor ]; then src=$d/src/$name; out=$d/out/$name; else src=$s/$name; out=$d/out-synexia/$name; fi
  if grep -q 'planes_exit=0' "$out/run.log" 2>/dev/null && [ -s "$out/planes/ATOM_HASHES.tsv" ]; then
    echo "$name already done"; continue
  fi
  rm -rf "$out"; mkdir -p "$out"
  bash "$kit/atomize.sh" com.synexia.rewrite.M3AtomizeInventoryNoIdeas "$(cygpath -m "$src")" "$(cygpath -m "$out/atomize")" \
    > "$out/run.log" 2>&1
  echo "atomize_exit=$?" >> "$out/run.log"
  python -I "$kit/atom_planes.py" planes "$(cygpath -m "$out/atomize")" "$(cygpath -m "$out/planes")" >> "$out/run.log" 2>&1
  echo "planes_exit=$?" >> "$out/run.log"
  prune "$out"
  echo "$name $(grep -o 'atomize_exit=[0-9]*\|planes_exit=[0-9]*' "$out/run.log" | tr '\n' ' ') atoms $(($(wc -l < "$out/planes/ATOM_HASHES.tsv" 2>/dev/null || echo 1) - 1))"
done
