#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed current-head receiver check for Synexia-owned canonical translateEscapes."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
M3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")
STRING = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
TEST = (ROOT / "test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java").read_text(encoding="utf-8")
WORKFLOW = (ROOT / ".github/workflows/mindex-string-backing.yml").read_text(encoding="utf-8")

def fail(message: str) -> None:
    raise SystemExit("M3_TRANSLATE_ESCAPES_FOCUSED_REFUSAL: " + message)

for fragment in [
    "M3String translateEscapes()",
    "if (cursor < slash) pieces.add(slice(cursor, slash));",
    "pieces.add(M3StringPool.internUnit(escaped));",
    "case '\\n' -> emit = false;",
    "case '\\r' -> {",
]:
    if fragment not in M3:
        fail("canonical M3 escape translator missing: " + fragment)

for fragment in [
    "M3String storage = m3();",
    "return new String(storage.translateEscapes());",
    "char[] chars = toCharArray();",
]:
    if fragment not in STRING:
        fail("String translateEscapes route/fallback missing: " + fragment)

# M3 implementation itself must not materialize the source into char[].
method_start = M3.find("M3String translateEscapes()")
method_end = M3.find("\n    M3String ", method_start + 1)
body = M3[method_start: method_end if method_end >= 0 else len(M3)]
if "toCharArray()" in body or "new char[" in body:
    fail("canonical M3 escape translator reintroduced source char-array staging")

for fragment in [
    "translateEscapes canonical",
    "random translateEscapes",
    "translateEscapes trailing backslash",
]:
    if fragment not in TEST:
        fail("faithful translateEscapes differential proof lost: " + fragment)

if "test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java" not in WORKFLOW:
    fail("matched-image workflow no longer selects String precompute differential")

for relative in [
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-translate-escapes.yml",
    "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-translate-escapes",
    "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-translate-escapes",
]:
    if (ROOT / relative).exists():
        fail("focused Synexia recipe duplicated in target: " + relative)

print("M3_TRANSLATE_ESCAPES_FOCUSED_RECEIVER_PASS")
