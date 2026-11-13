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
        "9536a6df2042fe047f02ef07834edca91ce7a863e5b3c1daafffe55fdad48e6e",
    "test/jdk/java/lang/String/M3StringRegexDpTest.java":
        "971290714bb056831ad844288e66562f88fa87fbf0c4cd17fa0716f9df797ffc"
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
