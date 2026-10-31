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
    "m3/collections/src/main/java/com/m3/collections/M3BagRankIndex.java":
        ("a8686254c2439bcdd1472bdddbefd5bfa7c4b2efce24aa89b6c1453e208515a5", "M3BagRankIndex.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3BagRankIndexSelfTest.java":
        ("9f79cf7605c30bd1766878312af6ac2e880d2faf03f790c579146aee951704ec", "M3BagRankIndexSelfTest.java.txt"),
    "m3/collections/src/test/java/com/m3/collections/M3BagRankIndexTest.java":
        ("58709c47d26fb7819d314fd2793caf85bd24edf84242f7db27a774b058abe827", "M3BagRankIndexTest.java.txt"),
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
