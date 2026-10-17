#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact current java.base, jtreg target source seals and changed-atom guard."""
from hashlib import sha256
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[3]
files = {
    "src/java.base/share/classes/java/lang/M3StringRegexDp.java":
        "f875210dfb605799b6c8dbee53b28ad6383e000d90da775f7fd285464da7b2d3",
    "test/jdk/java/lang/String/M3StringRegexDpTest.java":
        "108087f9706b1f426b9330eccd4baa2e4e30ed327f0dba644a11e21f3a6eb0ad"
}
for file, expected in files.items():
    path = ROOT / file
    if not path.is_file() or sha256(path.read_bytes()).hexdigest() != expected:
        sys.exit("M3_REGEX_ROLLING_RECEIVER_FAIL missing/drift=" + file)
body = (ROOT / "src/java.base/share/classes/java/lang/M3StringRegexDp.java").read_text()
for snippet in ("M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);",
                "char value = units.at(i);",
                "static int tryMatch(M3String source, String regex)",
                "private static native long advance0(",
                "private static long evaluate(M3StringOwner owner, int start, int length,"):
    if snippet not in body:
        sys.exit("M3_REGEX_ROLLING_RECEIVER_FAIL missing=" + snippet)
print("M3_REGEX_ROLLING_RECEIVER_PASS targets=2 preserves_native=true"
      " full_m3jdk_unqualified=true")
