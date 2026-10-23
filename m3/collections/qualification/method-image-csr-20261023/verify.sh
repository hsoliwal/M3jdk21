#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/method-image-csr-20261023/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo "FAIL absent source manifest"; exit 11; }

previous=''
count=0
while IFS=$'\t' read -r path before after blob resource || [[ -n "${path:-}" ]]; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$previous" && "$before" == ABSENT && -n "$resource" ]] || {
        echo "FAIL: unsorted, nonadditive or incomplete source record $path" >&2; exit 12;
    }
    [[ "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
        echo "FAIL unsafe path $path" >&2; exit 13;
    }
    [[ -f "$root/$path" ]] || { echo "FAIL missing product source $path" >&2; exit 14; }
    actual="$(sha256sum "$root/$path" | cut -d' ' -f1)"
    [[ "$actual" == "$after" ]] || {
        echo "FAIL product SHA-256 drift $path" >&2; exit 15;
    }
    object="$(git -C "$root" hash-object "$path")"
    [[ "$object" == "$blob" ]] || { echo "FAIL Git blob drift $path" >&2; exit 16; }
    previous="$path"
    count=$((count + 1))
done < "$manifest"
[[ "$count" -eq 3 ]] || { echo "FAIL expected 3 original-owner postimages"; exit 17; }
echo "PASS source custody: exact three M3JDK21 method-image Java21 postimages"
echo "NOTE source identity does not imply javac/JUnit/JNI/installed-JDK acceptance"
