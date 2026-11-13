#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
string = (ROOT / "src/java.base/share/classes/java/lang/String.java").read_text(encoding="utf-8")
search = (ROOT / "src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java").read_text(
    encoding="utf-8"
)

index_start = string.find("static int indexOf(byte[] src, byte srcCoder, int srcCount,")
index_end = string.find("/**", index_start + 16)
index_body = string[index_start:index_end] if index_start >= 0 and index_end >= 0 else ""

last_start = string.find("static int lastIndexOf(byte[] src, byte srcCoder, int srcCount,")
last_end = string.find("/**", last_start + 16)
last_body = string[last_start:last_end] if last_start >= 0 and last_end >= 0 else ""

for label, body, route in [
    ("index", index_body, "M3StringSearchPrecompute.indexOf("),
    ("last", last_body, "M3StringSearchPrecompute.lastIndexOf("),
]:
    if not body:
        raise SystemExit(f"M3_BYTE_SOURCE_NEEDLE_FAIL|missing_{label}_helper")
    for fragment in [
        "M3String target = tgtStr.m3();",
        "if (target != null)",
        route,
        "byte[] tgt = tgtStr.value();",
    ]:
        if fragment not in body:
            raise SystemExit(f"M3_BYTE_SOURCE_NEEDLE_FAIL|{label}|missing={fragment}")
    if body.index("M3String target = tgtStr.m3();") > body.index("byte[] tgt = tgtStr.value();"):
        raise SystemExit(f"M3_BYTE_SOURCE_NEEDLE_FAIL|{label}|materializes_before_m3_route")

for fragment in [
    "static int indexOf(\n            byte[] source,",
    "static int lastIndexOf(\n            byte[] source,",
    "private static char sourceUnit(byte[] source, byte sourceCoder, int index)",
    "StringLatin1.charAt(source, index)",
    "StringUTF16.charAt(source, index)",
    "Plan plan = prepare(pattern);",
]:
    if fragment not in search:
        raise SystemExit(f"M3_BYTE_SOURCE_NEEDLE_FAIL|search_missing={fragment}")

if "pattern.materialize()" in search or "pattern.compatibilityValue()" in search:
    raise SystemExit("M3_BYTE_SOURCE_NEEDLE_FAIL|needle_materialization_reintroduced")

print("M3_STRING_BYTE_SOURCE_NEEDLE_SOURCE_PASS|logical_utf16=true|materialize=false|prepared=true")
