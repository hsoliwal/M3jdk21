#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Stage the pinned inputs of proof.sh: master's viewport owners (M3MaskViewportKernel, M3MaskViewportNative,
# m3maskviewport.c), the origin contracts of the dropped code (M3ViewportFrameContract of 3f4e51ad80a, retained under
# retained-variants/pr1766; M3MaskViewportWindowContract and m3_mask_viewport_regions.c of d0cec971780, live), the
# origins' m3maskviewport.c, and #2547's held M3MaskViewportNative.   usage: stage.sh <repo> <master-commit> <out-dir>
set -euo pipefail
REPO=$1; M=$2; OUT=$3
J=m3/collections/src/main/java/jdk/m3/collections
T=m3/collections/src/test/java/jdk/m3/collections
mkdir -p "$OUT/master/jdk/m3/collections" "$OUT/held/jdk/m3/collections" "$OUT/native"
g() { git -C "$REPO" show "$1" > "$OUT/$2"; }
for c in M3MaskViewportKernel M3MaskViewportNative; do g "$M:$J/$c.java" "master/jdk/m3/collections/$c.java"; done
g "$M:$T/M3MaskViewportWindowContract.java" master/jdk/m3/collections/M3MaskViewportWindowContract.java
g "$M:m3/qualification/master-landing-20261028/retained-variants/pr1766/$T/M3ViewportFrameContract.java" master/jdk/m3/collections/M3ViewportFrameContract.java
g "$M:m3/collections/src/main/c/m3maskviewport.c" native/master_m3maskviewport.c
g "d0cec9717801b9f81f308053dd1648ba43a25af9:m3/collections/src/main/c/m3maskviewport.c" native/d0cec971780_m3maskviewport.c
g "3f4e51ad80a3c3664b532b16f7c905ce5b1d07ba:m3/collections/src/main/c/m3maskviewport.c" native/3f4e51ad80a_m3maskviewport.c
g "$M:m3/collections/src/test/native/m3_mask_viewport_regions.c" native/m3_mask_viewport_regions.c
g "$M:make/m3/qualification/retained-twins-core-lanes-20261106/held/$J/M3MaskViewportNative.java" held/jdk/m3/collections/M3MaskViewportNative.java
cp "$OUT/master/jdk/m3/collections/M3MaskViewportKernel.java" "$OUT/held/jdk/m3/collections/"
echo "staged $(find "$OUT" -type f | wc -l) files"
