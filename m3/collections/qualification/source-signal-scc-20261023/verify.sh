#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
directory="$root/m3/collections/qualification/source-signal-scc-20261023"
previous=''
count=0
while IFS=$'\t' read -r path before after git_blob resource; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$previous" && "$before" == ABSENT && "$path" != /* &&
       "$path" != *'..'* && "$resource" != */* ]] || {
        echo "FAIL: unsafe or nonadditive SCC source target $path" >&2; exit 11;
    }
    [[ -f "$root/$path" ]] || { echo "FAIL: missing target $path" >&2; exit 12; }
    [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
        echo "FAIL: target source SHA-256 drift at $path" >&2; exit 13;
    }
    [[ "$(git -C "$root" hash-object "$path")" == "$git_blob" ]] || {
        echo "FAIL: SCC source Git blob mismatch at $path" >&2; exit 14;
    }
    previous="$path"
    count=$((count+1))
done < "$directory/SOURCE_MANIFEST.tsv"
[[ "$count" -eq 3 ]] || { echo "FAIL: expected three SCC Java source postimages" >&2; exit 15; }
owners=0
while IFS=$'\t' read -r path blob disposition; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$disposition" == UNCHANGED && -f "$root/$path" ]] || {
        echo "FAIL: missing or unreviewed original source owner $path" >&2; exit 16;
    }
    [[ "$(git -C "$root" hash-object "$path")" == "$blob" ]] || {
        echo "FAIL: SCC source-owner input drift $path" >&2; exit 17;
    }
    owners=$((owners+1))
done < "$directory/SOURCE_OWNER_PINS.tsv"
[[ "$owners" -eq 4 ]] || { echo "FAIL: expected four original source-owner pins" >&2; exit 18; }
echo "PASS source custody: 3 exact additive SCC postimages, 4 unchanged source owners"
echo "NOTE: source identities do not establish compiler/VM/JUnit/JNI gate success"
