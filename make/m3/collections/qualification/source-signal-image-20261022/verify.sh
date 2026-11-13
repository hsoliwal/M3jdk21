#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/source-signal-image-20261022/SOURCE_MANIFEST.tsv"
previous=''
count=0
while IFS=$'\t' read -r path before after git_blob resource; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && "$before" == ABSENT && "$path" != /* && "$path" != *'..'* ]] || {
    echo "FAIL: unsafe/unordered/modified source preimage $path"; exit 11;
  }
  [[ -f "$root/$path" ]] || { echo "FAIL: missing received file $path"; exit 12; }
  [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL: SHA256 source drift $path"; exit 13;
  }
  [[ "$(git -C "$root" hash-object "$path")" == "$git_blob" ]] || {
    echo "FAIL: source Git blob mismatch $path"; exit 14;
  }
  count=$((count+1))
  previous="$path"
done < "$manifest"
[[ "$count" -eq 4 ]] || { echo "FAIL: expected four sealed Java21 source outputs"; exit 15; }
echo "PASS: four exact M3JDK21 compiler-image target postimages"
echo "NOTE: source custody does not imply JVM/JNI/runtime success"
