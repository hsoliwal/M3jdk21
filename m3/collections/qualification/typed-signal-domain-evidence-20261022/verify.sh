#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/typed-signal-domain-evidence-20261022/SOURCE_MANIFEST.tsv"
count=0
previous=''
while IFS=$'\t' read -r path before after git_blob resource; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && -n "$after" && -n "$git_blob" && -n "$resource" ]] || {
    echo "FAIL: unsorted/incomplete source receipt at $path" >&2; exit 10;
  }
  [[ "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
    echo "FAIL: unsafe product path" >&2; exit 11;
  }
  [[ -f "$root/$path" ]] || { echo "FAIL: missing product source $path" >&2; exit 12; }
  [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL: changed source SHA-256 $path" >&2; exit 13;
  }
  [[ "$(git -C "$root" hash-object "$path")" == "$git_blob" ]] || {
    echo "FAIL: changed Git source blob $path" >&2; exit 14;
  }
  if [[ "$count" == 0 ]]; then
    [[ "$before" == f4caab98f766b076894e478444067c4245f61c6a2e660bda52f39c481af6e314 ]] || exit 15
  else
    [[ "$before" == ABSENT ]] || exit 16
  fi
  previous="$path"
  count=$((count+1))
done < "$manifest"
[[ "$count" -eq 4 ]] || { echo "FAIL: expected exactly four source postimages" >&2; exit 17; }
echo "PASS: M3JDK21 domain evidence source seals=4 and original typed-column preimage pinned"
echo "NOTE: source seals are not SDK_EXECUTION/JUnit/JNI/modified-JDK acceptance"
