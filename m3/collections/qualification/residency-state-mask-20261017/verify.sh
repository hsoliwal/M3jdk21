#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
python3 - "$repo" <<'PY'
from pathlib import Path
import hashlib, sys
root = Path(sys.argv[1]).resolve()
recipe = root/'m3/collections/qualification/residency-state-mask-20261017/recipe/src/main/resources/com/synexia/rewrite'
j = recipe/'hash-pinned-java/m3jdk21-residency-state-mask-20261017'
n = recipe/'hash-pinned-text/m3jdk21-residency-state-mask-native-20261017'
paths=[]
for line in (j/'manifest.tsv').read_text().splitlines():
    path, before, sha, template = line.split('\t')
    relative = Path(path)
    assert before=='ABSENT' and path.startswith('m3/collections/src/') and path.endswith('.java')
    assert not relative.is_absolute() and '..' not in relative.parts
    source=(j/template).read_bytes()
    destination=root/relative
    assert destination.is_file() and not destination.is_symlink()
    assert hashlib.sha256(source).hexdigest()==sha, ('template drift',path)
    assert source==destination.read_bytes(), ('target differs from recipe',path)
    paths.append(path)
assert len(paths)==3 and paths==sorted(set(paths)),paths
parts=(n/'manifest.tsv').read_text().strip().split('\t')
assert len(parts)==4 and parts[:2]==['src/main/native/m3_residency_state_mask.c','ABSENT']
native=(n/parts[3]).read_bytes()
output=root/'m3/collections/src/main/native/collections/m3_residency_state_mask.c'
assert output.is_file() and not output.is_symlink()
assert hashlib.sha256(native).hexdigest()==parts[2] and output.read_bytes()==native
print('M3_RESIDENCY_MASK_SOURCE_RECEIPT_PASS java=3 native=1')
PY
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT HUP INT TERM
mkdir -p "$work/classes" "$work/headers"
src="$repo/m3/collections/src/main/java/com/m3/collections"
tests="$repo/m3/collections/src/test/java/com/m3/collections"
javac --release 21 -proc:none -Xlint:all,-serial -Werror -h "$work/headers" -d "$work/classes" \
    "$src/M3Address28.java" "$src/M3IntLane28.java" "$src/M3BitLane28.java" \
    "$src/M3SlotAllocator28.java" "$src/M3ObjectResidency.java" \
    "$src/M3ResidencyStateMaskIndex.java" "$tests/M3ResidencyStateMaskProof.java"
java -Xmx128m -cp "$work/classes" com.m3.collections.M3ResidencyStateMaskProof > "$work/java.log"
grep '^M3_RESIDENCY_STATE_MASK_PASS' "$work/java.log"
javahome=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
gcc -std=c17 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -fPIC -shared \
    -I"$javahome/include" -I"$javahome/include/linux" -I"$work/headers" \
    "$repo/m3/collections/src/main/native/collections/m3_residency_state_mask.c" \
    -o "$work/libm3_residency_state_mask.so"
expected=$(sed -n 's/.*fingerprint=\([0-9]*\).*/\1/p' "$work/java.log")
for collector in Serial Parallel G1 Z Shenandoah; do
    java -Xmx128m -Xcheck:jni "-XX:+Use${collector}GC" -Djava.library.path="$work" \
        -cp "$work/classes" com.m3.collections.M3ResidencyStateMaskProof native > "$work/$collector.log"
    grep '^M3_RESIDENCY_STATE_MASK_PASS' "$work/$collector.log"
    actual=$(sed -n 's/.*fingerprint=\([0-9]*\).*/\1/p' "$work/$collector.log")
    test "$expected" = "$actual"
done
java -Xmx128m -Xcheck:jni -Xint -XX:+UseG1GC -Djava.library.path="$work" \
    -cp "$work/classes" com.m3.collections.M3ResidencyStateMaskProof native > "$work/interpreter.log"
grep '^M3_RESIDENCY_STATE_MASK_PASS' "$work/interpreter.log"
test "$expected" = "$(sed -n 's/.*fingerprint=\([0-9]*\).*/\1/p' "$work/interpreter.log")"
echo M3_RESIDENCY_STATE_MASK_REAL_JDK_JNI_PASS
