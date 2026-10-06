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
tq = read("src/java.base/share/classes/jdk/internal/mindex/M3TQ.java")
string = read("src/java.base/share/classes/java/lang/String.java")
symbols = read("src/hotspot/share/classfile/vmSymbols.hpp")
classes = read("src/hotspot/share/classfile/vmClassMacros.hpp")
inline = read("src/hotspot/share/classfile/javaClasses.inline.hpp")
stringopts = read("src/hotspot/share/opto/stringopts.cpp")
archive_writer = read("src/hotspot/share/cds/archiveHeapWriter.cpp")
dedup = read("src/hotspot/share/gc/shared/stringdedup/stringDedupTable.cpp")
mapping = read("m3/docs/name-mapping.json")
port_map = read("m3/docs/synexia-string-precompute-port-map.tsv")
workflow = read(".github/workflows/mindex-string-backing.yml")
native_string = read("src/java.base/share/native/libjava/String.c")
pattern = read("src/java.base/share/classes/java/util/regex/Pattern.java")
matcher = read("src/java.base/share/classes/java/util/regex/Matcher.java")

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
    # Static implementation metadata is outside the value object; no instance array fields
    # are allowed on the canonical value/owner/fact graph.
    if path == "M3String.java":
        retained_arrays = [row for row in retained_arrays if "static" not in row]
    if retained_arrays:
        fail(f"{path} retains array field(s): {retained_arrays!r}")

# Encoding facts must be executable, not decorative metadata.
for fragment in [
    "if (prepared.ascii) {",
    "(!asciiOnly && prepared.latin1)",
    "if (prepared.latin1) {",
    "getBytes(output, 0, 0, String.LATIN1, length());",
]:
    if fragment not in m3:
        fail(f"M3 byte precompute fast path missing: {fragment}")

# Byte/char projections must descend through canonical owner geometry, not walk M3 tuple
# charAt one unit at a time.
if "owner.getBytes(" not in m3:
    fail("M3String byte projection no longer delegates to canonical owner")
if "void getBytes(" not in owner:
    fail("M3StringOwner bulk byte projection missing")
for fragment in [
    "void getChars(int start, int end, char[] destination, int destinationStart)",
    "void getBytes(",
    "UNSAFE.getByte(source)",
]:
    if fragment not in atom:
        fail(f"M3StringAtom bulk address projection missing: {fragment}")
for fragment in [
    "void getBytes(",
    "left.owner().getBytes(",
    "right.owner().getBytes(",
]:
    if fragment not in tuple_:
        fail(f"M3StringTuple bulk range projection missing: {fragment}")

# Exact trigram membership is owned by M3TQ.Facts and reused by a separate bounded weak
# source-range cache. Do not duplicate exact trigram arrays in M3StringFacts.
for fragment in [
    "private static final int SOURCE_SLOTS = 64;",
    "private static final int MAX_TRIGRAM_SOURCE_UNITS = 32_768;",
    "M3TQ.Facts trigrams;",
    "M3TQ.precompute(source, MAX_TRIGRAM_SOURCE_UNITS)",
    "sourceFacts(source).containsAll(plan.trigrams)",
]:
    if fragment not in search_precompute:
        fail(f"M3 exact trigram search reuse missing: {fragment}")
for fragment in [
    "static long maximumRetainedPrimitiveBytes()",
    "(long) SLOTS * MAX_PATTERN_UNITS * (Integer.BYTES + Long.BYTES)",
    "(long) SOURCE_SLOTS * MAX_TRIGRAM_SOURCE_UNITS * Long.BYTES",
]:
    if fragment not in search_precompute:
        fail(f"M3 search precompute memory ceiling missing: {fragment}")
if "public boolean containsAll(Facts required)" not in tq:
    fail("M3TQ exact trigram fact containment missing")
if "long[] trigram" in facts or "M3TQ.Facts" in facts:
    fail("M3StringFacts illegally owns length-proportional exact trigram state")

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
for encoding_marker in [
    "byte[] encode(Charset charset)",
    "private byte[] encodeUtf8()",
    "private byte[] encodeSingleByte(boolean asciiOnly)",
    "private byte[] encodeWithEncoder(Charset charset)",
    "byte[] encodeNoRepl(Charset charset)",
    "byte[] encodeUtf8NoRepl()",
    "private byte[] encodeWithEncoderNoRepl(Charset charset)",
]:
    if encoding_marker not in m3:
        fail(f"M3String direct byte projection missing: {encoding_marker}")
if "prepared.utf8Length" not in m3 or "prepared.codePointCount" not in m3:
    fail("M3String direct byte projection no longer consumes canonical length facts")
