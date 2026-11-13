#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed exact M3JDK21 receiving seal for one canonical Synexia Java recipe."""
from hashlib import sha256
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
OWNER = "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java"
JTREG = "test/jdk/java/lang/String/M3StringCodePointRankCursorTest.java"
PINNED = {
    OWNER: "43f65f6e578dc5b3ad59046a52ea70bffae3bbff855c861bc2ffd24d1f4bf3d2",
    JTREG: "7e563371f9eaf349c09618d213a76dc53a7b184ed84e61e0492392414d20bc1b",
}
for relative, expected in PINNED.items():
    file = ROOT / relative
    if not file.is_file():
        sys.exit(f"M3_CODEPOINT_RANK_RECEIVER_FAIL missing={relative}")
    actual = sha256(file.read_bytes()).hexdigest()
    if actual != expected:
        sys.exit(f"M3_CODEPOINT_RANK_RECEIVER_FAIL drift={relative} sha256={actual}")

source = (ROOT / OWNER).read_text(encoding="utf-8")
for required in (
    "M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);",
    "int first = lowerBound(pairedLowOffsets, index + 1);",
    "(long) pairedLowOffsets[middle] - middle <= threshold",
    "int limit = lowerBound(pairedLowOffsets, index);",
    "(long) pairedLowOffsets[middle] - middle < threshold",
):
    if required not in source:
        sys.exit(f"M3_CODEPOINT_RANK_RECEIVER_FAIL required={required}")

print("M3_CODEPOINT_RANK_RECEIVER_SOURCE_PASS targets=2"
      " matched_product=false jtreg_not_run=true")
