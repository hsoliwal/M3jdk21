#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
python3 - "$repo" <<'PY'
from pathlib import Path
import hashlib, sys
root=Path(sys.argv[1]).resolve()
crate=root/'m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-writable-map-avl-20261017'
rows=[]
for row in (crate/'manifest.tsv').read_text(encoding='utf-8').splitlines():
    path,before,after,filename=row.split('\t')
    source=crate/filename
    target=root/path
    if hashlib.sha256(source.read_bytes()).hexdigest()!=after:
        raise SystemExit('M3_WRITABLE_MAP_RECIPE_TEMPLATE_DRIFT: '+path)
    if target.read_bytes()!=source.read_bytes():
        raise SystemExit('M3_WRITABLE_MAP_TARGET_DRIFT: '+path)
    rows.append(path)
if len(rows)!=5 or rows!=sorted(set(rows)):
    raise SystemExit('M3_WRITABLE_MAP_INCOMPLETE_FILE_INVENTORY')
pre=(crate/'pre/M3FilteredMaps.java.txt').read_bytes()
if hashlib.sha256(pre).hexdigest()!=(
        (crate/'manifest.tsv').read_text(encoding='utf-8').splitlines()[0].split('\t')[1]):
    raise SystemExit('M3_WRITABLE_MAP_PREIMAGE_DRIFT')
print('M3_WRITABLE_MAP_SHA256_PASS rows=5')
PY
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT HUP INT TERM
mkdir -p "$build/before-src/com/m3/collections" "$build/before" "$build/after"
owner="$repo/m3/collections/src/main/java/com/m3/collections"
oracle="$repo/m3/collections/src/test/java/com/m3/collections/M3SortedBatchPutAllProof.java"
cp "$repo/m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-writable-map-avl-20261017/pre/M3FilteredMaps.java.txt" \
    "$build/before-src/com/m3/collections/M3FilteredMaps.java"
javac --release 21 -proc:none -Xlint:all -Werror -d "$build/before" \
    "$owner/M3CollectionViews.java" "$build/before-src/com/m3/collections/M3FilteredMaps.java"
javac --release 21 -proc:none -Xlint:all -Werror -d "$build/after" \
    "$owner/M3CollectionViews.java" "$owner/M3FilteredMaps.java" \
    "$owner/M3PendingSortedKeys.java" "$owner/M3WritableFilteredMaps.java" "$oracle"
javap -classpath "$build/before" -public -s com.m3.collections.M3FilteredMaps > "$build/before.txt"
javap -classpath "$build/after" -public -s com.m3.collections.M3FilteredMaps > "$build/after.txt"
cmp "$build/before.txt" "$build/after.txt"
echo M3_WRITABLE_MAP_OLD_PUBLIC_API_UNCHANGED
java -Xmx256m -cp "$build/after" com.m3.collections.M3SortedBatchPutAllProof
java -Xint -Xmx256m -cp "$build/after" com.m3.collections.M3SortedBatchPutAllProof
echo M3_WRITABLE_MAP_JAVA21_PASS
