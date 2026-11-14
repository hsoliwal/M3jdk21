#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/indexpath-bulk-20261021/SOURCE_MANIFEST.tsv"
count=0
previous=''
while IFS=$'\t' read -r path before after git_blob; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && "$path" != /* && "$path" != *'..'* ]] || exit 10
  [[ -f "$root/$path" ]] || exit 11
  actual="$(sha256sum "$root/$path" | cut -d' ' -f1)"
  [[ "$actual" == "$after" ]] || { echo "source digest drift: $path" >&2; exit 12; }
  blob="$(git -C "$root" hash-object "$path")"
  [[ "$blob" == "$git_blob" ]] || { echo "source Git blob drift: $path" >&2; exit 13; }
  previous="$path"
  count=$((count + 1))
done < "$manifest"
[[ "$count" -eq 4 ]] || exit 14
echo "PASS: 4 exact M3JDK21 rank bulk source postimages; SDK_EXECUTION=NOT_RUN"
