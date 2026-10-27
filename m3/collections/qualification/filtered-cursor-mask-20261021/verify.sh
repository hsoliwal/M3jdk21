#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
# Verify original first-party products plus one reviewed JDK source successor.
# This proves source identity only: Java/JNI qualification runs separately.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_MANIFEST.tsv"
successor="$root/m3/compatibility/collection-views/source-successions.tsv"
[[ -f "$manifest" && ! -L "$manifest" ]] || { echo 'FAIL absent or linked historical manifest' >&2; exit 11; }
[[ "$(git -C "$root" hash-object "$manifest")" == '4ca33e23eabc23764f7e43f30cba9b753cbd0b1f' ]] || {
    echo 'FAIL historical manifest source drift' >&2; exit 18;
}
[[ -f "$successor" && ! -L "$successor" ]] || { echo 'FAIL absent or linked successor' >&2; exit 19; }
expected_successor=$'src/java.base/share/classes/java/util/CollectionViews.java\tc8820e5bbd7b7c7b4c683f5081ec74b99a4aac6b854f6c8dc79d4e0ee4767ffb\t2fe6e7ede2af18d4523f3a5531038e9a865cdb6d\t793eb7de2a92ea1cc5c9311ad4e2360bedeeab3c96077146e59d5e16e3f66a65\t8840da98c37e2af8da36c63f2cacd6bdaf1cb0a8\thsoliwal/com.synexia#13652\thsoliwal/M3jdk21#1891\tPENDING_FULL_JDK_QUALIFICATION'
records=()
while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "${line:0:1}" == '#' ]] && continue
    records+=("$line")
done < "$successor"
[[ "${#records[@]}" -eq 1 && "${records[0]}" == "$expected_successor" ]] || {
    echo 'FAIL unreviewed source successor' >&2; exit 20;
}
IFS=$'\t' read -r successor_path successor_before successor_before_blob successor_after successor_after_blob successor_recipe successor_pr successor_state <<< "${records[0]}"
count=0
existing=0
replaced=0
previous=''
while IFS=$'\t' read -r path before after blob resource || [[ -n "${path:-}" ]]; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$previous" && -n "$before" && -n "$after" && -n "$blob" && -n "$resource" ]] || {
        echo "FAIL unsorted or incomplete receiving record $path" >&2; exit 12;
    }
    [[ "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
        echo "FAIL unsafe receiving path $path" >&2; exit 13;
    }
    [[ -f "$root/$path" && ! -L "$root/$path" ]] || {
        echo "FAIL absent or linked product owner $path" >&2; exit 14;
    }
    if [[ "$path" == "$successor_path" ]]; then
        [[ "$after" == "$successor_before" && "$blob" == "$successor_before_blob" ]] || {
            echo 'FAIL historical JDK owner does not match reviewed predecessor' >&2; exit 21;
        }
        after="$successor_after"
        blob="$successor_after_blob"
        replaced=$((replaced + 1))
    fi
    [[ "$(sha256sum "$root/$path" | cut -d' ' -f1)" == "$after" ]] || {
        echo "FAIL SHA-256 product drift $path" >&2; exit 15;
    }
    [[ "$(git -C "$root" hash-object "$root/$path")" == "$blob" ]] || {
        echo "FAIL Git blob drift $path" >&2; exit 16;
    }
    [[ "$before" == ABSENT ]] || existing=$((existing + 1))
    previous="$path"
    count=$((count + 1))
done < "$manifest"
[[ "$count" -eq 8 && "$existing" -eq 4 && "$replaced" -eq 1 ]] || {
    echo "FAIL expected eight products, four modified owners and one successor: count=$count existing=$existing successor=$replaced" >&2; exit 17;
}
echo 'M3_FILTERED_CURSOR_RECEIVER_PASS files=8 existing=4 successor=1'
echo 'NOTE: source identity is NOT a Java, JNI, jtreg or installed-JDK runtime pass'
