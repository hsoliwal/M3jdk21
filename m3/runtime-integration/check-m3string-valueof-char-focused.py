#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed current-head receiver check for Synexia-owned String.valueOf(char) route."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STRING = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
POOL = (ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java").read_text(encoding="utf-8")
ATOM = (ROOT / "src/java.base/share/classes/java/lang/M3StringAtom.java").read_text(encoding="utf-8")
TEST = (ROOT / "test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java").read_text(encoding="utf-8")
WORKFLOW = (ROOT / ".github/workflows/mindex-string-backing.yml").read_text(encoding="utf-8")

def fail(message: str) -> None:
    raise SystemExit("M3_VALUEOF_CHAR_FOCUSED_REFUSAL: " + message)

for fragment in [
    "public static String valueOf(char c)",
    "if (M3String.admissionEnabled())",
    "return new String(M3StringPool.internUnit(c));",
    "if (COMPACT_STRINGS && StringLatin1.canEncode(c))",
    "return new String(StringLatin1.toBytes(c), LATIN1);",
    "return new String(StringUTF16.toBytes(c), UTF16);",
]:
    if fragment not in STRING:
        fail("String.valueOf(char) route/bootstrap contract missing: " + fragment)

for fragment in [
    "static M3String internUnit(char unit)",
    "active.findUnit(unit)",
    "M3StringAtom.localUnit(unit, coder, id, hash64)",
]:
    if fragment not in POOL:
        fail("canonical unit-pool capability missing: " + fragment)

if "static M3StringAtom localUnit(" not in ATOM:
    fail("native one-unit atom constructor missing")

for fragment in [
    "valueOf Latin1 char content",
    "valueOf Latin1 fresh wrapper",
    "valueOf UTF16 char content",
    "valueOf UTF16 fresh wrapper",
]:
    if fragment not in TEST:
        fail("valueOf(char) differential proof lost: " + fragment)

if "test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java" not in WORKFLOW:
    fail("matched-image workflow no longer selects String differential")

for relative in [
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-valueof-char.yml",
    "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-valueof-char",
]:
    if (ROOT / relative).exists():
        fail("focused Synexia recipe duplicated in target: " + relative)

print("M3_VALUEOF_CHAR_FOCUSED_RECEIVER_PASS")
