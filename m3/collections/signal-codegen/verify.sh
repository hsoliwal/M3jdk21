#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes"
find src/main/java -name '*.java' -print | LC_ALL=C sort > "$work/main.sources"
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" @"$work/main.sources"
find src/test/java -name '*.java' -print | LC_ALL=C sort > "$work/test.sources"
javac --release 21 -proc:none -Xlint:all -Werror -cp "$work/classes" \
    -d "$work/classes" @"$work/test.sources"
java -ea -cp "$work/classes" com.m3.collections.sortable.SortableProcessorSelfTest
