#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail closed unless all four Java sources exactly match the Synexia recipe."""
from pathlib import Path
from hashlib import sha256

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
MIRROR = HERE / "recipe-mirror"
TARGETS = {
    "m3/collections/src/main/java/com/m3/collections/M3ObjectRangeMap.java":
       ("8bed87ee1b2b884fa0bbd0934472305698bb7b7561a7bc0c0d6a772977818317", "M3ObjectRangeMap.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3ObjectRangeMapNativeProof.java":
       ("0ffd19f51b13ff26f14e795d4779c9c656a078699623ef28efc9b36461ce04cc", "M3ObjectRangeMapNativeProof.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3ObjectRangeMapSelfTest.java":
       ("f78aff3fdd0dff9d7e8faa685a92f26ba1a41d29c1c9a07d46a1af76b46a6381", "M3ObjectRangeMapSelfTest.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3ObjectRangeMapTest.java":
       ("55267424febd8d04b5ecf040582b73d7c0b1e831bdef2581d07ff20f98c1cd2a", "M3ObjectRangeMapTest.java.txt"),
}
def read(path):
    if path.is_symlink() or not path.is_file():
        raise SystemExit(f"missing or symbolic source path: {path}")
    return path.read_bytes()
lines = read(MIRROR / "manifest.tsv").decode("utf-8").splitlines()
if len(lines) != len(TARGETS):
    raise SystemExit("wrong candidate count")
seen = set()
for line in lines:
    row = line.split("\t")
    if len(row) != 4:
        raise SystemExit("bad manifest row")
    path, before, after, name = row
    if path in seen or path not in TARGETS or before != "ABSENT":
        raise SystemExit(f"unexpected source owner: {path}")
    seen.add(path)
    if (after, name) != TARGETS[path]:
        raise SystemExit(f"wrong canonical hash: {path}")
    source = read(MIRROR / name)
    generated = read(REPO / path)
    if sha256(source).hexdigest() != after or source != generated:
        raise SystemExit(f"generated Java not the exact Synexia template: {path}")
if seen != set(TARGETS):
    raise SystemExit("missing source targets")
print("PASS M3 generic object RangeMap: 4 exact generated Java sources")
