#!/usr/bin/env bash
# M3JDK21 focused Java21 / JNI C17 product-owner qualification, not a full JDK release.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

python3 - "$root" <<'PY'
from hashlib import sha256
from pathlib import Path
import sys
root = Path(sys.argv[1])
crate = root / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-index-pathmask-classification-20261021"
expected_paths = [
  "m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java",
  "m3/collections/src/test/java/com/m3/collections/M3IndexPathMaskClassificationProof.java",
]
rows = [line.split("\t") for line in (crate / "manifest.tsv").read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")]
if len(rows) != 2 or [r[0] for r in rows] != expected_paths:
    raise AssertionError("unknown, missing or unordered recipe target")
for path, before, after, name in rows:
    if before != "ABSENT" and (len(before) != 64 or
            any(c not in "0123456789abcdef" for c in before)):
        raise AssertionError("invalid source preimage hash")
    if len(after) != 64 or any(c not in "0123456789abcdef" for c in after):
        raise AssertionError("invalid recipe postimage hash")
    for candidate in (root / path, crate / name):
        if not candidate.is_file() or sha256(candidate.read_bytes()).hexdigest() != after:
            raise AssertionError("source/template drift: " + str(candidate))
print("Source-sealed Java 21 product owner and test: 2/2", flush=True)
PY

src="$root/m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java"
test="$root/m3/collections/src/test/java/com/m3/collections/M3IndexPathMaskClassificationProof.java"
native="$root/m3/collections/src/main/native/collections/m3_index_path_masks.c"
mkdir -p "$work/classes" "$work/missing" "$work/native"
javac --release 21 -Xlint:all -Werror -d "$work/classes" "$src" "$test"
java -Xcheck:jni -Djava.library.path="$work/missing" -cp "$work/classes" \
  com.m3.collections.M3IndexPathMaskClassificationProof

jdk_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
cc -std=c17 -g -O1 -fPIC -shared -Wall -Wextra -Wpedantic -Werror \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$jdk_home/include" -I"$jdk_home/include/linux" \
  "$native" -o "$work/native/libm3_collections_pathmask.so"
java -Xcheck:jni -Djava.library.path="$work/native" -cp "$work/classes" \
  com.m3.collections.M3IndexPathMaskClassificationProof
