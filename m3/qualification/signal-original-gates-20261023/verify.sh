#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
manifest="$root/m3/qualification/signal-original-gates-20261023/SOURCE_MANIFEST.tsv"
count=0
previous=''
while IFS=$'\t' read -r path before after blob; do
  [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
  [[ "$path" > "$previous" && "$before" != ABSENT && "$path" != /* && "$path" != *'..'* ]] || {
    echo "FAIL: path or original-input contract error $path" >&2; exit 11;
  }
  [[ -f "$root/$path" ]] || { echo "FAIL: missing M3JDK21 owner $path" >&2; exit 12; }
  [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
    echo "FAIL: modified source digest drift at $path" >&2; exit 13;
  }
  [[ "$(git -C "$root" hash-object "$path")" == "$blob" ]] || {
    echo "FAIL: modified source Git blob drift at $path" >&2; exit 14;
  }
  count=$((count + 1))
  previous="$path"
done < "$manifest"
[[ "$count" -eq 2 ]] || { echo "FAIL: original-gate receiver expects two files" >&2; exit 15; }
echo "PASS: two M3JDK21 original-source Maven/JUnit metadata outputs match canonical hashes"
echo "NOTE: source seal does not imply original plugin/JUnit/JNI runtime execution"
