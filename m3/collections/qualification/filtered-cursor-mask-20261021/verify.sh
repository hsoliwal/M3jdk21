#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
manifest="$root/m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_MANIFEST.tsv"
[[ -f "$manifest" ]] || { echo "FAIL absent receiving manifest"; exit 11; }
[[ "$(git -C "$root" hash-object "m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_MANIFEST.tsv")" == "4ca33e23eabc23764f7e43f30cba9b753cbd0b1f" ]] || {
    echo "FAIL historic cursor manifest modified"; exit 18;
}
successor="$root/m3/compatibility/collection-views/source-successions.tsv"
[[ -f "$successor" && ! -L "$successor" ]] || {
    echo "FAIL missing reviewed source successor"; exit 19;
}
mapfile -t records < <(grep -vE '^(#|$)' "$successor")
expected=
while IFS=$'\t' read -r path before after blob resource || [[ -n "${path:-}" ]]; do
    [[ -z "${path:-}" || "${path:0:1}" == '#' ]] && continue
    [[ "$path" > "$previous" && -n "$resource" ]] || {
        echo "FAIL unsorted or incomplete receiving record $path"; exit 12;
    }
    [[ "$path" != /* && "$path" != *'..'* && "$resource" != */* ]] || {
        echo "FAIL unsafe receiving path"; exit 13;
    }
    [[ -f "$root/$path" ]] || { echo "FAIL absent product owner $path"; exit 14; }
    if [[ "$path" == "src/java.base/share/classes/java/util/CollectionViews.java" ]]; then
        [[ "$after" == "c8820e5bbd7b7c7b4c683f5081ec74b99a4aac6b854f6c8dc79d4e0ee4767ffb" && "$blob" == "2fe6e7ede2af18d4523f3a5531038e9a865cdb6d" ]] || {
            echo "FAIL invalid historical owner source"; exit 21;
        }
        after="793eb7de2a92ea1cc5c9311ad4e2360bedeeab3c96077146e59d5e16e3f66a65"
        blob="8840da98c37e2af8da36c63f2cacd6bdaf1cb0a8"
    fi
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
src/java.base/share/classes/java/util/CollectionViews.java\tc8820e5bbd7b7c7b4c683f5081ec74b99a4aac6b854f6c8dc79d4e0ee4767ffb\t2fe6e7ede2af18d4523f3a5531038e9a865cdb6d\t793eb7de2a92ea1cc5c9311ad4e2360bedeeab3c96077146e59d5e16e3f66a65\t8840da98c37e2af8da36c63f2cacd6bdaf1cb0a8\thsoliwal/com.synexia#13652\thsoliwal/M3jdk21#1891\tPENDING_FULL_JDK_QUALIFICATION'
[[ "${#records[@]}" -eq 1 && "${records[0]}" == "$expected" ]] || {
    echo "FAIL unreviewed source successor"; exit 20;
}
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
