#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
home="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
manifest="$home/m3/collections/signal-codegen/qualification/m3-compiler-signal-facts-20261022/SOURCE_MANIFEST.tsv"
last=''
count=0
while IFS=$'\t' read -r path before after git_blob resource || [[ -n "${path:-}" ]]; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$last" && "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || exit 10
    [[ -f "$home/$path" ]] || exit 11
    [[ "$(sha256sum "$home/$path" | cut -d' ' -f1)" == "$after" ]] || exit 12
    [[ "$(git -C "$home" hash-object "$path")" == "$git_blob" ]] || exit 13
    last="$path"
    count=$((count+1))
done < "$manifest"
[[ "$count" -eq 5 ]] || exit 14
echo "PASS M3JDK21 five canonical compiler fact Java source postimages"
echo "NOTE source receipt does not establish javac/JNI/JDK installed-image execution"
