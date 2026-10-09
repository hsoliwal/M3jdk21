#!/usr/bin/env python3
"""Fail-closed source proof for mapped M3 String precompute ownership."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
mapped = (
    ROOT / "src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java"
).read_text(encoding="utf-8")
pool = (ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java").read_text(
    encoding="utf-8"
)
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(
    encoding="utf-8"
)

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"M3_MAPPED_PRECOMPUTE_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


for fragment in [
    "public final class M3MappedStringBacking implements M3StringBacking",
    "private final int[] javaHashes;",
    "private final int[] hash31Powers;",
    "public static M3MappedStringBacking open(Path textPath)",
    "FileChannel.open(this.textPath, StandardOpenOption.READ)",
    "Pages.map(textChannel, textCommit.committedLength())",
    "public int hashCode(long id)",
    "public int hash31Power(long id)",
    "public char charAt(long id, int index)",
    "public CharBuffer utf16View(long id)",
    "asCharBuffer().asReadOnlyBuffer()",
    "public ByteBuffer utf8View(long id)",
    "if (byteStore.length(utf8Handle) != utf8Length)",
    "verifyTextPayload(cursor, charOffset, utf16Length)",
    "verifyTextUtf8(cursor, charOffset, utf16Length, utf8Handle, encoder, scratch)",
    'throw corrupt("text payload alias facts at " + cursor)',
    "private int row(long id)",
    'throw new IllegalArgumentException("unknown MIndex String id: "',
]:
    require("mapped:" + fragment, mapped, fragment)

for fragment in [
    "mapped.getLong(0) == 0x53594e4152523031L",
    "mapped.getLong(0) != LEXICON_MAGIC",
    "byte[] actual = digestImage(mapped);",
    "javaHashes[row] =",
    "if (javaHashes[row] != javaHash(mapped, (int) payload, offset, length))",
    "MappedByteBuffer mapping",
    "mapping = null;",
]:
    require("pool:" + fragment, pool, fragment)

for fragment in [
    "canonical owner + packed coordinate",
    "private final M3StringOwner owner;",
    "Text payload belongs to canonical native/mapped atom owners.",
    "Contiguous arrays exist only as explicit JNI compatibility shadows.",
]:
    require("m3-owner:" + fragment, m3, fragment)

print(
    "M3_MAPPED_PRECOMPUTE_SOURCE_PASS "
    f"checks={len(checks)}|read_only_mapping=true|owner_coordinates=true|runtime=NOT_RUN"
)
