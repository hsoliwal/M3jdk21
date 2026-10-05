#!/usr/bin/env python3
"""Fail-closed source invariant gate for M3JDK String representation.

This is intentionally structural and cheap. Runtime/jtreg gates remain authoritative for
behavior, but source changes cannot silently reintroduce donor naming, retained String payload,
or public precompute surfaces.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def fail(message: str) -> None:
    print("M3_STRING_INVARIANT_FAIL:", message, file=sys.stderr)
    raise SystemExit(1)

m3 = read("src/java.base/share/classes/java/lang/M3String.java")
owner = read("src/java.base/share/classes/java/lang/M3StringOwner.java")
atom = read("src/java.base/share/classes/java/lang/M3StringAtom.java")
tuple_ = read("src/java.base/share/classes/java/lang/M3StringTuple.java")
facts = read("src/java.base/share/classes/java/lang/M3StringFacts.java")
string = read("src/java.base/share/classes/java/lang/String.java")
symbols = read("src/hotspot/share/classfile/vmSymbols.hpp")
classes = read("src/hotspot/share/classfile/vmClassMacros.hpp")
inline = read("src/hotspot/share/classfile/javaClasses.inline.hpp")
mapping = read("m3/docs/name-mapping.json")
port_map = read("m3/docs/synexia-string-precompute-port-map.tsv")

# M3String must remain owner + coordinate only.
instance_fields = re.findall(
    r"^\s*private\s+final\s+([\w.<>\[\]]+)\s+(\w+)\s*;",
    m3,
    flags=re.MULTILINE,
)
if instance_fields != [("M3StringOwner", "owner"), ("long", "value")]:
    fail(f"M3String instance fields changed: {instance_fields!r}")

# Canonical value/owners/facts may not retain array payload.
for path, text in [
    ("M3String.java", m3),
    ("M3StringOwner.java", owner),
    ("M3StringAtom.java", atom),
    ("M3StringTuple.java", tuple_),
    ("M3StringFacts.java", facts),
]:
    retained_arrays = re.findall(
        r"^\s*(?:private|protected|public)?\s*(?:volatile\s+)?(?:final\s+)?[\w.<>?]+\[\]\s+\w+\s*;",
        text,
        flags=re.MULTILINE,
    )
    # M3String's one static empty sentinel is allowed; no instance array fields are.
    if path == "M3String.java":
        retained_arrays = [row for row in retained_arrays if "static" not in row]
    if retained_arrays:
        fail(f"{path} retains array field(s): {retained_arrays!r}")

if "private volatile M3String m3;" not in string:
    fail("java.lang.String no longer owns M3String")

# M3-backed wrappers must not retain a spelling byte[].
if "return EMPTY_COMPATIBILITY_SHADOW;" not in m3:
    fail("M3String compatibilityValue must return the empty VM sentinel")

# Donor class naming must not leak back into live VM symbols/layout.
for path, text in [
    ("vmSymbols.hpp", symbols),
    ("vmClassMacros.hpp", classes),
    ("javaClasses.inline.hpp", inline),
]:
    if "MIndexString" in text:
        fail(f"stale donor runtime name in {path}")

# HotSpot must consume canonical M3 coordinates, not String.value for M3 char access.
char_at = re.search(
    r"jchar\s+java_lang_String::char_at\([^)]*\)\s*\{(?P<body>.*?)\n\}",
    inline,
    flags=re.DOTALL,
)
if not char_at or "java_lang_M3String::char_at(storage, index)" not in char_at.group("body"):
    fail("HotSpot String::char_at does not route M3 values through M3String")

# Precompute is implementation-internal: String must expose none.
if re.search(r"\bpublic\b[^\n{;]*\bprecompute\s*\(", string, flags=re.IGNORECASE):
    fail("java.lang.String exposes precompute API")

# Critical String surfaces must remain M3-aware. We deliberately check named semantic
# entry points instead of trying to parse Java with regular expressions.
critical_surfaces = {
    "length": "M3String storage = m3();",
    "charAt": "M3String storage = m3();",
    "codePointAt": "if (m3() != null)",
    "codePointBefore": "if (m3() != null)",
    "codePointCount": "M3String storage = m3();",
    "getChars": "M3String storage = m3();",
    "equals": "M3String leftStorage = m3();",
    "compareTo": "if (m3() != null || anotherString.m3() != null)",
    "regionMatches": "if (m3() != null || other.m3() != null)",
    "startsWith": "M3String sourceM3 = m3();",
    "hashCode": "M3String storage = m3();",
    "indexOf(String)": "M3String sourceM3 = m3();",
    "lastIndexOf(String)": "M3String storage = m3();",
    "substring": "M3String.sliceOf(this, beginIndex, endIndex)",
    "concat": "m3Concat(this, str)",
    "trim": "M3StringFacts facts = storage.facts();",
    "strip": "facts.stripStart",
    "stripLeading": "storage.facts().stripStart",
    "stripTrailing": "storage.facts().stripEnd",
    "indexOfNonWhitespace": "storage.facts().stripStart",
    "lastIndexOfNonWhitespace": "storage.facts().stripEnd",
    "toCharArray": "return storage.charShadow();",
    "value": "return storage == null ? value : storage.materialize();",
}
for surface, marker in critical_surfaces.items():
    if marker not in string:
        fail(f"critical String surface lost M3 route: {surface}")

# M3-backed constructors must store only the empty compatibility sentinel.
if string.count("storage.compatibilityValue()") < 4:
    fail("M3-backed String constructors no longer consistently use the empty sentinel")

# Mapping authority must preserve donor->target lineage and shadow names.
required_mapping_fragments = [
    '"source": "com.synexia.indexstring.MIndexString"',
    '"target": "java.lang.M3String"',
    '"target": "java.lang.M3StringOwner / java.lang.M3StringAtom / java.lang.M3StringTuple"',
    '"target": "java.lang.M3StringFacts plus owner-internal bounded range facts"',
    '"shadow only; never canonical M3String payload"',
]
for fragment in required_mapping_fragments:
    if fragment not in mapping:
        fail(f"name mapping missing: {fragment}")

for fragment in [
    "MIndexWhitespaceBoundaries\tjava.lang.M3StringFacts\tIMPLEMENTED",
    "MIndexUtf16RangeFacts\tM3StringOwner.rangeFacts\tIMPLEMENTED",
    "MIndexRegexTrigramQuery\tjdk.internal.mindex.M3TQ\tIMPLEMENTED",
    "DO_NOT_PORT_TO_JAVA_LANG_STRING",
]:
    if fragment not in port_map:
        fail(f"precompute port map missing: {fragment}")

print("M3_STRING_SOURCE_INVARIANTS_PASS")
