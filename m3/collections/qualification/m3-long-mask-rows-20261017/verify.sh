#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/../../../.." && pwd)
TASK="$ROOT/m3/collections/qualification/m3-long-mask-rows-20261017"
MAIN="$ROOT/m3/collections/src/main/java/com/m3/collections"
TEST="$ROOT/m3/collections/src/test/java/com/m3/collections/M3LongMaskRowsContract.java"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$TMP/classes" "$TMP/headers"
(cd "$ROOT" && sha256sum -c "$TASK/outputs.sha256" >/dev/null)
TAB=$(printf '	')
while IFS="$TAB" read -r blob path; do
  case "$blob" in ''|'git_blob'|'#'*) continue;; esac
  actual=$(git -C "$ROOT" hash-object "$ROOT/$path")
  if [ "$actual" != "$blob" ]; then
    echo "OWNER_DRIFT: $path $actual != $blob" >&2
    exit 1
  fi
done < "$TASK/OWNER_GUARDS.tsv"
# Compile against actual M3JDK21 owners via sourcepath, not local mock classes.
javac --release 21 -proc:none -Xlint:all,-module -Werror -h "$TMP/headers"   -sourcepath "$ROOT/m3/collections/src/main/java:$ROOT/m3/collections/src/test/java"   -d "$TMP/classes" "$MAIN/M3LongMaskRows.java" "$TEST"   "$MAIN/M3IndexMaskCompactor.java"
java -Xmx16m -cp "$TMP/classes" com.m3.collections.M3LongMaskRowsContract
java -Xmx32m -cp "$TMP/classes" com.m3.collections.M3LongMaskRowsContract
# Reuse the existing native mask selector; do not create another JNI ABI.
JDK=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c17 -Wall -Wextra -Wpedantic -Werror -fPIC -shared   -fsanitize=undefined -fno-sanitize-recover=all   -I"$JDK/include" -I"$JDK/include/linux"   -include "$TMP/headers/com_m3_collections_M3IndexMaskCompactor.h"   "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_compactor.c"   -o "$TMP/libm3_collections_mask.so"
java -Xmx32m -Xcheck:jni -Dm3.mask.requireNative=true -Djava.library.path="$TMP"   -cp "$TMP/classes" com.m3.collections.M3LongMaskRowsContract
printf '%s\n' 'M3_LONG_MASK_ROWS JAVA21/JNI/UBSAN PASS; SDK/JTREG/REACTOR separate gates'
