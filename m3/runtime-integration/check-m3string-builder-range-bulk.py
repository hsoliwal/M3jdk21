#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
source = (ROOT / "src/java.base/share/classes/java/lang/AbstractStringBuilder.java").read_text(
    encoding="utf-8"
)

start = source.find("private final void appendChars(String s, int off, int end)")
end = source.find("private final void appendChars(CharSequence s, int off, int end)", start)
if start < 0 or end < 0:
    raise SystemExit("M3_BUILDER_RANGE_BULK_FAIL|appendChars(String) not found")
body = source[start:end]

required = [
    "M3String storage = s.m3();",
    "storage != null && isLatin1() && !s.isLatin1()",
    "M3String range = storage.slice(off, end);",
    "M3StringFacts prepared = range.factsIfPrepared();",
    "prepared != null && prepared.latin1",
    "range.getBytes(this.value, 0, this.count, LATIN1, end - off);",
    "for (int i = off, j = count; i < end; i++)",
]
for fragment in required:
    if fragment not in body:
        raise SystemExit(f"M3_BUILDER_RANGE_BULK_FAIL|missing={fragment}")

if "M3StringFacts prepared = range.facts();" in body:
    raise SystemExit("M3_BUILDER_RANGE_BULK_FAIL|forced_range_fact_scan")

if body.index("range.factsIfPrepared()") > body.index("for (int i = off, j = count; i < end; i++)"):
    raise SystemExit("M3_BUILDER_RANGE_BULK_FAIL|prepared_lane_after_fallback")

print(
    "M3_STRING_BUILDER_RANGE_BULK_SOURCE_PASS|prepared_only=true|"
    "cold_fallback=true|bulk_latin1=true"
)
