#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Sync the generator into both trees, commit and push one family batch. Usage: push_batch.sh <title> <body-file>
set -euo pipefail
title=$1; body=$2; W=H:/m3-scratch/webwidgets
bash "$W/gen/tools/sync.sh" "$W/gen" "$W" C:/m3-superset/nebula-webwidgets/widgets/m3web "$W/m3jdk21/m3/ports/swing-viewport" >/dev/null
for repo in "$W/m3jdk21:m3/ports/swing-viewport:m3/swing-viewport" "C:/m3-superset/nebula-webwidgets:widgets/m3web:m3web"; do
  IFS=: read -r dir path prefix <<<"$(echo "$repo" | sed 's|^\([A-Z]\):|\1@|')"; dir=${dir/@/:}
  { echo "$prefix: $title"; echo; cat "$body"; } > "$W/.msg"
  git -C "$dir" -c core.autocrlf=false add -A "$path" 2>/dev/null
  if git -C "$dir" diff --cached --quiet; then echo "nothing to commit in $dir"; continue; fi
  git -C "$dir" commit -q -F "$W/.msg"
  git -C "$dir" push -q origin m3/webwidgets-scaffold-20261030 2>&1 | grep -v "^remote:" || true
  echo "$(git -C "$dir" log -1 --format=%H) $dir"
done
