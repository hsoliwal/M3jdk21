#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact M3JDK21 regex amalgamation product source seals."""
from hashlib import sha256
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[3]
FILES = {
    "src/java.base/share/classes/java/lang/M3StringRegexDp.java":
        "3029950078524419af52edbcbaaccab545bd5b1bed824ee80218250b8c748ef4",
    "test/jdk/java/lang/String/M3StringRegexDpTest.java":
        "b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a",
    "src/java.base/share/classes/java/lang/M3StringFacts.java":
        "d18122b42e9e4bcb76fd3e807467a59160e012d7ac249efe9cc71e5f6b8faa22"
}
for name, expected in FILES.items():
    file = ROOT / name
    if not file.is_file() or sha256(file.read_bytes()).hexdigest() != expected:
        sys.exit(f"M3_REGEX_AMALGAMATION_RECEIVER_FAIL source={name}")
prior = Path(__file__).with_name("M3StringRegexDpBefore.java.txt")
if not prior.is_file() or sha256(prior.read_bytes()).hexdigest() != (
        "f875210dfb605799b6c8dbee53b28ad6383e000d90da775f7fd285464da7b2d3"):
    sys.exit("M3_REGEX_AMALGAMATION_RECEIVER_FAIL before-snapshot")
source = (ROOT / "src/java.base/share/classes/java/lang/M3StringRegexDp.java").read_text()
for atom in (
    "if (rejectsByPrecompute(source, plan)) return NO_MATCH;",
    "source.factsIfPrepared()",
    "minimumUtf16Units = minimum;",
    "mandatoryUnitSignals = requiredSignals;",
    "fixedFirstUnit =",
    "fixedLastUnit =",
    "mandatoryBigramSignals = requiredPairs;",
    "mandatoryTrigramSignals = requiredTriples;",
    "M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);",
    "private static native long advance0(",
    "private static long evaluate(M3StringOwner owner, int start, int length,",
):
    if atom not in source:
        sys.exit("M3_REGEX_AMALGAMATION_RECEIVER_FAIL lost=" + atom)
print("M3_REGEX_AMALGAMATION_RECEIVER_PASS files=3 native_untouched=true"
      " m3_string_owner_unchanged=true")