if "final int utf8Length;" not in facts or "final boolean ascii;" not in facts or "final boolean latin1;" not in facts:
    fail("M3StringFacts lost byte-encoding geometry")

if "M3String replace(char oldChar, char newChar)" not in m3:
    fail("M3String canonical char replacement path missing")
if "M3String replace(M3String target, M3String replacement)" not in m3:
    fail("M3String canonical literal replacement path missing")
if "Required length exceeds implementation limit" not in m3:
    fail("M3String literal replacement lost expansion OOME contract")
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
# M3String may allocate caller-owned byte[] results because getBytes/charset encoding requires
# real Java arrays. It must not stage canonical text through a Java char[] or retain text arrays.
if "new char[" in m3:
    fail("M3String stages canonical text through a Java char[]")
if "CharBuffer.wrap(this)" not in m3:
    fail("generic charset encoding no longer reads canonical M3 String directly")
if "nativeByteShadow(EMPTY, 0, 0, String.LATIN1)" not in m3:
    fail("M3String empty compatibility sentinel is not JNI-created")
if "return storage == null ? checked.value : storage.compatibilityValue();" not in string:
    fail("JNI ingress no longer discards the temporary construction payload")

# JNI creates only the final compatibility arrays, then bulk-fills them from canonical M3
# storage. Per-code-unit JNI dispatch and temporary C spelling buffers are forbidden.
byte_shadow = re.search(
    r"Java_java_lang_M3String_nativeByteShadow\((?P<body>.*?)\n\}",
    native_string,
    flags=re.DOTALL,
)
char_shadow = re.search(
    r"Java_java_lang_M3String_nativeCharShadow\((?P<body>.*?)\n\}",
    native_string,
    flags=re.DOTALL,
)
if not byte_shadow or not char_shadow:
    fail("M3 JNI shadow functions missing")
for label, body in [("byte", byte_shadow.group("body")), ("char", char_shadow.group("body"))]:
    if "malloc(" in body or "CallCharMethod" in body:
        fail(f"M3 {label} shadow reintroduced per-character/native staging")
if '"getBytes", "([BIIBI)V"' not in byte_shadow.group("body"):
    fail("M3 byte shadow does not bulk-fill through M3String.getBytes")
if '"getChars", "(II[CI)V"' not in char_shadow.group("body"):
    fail("M3 char shadow does not bulk-fill through M3String.getChars")
if "CallVoidMethodA" not in byte_shadow.group("body") or "CallVoidMethodA" not in char_shadow.group("body"):
    fail("M3 JNI shadows lost single bulk dispatch")

# Regex TQ integration is candidate-only and deliberately narrow. Literal find/search may
# reject proven absence, but anchored match/lookingAt and case-insensitive literal semantics stay
# entirely with the stock node engine.
for fragment in [
    "transient M3TQ m3Tq;",
    "has(LITERAL) && !has(CASE_INSENSITIVE)",
    "M3TQ.fromExact(List.of(pattern))",
]:
    if fragment not in pattern:
        fail(f"Pattern literal TQ compilation boundary missing: {fragment}")
for fragment in [
    "private static final int M3_TQ_MAX_UTF16_UNITS = 32_768;",
    "!(text instanceof String)",
    "M3TQ.precompute(text, this.from, to, M3_TQ_MAX_UTF16_UNITS)",
    "if (!m3TqAllowsSearch())",
    "this.hitEnd = true;",
]:
    if fragment not in matcher:
        fail(f"Matcher literal TQ search boundary missing: {fragment}")
match_method = re.search(
    r"boolean\s+match\(int from, int anchor\)\s*\{(?P<body>.*?)\n\s*\}",
    matcher,
    flags=re.DOTALL,
)
if not match_method or "m3TqAllowsSearch" in match_method.group("body"):
    fail("anchored Matcher.match must not use M3TQ candidate gate")

if "catch (OutOfMemoryError unavailable)" not in pattern:
    fail("Pattern literal TQ compile must fail open on precompute OOME")
if matcher.count("catch (OutOfMemoryError unavailable)") < 1:
    fail("Matcher literal TQ source precompute must fail open on OOME")
if "m3TqFactsText" in matcher:
    fail("Matcher TQ cache must not retain a duplicate strong input reference")
