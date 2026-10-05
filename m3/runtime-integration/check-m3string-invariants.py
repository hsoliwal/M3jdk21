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
pool = read("src/java.base/share/classes/java/lang/M3StringPool.java")
owner = read("src/java.base/share/classes/java/lang/M3StringOwner.java")
atom = read("src/java.base/share/classes/java/lang/M3StringAtom.java")
tuple_ = read("src/java.base/share/classes/java/lang/M3StringTuple.java")
facts = read("src/java.base/share/classes/java/lang/M3StringFacts.java")
search_precompute = read("src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java")
string = read("src/java.base/share/classes/java/lang/String.java")
symbols = read("src/hotspot/share/classfile/vmSymbols.hpp")
classes = read("src/hotspot/share/classfile/vmClassMacros.hpp")
inline = read("src/hotspot/share/classfile/javaClasses.inline.hpp")
stringopts = read("src/hotspot/share/opto/stringopts.cpp")
archive_writer = read("src/hotspot/share/cds/archiveHeapWriter.cpp")
dedup = read("src/hotspot/share/gc/shared/stringdedup/stringDedupTable.cpp")
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

# Length-proportional operation precompute is separate and bounded. It may retain primitive
# algorithm lanes but never canonical spelling/payload or strong M3 owner/value references.
for forbidden in [
    r"\bString\s+\w+\s*;",
    r"\bchar\[\]\s+\w+\s*;",
    r"\bbyte\[\]\s+\w+\s*;",
    r"\bM3String\s+\w+\s*;",
    r"\bM3StringOwner\s+\w+\s*;",
]:
    if re.search(forbidden, search_precompute):
        fail(f"M3StringSearchPrecompute retains forbidden payload/strong owner: {forbidden}")
if "WeakReference<M3StringOwner>" not in search_precompute:
    fail("M3StringSearchPrecompute must weakly key canonical owner")
if "private static final int SLOTS = 256;" not in search_precompute:
    fail("M3StringSearchPrecompute cache bound changed without invariant review")
if "private static final int MAX_PATTERN_UNITS = 8_192;" not in search_precompute:
    fail("M3StringSearchPrecompute pattern budget changed without invariant review")
if "final int[] prefix;" not in search_precompute:
    fail("prepared search plan lost primitive KMP metadata")
if "M3StringSearchPrecompute.prepare(checked)" not in m3:
    fail("M3String canonical search no longer reuses prepared operation metadata")
if "trigramSignal64" not in facts or "bigramSignal64" not in facts:
    fail("M3StringFacts lost canonical n-gram candidate facts")
if "prefixMayMatch" not in facts or "suffixMayMatch" not in facts:
    fail("M3StringFacts lost canonical boundary candidate facts")

if "private volatile M3String m3;" not in string:
    fail("java.lang.String no longer owns M3String")

# M3-backed wrappers must not create Java text arrays. Even the shared empty compatibility
# sentinel is created through the JNI shadow boundary.
if "new byte[" in m3 or "new char[" in m3:
    fail("M3String creates Java byte/char shadow directly")
if "nativeByteShadow(EMPTY, 0, 0, String.LATIN1)" not in m3:
    fail("M3String empty compatibility sentinel is not JNI-created")
if "return storage == null ? checked.value : storage.compatibilityValue();" not in string:
    fail("JNI ingress no longer discards the temporary construction payload")

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
    "bounded indexOf(String)": "return sourceM3.indexOf(targetM3, beginIndex, endIndex);",
    "lastIndexOf(String)": "M3String storage = m3();",
    "prepared reverse search": "return storage.lastIndexOf(target, fromIndex);",
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

if "if (java_lang_String::is_m3_joined(java_string))" not in dedup:
    fail("String deduplication is not fail-closed for M3-backed values")
if "if (UseM3StringStorage)" not in stringopts:
    fail("legacy C2 StringConcat optimization is not disabled for M3 storage")
if "if (java_lang_String::is_m3_joined(string))" not in archive_writer:
    fail("CDS String sizing is not fail-closed for M3 values")


# VM-local native atoms must not be retained forever by the canonical lookup table.
# Live M3String/Tuple owners provide the strong lifetime. The pool keeps weak refs and frees
# the native block when the owner becomes unreachable.
for fragment in [
    "ReferenceQueue<M3StringAtom> LOCAL_QUEUE",
    "ArrayList<LocalRef> values",
    "extends WeakReference<M3StringAtom>",
    "UNSAFE.freeMemory(address)",
    "AtomicLong LOCAL_NATIVE_BYTES",
    "LOCAL_NATIVE_BYTES.addAndGet(-retainedBytes)",
    "created.nativePayloadBytes()",
]:
    if fragment not in pool:
        fail(f"VM-local M3 atom lifecycle missing: {fragment}")
if "ArrayList<M3StringAtom> values" in pool:
    fail("VM-local M3 atom pool strongly retains native atoms")

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
    "MIndexStringSearchPlan\tjava.lang.M3StringSearchPrecompute\tIMPLEMENTED",
    "DO_NOT_PORT_TO_JAVA_LANG_STRING",
]:
    if fragment not in port_map:
        fail(f"precompute port map missing: {fragment}")

print("M3_STRING_SOURCE_INVARIANTS_PASS")
