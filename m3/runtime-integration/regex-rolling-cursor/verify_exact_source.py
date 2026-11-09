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
        "3029950078524419af52edbcbaaccab545bd5b1bed824ee80218250b8c748ef4",
    "test/jdk/java/lang/String/M3StringRegexDpTest.java":
        "b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a"
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
