#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact generated Java postimages pinned to one canonical Synexia Maven recipe."""
from hashlib import sha256
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
ORACLE = HERE / "recipe-mirror"
TARGETS = {
    "m3/collections/src/main/java/jdk/m3/collections/M3BagRankIndex.java":
        ("de56d6976a1753c3a21e793fc9c11d08f3ad52aa785dd5b02f8908b39008a5ac", "M3BagRankIndex.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3BagRankIndexSelfTest.java":
        ("ca8149db3e2ec4b7a5d65d8ebd9660a3908487f8dc7ad84104bc65a9ecefa175", "M3BagRankIndexSelfTest.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3BagRankIndexTest.java":
        ("cee68f53d50536c071db06965a05b3ed9aa0bf94f1f2c91703355e3129898322", "M3BagRankIndexTest.java.txt"),
}
def read(path):
    if path.is_symlink() or not path.is_file():
        raise SystemExit(f"missing or symbolic path: {path}")
    return path.read_bytes()
seen = set()
rows = read(ORACLE / "manifest.tsv").decode("utf-8").splitlines()
if len(rows) != len(TARGETS):
    raise SystemExit("unexpected source manifest count")
for line in rows:
    row = line.split("\t")
    if len(row) != 4:
        raise SystemExit("invalid source manifest width")
    path, previous, digest, name = row
    if path in seen or path not in TARGETS or previous != "ABSENT":
        raise SystemExit(f"duplicate/drifted target: {path}")
    seen.add(path)
    if (digest, name) != TARGETS[path]:
        raise SystemExit(f"wrong source target seal: {path}")
    template = read(ORACLE / name)
    product = read(REPO / path)
    if sha256(template).hexdigest() != digest or template != product:
        raise SystemExit(f"not recipe generated/byte-exact: {path}")
if seen != set(TARGETS):
    raise SystemExit("missing target coverage")
print("PASS M3 native bag rank: 3 exact original canonical Java postimages")
