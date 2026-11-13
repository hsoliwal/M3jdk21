#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Stage the pinned inputs of proof.sh (any host with the repository): master and origin blobs of the two C files that
# form libm3_collections_mask, and the origin's appended window block (aeece7a984e minus its parent), JNI names
# normalised com_m3 -> jdk_m3 as JDK_NAMING_RENAME did.   usage: stage.sh <repo> <master-commit> <out-dir>
set -euo pipefail
REPO=$1; MASTER=$2; OUT=$3
ORIGIN=aeece7a984e8b5f88120fa61dd0cd7b9a74485be
N=m3/collections/src/main/native/collections
mkdir -p "$OUT"
g() { git -C "$REPO" show "$1" > "$OUT/$2"; }
g "$MASTER:$N/m3_index_mask_compactor.c" master_compactor.c
g "$MASTER:$N/m3_index_mask_window.c" master_window.c
g "$MASTER:m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskWindow.java" M3IndexMaskWindow.master.java
g "$ORIGIN:$N/m3_index_mask_compactor.c" origin_compactor.c
g "$ORIGIN:$N/m3_index_mask_window.c" origin_window.c
g "$ORIGIN^:$N/m3_index_mask_compactor.c" origin_parent_compactor.c
n=$(wc -l < "$OUT/origin_parent_compactor.c")
tail -n +$((n + 1)) "$OUT/origin_compactor.c" | sed 's/Java_com_m3_collections_/Java_jdk_m3_collections_/' > "$OUT/origin_block.c"
cat "$OUT/master_compactor.c" "$OUT/origin_block.c" > "$OUT/restored_compactor.c"
echo "staged: origin block $(wc -l < "$OUT/origin_block.c") lines appended to master compactor"
