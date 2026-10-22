#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/method-signal-image-20261022/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo "FAIL missing method image manifest"; exit 11; }
previous=''
count=0
while IFS=$'\t' read -r path before after git_blob canonical || [[ -n "${path:-}" ]]; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && "$path" != /* && "$path" != *'..'* ]] || {
    echo "FAIL unsorted or unsafe source path $path"; exit 12;
  }
  [[ "$before" == ABSENT && -n "$canonical" && -f "$root/$path" ]] || {
    echo "FAIL unexpected preimage or missing product file $path"; exit 13;
  }
  [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL product SHA256 source drift $path"; exit 14;
  }
  [[ "$(git -C "$root" hash-object "$path")" == "$git_blob" ]] || {
    echo "FAIL received Git blob identity drift $path"; exit 15;
  }
  previous="$path"
  count=$((count+1))
done < "$manifest"
[[ "$count" -eq 4 ]] || { echo "FAIL expected four source-sealed Java/POM outputs"; exit 16; }
echo "PASS four exact original M3JDK21 method image Java/POM postimages"
echo "SOURCE_CHECK_ONLY; Maven/OpenRewrite/JNI/Jtreg still require independent execution"
