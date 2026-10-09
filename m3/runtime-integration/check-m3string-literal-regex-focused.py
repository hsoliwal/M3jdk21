#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed current-head receiver check for Synexia-owned literal-regex replacement."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

M3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")
STRING = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
TEST = (ROOT / "test/jdk/java/lang/String/M3StringRegexReplacementTest.java").read_text(encoding="utf-8")
WORKFLOW = (ROOT / ".github/workflows/mindex-string-backing.yml").read_text(encoding="utf-8")

def fail(message: str) -> None:
    raise SystemExit("M3_LITERAL_REGEX_FOCUSED_REFUSAL: " + message)

for fragment in [
    "static boolean isLiteralRegexReplacement(String regex, String replacement)",
    "String replaceLiteralRegex(String original, String regex, String replacement, boolean firstOnly)",
    "replaceMatches(target, canonicalize(replacement), found, firstOnly)",
    "Character.isSurrogate(unit)",
]:
    if fragment not in M3:
        fail("M3String safe literal-regex owner missing: " + fragment)

for fragment in [
    "M3String.isLiteralRegexReplacement(regex, replacement)",
    "return storage.replaceLiteralRegex(this, regex, replacement, true);",
    "return storage.replaceLiteralRegex(this, regex, replacement, false);",
    "return Pattern.compile(regex).matcher(this).replaceFirst(replacement);",
    "return Pattern.compile(regex).matcher(this).replaceAll(replacement);",
]:
    if fragment not in STRING:
        fail("String route/fallback missing: " + fragment)

for fragment in [
    "MATCHED_M3_RUNTIME_REQUIRED",
    "M3_STRING_REGEX_REPLACEMENT_PASS",
    "InMemoryJavaCompiler",
    "preparedPlanReuse();",
    "fallbackAndExceptions();",
    "compilerProjects();",
]:
    if fragment not in TEST:
        fail("runtime differential proof lost discriminator: " + fragment)

if "test/jdk/java/lang/String/M3StringRegexReplacementTest.java" not in WORKFLOW:
    fail("matched-image workflow no longer selects literal-regex replacement jtreg")

# Focused portable recipe authority lives in Synexia. M3JDK may retain old generic/history
# receiver machinery, but must not grow a duplicate focused crate/descriptor for this theme.
for relative in [
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-literal-regex-replacement.yml",
    "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-literal-regex-replacement",
    "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-literal-regex-replacement",
]:
    if (ROOT / relative).exists():
        fail("focused Synexia recipe duplicated in target: " + relative)

print("M3_LITERAL_REGEX_FOCUSED_RECEIVER_PASS")
