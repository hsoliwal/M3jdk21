#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
manifest="$root/m3/compiler/source-signals/qualification/method-signals-20261022/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo 'FAIL missing method signal manifest' >&2; exit 11; }
previous=''
count=0
old=0
while IFS=$'\t' read -r path before after blob resource || [[ -n "${path:-}" ]]; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ -n "$resource" && "$path" > "$previous" && "$path" != /* && "$path" != *'..'* ]] || {
    echo "FAIL unsafe or unsorted path $path" >&2; exit 12;
  }
  [[ -f "$root/$path" ]] || { echo "FAIL missing output $path" >&2; exit 13; }
  [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL current method Java21 source SHA256 drift $path" >&2; exit 14;
  }
  [[ "$(git -C "$root" hash-object "$path")" == "$blob" ]] || {
    echo "FAIL source Git blob mismatch $path" >&2; exit 15;
  }
  if [[ "$before" != ABSENT ]]; then old=$((old + 1)); fi
  previous="$path"
  count=$((count + 1))
done < "$manifest"
[[ "$count" -eq 5 && "$old" -eq 1 ]] || { echo 'FAIL expected five targets and one current plugin preimage' >&2; exit 16; }
echo 'PASS: five exact M3JDK21 method signal original-owner SHA256/Git postimages'
echo 'NOTE: source checksum does not establish compiler/JUnit/JNI/installed-JDK success'
