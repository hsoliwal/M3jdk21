#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
python3 - "$repo" <<'PY'
from pathlib import Path
import hashlib, subprocess, sys
root=Path(sys.argv[1]).resolve()
helper=root/'m3/collections/wrappers/deep-filter-list-20261017/verify_successor.py'
if hashlib.sha256(helper.read_bytes()).hexdigest()!='8eadcc8aef4de85b8d262fea018f60d21d690ddbe31ddfde0b326ebbeeb8e102':
    raise SystemExit('M3_DEEP_FILTER_SUCCESSOR_VALIDATOR_DRIFT')
resources=root/'m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite'
total=0
for kind,crate in [('hash-pinned-java','m3jdk21-filtered-mat-receiver-20261017'),
                   ('hash-pinned-text','m3jdk21-filtered-mat-native-20261017')]:
    c=resources/kind/crate
    rows=[]
    for row in (c/'manifest.tsv').read_text().splitlines():
        path,before,after,template=row.split('\t')
        candidate=(c/template).read_bytes()
        actual=(root/path).read_bytes()
        assert before=='ABSENT',(path,'non-additive preimage')
        assert hashlib.sha256(candidate).hexdigest()==after,(path,'bad recipe template')
        if path=='m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java':
            subprocess.run([sys.executable,str(helper),'--root',str(root),'--edge','filtered-list'],check=True)
        else:
            assert candidate==actual,(path,'target differs from canonical recipe')
        rows.append(path);total+=1
    assert rows==sorted(set(rows)) and len(rows)==(10 if kind=='hash-pinned-java' else 1)
print(f'M3_JDK21_FILTERED_MAT_SOURCE_SEAL_PASS outputs={total}')
PY
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT HUP INT TERM
src="$repo/m3/collections/src"
javac --release 21 -proc:none -Xlint:all -Werror -d "$build" \
    "$src/main/java/com/m3/collections/M3CollectionViews.java" \
    "$src/main/java/com/m3/collections/M3FilteredLists.java" \
    "$src/main/java/com/m3/collections/M3SignalSortedViews.java" \
    "$src/main/java/com/m3/collections/M3IntFilterKernel.java" \
    "$src/test/java/com/m3/collections/M3FilteredListsContract.java" \
    "$src/test/java/com/m3/collections/M3FilteredListsEdges.java" \
    "$src/test/java/com/m3/collections/M3FilteredListsWorkProof.java" \
    "$src/test/java/com/m3/collections/M3SignalSortedViewsSelfTest.java" \
    "$src/test/java/com/m3/collections/M3IntFilterDenseContract.java"
java -Xmx128m -cp "$build" com.m3.collections.M3FilteredListsContract
java -Xmx128m -cp "$build" com.m3.collections.M3FilteredListsEdges
java -Xmx128m -cp "$build" com.m3.collections.M3FilteredListsWorkProof
java -Xmx128m -cp "$build" com.m3.collections.M3SignalSortedViewsSelfTest
java_home=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c17 -Wall -Wextra -Werror -fPIC -shared -I"$java_home/include" -I"$java_home/include/linux" \
    "$src/main/native/collections/m3_int_filter_jni.c" -o "$build/libm3_int_filter.so"
for collector in Serial Parallel G1 Z Shenandoah; do
    java -Xmx128m -Xcheck:jni "-XX:+Use${collector}GC" \
        -Dm3.collections.intfilter.jni=true -Djava.library.path="$build" \
        -cp "$build" com.m3.collections.M3IntFilterDenseContract
done
java -Xmx128m -Xint -Xcheck:jni -XX:+UseG1GC \
    -Dm3.collections.intfilter.jni=true -Djava.library.path="$build" \
    -cp "$build" com.m3.collections.M3IntFilterDenseContract
