#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/../../../.." && pwd)
TASK="$ROOT/m3/collections/qualification/m3-long-mask-algebra-20261017"
SRC="$ROOT/m3/collections/src/main/java/com/m3/collections"
TST="$ROOT/m3/collections/src/test/java/com/m3/collections"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$TMP/classes" "$TMP/headers"
(cd "$ROOT" && sha256sum -c "$TASK/outputs.sha256")
tab=$(printf '\t')
while IFS="$tab" read -r blob path; do
  case "$blob" in ''|'#'*) continue ;; esac
  actual=$(git -C "$ROOT" hash-object "$ROOT/$path")
  if [ "$actual" != "$blob" ]; then echo "OWNER_DRIFT: $path" >&2; exit 1; fi
done < "$TASK/OWNER_GUARDS.tsv"
javac --release 21 -proc:none -Xlint:all,-module -Werror   -sourcepath "$ROOT/m3/collections/src/main/java:$ROOT/m3/collections/src/test/java"   -h "$TMP/headers" -d "$TMP/classes"   "$SRC/M3LongMaskRows.java" "$TST/M3LongMaskRowsContract.java" "$SRC/M3IndexMaskCompactor.java"
java -Xmx16m -cp "$TMP/classes" com.m3.collections.M3LongMaskRowsContract
java -Xmx32m -cp "$TMP/classes" com.m3.collections.M3LongMaskRowsContract
JDK=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c17 -Wall -Wextra -Wpedantic -Werror -fPIC -shared   -fsanitize=undefined -fno-sanitize-recover=all   -I"$JDK/include" -I"$JDK/include/linux"   -include "$TMP/headers/com_m3_collections_M3IndexMaskCompactor.h"   "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_compactor.c"   -o "$TMP/libm3_collections_mask.so"
java -Xmx32m -Xcheck:jni -Dm3.mask.requireNative=true   -Djava.library.path="$TMP" -cp "$TMP/classes" com.m3.collections.M3LongMaskRowsContract
echo 'M3_LONG_MASK_ALGEBRA Java21/checked JNI/UBSan PASS; SDK, full module and jtreg are separate'
