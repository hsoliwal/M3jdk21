#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Immutable source handoff; edit the Synexia owner, never this generated snapshot.
set -euo pipefail
root="$(cd "${1:?repository root}" && pwd)"
mode="${2:-verify}"
module="$root/.m3/text-integration"
packet="$module/source-packet"
for dir in "$root/.m3" "$module" "$module/target" "$packet"; do
  [[ ! -L "$dir" ]] || { echo "symlink refused: $dir" >&2; exit 2; }
done
[[ -d "$packet" ]] || { echo 'missing generated source handoff' >&2; exit 2; }
[[ -z "$(find "$packet" -type l -print -quit)" ]] || { echo 'source symlink refused' >&2; exit 2; }
mkdir -p "$module/target/qualification/logs"
exec > >(tee "$module/target/qualification/logs/receiver.log") 2>&1
while IFS=$'\t' read -r ref source expected dest extra; do
  [[ "$ref" =~ ^[0-9a-f]{40}$ && "$expected" =~ ^[0-9a-f]{64}$ && -z "$extra" ]] || exit 2
  for path in "$source" "$dest"; do
    [[ "$path" =~ ^[a-zA-Z0-9_./-]+$ && "$path" != /* && "$path" != *..* ]] || exit 2
  done
  [[ -f "$packet/$dest" ]] || exit 2
  [[ "$(sha256sum "$packet/$dest" | cut -d' ' -f1)" == "$expected" ]] || {
    echo "canonical source drift: $source" >&2; exit 2;
  }
done < "$module/source-packet.tsv"
printf '%s\n' 'Verified immutable source handoff; no credentials or network source fetch.'
bash "$packet/qualify.sh" "$root" "$mode" "$packet"
