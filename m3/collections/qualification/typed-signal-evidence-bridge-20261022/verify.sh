#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/typed-signal-evidence-bridge-20261022/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo "FAIL: source manifest absent"; exit 11; }
previous=''
count=0
existing=0
while IFS=$'\t' read -r path before after git_blob resource || [[ -n "${path:-}" ]]; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && -n "$resource"
      && "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
    echo "FAIL: unexpected receiving path or manifest order"; exit 12;
  }
  [[ -f "$root/$path" ]] || { echo "FAIL: missing target $path"; exit 13; }
  [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL: SHA256 postimage drift $path"; exit 14;
  }
  [[ "$(git -C "$root" hash-object "$path")" == "$git_blob" ]] || {
    echo "FAIL: Git blob postimage drift $path"; exit 15;
  }
  [[ "$before" == ABSENT ]] || existing=$((existing + 1))
  previous="$path"
  count=$((count+1))
done < "$manifest"
[[ "$count" -eq 4 && "$existing" -eq 1 ]] || {
  echo "FAIL: expected one source replacement and three additive Java targets"; exit 16;
}
echo "PASS: 4 exact M3JDK21 typed-progressive signal outputs, one reviewed current preimage"
echo "NOTE: source custody does not establish Maven/JUnit, source-built JDK or JNI acceptance"
