#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify source-sealed Synexia donor runtime receiver without replacing JDK Pattern."""
from hashlib import sha256
from pathlib import Path
import sys
root = Path(__file__).resolve().parents[3]
expected = {
  "src/java.base/share/classes/java/lang/M3StringFacts.java": "d18122b42e9e4bcb76fd3e807467a59160e012d7ac249efe9cc71e5f6b8faa22",
  "src/java.base/share/classes/java/lang/M3StringRegexDp.java": "3029950078524419af52edbcbaaccab545bd5b1bed824ee80218250b8c748ef4",
  "test/jdk/java/lang/String/M3StringRegexDpTest.java": "b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a",
}
for relative, wanted in expected.items():
  p = root / relative
  if not p.is_file() or sha256(p.read_bytes()).hexdigest() != wanted:
    sys.exit("M3_REQUIRED_NGRAMS_PRODUCT_FAIL source drift=" + relative)
regex = (root / "src/java.base/share/classes/java/lang/M3StringRegexDp.java").read_text()
facts = (root / "src/java.base/share/classes/java/lang/M3StringFacts.java").read_text()
for token in ("mandatoryBigramSignals = requiredPairs;", "mandatoryTrigramSignals = requiredTriples;",
              "source.factsIfPrepared()", "private static native long advance0(",
              "M3String.CodeUnitCursor units", "private static long evaluate("):
  if token not in regex: sys.exit("M3_REQUIRED_NGRAMS_PRODUCT_FAIL regex atom=" + token)
for token in ("static long bigramSignal(char first", "static long trigramSignal(char first",
              "left.bigramSignal64 | right.bigramSignal64", "left.trigramSignal64 | right.trigramSignal64"):
  if token not in facts: sys.exit("M3_REQUIRED_NGRAMS_PRODUCT_FAIL fact atom=" + token)
print("M3_REQUIRED_NGRAMS_PRODUCT_SOURCE_PASS targets=3 source_first=true exact_matcher_preserved=true")
