#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# CPU-only inventory of Swing/AWT object-heavy, paint-materializing and work-while-hidden sites.
# Usage: inventory.sh <java.desktop/share/classes root> [PATTERNS.tsv] > INVENTORY.tsv
# Every row is one rg hit; zero hits for a pattern emit a MISSING row so drift is visible.
set -euo pipefail
root=${1:?java.desktop share/classes root}
patterns=${2:-$(dirname "$0")/PATTERNS.tsv}
printf 'id\tlaw\tkind\tfile\tline\tsha256_12\tmatch\tnote\n'
tail -n +2 "$patterns" | while IFS=$'\t' read -r id law kind file regex note; do
  path="$root/$file"
  if [ ! -f "$path" ]; then
    printf '%s\t%s\t%s\t%s\t0\tABSENT\t\t%s\n' "$id" "$law" "$kind" "$file" "$note"; continue
  fi
  sha=$(sha256sum "$path" | cut -c1-12)
  if command -v rg >/dev/null 2>&1; then hits=$(rg -n --no-heading -e "$regex" "$path" || true)
  else hits=$(grep -nE -e "$regex" "$path" || true); fi
  if [ -z "$hits" ]; then
    printf '%s\t%s\t%s\t%s\t0\t%s\tMISSING\t%s\n' "$id" "$law" "$kind" "$file" "$sha" "$note"; continue
  fi
  printf '%s\n' "$hits" | while IFS= read -r hit; do
    line=${hit%%:*}; text=${hit#*:}
    text=$(printf '%s' "$text" | sed -e 's/^[[:space:]]*//' -e 's/\t/ /g' | cut -c1-120)
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$law" "$kind" "$file" "$line" "$sha" "$text" "$note"
  done
done
