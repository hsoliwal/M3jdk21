#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
# Finite JNI-vtable fault proof, separate from the actual JVM native-required gate.
# Approved donor: hsoliwal/com.synexia @ 8e1f56d448fd31ee5b666c922947e39c2036d4fd
# synexia-indexstring/native/src/mindex_array_flatten_jni.c
# donor Git blob b35c9143e27d27c913f9e3bcd407601f8a3e50bb, Apache-2.0.
# The target keeps its existing IllegalArgumentException/message mapping; the
# donor's failed second-pass lookup cleanup/refusal and pending throwable law are reused.
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to an actual JDK 21 installation}"
proof_repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../../../../.." && pwd)
proof_out=$(mktemp -d "${TMPDIR:-/tmp}/m3-jni-failure-proof.XXXXXXXX")
trap 'rm -rf -- "$proof_out"' EXIT
"${CC:-cc}" -std=c17 -Wall -Wextra -Werror -O1 -g \
    -fsanitize=address,undefined -fno-sanitize-recover=all -fno-omit-frame-pointer \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    -I"$proof_repo/m3/arrays/src/main/native/include" \
    "$proof_repo/m3/arrays/src/main/native/m3_arrays.c" \
    "$proof_repo/m3/arrays/src/main/native/m3_arrays_jni.c" \
    "$proof_repo/m3/arrays/src/main/native/tests/m3_array_concat_faults.c" \
    -o "$proof_out/m3_array_concat_faults"
# ASan/UBSan cover this bounded fixture; this invocation does not claim LeakSanitizer coverage.
ASAN_OPTIONS=detect_leaks=0 "$proof_out/m3_array_concat_faults"
