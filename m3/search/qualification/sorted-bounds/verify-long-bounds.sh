#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Synexia contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
here=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
jdk_home=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
mkdir -p "$work/classes" "$work/headers"
mapfile -t java_files < <(find "$here/src/main/java" "$here/src/test/java" -name 'M3*Sorted*Bounds*.java' -type f | LC_ALL=C sort)
javac --release 21 -Xlint:all -Werror -h "$work/headers" -d "$work/classes" "${java_files[@]}"
header="$work/headers/jdk_m3_search_M3NativeSortedLongBounds.h"
test -s "$header"
grep -F 'Java_jdk_m3_search_M3NativeSortedLongBounds_nativeBounds' "$header"
gcc -std=c17 -O2 -Wall -Wextra -Werror -Wpedantic -fPIC -shared \
  -I"$jdk_home/include" -I"$jdk_home/include/linux" -I"$work/headers" \
  "$here/src/main/native/m3_native_sorted_long_bounds.c" -o "$work/libm3_sorted_long_bounds.so"
nm -D --defined-only "$work/libm3_sorted_long_bounds.so" | grep -F 'Java_jdk_m3_search_M3NativeSortedLongBounds_nativeBounds'
sha_of() { sha256sum "$1" | cut -d' ' -f1; }
native_hash=$(sha_of "$work/libm3_sorted_long_bounds.so")
printf 'M3_LONG_JNI_SHA256\tC17_SOURCE\t%s\n' "$(sha_of "$here/src/main/native/m3_native_sorted_long_bounds.c")"
printf 'M3_LONG_JNI_SHA256\tJNI_HEADER\t%s\n' "$(sha_of "$header")"
printf 'M3_LONG_JNI_SHA256\tNATIVE_SHARED_LIBRARY\t%s\n' "$native_hash"
while IFS= read -r -d '' entry; do
  printf 'M3_LONG_JNI_SHA256\tJAVA_CLASS:%s\t%s\n' "${entry#"$work/classes/"}" "$(sha_of "$entry")"
done < <(find "$work/classes" -name '*.class' -type f -print0 | LC_ALL=C sort -z)
echo "M3_SORTED_LONG_STAGE=invalid-without-library"
java -cp "$work/classes" jdk.m3.search.M3SortedLongBoundsSelfTest invalid-without-library
for mode in interp c1 tiered; do
  case "$mode" in
    interp) flags=(-Xint);;
    c1) flags=(-XX:TieredStopAtLevel=1);;
    tiered) flags=();;
  esac
  echo "M3_SORTED_LONG_STAGE=$mode"
  java "${flags[@]}" -Dm3.native.sorted.long.search.library="$work/libm3_sorted_long_bounds.so" \
    -cp "$work/classes" jdk.m3.search.M3SortedLongBoundsSelfTest
done
java -Dm3.native.sorted.long.search.library="$work/libm3_sorted_long_bounds.so" \
  -cp "$work/classes" jdk.m3.search.M3SortedLongBoundsBenchmark
if [[ "$native_hash" != "$(sha_of "$work/libm3_sorted_long_bounds.so")" ]]; then
  echo 'NATIVE_LIB_MUTATED_AFTER_EXECUTION' >&2
  exit 1
fi
printf 'M3_LONG_JNI_HOST\tstate=PASS\tproduct=HOLD\tsha256=%s\n' "$native_hash"
