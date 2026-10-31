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
    "m3/collections/src/main/java/jdk/m3/collections/M3ObjectRangeMap.java":
       ("319398e3dad77d4773aabba0405345f1842bc385415e63242e3d997f802d4386", "M3ObjectRangeMap.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3ObjectRangeMapNativeProof.java":
       ("a896c280666e9f2a4026f86bff29558ce0cdc2a331c4e574c6d587c255bf83e9", "M3ObjectRangeMapNativeProof.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3ObjectRangeMapSelfTest.java":
       ("cffa1740a7bb1b8cfdd5e62378af26657ced83fe1e9ad1e8ccf09267c3a601c5", "M3ObjectRangeMapSelfTest.java.txt"),
    "m3/collections/src/test/java/jdk/m3/collections/M3ObjectRangeMapTest.java":
       ("51c18f26295a775d642051ad0fe818c17369cbabb6e111809b36a3714456b702", "M3ObjectRangeMapTest.java.txt"),
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
