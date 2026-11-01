#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed byte and SHA-256 proof of an exact noncanonical Synexia recipe mirror."""
from pathlib import Path
import hashlib

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[5]
EXPECTED = {
    "m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ASTLeafIndex.java": (
        "d594b4d62b345f6614938f09fe2884041b352c2626c36a07bf30c316c67d684a",
        "7616282198225b23b3fb60270dd14440c7f117285a039a07c24c51b3b1ebca2d",
        "M3ASTLeafIndex.java.txt"),
    "m3/ports/compiletime/src/test/java/jdk/m3/compiletime/M3ASTLeafIndexSelfTest.java": (
        "fe545c7f831d3d6261989f0252951173a1c592f4bc68526a2eddf01811d197e3",
        "b0ff01c06b68266430adba9884bb32b72205ac37827fe7a07b3e22014f05caea",
        "M3ASTLeafIndexSelfTest.java.txt"),
}
def read_checked(path: Path) -> bytes:
    if path.is_symlink() or not path.is_file():
        raise SystemExit(f"missing or symbolic recipe receiver: {path}")
    return path.read_bytes()

manifest = read_checked(HERE / "manifest.tsv").decode("utf-8").splitlines()
rows = [line.split("\t") for line in manifest if line]
if len(rows) != len(EXPECTED):
    raise SystemExit("unexpected recipe target count")
seen = set()
for cells in rows:
    if len(cells) != 4:
        raise SystemExit("invalid recipe manifest row")
    path, before, after, basename = cells
    if path not in EXPECTED or path in seen:
        raise SystemExit("unknown/duplicate recipe target")
    seen.add(path)
    if (before, after, basename) != EXPECTED[path]:
        raise SystemExit(f"source pin drift: {path}")
    pre = read_checked(HERE / "pre" / basename)
    post = read_checked(HERE / "templates" / basename)
    target = read_checked(ROOT / path)
    if hashlib.sha256(pre).hexdigest() != before:
        raise SystemExit(f"original source preimage drift: {path}")
    if hashlib.sha256(post).hexdigest() != after or post != target:
        raise SystemExit(f"generated target not the canonical template: {path}")
if seen != set(EXPECTED):
    raise SystemExit("recipe target coverage incomplete")
print(f"PASS M3AST canonical source mirror: {len(EXPECTED)} exact Java postimages and preimages")
