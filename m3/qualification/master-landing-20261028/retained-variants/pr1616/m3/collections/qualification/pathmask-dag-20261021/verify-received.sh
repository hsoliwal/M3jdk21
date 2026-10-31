#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Verify exact generated product owners; no canonical recipe or runtime source edits.
set -euo pipefail
HERE=$(cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(cd -- "$HERE/../../../.." && pwd)
OUT=${1:-$(mktemp -d "${TMPDIR:-/tmp}/m3-pathmask-received.XXXXXX")}
mkdir -p "$OUT"
if find "$OUT" -mindepth 1 -print -quit | grep -q .; then
  echo 'A separate empty output directory is required' >&2; exit 2
fi
OUT=$(cd -- "$OUT" && pwd)
JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
JAVAC="$JAVA_HOME/bin/javac"; JAVA="$JAVA_HOME/bin/java"
J=m3/collections/src/main/java/jdk/m3/collections/M3IndexPathMasks.java
C=m3/collections/src/main/native/collections/m3_index_path_masks.c
T=m3/collections/src/test/java/jdk/m3/collections/M3PathMaskChunkProof.java
(cd "$ROOT"; printf '%s  %s\n' \
  0c446d0a2b78f5df90e21dd63cf32a146f6287945ce533c2aa2f4a9aa4ebf457 "$J" \
  0367c0683c10b8d403bd62a03e5a29f3a84a083d4f79b40e2110cbfbe301cc83 "$C" \
  2d455173d7336a63f5857ce03b35e30581266365c059946921ec563162b535b8 "$T" | sha256sum -c -)
mkdir -p "$OUT/classes" "$OUT/headers" "$OUT/native" "$OUT/missing"
"$JAVAC" --release 21 -encoding UTF-8 -Xlint:all -Werror -h "$OUT/headers" \
  -d "$OUT/classes" "$ROOT/$J" "$ROOT/$T"
"${CC:-gcc}" -std=c17 -Wall -Wextra -Werror -pedantic -fPIC -shared \
  -fsanitize=undefined -fno-sanitize-recover=all \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  -include "$OUT/headers/jdk_m3_collections_M3IndexPathMasks.h" \
  "$ROOT/$C" -o "$OUT/native/libm3_collections_pathmask.so"
export UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1
"$JAVA" -Xcheck:jni -Djava.library.path="$OUT/missing" -cp "$OUT/classes" jdk.m3.collections.M3PathMaskChunkProof java
"$JAVA" -Xcheck:jni -Djava.library.path="$OUT/missing" -cp "$OUT/classes" jdk.m3.collections.M3PathMaskChunkProof missing
for mode in native defensive-null defensive-offset; do
  "$JAVA" -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" jdk.m3.collections.M3PathMaskChunkProof "$mode"
done
"$JAVA" -Xint -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" jdk.m3.collections.M3PathMaskChunkProof native
"$JAVA_HOME/bin/javap" -public -s -classpath "$OUT/classes" jdk.m3.collections.M3IndexPathMasks > "$OUT/public-api.txt"
printf '%s  %s\n' a05a6c0d9517cce9a71778f724f36753b38af0b1d71dd30afb7f296649b02537 "$OUT/public-api.txt" | sha256sum -c -
printf 'PASS exact received product-kernel proof; full collection/JDK/Windows qualification NOT_RUN\n'
