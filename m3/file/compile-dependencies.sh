#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Build the existing dependency modules for the file contract workflows.
set -euo pipefail
if [[ "$#" -ne 2 ]]; then
  echo 'usage: compile-dependencies.sh <java-home> <new-output-directory>' >&2
  exit 2
fi
java_home=$1
out=$2
if [[ -e "$out" || -L "$out" ]]; then
  echo 'dependency output already exists; use a new directory' >&2
  exit 2
fi
repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
mkdir -p "$out"
# arrays and collections are one JDK module since the approved layout 2026-11-08 (MOVE_TO_JDK_MODULE, #2825 and
# 754d737f0): src/jdk.m3.collections/share/classes holds the jdk.m3.arrays* and jdk.m3.collections* packages and the
# module descriptor; m3/arrays and m3/collections keep only Maven bridge descriptors, which this build does not use.
for module in jdk.m3.collections; do
  find "$repo/src/$module/share/classes" -type f -name '*.java' -print0 \
    | LC_ALL=C sort -z > "$out/$module.sources"
  mapfile -d '' -t sources < "$out/$module.sources"
  if [[ "${#sources[@]}" -eq 0 ]]; then
    echo "no main Java sources for $module" >&2
    exit 2
  fi
  "$java_home/bin/javac" --release 21 -proc:none -Xlint:all,-module -Werror \
    --module-path "$out" -d "$out/$module" "${sources[@]}"
done
echo 'M3_FILE_DEPENDENCY_MODULES_PASS'
