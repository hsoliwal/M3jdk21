#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Stage the R1 probe inputs (any host with the repository): the retired test by its pinned blob and master's
# m3maskviewport.c, in the test's include layout (../../main/c/m3maskviewport.c).
# usage: stage.sh <repo> <master-commit> <out-dir>
set -euo pipefail
REPO=$1; M=$2; OUT=$3
mkdir -p "$OUT/src/main/c" "$OUT/src/test/native"
git -C "$REPO" cat-file blob 22f8eaf891304f2fd35b6198003c5f57ab73e444 > "$OUT/regions.retired.c"
git -C "$REPO" show "$M:m3/collections/src/main/c/m3maskviewport.c" > "$OUT/src/main/c/m3maskviewport.c"
sed 's/REGION_SIZE/M3_VIEWPORT_CHUNK/g' "$OUT/regions.retired.c" > "$OUT/src/test/native/m3_mask_viewport_regions.c"
echo "staged: retired test $(git -C "$REPO" hash-object "$OUT/regions.retired.c"), master m3maskviewport.c $(git -C "$REPO" hash-object "$OUT/src/main/c/m3maskviewport.c")"
