#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
manifest="$root/m3/compiler/source-signals/qualification/method-v2-20261022/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo "FAIL missing method-source manifest"; exit 10; }
previous=''
count=0
existing=0
while IFS=$'\t' read -r path before after blob resource; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$previous" && "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
        echo "FAIL unsafe/unordered method source $path" >&2; exit 11;
    }
    [[ -f "$root/$path" ]] || { echo "FAIL missing received method source $path"; exit 12; }
    [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
        echo "FAIL method source sha256 drift $path"; exit 13;
    }
    [[ "$(git -C "$root" hash-object "$path")" == "$blob" ]] || {
        echo "FAIL method source Git blob drift $path"; exit 14;
    }
    if [[ "$before" != ABSENT ]]; then
        [[ "$before" =~ ^[0-9a-f]{64}$ ]] || exit 15
        existing=$((existing + 1))
    fi
    count=$((count + 1))
    previous="$path"
done < "$manifest"
[[ "$count" -eq 6 && "$existing" -eq 3 ]] || {
    echo "FAIL expected 6 Java outputs and 3 modified existing owners"; exit 16;
}
echo "PASS: six exactly received original M3JDK21 /2 source outputs (3 original owners)"
echo "NOTE: source seals alone do not certify compiler/JUnit/JNI/installed JDK behavior"
