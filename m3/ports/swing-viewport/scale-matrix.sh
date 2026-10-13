#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Runs the headless scale matrix, one JVM at a time, and appends TSV rows to $OUT_TSV.
# Memory gate (Windows): skips a run when commit headroom is below 8 GB.
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
out=${OUT_TSV:-"$here/target/scale-results.tsv"}
mkdir -p "$(dirname "$out")"
gate() {
  command -v powershell >/dev/null 2>&1 || return 0
  local mb; mb=$(powershell -NoProfile -Command "[int]((Get-CimInstance Win32_OperatingSystem).FreeVirtualMemory/1024)" | tr -d '\r')
  [ "${mb:-0}" -ge 8192 ] || { echo "GATE_SKIP headroom=${mb}MB" >&2; return 1; }
}
run() { # xmx mode items
  gate || { printf 'SKIPPED_MEMORY_GATE\t%s\t%s\n' "$2" "$3" >> "$out"; return; }
  M3_SCALE_XMX=$1 bash "$here/run-tests.sh" scale "$2" "$3" > "$out.tmp" 2>&1
  local rc=$?
  if [ $rc -eq 0 ]; then tail -n 1 "$out.tmp" | sed "s/^/xmx=$1\t/" >> "$out"; else printf 'FAILED rc=%s\txmx=%s\t%s\t%s\t%s\n' "$rc" "$1" "$2" "$3" "$(tail -n 3 "$out.tmp" | tr '\n\t' '  ')" >> "$out"; fi
}
[ -f "$out" ] || printf 'xmx\tmode\titems\trows\tbuild_ms\theap_after_gc_mb\tframe_p50_ms\tframe_p95_ms\talloc_bytes_per_frame_p50\talloc_bytes_per_frame_max\tmaterialized_per_paint\textra\n' > "$out"
specs=(256m:table-virtual:1000000 256m:table-virtual:10000000 256m:list-virtual:1000000 256m:list-virtual:10000000
  256m:list-virtual:100000000 256m:tree-virtual:1000,1000 256m:tree-virtual:1000,1000,1000
  2g:table-packed:1000000 2g:table-packed:10000000 2g:list-packed:1000000 2g:list-packed:10000000
  2g:table-default:1000000 2g:list-default:1000000 2g:tree-default:1000,1000)
if [ -n "${MATRIX:-}" ]; then read -ra specs <<< "$MATRIX"; fi
for spec in "${specs[@]}"; do
  IFS=: read -r xmx mode items <<< "$spec"
  run "$xmx" "$mode" "$items"
done
echo DONE
