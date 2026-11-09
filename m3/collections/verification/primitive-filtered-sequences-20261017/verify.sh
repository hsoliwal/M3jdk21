#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/../../../.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
arrays="$work/arrays"
collections="$work/collections"
mkdir -p "$arrays" "$collections"
sep="${M3_PATH_SEPARATOR:-:}"
# Build the actual original jdk.m3.arrays module, not a synthetic facade. Its module-info stays in m3/arrays/src/main/java;
# since the arrays area moved into the JDK module tree (MOVE_TO_JDK_MODULE v2, #2825) the product sources live in
# src/jdk.m3.collections/share/classes/jdk/m3/arrays (before the move both were under m3/arrays/src/main/java).
arrays_area=src/jdk.m3.collections/share/classes/jdk/m3/arrays
mapfile -d '' array_sources < <({ find m3/arrays/src/main/java -type f -name '*.java' -print0
  if [[ -d "$arrays_area" ]]; then find "$arrays_area" -type f -name '*.java' -print0; fi; } | sort -z)
if [[ "${#array_sources[@]}" -lt 2 ]]; then
  echo 'missing original jdk.m3.arrays source closure' >&2
  exit 1
fi
printf '%s\n' "${array_sources[@]}" > "$work/arrays.args"
javac --release 21 -proc:none -Xlint:all,-module -Werror -d "$arrays" @"$work/arrays.args"
test -s "$arrays/module-info.class"
# Preserve the original module-info and resolve its real 'requires jdk.m3.arrays'. The whole jdk.m3.collections source set
# is compiled: module-info exports the family packages (bits, concurrent, graph, packed, range, sketch, text), which must
# hold classes, and the product owner moved with the curation passes (A1 #2781 to jdk.internal.m3.collections, CURATE_E
# E1/E2 dropping the M3 prefix), so no single source path is pinned here. The runtime contract is found by its name.
src=m3/collections/src/main/java
mapfile -d '' collection_sources < <(find "$src" -type f -name '*.java' -print0 | sort -z)
contract="$(find m3/collections/src/test/java -type f -name '*PrimitiveFilteredSequencesRuntimeContract.java' | sort | head -1)"
if [[ -z "$contract" ]]; then
  echo 'missing primitive filtered sequences runtime contract' >&2
  exit 1
fi
printf '%s\n' "${collection_sources[@]}" "$contract" > "$work/collections.args"  # an argfile: about 900 sources
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  --module-path "$arrays" -d "$collections" @"$work/collections.args"
test -s "$collections/module-info.class"
main="$(sed -E 's#^m3/collections/src/test/java/##; s#\.java$##' <<< "$contract")"
test -s "$collections/$main.class"
java -Xmx256m --module-path "$arrays$sep$collections" --module "jdk.m3.collections/${main//\//.}"
