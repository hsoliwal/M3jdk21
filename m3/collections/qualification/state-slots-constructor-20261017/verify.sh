#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
repo=$(CDPATH= cd -- "$(dirname -- "$0")/../../../.." && pwd)
python3 - "$repo" <<'PY'
from pathlib import Path
import hashlib,sys
root=Path(sys.argv[1]).resolve()
crate=root/'m3/collections/qualification/state-slots-constructor-20261017/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-state-slots-constructor-20261017'
row=(crate/'manifest.tsv').read_text(encoding='utf-8').strip().split('\t')
assert len(row)==4 and row[0]=='m3/collections/src/main/java/com/m3/collections/M3ObjectStateSlots28.java'
before=(crate/'pre'/row[3]).read_bytes()
after=(crate/row[3]).read_bytes()
actual=(root/row[0]).read_bytes()
assert hashlib.sha256(before).hexdigest()==row[1]
assert hashlib.sha256(after).hexdigest()==row[2]
assert actual==after, 'generated receiver differs from canonical recipe'
old=b'public final class M3ObjectStateSlots28 {\n'
new=(old+b'    /** Retains the original implicit public no-argument constructor contract. */\n'
     +b'    public M3ObjectStateSlots28() { }\n\n')
assert before.count(old)==1 and before.replace(old,new)==after, 'unexpected behavioral change'
print('M3_STATE_SLOTS_EXPLICIT_CTOR_SOURCE_SEAL_PASS')
PY
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT HUP INT TERM
mkdir -p "$work/old-src/com/m3/collections" "$work/old" "$work/new"
main="$repo/m3/collections/src/main/java/com/m3/collections"
test="$repo/m3/collections/src/test/java/com/m3/collections"
cp "$repo/m3/collections/qualification/state-slots-constructor-20261017/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-state-slots-constructor-20261017/pre/M3ObjectStateSlots28.java.txt" \
    "$work/old-src/com/m3/collections/M3ObjectStateSlots28.java"
javac --release 21 -proc:none -Xlint:all,-missing-explicit-ctor,-serial -Werror -d "$work/old" \
  "$main/M3Address28.java" "$main/M3IntLane28.java" "$main/M3BitLane28.java" \
  "$main/M3SlotAllocator28.java" "$work/old-src/com/m3/collections/M3ObjectStateSlots28.java"
javac --release 21 -proc:none -Xlint:all,-serial -Werror -d "$work/new" \
  "$main/M3Address28.java" "$main/M3IntLane28.java" "$main/M3BitLane28.java" \
  "$main/M3SlotAllocator28.java" "$main/M3ObjectStateSlots28.java" \
  "$test/M3ObjectStateSlots28SelfTest.java"
javap -classpath "$work/old" -public -s com.m3.collections.M3ObjectStateSlots28 > "$work/old-api.txt"
javap -classpath "$work/new" -public -s com.m3.collections.M3ObjectStateSlots28 > "$work/new-api.txt"
cmp "$work/old-api.txt" "$work/new-api.txt"
echo M3_STATE_SLOTS_PUBLIC_API_UNCHANGED
java -Xmx128m -cp "$work/new" com.m3.collections.M3ObjectStateSlots28SelfTest
echo M3_STATE_SLOTS_JAVA21_LINT_PASS
