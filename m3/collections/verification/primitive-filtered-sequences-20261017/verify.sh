#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/../../../.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
arrays="$work/arrays"
collections="$work/collections"
mkdir -p "$arrays" "$collections"
# Build the actual original com.m3.arrays module, not a synthetic facade.
mapfile -d '' array_sources < <(find m3/arrays/src/main/java -type f -name '*.java' -print0 | sort -z)
if [[ "${#array_sources[@]}" -lt 2 ]]; then
  echo 'missing original com.m3.arrays source closure' >&2
  exit 1
fi
javac --release 21 -proc:none -Xlint:all,-module -Werror -d "$arrays" "${array_sources[@]}"
test -s "$arrays/module-info.class"
src=m3/collections/src/main/java
# Preserve original module-info and resolve its real 'requires com.m3.arrays'.
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  --module-path "$arrays" \
  -sourcepath "$src:m3/collections/src/test/java" -d "$collections" \
  "$src/com/m3/collections/M3PrimitiveFilteredSequences.java" \
  "m3/collections/src/test/java/com/m3/collections/M3PrimitiveFilteredSequencesRuntimeContract.java"
test -s "$collections/module-info.class"
test -s "$collections/com/m3/collections/M3PrimitiveFilteredSequencesRuntimeContract.class"
java -Xmx256m --module-path "$arrays:$collections" \
  --module com.m3.collections/com.m3.collections.M3PrimitiveFilteredSequencesRuntimeContract
