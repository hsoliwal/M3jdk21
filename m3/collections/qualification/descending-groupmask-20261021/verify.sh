#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$repo_root/m3/collections/qualification/descending-groupmask-20261021/SOURCE_MANIFEST.tsv"
previous=''
count=0
while IFS=$'\t' read -r path before after git_blob; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && "$path" != /* && "$path" != *'..'* ]] || {
    echo "FAIL: unsafe or out-of-order product source $path" >&2; exit 10;
  }
  [[ -f "$repo_root/$path" ]] || { echo "FAIL: absent product source $path" >&2; exit 11; }
  [[ "$(sha256sum "$repo_root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL: product SHA-256 drift $path" >&2; exit 12;
  }
  [[ "$(git -C "$repo_root" hash-object "$path")" == "$git_blob" ]] || {
    echo "FAIL: product Git blob drift $path" >&2; exit 13;
  }
  previous="$path"
  count=$((count+1))
done < "$manifest"
[[ "$count" -eq 3 ]] || { echo "FAIL: expected three product sources" >&2; exit 14; }
echo "PASS: three source-sealed M3JDK21 indexed descending group-mask files"
echo "NOTE: no product build or JNI success implied by source hash verification"
