#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
python3 - "$repo" <<'PY'
from pathlib import Path
import hashlib, sys
repo=Path(sys.argv[1]).resolve()
crate=repo/'m3/collections/wrappers/index-counted-receiver-20261017/recipe/src/main/resources/com/synexia/rewrite'
source=crate/'hash-pinned-java/m3jdk21-index-counted-20261017'
natives=crate/'hash-pinned-text/m3jdk21-index-counted-native-20261017'
rows=[]
for line in (source/'manifest.tsv').read_text(encoding='utf-8').splitlines():
    if not line.strip(): continue
    path,before,sha,template=line.split('\t')
    relative=Path(path)
    assert before=='ABSENT' and not relative.is_absolute() and '..' not in relative.parts
    assert path.startswith('m3/collections/src/') and path.endswith('.java')
    candidate=(source/template).read_bytes()
    actual=(repo/relative)
    assert actual.is_file() and not actual.is_symlink(),(path,'missing or symlink')
    assert hashlib.sha256(candidate).hexdigest()==sha,(path,'template-drift')
    assert actual.read_bytes()==candidate,(path,'receiver-drift')
    rows.append(path)
assert len(rows)==12 and rows==sorted(set(rows)),('java inventory',len(rows))
assert sum('/src/main/java/' in p for p in rows)==8
assert sum('/src/test/java/' in p for p in rows)==4
native_rows=(natives/'manifest.tsv').read_text(encoding='utf-8').strip().split('\t')
assert len(native_rows)==4 and native_rows[0]=='src/main/native/m3_count_kernel.c'
assert native_rows[1]=='ABSENT'
native=(natives/native_rows[3]).read_bytes()
assert hashlib.sha256(native).hexdigest()==native_rows[2],'native template sha'
product=repo/'m3/collections/src/main/native/collections/m3_count_kernel.c'
assert product.is_file() and not product.is_symlink()
assert product.read_bytes()==native,'native receiver != recipe output'
print('M3_INDEX_COUNTED_SOURCE_SEAL_PASS java=12 native=1')
PY
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT HUP INT TERM
mkdir -p "$work/classes" "$work/headers"
main="$repo/m3/collections/src/main/java/com/m3/collections"
test="$repo/m3/collections/src/test/java/com/m3/collections"
javac --release 21 -proc:none -Xlint:all,-serial -Werror \
    -h "$work/headers" -d "$work/classes" \
    "$main/MIndexSpace.java" "$main/M3IndexHeapAdmission.java" \
    "$main/M3IndexOrderedStore.java" "$main/M3IndexRange.java" \
    "$main/M3IndexCountKernel.java" "$main/M3IndexNavigableSet.java" \
    "$main/M3IndexSortedMultiset.java" "$main/M3FilteredSortedMultisets.java" \
    "$test/M3FilteredSortedMultisetContract.java" \
    "$test/M3FilteredSortedMultisetEdges.java" \
    "$test/M3IndexCountKernelJniParityProof.java"
java -Xmx256m -XX:+UseG1GC -cp "$work/classes" \
    com.m3.collections.M3FilteredSortedMultisetContract
java -Xmx256m -XX:+UseG1GC -cp "$work/classes" \
    com.m3.collections.M3FilteredSortedMultisetEdges
java -Xmx256m -XX:+UseG1GC -cp "$work/classes" \
    com.m3.collections.M3IndexCountKernelJniParityProof > "$work/reference.out"
grep '^M3_COUNTED_JNI_PARITY_PASS checks=' "$work/reference.out"
java_home=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c17 -Wall -Wextra -Werror -fPIC -shared \
    -I"$java_home/include" -I"$java_home/include/linux" -I"$work/headers" \
    "$repo/m3/collections/src/main/native/collections/m3_count_kernel.c" \
    -o "$work/libm3_index_counts.so"
for collector in Serial Parallel G1 Z Shenandoah; do
    java -Xmx256m -Xcheck:jni "-XX:+Use${collector}GC" \
        -Dm3.collections.counts.native=true -Djava.library.path="$work" \
        -cp "$work/classes" com.m3.collections.M3IndexCountKernelJniParityProof \
        > "$work/native-${collector}.out"
    grep '^M3_COUNTED_JNI_PARITY_PASS checks=' "$work/native-${collector}.out"
    grep '^M3_COUNTED_JNI_PARITY_PASS' "$work/reference.out" > "$work/ref-only"
    grep '^M3_COUNTED_JNI_PARITY_PASS' "$work/native-${collector}.out" > "$work/native-only"
    cmp "$work/ref-only" "$work/native-only"
done
java -Xmx256m -Xcheck:jni -XX:+UseG1GC \
    -Dm3.collections.counts.native=true -Djava.library.path="$work" \
    -cp "$work/classes" com.m3.collections.M3FilteredSortedMultisetContract
java -Xmx256m -Xint -Xcheck:jni -XX:+UseG1GC \
    -Dm3.collections.counts.native=true -Djava.library.path="$work" \
    -cp "$work/classes" com.m3.collections.M3IndexCountKernelJniParityProof \
    > "$work/native-interpreter.out"
grep '^M3_COUNTED_JNI_PARITY_PASS' "$work/native-interpreter.out" > "$work/native-only"
cmp "$work/ref-only" "$work/native-only"
echo M3_INDEX_COUNTED_JAVA_JNI_PASS
