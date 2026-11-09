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
for family in arrays collections; do
  find "$repo/m3/$family/src/main/java" -type f -name '*.java' -print0 \
    | LC_ALL=C sort -z > "$out/$family.sources"
  mapfile -d '' -t sources < "$out/$family.sources"
  if [[ "${#sources[@]}" -eq 0 ]]; then
    echo "no main Java sources for $family" >&2
    exit 2
  fi
  "$java_home/bin/javac" --release 21 -proc:none -Xlint:all,-module -Werror \
    --module-path "$out" -d "$out/jdk.m3.$family" "${sources[@]}"
done
echo 'M3_FILE_DEPENDENCY_MODULES_PASS'
