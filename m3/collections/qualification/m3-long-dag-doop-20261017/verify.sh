#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/../../../.." && pwd)
TASK="$ROOT/m3/collections/qualification/m3-long-dag-doop-20261017"
SOURCE="$ROOT/m3/collections/src/main/java"
TEST="$ROOT/m3/collections/src/test/java/com/m3/collections/M3LongDagContract.java"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$TMP/classes" "$TMP/headers"
(cd "$ROOT" && sha256sum -c "$TASK/outputs.sha256" >/dev/null)
# Historical/current parent owners must be exactly the qualified source blob versions.
TAB=$(printf '\t')
while IFS="$TAB" read -r git_blob path rest; do
    case "$git_blob" in ''|'owner_git_blob'|\#*) continue;; esac
    actual=$(git -C "$ROOT" hash-object "$ROOT/$path")
    if [ "$actual" != "$git_blob" ]; then
        echo "OWNER_DRIFT: $path $actual != $git_blob" >&2
        exit 1
    fi
done < "$TASK/OWNER_GUARDS.tsv"
# Compile against the actual receiver owners, never local mocks.
javac --release 21 -proc:none -Xlint:all,-module -Werror -h "$TMP/headers" \
    -sourcepath "$SOURCE:$ROOT/m3/collections/src/test/java" -d "$TMP/classes" \
    "$SOURCE/com/m3/collections/M3LongDag.java" \
    "$SOURCE/com/m3/collections/M3VirtualAccumulator.java" \
    "$SOURCE/com/m3/collections/M3VirtualCollector.java" \
    "$TEST"
java -Xmx16m -cp "$TMP/classes" com.m3.collections.M3LongDagContract
java -Xmx32m -cp "$TMP/classes" com.m3.collections.M3LongDagContract
# Reuse the unchanged optional JNI selection owner; never copy predicates over JNI.
JDK=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c17 -Wall -Wextra -Wpedantic -Werror -fPIC -shared \
    -fsanitize=undefined -fno-sanitize-recover=all \
    -I"$JDK/include" -I"$JDK/include/linux" \
    -include "$TMP/headers/com_m3_collections_M3IndexMaskCompactor.h" \
    "$ROOT/m3/collections/src/main/native/collections/m3_index_mask_compactor.c" \
    -o "$TMP/libm3_collections_mask.so"
java -Xmx32m -Xcheck:jni -Dm3.dag.nativeRequired=true \
    -Djava.library.path="$TMP" -cp "$TMP/classes" \
    com.m3.collections.M3LongDagContract
echo 'M3_LONG_DAG JAVA21/JNI/UBSAN=PASS (OpenRewrite SDK & reactor are independent gates)'
