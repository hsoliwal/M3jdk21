#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify the two source-sealed M3AST scoped-identity receiver postimages."""
from pathlib import Path
import hashlib

ROOT = Path(__file__).resolve().parents[5]
EXPECTED = {
    "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ASTLeafIndex.java":
        "0daf29ca1e035d8facfaea0ef86f0f91158320f0d343f51b12dd6fc81a4f9428",
    "m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ASTLeafIndexSelfTest.java":
        "7d99521d1649131270b9dd18b3a1f2fed14a44b82447d254ad57d68ccdbb1c64",
}
for relative, expected in EXPECTED.items():
    full = ROOT / relative
    if full.is_symlink() or not full.is_file():
        raise SystemExit(f"invalid or absent receiver: {relative}")
    actual = hashlib.sha256(full.read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit(f"receiver source drift: {relative}, SHA256={actual}")
print("PASS M3AST scoped-file source pins: 2 of 2")
