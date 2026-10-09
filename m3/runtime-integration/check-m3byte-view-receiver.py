#!/usr/bin/env python3
"""Fail-closed source proof for the M3 segmented byte receiver contract."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
view = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3ByteArrayView.java").read_text(
    encoding="utf-8"
)
arrays = (ROOT / "m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java").read_text(
    encoding="utf-8"
)
tests = (ROOT / "m3/arrays/src/test/java/com/m3/arrays/M3ArraysTest.java").read_text(
    encoding="utf-8"
)

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"M3_BYTE_VIEW_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


for fragment in [
    "public interface M3ByteArrayView",
    "int length();",
    "byte byteAt(int index);",
    "M3ByteArrayView slice(int start, int end);",
    "int segmentCount();",
    "ByteBuffer[] asReadOnlyBuffers();",
    "default void copyTo(int sourceStart, byte[] target, int targetStart, int count)",
    "asReadOnlyBuffer()",
    "default byte[] copy()",
]:
    require("view:" + fragment, view, fragment)

for fragment in [
    "public static M3ByteArrayView bytes(byte[] source)",
    "Objects.requireNonNull(source, \"source\").clone()",
    "public static M3ByteArrayView join(M3ByteArrayView... values)",
    "class ByteSnapshot implements M3ByteArrayView",
    "class ByteSlice extends SliceBase implements M3ByteArrayView",
    "class ByteJoin implements M3ByteArrayView",
    "this.segments = segments.clone();",
    "return new ByteJoin(segments);",
    "if (buffer.hasRemaining()) result.add(buffer.asReadOnlyBuffer());",
]:
    require("arrays:" + fragment, arrays, fragment)

for fragment in [
    "byteJoinUsesScatterGatherReadOnlyDescriptors",
    "assertEquals(5, view.length());",
    "assertEquals(2, view.segmentCount());",
    "assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, view.copy());",
    "assertArrayEquals(new byte[] {2, 3, 4}, view.slice(1, 4).copy());",
    "assertEquals(2, view.asReadOnlyBuffers().length);",
    "ReadOnlyBufferException.class",
    "byteView.asReadOnlyBuffers()[0].put(0, (byte) 9)",
]:
    require("test:" + fragment, tests, fragment)

print(
    "M3_BYTE_VIEW_SOURCE_PASS "
    f"checks={len(checks)}|read_only_buffers=true|copy_boundary=true|runtime=NOT_RUN"
)
