#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify the two source-sealed M3AST scoped-identity receiver postimages."""
from pathlib import Path
import hashlib

ROOT = Path(__file__).resolve().parents[5]
EXPECTED = {
    "m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ASTLeafIndex.java":
        "7616282198225b23b3fb60270dd14440c7f117285a039a07c24c51b3b1ebca2d",
    "m3/ports/compiletime/src/test/java/jdk/m3/compiletime/M3ASTLeafIndexSelfTest.java":
        "b0ff01c06b68266430adba9884bb32b72205ac37827fe7a07b3e22014f05caea",
}
for relative, expected in EXPECTED.items():
    full = ROOT / relative
    if full.is_symlink() or not full.is_file():
        raise SystemExit(f"invalid or absent receiver: {relative}")
    actual = hashlib.sha256(full.read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit(f"receiver source drift: {relative}, SHA256={actual}")
print("PASS M3AST scoped-file source pins: 2 of 2")
