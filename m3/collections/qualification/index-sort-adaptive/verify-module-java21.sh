#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Synexia recipe candidate; product runs this proof against its own real source.
set -euo pipefail
if [[ "$#" -ne 1 ]]; then
  echo 'usage: verify-module-java21.sh M3JDK21_ROOT' >&2
  exit 2
fi
root="$(cd "$1" && pwd -P)"
array_src="$root/m3/arrays/src/main/java"
collection_src="$root/m3/collections/src/main/java"
collection_tests="$root/m3/collections/src/test/java/com/m3/collections"
for dir in "$array_src" "$collection_src" "$collection_tests"; do
  [[ -d "$dir" ]] || { echo "missing original source directory: $dir" >&2; exit 1; }
done
[[ -f "$array_src/module-info.java" ]] || { echo 'missing com.m3.arrays module-info' >&2; exit 1; }
[[ -f "$collection_src/module-info.java" ]] || { echo 'missing com.m3.collections module-info' >&2; exit 1; }
javac="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
version="$($javac -version 2>&1)"
[[ "$version" =~ ^javac[[:space:]]21([.]|$) ]] || { echo "requires JDK 21: $version" >&2; exit 1; }
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
mkdir -p "$scratch/arrays" "$scratch/collections" "$scratch/tests"
find "$array_src" -type f -name '*.java' | LC_ALL=C sort > "$scratch/arrays.sources"
find "$collection_src" -type f -name '*.java' | LC_ALL=C sort > "$scratch/collections.sources"
[[ -s "$scratch/arrays.sources" && -s "$scratch/collections.sources" ]] || { echo 'empty owner source census' >&2; exit 1; }
# Original Java module descriptors are authoritative: no bypass or classpath flattening.
"$javac" --release 21 -Xlint:all,-module -Werror \
  -d "$scratch/arrays" @"$scratch/arrays.sources"
"$javac" --release 21 -Xlint:all,-module -Werror \
  --module-path "$scratch/arrays" \
  -d "$scratch/collections" @"$scratch/collections.sources"
"$java" --module-path "$scratch/arrays:$scratch/collections" \
  --describe-module com.m3.collections > "$scratch/descriptor.txt"
grep -Fxq 'requires com.m3.arrays' "$scratch/descriptor.txt" || {
  echo 'original collections module no longer declares arrays dependency' >&2; exit 1;
}
# Compile standalone contract oracles as patched module tests, including package-private owner.
"$javac" --release 21 -Xlint:all,-module -Werror \
  --module-path "$scratch/arrays:$scratch/collections" \
  --patch-module "com.m3.collections=$root/m3/collections/src/test/java" \
  -d "$scratch/tests" \
  "$collection_tests/M3IndexSortAdaptiveContract.java" \
  "$collection_tests/M3IndexSortStrictContract.java"
for mode in normal interpreted; do
  opts=()
  if [[ "$mode" = interpreted ]]; then opts=(-Xint); fi
  for entry in 'M3IndexSortAdaptiveContract:6576:ADAPTIVE' 'M3IndexSortStrictContract:9:STRICT'; do
    IFS=: read -r clazz expected label <<< "$entry"
    "$java" "${opts[@]}" --module-path "$scratch/arrays:$scratch/collections" \
      --patch-module "com.m3.collections=$scratch/tests" \
      -m "com.m3.collections/com.m3.collections.$clazz" \
      | tee "$scratch/$clazz.$mode.txt"
    grep -Fxq "PASS M3_INDEX_SORT_${label} checks=$expected" "$scratch/$clazz.$mode.txt"
  done
done
cmp "$scratch/M3IndexSortAdaptiveContract.normal.txt" "$scratch/M3IndexSortAdaptiveContract.interpreted.txt"
cmp "$scratch/M3IndexSortStrictContract.normal.txt" "$scratch/M3IndexSortStrictContract.interpreted.txt"
echo 'PASS M3_INDEX_SORT_MODULE_SOURCE_AND_STRICT_CONTRACT Java21 module-path=arrays,collections'