if "m3TqFacts = null;" not in matcher or "m3TqFactsFrom = -1;" not in matcher:
    fail("Matcher reset must invalidate cached TQ facts")

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
    "replace(char,char)": "M3String replaced = storage.replace(oldChar, newChar);",
    "replace(CharSequence,CharSequence)": "M3String replaced = storage.replace(targetM3, replacementM3);",
    "substring": "M3String.sliceOf(this, beginIndex, endIndex)",
    "concat": "m3Concat(this, str)",
    "trim": "M3StringFacts facts = storage.facts();",
    "strip": "facts.stripStart",
    "stripLeading": "storage.facts().stripStart",
    "stripTrailing": "storage.facts().stripEnd",
    "indexOfNonWhitespace": "storage.facts().stripStart",
    "lastIndexOfNonWhitespace": "storage.facts().stripEnd",
    "toCharArray": "return storage.charShadow();",
    "getBytes(Charset)": "return storage != null ? storage.encode(charset) : encode(charset, coder(), value());",
    "getBytes(String)": "return storage != null ? storage.encode(charset) : encode(charset, coder(), value());",
    "getBytesUTF8NoRepl": "return storage != null ? storage.encodeUtf8NoRepl()",
    "getBytesNoRepl": "return storage.encodeNoRepl(cs);",
    "value": "return storage == null ? value : storage.materialize();",
}
for surface, marker in critical_surfaces.items():
    if marker not in string:
        fail(f"critical String surface lost M3 route: {surface}")

# Canonical equality may use Java hash only as a negative filter. Equal hashes still require
# exact UTF-16 comparison because collisions are part of the String.hashCode contract.
for fragment in [
    "if (sameCoordinate(that)) return true;",
    "if (hashCodeValue() != that.hashCodeValue()) return false;",
    "if (charAt(index) != other.charAt(index)) return false;",
]:
    if fragment not in m3:
        fail(f"M3 collision-safe equality path missing: {fragment}")

# Range hashes and ASCII case hashes are negative filters only; exact comparison remains in
# String.regionMatches for every surviving candidate.
for fragment in [
    "leftRange.hashCodeValue() != rightRange.hashCodeValue()",
    "leftFacts.asciiLowerHash != rightFacts.asciiLowerHash",
]:
    if fragment not in string:
        fail(f"M3 region precompute filter missing: {fragment}")
if "for (int index = 0; index < len; index++)" not in string:
    fail("exact region comparison loop missing after precompute filter")

# Case conversion is canonical only for ASCII + Locale.ROOT. Locale-sensitive and non-ASCII
# transformations must continue through the stock JDK case engine.
for fragment in [
    "locale.equals(Locale.ROOT) && storage.facts().ascii",
    "storage.asciiCase(false)",
    "storage.asciiCase(true)",
]:
    if fragment not in string:
        fail(f"M3 ROOT ASCII case boundary missing: {fragment}")
if "M3String asciiCase(boolean upper)" not in m3:
    fail("M3String ROOT ASCII canonical case mapper missing")

# M3-backed constructors must store only the empty compatibility sentinel.
if string.count("storage.compatibilityValue()") < 4:
    fail("M3-backed String constructors no longer consistently use the empty sentinel")

if "if (java_lang_String::is_m3_joined(java_string))" not in dedup:
    fail("String deduplication is not fail-closed for M3-backed values")
if "if (UseM3StringStorage)" not in stringopts:
    fail("legacy C2 StringConcat optimization is not disabled for M3 storage")
if "if (java_lang_String::is_m3_joined(string))" not in archive_writer:
    fail("CDS String sizing is not fail-closed for M3 values")


# Canonical single-unit transforms must re-enter the native pool directly rather than create
# temporary one-character String/byte[] payloads.
for fragment in [
    "static M3String internUnit(char unit)",
    "M3StringAtom.localUnit(unit, coder, id, hash64)",
    "existing.charAt(0) == unit",
]:
    if fragment not in pool:
        fail(f"M3 direct unit interning missing: {fragment}")
for fragment in [
    "M3StringPool.internUnit(mapped)",
    "M3StringPool.internUnit(newChar)",
]:
    if fragment not in m3:
        fail(f"M3 canonical transform still detours through String: {fragment}")
if "canonicalize(String.valueOf(mapped))" in m3 or "canonicalize(String.valueOf(newChar))" in m3:
    fail("M3 canonical single-unit transform reintroduced one-char String churn")

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

for required_gate in [
    "M3StringFactsCompositionTest.java",
    "M3StringPrecomputeSearchTest.java",
    "M3TQFactsTest.java",
    "M3RegexLiteralTQTest.java",
    "M3StringHistoryConvergenceRecipeTest",
]:
    if required_gate not in workflow:
        fail(f"M3 String workflow lost verification gate: {required_gate}")

print("M3_STRING_SOURCE_INVARIANTS_PASS")
