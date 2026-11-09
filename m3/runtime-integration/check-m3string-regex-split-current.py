#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
pattern = (ROOT / "src/java.base/share/classes/java/util/regex/Pattern.java").read_text(encoding="utf-8")
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")

for fragment in [
    "input.subSequence(index, m.start()).toString()",
    "input.subSequence(m.start(), index).toString()",
    "input.subSequence(index, input.length()).toString()",
    "return new String[] {input.toString()};",
]:
    if fragment not in pattern:
        raise SystemExit(f"M3_REGEX_SPLIT_CURRENT_FAIL|pattern={fragment}")

for fragment in [
    "public CharSequence subSequence(int beginIndex, int endIndex)",
    "return this.substring(beginIndex, endIndex);",
    "M3String storage = m3();",
    "return new String(storage);",
]:
    if fragment not in string:
        raise SystemExit(f"M3_REGEX_SPLIT_CURRENT_FAIL|string={fragment}")

if "M3String slice(int beginIndex, int endIndex)" not in m3:
    raise SystemExit("M3_REGEX_SPLIT_CURRENT_FAIL|M3 slice missing")

print("M3_STRING_REGEX_SPLIT_CURRENT_SOURCE_PASS|pattern_subsequence=true|m3_slice=true")
