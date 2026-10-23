#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo "FAIL absent receiving manifest"; exit 11; }
count=0
existing=0
previous=''
while IFS=$'\t' read -r path before after blob resource || [[ -n "${path:-}" ]]; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$previous" && -n "$resource" ]] || {
        echo "FAIL unsorted or incomplete receiving record $path"; exit 12;
    }
    [[ "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
        echo "FAIL unsafe receiving path"; exit 13;
    }
    [[ -f "$root/$path" ]] || { echo "FAIL absent product owner $path"; exit 14; }
    [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
        echo "FAIL SHA-256 product drift $path"; exit 15;
    }
    [[ "$(git -C "$root" hash-object "$path")" == "$blob" ]] || {
        echo "FAIL Git blob drift $path"; exit 16;
    }
    [[ "$before" == ABSENT ]] || existing=$((existing+1))
    previous="$path"
    count=$((count+1))
done < "$manifest"
[[ "$count" -eq 8 && "$existing" -eq 4 ]] || {
    echo "FAIL expected 8 postimages and 4 modified current owners"; exit 17;
}
echo "PASS: eight exact M3JDK21 Java/JNI-preserving cursor source postimages"
echo "NOTE: source identity is not JVM compilation, JNI, jtreg or SDK_EXECUTION evidence"
