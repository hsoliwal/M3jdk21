#!/usr/bin/env bash
# M3JDK21 product-owned Java 21/C17/JNI differential; no extra runtime layer.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

python3 - "$root" <<'PY'
from pathlib import Path
from hashlib import sha256
import sys

root = Path(sys.argv[1])
resources = root / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java"
crates = ("m3-native-bulk-pathmask-20261021",
          "m3-native-bulk-pathmask-test-20261021")
seen = set()
for crate_name in crates:
    crate = resources / crate_name
    lines = [line.split("\t") for line in (crate / "manifest.tsv").read_text(encoding="utf-8").splitlines()
             if line and not line.startswith("#")]
    assert len(lines) == 1, "one meaningful behavioral atom per recipe"
    for path, before, after, template in lines:
        assert path not in seen, f"duplicate source owner {path}"
        seen.add(path)
        assert before == "ABSENT" or (len(before) == 64 and all(ch in "0123456789abcdef" for ch in before))
        assert len(after) == 64 and all(ch in "0123456789abcdef" for ch in after)
        for source in (root / path, crate / template):
            assert source.is_file(), f"missing source/template {source}"
            assert sha256(source.read_bytes()).hexdigest() == after, f"source drift {source}"
        if before != "ABSENT":
            parent = resources / "m3-native-pathmask-stagecache-20261020" / template
            assert parent.is_file(), "missing exact parent recipe product snapshot"
            assert sha256(parent.read_bytes()).hexdigest() == before, "parent native source drift"
print(f"M3JDK21 native and Java recipe postimages pinned: {len(seen)}/{len(seen)}", flush=True)
PY

java_owner="$root/m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java"
test_source="$root/m3/collections/qualification/native-bulk-pathmask-20261021/src/test/java/com/m3/collections/M3NativeBulkPathMaskProof.java"
parent_test="$root/m3/collections/qualification/pathmask-stage-cache-20261020/src/test/java/com/m3/collections/M3PathMaskStageCacheParity.java"
baseline="$root/m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-native-pathmask-stagecache-20261020/m3_index_path_masks.c"
candidate="$root/m3/collections/src/main/native/collections/m3_index_path_masks.c"

mkdir -p "$work/classes" "$work/before" "$work/after"
javac --release 21 -Xlint:all -Werror -d "$work/classes" "$java_owner" "$test_source" "$parent_test"
jni_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
for variant in before after; do
    input="$baseline"
    if [ "$variant" = after ]; then input="$candidate"; fi
    cc -std=c17 -O1 -g -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
       -fsanitize=undefined -fno-sanitize-recover=undefined \
       -I"$jni_home/include" -I"$jni_home/include/linux" \
       "$input" -o "$work/$variant/libm3_collections_pathmask.so"
    java -Xcheck:jni -Djava.library.path="$work/$variant" -cp "$work/classes" \
        com.m3.collections.M3NativeBulkPathMaskProof >"$work/$variant.stdout"
    java -Xcheck:jni -Djava.library.path="$work/$variant" -cp "$work/classes" \
        com.m3.collections.M3PathMaskStageCacheParity >"$work/$variant.parent.stdout"
done

cmp "$work/before.stdout" "$work/after.stdout"
cmp "$work/before.parent.stdout" "$work/after.parent.stdout"
echo "Exact parent and 256-row native successor agree under checked JNI and UBSan:"
cat "$work/after.stdout" "$work/after.parent.stdout"
