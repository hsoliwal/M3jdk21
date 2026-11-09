#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# build_check.sh <repo root> <build dir> [extra cmake configure arguments...]
#
# NATIVE_BUILD_CHECK v1 (2026-11-09): the N1 gate of NATIVE_ABSORB. It configures and builds make/m3/collections,
# runs export_check.sh against the census in this directory and runs mutant_check.sh. JAVA_HOME must name a JDK 21.
# Windows (MinGW): pass -G Ninja -DCMAKE_C_COMPILER=gcc. Linux: no extra arguments. M3_NATIVE_CENSUS overrides the census
# (v1.1: a later leaf checks against its own census).
set -euo pipefail
root=$(cd "$1" && pwd)
build=$2
shift 2
here=$(cd "$(dirname "$0")/.." && pwd)
rm -rf "$build"
cmake -S "$root/make/m3/collections" -B "$build" -DCMAKE_BUILD_TYPE=Release "$@" > /dev/null
cmake --build "$build" > "$build.log" 2>&1 || { tail -20 "$build.log"; exit 1; }
shopt -s nullglob
libraries=("$build"/*.so "$build"/*.dll)
echo "NATIVE_BUILD_CHECK libraries ${#libraries[@]}"
bash "$here/tools/export_check.sh" "$build" "${M3_NATIVE_CENSUS:-$here/NATIVE_CENSUS.tsv}"
bash "$here/tools/mutant_check.sh" "$root" "$build"
