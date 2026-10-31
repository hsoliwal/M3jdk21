#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
geometry = (ROOT / "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java").read_text(encoding="utf-8")
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(encoding="utf-8")
facts = (ROOT / "src/java.base/share/classes/java/lang/M3StringFacts.java").read_text(encoding="utf-8")

for fragment in [
    "private static final int SLOTS = 64;",
    "private static final int MAX_SOURCE_UNITS = 32_768;",
    "WeakReference<M3StringOwner>",
    "private final int[] pairedLowOffsets;",
    "pairedLowOffsets, beginIndex + 1",
    "return (endIndex - beginIndex) - (afterLastPair - firstPair);",
    "catch (OutOfMemoryError unavailable)",
    "static long maximumRetainedPrimitiveBytes()",
]:
    if fragment not in geometry:
        raise SystemExit(f"M3_CODEPOINT_GEOMETRY_FAIL|missing={fragment}")

for fragment in [
    "M3StringCodePointPrecompute.prepare(storage)",
    "geometry.codePointCount(beginIndex, endIndex)",
    "geometry.offsetByCodePoints(index, codePointOffset)",
    "Character.offsetByCodePoints(this, index, codePointOffset)",
    "storage.slice(beginIndex, endIndex).facts().codePointCount",
]:
    if fragment not in string:
        raise SystemExit(f"M3_CODEPOINT_GEOMETRY_FAIL|string_route={fragment}")

for forbidden in [
    "pairedLowOffsets",
    "M3StringCodePointPrecompute.Geometry",
]:
    if forbidden in m3 or forbidden in facts:
        raise SystemExit(f"M3_CODEPOINT_GEOMETRY_FAIL|per_value_owner={forbidden}")

print("M3_STRING_CODEPOINT_GEOMETRY_SOURCE_PASS|bounded=true|fallback=true")
