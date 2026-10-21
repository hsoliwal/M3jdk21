#!/usr/bin/env bash
# Source-guarded M3JDK21 Java 21 / JNI C17 differential, not a complete runtime release gate.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
python3 - "$root" <<'PY'
from hashlib import sha256
from pathlib import Path
import sys

root = Path(sys.argv[1])
resource = root / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java"
crates = ("m3-native-recipe-path-20261020", "m3-native-pathmask-stagecache-20261020",
          "m3-native-pathmask-test-20261020")
seen = set()
for name in crates:
    crate = resource / name
    rows = [row.split("\t") for row in (crate / "manifest.tsv").read_text(encoding="utf-8").splitlines()
            if row and not row.startswith("#")]
    assert rows, f"empty recipe crate: {name}"
    assert [row[0] for row in rows] == sorted(row[0] for row in rows), "unordered source manifest"
    for parts in rows:
        assert len(parts) == 4, f"bad manifest row: {name}"
        path, before, after, template = parts
        assert path not in seen, f"conflicting recipe owners: {path}"
        seen.add(path)
        target = root / path
        generated = crate / template
        assert target.is_file() and generated.is_file(), f"missing owner/template: {path}"
        assert sha256(target.read_bytes()).hexdigest() == after, f"source drift: {path}"
        assert sha256(generated.read_bytes()).hexdigest() == after, f"recipe drift: {path}"
        assert before == "ABSENT" or (len(before) == 64 and all(
            c in "0123456789abcdef" for c in before)), f"bad preimage: {path}"
print(f"Exact source/template postimages: {len(seen)}/{len(seen)}")
PY
jni_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
cc -std=c17 -g -O1 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$jni_home/include" -I"$jni_home/include/linux" \
  "$root/m3/collections/src/main/native/collections/m3_index_path_masks.c" \
  -o "$work/libm3_collections_pathmask.so"
mkdir -p "$work/classes"
javac -Xlint:all -Werror -d "$work/classes" \
  "$root/m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java" \
  "$root/m3/collections/qualification/pathmask-stage-cache-20261020/src/test/java/com/m3/collections/M3PathMaskStageCacheParity.java"
java -Xcheck:jni -Djava.library.path="$work" -cp "$work/classes" \
  com.m3.collections.M3PathMaskStageCacheParity
