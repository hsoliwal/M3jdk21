#!/usr/bin/env python3
"""Fail-closed source proof for the native joined-character receiver contract."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
view = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3Utf16ArrayView.java").read_text(
    encoding="utf-8"
)
facts = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3Utf16Facts.java").read_text(
    encoding="utf-8"
)
arrays = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java").read_text(
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
        raise SystemExit(f"M3_NATIVE_JOINED_CHAR_RECEIVER_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


for fragment in [
    "public interface M3Utf16ArrayView extends CharSequence",
    "M3Utf16ArrayView subSequence(int start, int end)",
    "int segmentCount()",
    "M3Utf16Facts facts()",
    "CharBuffer[] asReadOnlyCharBuffers()",
    "char charAt(int index)",
    "return facts().javaHashCode();",
    "return facts().codePointCount();",
    "return Character.codePointAt(this, index);",
    "return Character.codePointBefore(this, index);",
    "default int indexOf(CharSequence needle, int fromIndex)",
    "default int lastIndexOf(CharSequence needle, int fromIndex)",
    "default void copyTo(int sourceStart, char[] target, int targetStart, int count)",
    "default char[] copy()",
]:
    require("view:" + fragment, view, fragment)

for fragment in [
    "public record M3Utf16Facts(",
    "public static M3Utf16Facts of(CharSequence value)",
    "public static M3Utf16Facts combine(M3Utf16Facts left, M3Utf16Facts right)",
    "left.javaHashCode * right.hashPower31 + right.javaHashCode",
    "Character.isHighSurrogate(left.last) && Character.isLowSurrogate(right.first)",
]:
    require("facts:" + fragment, facts, fragment)

for fragment in [
    "public static M3Utf16ArrayView join(M3Utf16ArrayView... values)",
    "class CharJoin implements M3Utf16ArrayView",
    "return new CharJoin(segments);",
]:
    require("arrays:" + fragment, arrays, fragment)

for fragment in [
    "utf16JoinKeepsExactHashAndCodePointFactsAcrossSurrogateSeam",
    "assertEquals(expected.hashCode(), joined.javaHashCode());",
    "assertEquals(expected.codePointAt(1), joined.codePointAt(1));",
    "assertEquals(2, joined.segmentCount());",
    "slicesAndJoinsRemainCoordinateExactWithoutFlattening",
    "assertEquals(3, joined.segmentCount());",
    "assertEquals(3, joined.indexOf(\"lo wo\", 0));",
    "charView.asReadOnlyCharBuffers()[0].put(0, 'q')",
    "precomputedFactCompositionMatchesDirectComputation",
]:
    require("java-test:" + fragment, tests, fragment)

require(
    "native-differential-test",
    native_tests,
    "optionalNativeProviderMatchesJavaAcrossChunkAndUnicodeBoundaries",
)

print(
    "M3_NATIVE_JOINED_CHAR_RECEIVER_SOURCE_PASS "
    f"checks={len(checks)}|read_only_buffers=true|seam_codepoints=true|runtime=NOT_RUN"
)
