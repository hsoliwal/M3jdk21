#!/usr/bin/env python3
"""Fail-closed source proof for the M3 UTF-16 segmented receiver contract."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
view = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3Utf16ArrayView.java").read_text(
    encoding="utf-8"
)
facts = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3Utf16Facts.java").read_text(
    encoding="utf-8"
)
tests = (ROOT / "m3/arrays/src/test/java/com/m3/arrays/M3ArraysTest.java").read_text(
    encoding="utf-8"
)
native_tests = (ROOT / "m3/arrays/src/test/java/com/m3/arrays/M3ArrayNativeTest.java").read_text(
    encoding="utf-8"
)

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"M3_UTF16_VIEW_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


for fragment in [
    "public interface M3Utf16ArrayView extends CharSequence",
    "M3Utf16ArrayView subSequence(int start, int end)",
    "int segmentCount()",
    "M3Utf16Facts facts()",
    "CharBuffer[] asReadOnlyCharBuffers()",
    "return facts().javaHashCode();",
    "return facts().codePointCount();",
    "return Character.codePointAt(this, index);",
    "return Character.codePointBefore(this, index);",
    "default IntStream codePoints()",
    "Character.toCodePoint(first, second)",
    "default boolean contentEquals(M3Utf16ArrayView other)",
    "default int compareTo(M3Utf16ArrayView other)",
    "default int indexOf(CharSequence needle, int fromIndex)",
    "default int lastIndexOf(CharSequence needle, int fromIndex)",
    "default void copyTo(int sourceStart, char[] target, int targetStart, int count)",
    "asReadOnlyBuffer()",
    "default char[] copy()",
]:
    require("view:" + fragment, view, fragment)

for fragment in [
    "utf16JoinKeepsExactHashAndCodePointFactsAcrossSurrogateSeam",
    "assertEquals(expected.hashCode(), joined.javaHashCode());",
    "slicesAndJoinsRemainCoordinateExactWithoutFlattening",
    "precomputedFactCompositionMatchesDirectComputation",
    "charView.asReadOnlyCharBuffers()[0].put(0, 'q')",
]:
    require("java-test:" + fragment, tests, fragment)

require(
    "native-differential-test",
    native_tests,
    "optionalNativeProviderMatchesJavaAcrossChunkAndUnicodeBoundaries",
)

for fragment in [
    "public record M3Utf16Facts(",
    "if (length < 0 || codePointCount < 0 || codePointCount > length)",
    "public static M3Utf16Facts of(CharSequence value)",
    "hash = 31 * hash + c;",
    "power *= 31;",
    "codePoints++",
    "public static M3Utf16Facts combine(M3Utf16Facts left, M3Utf16Facts right)",
    "if (Character.isHighSurrogate(left.last) && Character.isLowSurrogate(right.first))",
    "left.javaHashCode * right.hashPower31 + right.javaHashCode",
]:
    require("facts:" + fragment, facts, fragment)

print(
    "M3_UTF16_VIEW_SOURCE_PASS "
    f"checks={len(checks)}|read_only_buffers=true|seam_codepoints=true|runtime=NOT_RUN"
)
