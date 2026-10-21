#!/usr/bin/env bash
# Source-sealed M3JDK21 owner proof. Build both real C17 JNI implementations.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

python3 - "$root" <<'PY'
from hashlib import sha256
from pathlib import Path
import sys

root = Path(sys.argv[1])
resources = root / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java"
crates = ("m3-native-plane-snapshot-20261021", "m3-native-plane-snapshot-test-20261021")
targets = set()
for name in crates:
    crate = resources / name
    rows = [s.split("\t") for s in (crate / "manifest.tsv").read_text(encoding="utf-8").splitlines()
            if s and not s.startswith("#")]
    if len(rows) != 1 or len(rows[0]) != 4:
        raise AssertionError("one guarded target per behavioral-atom crate: " + name)
    path, before, after, filename = rows[0]
    if path in targets:
        raise AssertionError("two recipes address one source file: " + path)
    targets.add(path)
    if before != "ABSENT" and (len(before) != 64
            or any(c not in "0123456789abcdef" for c in before)):
        raise AssertionError("invalid preimage hash")
    if len(after) != 64 or any(c not in "0123456789abcdef" for c in after):
        raise AssertionError("invalid postimage hash")
    for candidate in (root / path, crate / filename):
        if not candidate.is_file() or sha256(candidate.read_bytes()).hexdigest() != after:
            raise AssertionError("source/template mismatch: " + str(candidate))
    if before != "ABSENT":
        ancestor = resources / "m3-native-bulk-pathmask-20261021" / filename
        if not ancestor.is_file() or sha256(ancestor.read_bytes()).hexdigest() != before:
            raise AssertionError("parent #1663 source recipe no longer matches preimage")
print("M3JDK21 exact C17/Java21 source postimages: 2/2", flush=True)
PY

module="$root/m3/collections"
java_owner="$module/src/main/java/com/m3/collections/M3IndexPathMasks.java"
new_test="$module/qualification/native-plane-snapshot-20261021/src/test/java/com/m3/collections/M3NativePlaneSnapshotProof.java"
bulk_test="$module/qualification/native-bulk-pathmask-20261021/src/test/java/com/m3/collections/M3NativeBulkPathMaskProof.java"
stage_test="$module/qualification/pathmask-stage-cache-20261020/src/test/java/com/m3/collections/M3PathMaskStageCacheParity.java"
ancestor="$root/m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-native-bulk-pathmask-20261021/m3_index_path_masks.c"
candidate="$module/src/main/native/collections/m3_index_path_masks.c"

mkdir -p "$work/classes" "$work/before" "$work/after"
javac --release 21 -Xlint:all -Werror -d "$work/classes" \
  "$java_owner" "$new_test" "$bulk_test" "$stage_test"

jdk_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
for variant in before after; do
    input="$ancestor"
    if [ "$variant" = after ]; then input="$candidate"; fi
    cc -std=c17 -O1 -g -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
      -fsanitize=undefined -fno-sanitize-recover=undefined \
      -I"$jdk_home/include" -I"$jdk_home/include/linux" \
      "$input" -o "$work/$variant/libm3_collections_pathmask.so"

    for klass in M3NativePlaneSnapshotProof M3NativeBulkPathMaskProof M3PathMaskStageCacheParity; do
        java -Xcheck:jni -Djava.library.path="$work/$variant" -cp "$work/classes" \
          "com.m3.collections.$klass" >"$work/$variant-$klass.stdout"
    done
done

for klass in M3NativePlaneSnapshotProof M3NativeBulkPathMaskProof M3PathMaskStageCacheParity; do
    cmp "$work/before-$klass.stdout" "$work/after-$klass.stdout"
done
echo "Native small-plane snapshot, bulk JNI and original stage-cache parity agreed:"
cat "$work/after-M3NativePlaneSnapshotProof.stdout"
cat "$work/after-M3NativeBulkPathMaskProof.stdout"
cat "$work/after-M3PathMaskStageCacheParity.stdout"
