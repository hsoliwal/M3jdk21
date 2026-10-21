#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
base="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
build="$(mktemp -d)"
trap 'rm -rf "$build"' EXIT
javac --release 21 -proc:none -Xlint:all -Werror -d "$build" \
  "$base/src/main/java/com/m3/mop/M3RecipeMemory.java" \
  "$base/src/test/java/com/m3/mop/M3RecipeMemoryContract.java"
java -cp "$build" com.m3.mop.M3RecipeMemoryContract
java -Xint -cp "$build" com.m3.mop.M3RecipeMemoryContract
