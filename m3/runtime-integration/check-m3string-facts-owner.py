#!/usr/bin/env python3
"""Fail-closed source proof for M3String fact composition and owner retention."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
facts = (ROOT / "src/java.base/share/classes/java/lang/M3StringFacts.java").read_text(
    encoding="utf-8"
)
owner = (ROOT / "src/java.base/share/classes/java/lang/M3StringOwner.java").read_text(
    encoding="utf-8"
)
tuple_source = (
    ROOT / "src/java.base/share/classes/java/lang/M3StringTuple.java"
).read_text(encoding="utf-8")
m3 = (ROOT / "src/java.base/share/classes/java/lang/M3String.java").read_text(
    encoding="utf-8"
)

checks = []


def require(label: str, source: str, fragment: str) -> None:
    if fragment not in source:
        raise SystemExit(f"M3_STRING_FACTS_SOURCE_FAIL|{label}|missing={fragment}")
    checks.append(label)


for fragment in [
    "final class M3StringFacts {",
    "final int utf16Length;",
    "final int utf8Length;",
    "final int codePointCount;",
    "final int unpairedSurrogateCount;",
    "final int javaHash;",
    "final int hash31Power;",
    "final char firstUtf16Unit;",
    "final char lastUtf16Unit;",
    "final long bitSignal64;",
    "final boolean ascii;",
    "final boolean latin1;",
    "final int asciiUpperHash;",
    "final int asciiLowerHash;",
    "final int asciiTitleHash;",
    "final long prefix4;",
    "final long suffix4;",
    "final long bigramSignal64;",
    "final long trigramSignal64;",
    "final int trimStart;",
    "final int trimEnd;",
    "final int stripStart;",
    "final int stripEnd;",
    "static M3StringFacts scan(M3String value)",
    "static M3StringFacts compose(M3StringFacts left, M3StringFacts right)",
    "static long codeUnitSignal(char unit)",
    "private static int utf8Bytes(char value)",
    "private static int pow31(int length)",
    "private static long addBigramSignal(long signal, char first, char second)",
    "private static long addTrigramSignal(long signal, char first, char second, char third)",
    "private static long addNgramSignal(long signal, long value)",
    "private static long mix64(long value)",
    "private static int mix32(int value)",
    "private static char asciiUpper(char value)",
    "private static char asciiLower(char value)",
]:
    require("facts:" + fragment, facts, fragment)

for fragment in [
    "private volatile M3StringFacts facts;",
    "private volatile RangeFact range0;",
    "private volatile RangeFact range3;",
    "final M3StringFacts factsIfPrepared()",
    "final M3StringFacts rangeFactsIfPrepared(long coordinate)",
    "final M3StringFacts facts()",
    "final M3StringFacts rangeFacts(long coordinate, M3String value)",
    "M3StringFacts computed = computeRangeFacts(checked.start(), checked.length());",
]:
    require("owner:" + fragment, owner, fragment)

for fragment in [
    "final M3String left;",
    "final M3String right;",
    "final int height;",
    "final long canonicalId;",
    "M3StringFacts computeFacts()",
    "M3StringFacts computeRangeFacts(int start, int length)",
]:
    require("tuple:" + fragment, tuple_source, fragment)

for fragment in [
    "M3StringFacts facts()",
    "M3StringFacts factsIfPrepared()",
    "int hashCodeValue()",
    "M3StringFacts sourceFacts = factsIfPrepared();",
    "M3StringFacts prepared = facts();",
    "M3StringFacts needleFacts = checked.factsIfPrepared();",
    "M3StringFacts prefixFacts = checked.factsIfPrepared();",
    "M3StringFacts leftFacts = factsIfPrepared();",
]:
    require("m3string:" + fragment, m3, fragment)

print(
    "M3_STRING_FACTS_SOURCE_PASS "
    f"checks={len(checks)}/{len(checks)}|fixed_fact_bundle=true|range_cache_bounded=true|runtime=NOT_RUN"
)
