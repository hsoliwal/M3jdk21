#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed byte and SHA-256 proof of an exact noncanonical Synexia recipe mirror."""
from pathlib import Path
import hashlib

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[5]
EXPECTED = {
    "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ASTLeafIndex.java": (
        "0194ab2afa7c0a7ace8b8cb4e28aa142a44cf623176dce5feaa5e6c271173f8b",
        "0daf29ca1e035d8facfaea0ef86f0f91158320f0d343f51b12dd6fc81a4f9428",
        "M3ASTLeafIndex.java.txt"),
    "m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ASTLeafIndexSelfTest.java": (
        "b1303aa343f542cf6339d907369eb08bec68025eb41c43eaf6500abbb84998aa",
        "7d99521d1649131270b9dd18b3a1f2fed14a44b82447d254ad57d68ccdbb1c64",
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
