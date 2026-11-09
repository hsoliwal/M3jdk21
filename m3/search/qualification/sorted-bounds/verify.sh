#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Synexia contributors
# SPDX-License-Identifier: Apache-2.0
# Real host Java21 + C17/JNI sorted batch bounds qualification, NOT M3JDK product admission.
set -euo pipefail
here=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
jdk_home=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
java_sources=(
  "$here/src/main/java/jdk/m3/search/SortedIntBounds.java"
  "$here/src/main/java/jdk/m3/search/NativeSortedIntBounds.java"
  "$here/src/test/java/jdk/m3/search/SortedIntBoundsSelfTest.java"
  "$here/src/test/java/jdk/m3/search/SortedIntBoundsBenchmark.java"
)
mkdir -p "$work/classes" "$work/headers"
javac --release 21 -Xlint:all -Werror -h "$work/headers" -d "$work/classes" "${java_sources[@]}"
header="$work/headers/jdk_m3_search_NativeSortedIntBounds.h"
test -s "$header"
grep -F 'Java_jdk_m3_search_NativeSortedIntBounds_nativeBounds' "$header"
gcc -std=c17 -O3 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$jdk_home/include" -I"$jdk_home/include/linux" -I"$work/headers" \
  "$here/src/main/native/m3_native_sorted_bounds.c" -o "$work/libm3_sorted_bounds.so"
nm -D --defined-only "$work/libm3_sorted_bounds.so" | grep -F 'Java_jdk_m3_search_NativeSortedIntBounds_nativeBounds'
sha_of() { sha256sum "$1" | cut -d' ' -f1; }
native_sha=$(sha_of "$work/libm3_sorted_bounds.so")
printf 'M3_JNI_BINARY_SHA256\tC17_SOURCE\t%s\n' "$(sha_of "$here/src/main/native/m3_native_sorted_bounds.c")"
printf 'M3_JNI_BINARY_SHA256\tGENERATED_JNI_HEADER\t%s\n' "$(sha_of "$header")"
printf 'M3_JNI_BINARY_SHA256\tNATIVE_SHARED_LIBRARY\t%s\n' "$native_sha"
while IFS= read -r -d '' path; do
  printf 'M3_JNI_BINARY_SHA256\tJAVA_CLASS:%s\t%s\n' "${path#"$work/classes/"}" "$(sha_of "$path")"
done < <(find "$work/classes" -name '*.class' -type f -print0 | sort -z)
for mode in interp c1 tiered; do
  case "$mode" in
    interp) flags=(-Xint);;
    c1) flags=(-XX:TieredStopAtLevel=1);;
    tiered) flags=();;
  esac
  echo "M3_SEARCH_RUNTIME_STAGE=$mode"
  java "${flags[@]}" -Dm3.native.sorted.search.library="$work/libm3_sorted_bounds.so" \
    -cp "$work/classes" jdk.m3.search.SortedIntBoundsSelfTest
done
java -Dm3.native.sorted.search.library="$work/libm3_sorted_bounds.so" \
  -cp "$work/classes" jdk.m3.search.SortedIntBoundsBenchmark
if [[ "$(sha_of "$work/libm3_sorted_bounds.so")" != "$native_sha" ]]; then
  echo 'M3_NATIVE_LIBRARY_MUTATED_AFTER_RUNTIME' >&2
  exit 1
fi
printf 'M3_NATIVE_LIBRARY_FIXED_IDENTITY\tstatus=PASS\tsha256=%s\n' "$native_sha"
printf 'M3_HOST_JNI_PROOF\tstate=PASS\tproduct=HOLD\n'
