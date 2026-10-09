#!/usr/bin/env python3
"""Fail-closed source proof for the conservative literal regex replacement lane."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(
    encoding="utf-8"
)
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(
    encoding="utf-8"
)

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(
            f"M3_STRING_LITERAL_REGEX_REPLACEMENT_SOURCE_FAIL|{label}|missing={fragment}"
        )
    checks.append(label)


def body_between(source: str, start_fragment: str) -> str:
    start = source.find(start_fragment)
    end = source.find("\n    /**", start + len(start_fragment))
    if start < 0 or end < 0:
        raise SystemExit(
            "M3_STRING_LITERAL_REGEX_REPLACEMENT_SOURCE_FAIL|"
            f"cannot_bound={start_fragment}"
        )
    return source[start:end]


first = body_between(string, "public String replaceFirst(String regex, String replacement)")
all_matches = body_between(string, "public String replaceAll(String regex, String replacement)")

for fragment in [
    "storage != null && M3String.isLiteralRegexReplacement(regex, replacement)",
    "return storage.replaceLiteralRegex(this, regex, replacement, true);",
    "return Pattern.compile(regex).matcher(this).replaceFirst(replacement);",
]:
    require("replaceFirst:" + fragment, first, fragment)

for fragment in [
    "storage != null && M3String.isLiteralRegexReplacement(regex, replacement)",
    "return storage.replaceLiteralRegex(this, regex, replacement, false);",
    "return Pattern.compile(regex).matcher(this).replaceAll(replacement);",
]:
    require("replaceAll:" + fragment, all_matches, fragment)

for fragment in [
    "static boolean isLiteralRegexReplacement(String regex, String replacement)",
    "if (!isLiteralRegex(regex) || replacement == null) return false;",
    "if (unit == '$' || unit == '\\\\') return false;",
    "if (Character.isSurrogate(unit)",
]:
    require("M3String-guard:" + fragment, m3, fragment)

for fragment in [
    "M3String target = canonicalize(regex);",
    "int found = indexOf(target, 0);",
    "if (found < 0) return original;",
    "new String(replaceMatches(target, canonicalize(replacement), found, firstOnly))",
    "while (found >= 0)",
    "found = !firstOnly && cursor <= length() - checkedTarget.length()",
    "if (outputLength > Integer.MAX_VALUE)",
]:
    require("M3String-replacement:" + fragment, m3, fragment)

if string.find(
    "storage.replaceLiteralRegex(this, regex, replacement, true);"
) > string.find("Pattern.compile(regex).matcher(this).replaceFirst(replacement);"):
    raise SystemExit(
        "M3_STRING_LITERAL_REGEX_REPLACEMENT_SOURCE_FAIL|"
        "replaceFirst_fallback_order"
    )
checks.append("replaceFirst-fallback-order")

if string.find(
    "storage.replaceLiteralRegex(this, regex, replacement, false);"
) > string.find("Pattern.compile(regex).matcher(this).replaceAll(replacement);"):
    raise SystemExit(
        "M3_STRING_LITERAL_REGEX_REPLACEMENT_SOURCE_FAIL|"
        "replaceAll_fallback_order"
    )
checks.append("replaceAll-fallback-order")

print(
    "M3_STRING_LITERAL_REGEX_REPLACEMENT_SOURCE_PASS "
    f"checks={len(checks)}|runtime=NOT_RUN|benchmark=NOT_MEASURED"
)
