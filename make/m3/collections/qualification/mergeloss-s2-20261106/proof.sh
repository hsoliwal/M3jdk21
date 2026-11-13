#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Case 08 proof (Linux, gcc): libm3_collections_mask = m3_index_mask_compactor.c + m3_index_mask_window.c, as
# .github/workflows/m3jdk21-current-array-mask-acceptance.yml and m3jdk21-array-mask-current-20261026.yml build it.
# Master links; master plus the origin's window block does not (second scanNative); the origin's own tree does not.
# usage: TMPDIR=<tmp> proof.sh <staged-dir>   (JAVA_HOME = a JDK 21 for jni.h)
set -uo pipefail
cd "$1" || exit 9
JDK=${JAVA_HOME:?}
F="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared -I$JDK/include -I$JDK/include/linux"
mkdir -p out
link() { # <name> <c files...>
    local n=$1; shift
    if gcc $F "$@" -o out/$n.so 2> out/$n.log; then
        echo "$n: LINKED exports=$(nm -D --defined-only out/$n.so | grep -c ' Java_') window_exports=$(nm -D --defined-only out/$n.so | grep ' Java_.*M3IndexMaskWindow_' | sed 's/.*M3IndexMaskWindow_//' | tr '\n' ' ')"
    else
        echo "$n: FAILED $(grep -m1 -oE 'multiple definition of [^;]*|error: .*' out/$n.log | head -1)"
    fi
}
link master master_compactor.c master_window.c
link master_compactor_alone master_compactor.c
link restored restored_compactor.c master_window.c
link restored_compactor_alone restored_compactor.c
sed 's/Java_com_m3_collections_/Java_jdk_m3_collections_/' origin_compactor.c > origin_compactor_n.c
sed 's/Java_com_m3_collections_/Java_jdk_m3_collections_/' origin_window.c > origin_window_n.c
link origin_tree origin_compactor_n.c origin_window_n.c
echo "master M3IndexMaskWindow native declarations: $(grep -oE 'native long [a-zA-Z]+' M3IndexMaskWindow.master.java | sed 's/native long //' | tr '\n' ' ')"
echo "master M3IndexMaskWindow scanLong members: $(grep -cE 'scanLong' M3IndexMaskWindow.master.java)"
