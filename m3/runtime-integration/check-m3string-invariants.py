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

# Direct String.value reads in non-constructor instance methods are dangerous for M3-backed
# values because value is intentionally an empty VM-layout sentinel. Permit only methods that
# explicitly branch on m3() before the flat-array read. This is a conservative textual gate;
# the runtime API probe remains the semantic authority.
method_pattern = re.compile(
    r"(?ms)^\s{4}(?:public|private|protected|static|final|native|synchronized|\s)+"
    r"[\w<>\[\], ?]+\s+(\w+)\s*\([^)]*\)\s*\{"
)
starts = [(m.start(), m.group(1)) for m in method_pattern.finditer(string)]
for index, (start, name) in enumerate(starts):
    end = starts[index + 1][0] if index + 1 < len(starts) else len(string)
    body = string[start:end]
    if name in {"value", "maybeAdmit", "ensureM3", "m3AdmitNative"}:
        continue
    direct = re.search(r"(?<![.\w])value(?!\s*\()", body)
    if direct and "m3()" not in body and name not in {"repeatCopyRest"}:
        # Static helpers may own a local parameter named value; only flag methods that can read
        # this.value implicitly (no local/parameter declaration of byte[] value).
        header = body[: body.find("{") + 1]
        if "byte[] value" not in header and "char[] value" not in header:
            fail(f"String method {name} directly reads value without M3 branch")

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

print("M3_STRING_SOURCE_INVARIANTS_PASS")
