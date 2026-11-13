#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")

for fragment in [
    "private static boolean isConservativeLiteralRegex(String regex)",
    "Objects.requireNonNull(regex);",
    "storage != null && isConservativeLiteralRegex(regex)",
    "return storage.contentEquals(regex);",
    "return Pattern.matches(regex, this);",
    '".^$|?*+()[]{}"',
]:
    if fragment not in string:
        raise SystemExit(f"M3_LITERAL_MATCHES_FAIL|missing={fragment}")

if "isConservativeLiteralRegex" in m3:
    raise SystemExit("M3_LITERAL_MATCHES_FAIL|regex syntax leaked into M3String")

start = string.find("private static boolean isConservativeLiteralRegex")
end = string.find("\n    /**", start)
body = string[start:end] if start >= 0 and end >= 0 else ""
if "unit == '\\\\'" not in body:
    raise SystemExit("M3_LITERAL_MATCHES_FAIL|backslash fallback missing")

print("M3_STRING_LITERAL_MATCHES_SOURCE_PASS|literal_only=true|pattern_fallback=true")
