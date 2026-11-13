#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/../../../../.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
classes="$work/classes"
mkdir -p "$classes" "$work/empty-classpath"
sep="${M3_PATH_SEPARATOR:-:}"
# Arrays, algorithms and collections now belong to this single product module.
# Compile its complete original source tree and module descriptor; do not revive
# the historical split-module Maven descriptors or synthesize a replacement.
source_root=src/jdk.m3.collections/share/classes
contract=test/jdk/jdk/m3/collections/PrimitiveFilteredSequencesRuntimeContract.java
if [[ ! -f "$source_root/module-info.java" || ! -f "$contract" ]]; then
  echo 'missing current jdk.m3.collections module or primitive sequence contract' >&2
  exit 1
fi
mapfile -d '' sources < <(find "$source_root" -type f -name '*.java' -print0 | sort -z)
if [[ "${#sources[@]}" -lt 2 ]]; then
  echo 'missing current jdk.m3.collections source closure' >&2
  exit 1
fi
printf '%s\n' "${sources[@]}" "$contract" > "$work/sources.args"
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  --class-path "$work/empty-classpath" --source-path "$source_root${sep}test/jdk" \
  -d "$classes" @"$work/sources.args"
test -s "$classes/module-info.class"
test -s "$classes/jdk/m3/collections/PrimitiveFilteredSequencesRuntimeContract.class"
java -ea -Xmx256m --module-path "$classes" \
  --module jdk.m3.collections/jdk.m3.collections.PrimitiveFilteredSequencesRuntimeContract
