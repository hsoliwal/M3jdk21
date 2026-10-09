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
position_precompute = read("src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java")
codepoint_precompute = read("src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java")
tq = read("src/java.base/share/classes/jdk/internal/mindex/M3TQ.java")
string = read("src/java.base/share/classes/java/lang/String.java")
asb = read("src/java.base/share/classes/java/lang/AbstractStringBuilder.java")
abstract_builder = read("src/java.base/share/classes/java/lang/AbstractStringBuilder.java")
symbols = read("src/hotspot/share/classfile/vmSymbols.hpp")
classes = read("src/hotspot/share/classfile/vmClassMacros.hpp")
inline = read("src/hotspot/share/classfile/javaClasses.inline.hpp")
java_classes_cpp = read("src/hotspot/share/classfile/javaClasses.cpp")
java_classes_hpp = read("src/hotspot/share/classfile/javaClasses.hpp")
stringopts = read("src/hotspot/share/opto/stringopts.cpp")
archive_writer = read("src/hotspot/share/cds/archiveHeapWriter.cpp")
dedup = read("src/hotspot/share/gc/shared/stringdedup/stringDedupTable.cpp")
stringtable = read("src/hotspot/share/classfile/stringTable.cpp")
mapping = read("m3/docs/name-mapping.json")
port_map = read("m3/docs/synexia-string-precompute-port-map.tsv")
workflow = read(".github/workflows/mindex-string-backing.yml")
recipe_receipt = read("m3/runtime-integration/m3-string-synexia-recipe-application.tsv")
builder = read("src/java.base/share/classes/java/lang/AbstractStringBuilder.java")
native_string = read("src/java.base/share/native/libjava/String.c")
vm_intrinsics = read("src/hotspot/share/classfile/vmIntrinsics.cpp")
string_opts = read("src/hotspot/share/opto/stringopts.cpp")
native_string_test = read("test/jdk/java/lang/String/nativeEncoding/libstringPlatformChars.c")
hotspot_java_classes = read("src/hotspot/share/classfile/javaClasses.cpp")
hotspot_jni = read("src/hotspot/share/prims/jni.cpp")
native_encoding_java = read("test/jdk/java/lang/String/nativeEncoding/StringPlatformChars.java")
native_encoding_c = read("test/jdk/java/lang/String/nativeEncoding/libstringPlatformChars.c")
arguments = read("src/hotspot/share/runtime/arguments.cpp")
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

# Replacement-enabled generic encoders reproduce JDK21's ArrayEncoder ASCII-compatible
# fast path without materializing a source byte[]: fixed ASCII facts + canonical bulk projection.
for fragment in [
    "encoder instanceof ArrayEncoder arrayEncoder",
    "arrayEncoder.isASCIICompatible()",
    "if (prepared.ascii)",
    "getBytes(output, 0, 0, String.LATIN1, length);",
]:
    if fragment not in m3:
        fail(f"M3 generic ASCII-compatible encoder fast path missing: {fragment}")

# Encoding facts must be executable, not decorative metadata.
for fragment in [
    "if (prepared.ascii) {",
    "(!asciiOnly && prepared.latin1)",
    "if (prepared.latin1) {",
    "getBytes(output, 0, 0, String.LATIN1, length());",
]:
    if fragment not in m3:
        fail(f"M3 byte precompute fast path missing: {fragment}")

# Bulk byte projection validates coder and byte geometry before descending into owner recursion.
for fragment in [
    "destinationCoder != String.LATIN1 && destinationCoder != String.UTF16",
    "(long) destinationBegin << destinationCoder",
    "(long) count << destinationCoder",
]:
    if fragment not in m3:
        fail(f"M3 bulk byte entry validation missing: {fragment}")

if "storage.getBytes(dst, srcBegin, dstBegin, LATIN1, srcEnd - srcBegin);" not in string:
    fail("deprecated String.getBytes range lost M3 bulk projection")

# AbstractStringBuilder String append/insert must consume M3 storage through String.getBytes;
# direct String.value() copies are fallback-only for non-M3 inputs.
for fragment in [
    "M3String storage = s.m3();",
    "s.getBytes(this.value, off, this.count, LATIN1, end - off);",
    "s.getBytes(this.value, i, j, UTF16, end - i);",
    "s.getBytes(this.value, off, this.count, UTF16, end - off);",
]:
    if fragment not in asb:
        fail(f"AbstractStringBuilder M3 bulk append route missing: {fragment}")

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

# Position masks are a separate bounded weak-owner lane. They store one conservative signal per
# 64 UTF-16 units and may skip blocks only on a negative signal.
for fragment in [
    "catch (OutOfMemoryError unavailable)",
    "return linearIndexOf(source, unit, from, end);",
    "return linearLastIndexOf(source, unit, from);",
]:
    if fragment not in position_precompute:
        fail(f"M3 position precompute fail-open path missing: {fragment}")

if "char[] scratch" in position_precompute:
    fail("M3 position precompute reintroduced transient text staging")

for fragment in [
    "source.charAt(start + offset)",
    "final AtomicReferenceArray<ExactBlock> exact;",
    "Arrays.binarySearch(units, unit)",
    "Long.numberOfTrailingZeros(positions)",
    "Long.numberOfLeadingZeros(positions)",
    "MAX_SOURCE_UNITS * (Character.BYTES + Long.BYTES)",
]:
    if fragment not in position_precompute:
        fail(f"M3 exact position-mask layer missing: {fragment}")

for fragment in [
    "private static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;",
    "private static final int SLOTS = 64;",
    "private static final int MAX_SOURCE_UNITS = 32_768;",
    "WeakReference<M3StringOwner>",
    "AtomicLongArray signals",
    "M3StringFacts.codeUnitSignal(unit)",
    "static long maximumRetainedPrimitiveBytes()",
    "blockSignal(source, blocks, block)",
    "compareAndSet(block, 0L, computed)",
]:
    if fragment not in position_precompute:
        fail(f"M3 position precompute invariant missing: {fragment}")
for fragment in [
    "M3StringPositionPrecompute.indexOf(this, unit, fromIndex, endIndex)",
    "M3StringPositionPrecompute.lastIndexOf(this, unit, fromIndex)",
]:
    if fragment not in m3:
        fail(f"M3 String position precompute route missing: {fragment}")
if "M3StringPositionPrecompute" not in string:
    # String delegates through M3String methods; do not require direct coupling.
    if "return storage.indexOf((char) ch" not in string or "return storage.lastIndexOf((char) ch" not in string:
        fail("java.lang.String BMP search lost M3 position-precompute route")

for fragment in [
    "int indexOfCodePoint(int codePoint, int fromIndex, int endIndex)",
    "int lastIndexOfCodePoint(int codePoint, int fromIndex)",
    "candidate = indexOf(high, fromIndex",
    "candidate = lastIndexOf(high",
]:
    if fragment not in m3:
        fail(f"M3 supplementary position-precompute route missing: {fragment}")
for fragment in [
    "return storage.indexOfCodePoint(ch, from, storage.length());",
    "return storage.indexOfCodePoint(ch, beginIndex, endIndex);",
    "return storage.lastIndexOfCodePoint(ch, from);",
]:
    if fragment not in string:
        fail(f"String supplementary M3 route missing: {fragment}")

for fragment in [
    "if (checked.length() == 1) return indexOf(checked.charAt(0), from, end);",
    "if (checked.length() == 1) return lastIndexOf(checked.charAt(0), maximumStart);",
]:
    if fragment not in m3:
        fail(f"M3 one-unit literal search lost position-precompute route: {fragment}")

# Prepared literal AUTO search follows the mature Synexia convergence: bounded 256-entry
# conservative BMH skip metadata for long patterns, with existing KMP as the exact adversarial
# fallback. Low-byte collisions may only reduce skips; KMP remains semantic authority.
for fragment in [
    "int[] skip256 = new int[256];",
    "skip256[pattern.charAt(index) & 255] = length - 1 - index;",
    "if (plan.patternLength >= 8)",
    "failedComparisonWork > (long) plan.patternLength + 2L * (at - fromIndex)",
    "return kmp(source, pattern, plan, at, endIndex);",
]:
    if fragment not in search_precompute:
        fail(f"M3 adaptive BMH/KMP convergence missing: {fragment}")

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
    "(long) SLOTS * (",
    "(long) MAX_PATTERN_UNITS * (2L * Integer.BYTES + Long.BYTES)",
    "256L * Integer.BYTES",
    "(long) SOURCE_SLOTS * MAX_TRIGRAM_SOURCE_UNITS * Long.BYTES",
]:
    if fragment not in search_precompute:
        fail(f"M3 search precompute memory ceiling missing: {fragment}")
if "public boolean containsAll(Facts required)" not in tq:
    fail("M3TQ exact trigram fact containment missing")
if "long[] trigram" in facts or "M3TQ.Facts" in facts:
    fail("M3StringFacts illegally owns length-proportional exact trigram state")

# Cold range facts follow canonical owner geometry. Tuple ranges compose child facts and only
# atom-edge ranges may scan UTF-16 directly.
for fragment in [
    "computeRangeFacts(checked.start(), checked.length())",
    "abstract M3StringFacts computeRangeFacts(int start, int length)",
]:
    if fragment not in owner:
        fail(f"M3 structural range-fact dispatch missing: {fragment}")
for fragment in [
    "return left.slice(start, end).facts();",
    "return right.slice(start - leftLength, end - leftLength).facts();",
    "return M3StringFacts.compose(",
]:
    if fragment not in tuple_:
        fail(f"M3 tuple range-fact composition missing: {fragment}")
if "M3StringFacts.scan(M3String.range(this, start, length))" not in atom:
    fail("M3 atom range-fact edge scan missing")

# Fixed String facts are opportunistic search filters. Cold search must not force a complete
# M3StringFacts scan before the dedicated bounded search/position owners execute.
for fragment in [
    "M3StringFacts sourceFacts = factsIfPrepared();",
    "M3StringFacts needleFacts = checked.factsIfPrepared();",
    "M3StringFacts prepared = factsIfPrepared();",
]:
    if fragment not in m3:
        fail(f"M3 cold-search prepared-fact reuse missing: {fragment}")
for forbidden in [
    "if (!facts().mayContainCodeUnit(unit))",
    "M3StringFacts prepared = facts();\n        if (!prepared.mayContainCodeUnit",
]:
    if forbidden in m3:
        fail(f"M3 cold search forces whole facts: {forbidden}")

# AUTO literal search owns one compact 256-entry BMH skip table and may restart the existing
# exact KMP lane only at a start already proven unresolved by the conservative skip.
for fragment in [
    "final int[] skip256;",
    "Arrays.fill(skip256, Math.max(1, length));",
    "skip256[pattern.charAt(index) & 255] = length - 1 - index;",
    "if (plan.patternLength >= 8)",
    "failedComparisonWork > (long) plan.patternLength + 2L * (at - fromIndex)",
    "return kmp(source, pattern, plan, at, endIndex);",
]:
    if fragment not in search_precompute:
        fail(f"M3 adaptive BMH/KMP search invariant missing: {fragment}")
if "65536" in search_precompute and "skip" in search_precompute:
    fail("M3 literal search must not retain a 65536-entry UTF-16 skip table")

# UTF-16 code-point boundary metadata is a separate weak bounded owner. It stores only paired-low
# continuation masks/prefix counts and must never become canonical String payload.
for fragment in [
    "private static final int SLOTS = 64;",
    "private static final int MAX_SOURCE_UNITS = 32_768;",
    "final long[] continuationMasks;",
    "final int[] continuationPrefixCounts;",
    "WeakReference<M3StringOwner>",
    "static long maximumRetainedPrimitiveBytes()",
]:
    if fragment not in codepoint_precompute:
        fail(f"M3 code-point precompute boundary missing: {fragment}")
for fragment in [
    "M3StringCodePointPrecompute.codePointCount(storage, beginIndex, endIndex)",
    "M3StringCodePointPrecompute.offsetByCodePoints(",
]:
    if fragment not in string:
        fail(f"String code-point precompute consumer missing: {fragment}")
if "continuationMasks" in facts or "continuationPrefixCounts" in facts:
    fail("M3StringFacts illegally owns length-proportional code-point metadata")

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
    fail("prepared search plan lost forward KMP metadata")
if "final int[] reversePrefix;" not in search_precompute:
    fail("prepared search plan lost reverse KMP metadata")
if "return index;" not in search_precompute or "reverseUnit(pattern, matched)" not in search_precompute:
    fail("prepared reverse KMP execution path missing")
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

for fragment in [
    "int found = indexOf(oldChar, 0, length());",
    "found = cursor < length() ? indexOf(oldChar, cursor, length()) : -1;",
]:
    if fragment not in m3:
        fail(f"M3 char replacement lost position-precompute reuse: {fragment}")
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
if "String.COMPACT_STRINGS ? String.LATIN1 : String.UTF16" not in m3:
    fail("M3 canonical empty owner ignores CompactStrings mode")

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

# HotSpot native scratch copies must traverse canonical owner geometry in bulk. StringTable,
# symbols and JVMTI may materialize scoped native UTF-16 scratch, but per-unit M3 DAG descent is
# forbidden in java_lang_M3String::copy_chars.
for fragment in [
    "static void copy_owner_chars(",
    "copy_owner_chars(owner(value), java_lang_M3String::start(value) + start, len, destination);",
]:
    if fragment not in java_classes_hpp + java_classes_cpp:
        fail(f"HotSpot M3 structural copy contract missing: {fragment}")
for fragment in [
    "if (o->klass() == vmClasses::M3StringAtom_klass())",
    "copy_chars(left, start, left_count, destination);",
    "copy_chars(right, 0, len - left_count, destination + left_count);",
]:
    if fragment not in java_classes_cpp:
        fail(f"HotSpot M3 owner-range copier missing: {fragment}")
copy_chars_body = re.search(
    r"void java_lang_M3String::copy_chars\((?P<body>.*?)\n\}",
    java_classes_cpp,
    flags=re.DOTALL,
)
if not copy_chars_body or "char_at(value, start + index)" in copy_chars_body.group("body"):
    fail("HotSpot M3 copy_chars reintroduced per-unit char_at traversal")

# JNI StringCritical may return a required native copy for M3, but it must fill that copy through
# the same canonical bulk accessor as GetStringChars/Region, not per-unit VM dispatch.
critical = re.search(
    r"jni_GetStringCritical\((?P<body>.*?)\n\}?",
    hotspot_jni,
    flags=re.DOTALL,
)
if "java_lang_String::copy_chars(s, 0, s_len, ret);" not in hotspot_jni:
    fail("GetStringCritical lost canonical bulk copy")
if "ret[i] = java_lang_String::char_at(s, i);" in hotspot_jni:
    fail("GetStringCritical reintroduced per-unit M3 VM dispatch")

# HotSpot modified-UTF8 writing should bulk-convert complete M3 UTF-16 chunks when they fit,
# retaining the per-unit path only as the bounded-buffer truncation fallback.
for fragment in [
    "int chunk_bytes = 0;",
    "UNICODE::as_utf8(scratch, chunk, out, buflen);",
    "if (chunk_bytes < buflen)",
]:
    if fragment not in hotspot_java_classes:
        fail(f"HotSpot M3 UTF8 bulk chunk path missing: {fragment}")

# HotSpot equality may prove identity from the exact canonical owner+coordinate pair before
# falling back to logical UTF-16 comparison. It must not compare the String.value sentinel.
for fragment in [
    "java_lang_M3String::owner(left) == java_lang_M3String::owner(right)",
    "java_lang_M3String::coordinate(left) == java_lang_M3String::coordinate(right)",
    "char_at(str1, i) != char_at(str2, i)",
]:
    if fragment not in hotspot_java_classes:
        fail(f"HotSpot M3 String equality path missing: {fragment}")

# JNI region APIs must stay covered by the existing native String harness.
for fragment in [
    "GetStringRegion(env, value, start, length, buffer)",
    "GetStringUTFRegion(env, value, start, length, buffer)",
]:
    if fragment not in native_string_test:
        fail(f"JNI String region proof missing: {fragment}")

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
    "compiledM3Tq(matchRoot)",
    "node.getClass() == Slice.class",
    "node instanceof Branch branch",
    "atom == null || atom.getClass() != Slice.class",
    "M3TQ.fromExact(alternatives)",
]:
    if fragment not in pattern:
        fail(f"Pattern compiled literal-effect TQ boundary missing: {fragment}")

for fragment in [
    "skipM3TqTransparentLeading",
    "current instanceof Begin",
    "current instanceof Caret",
    "current instanceof UnixCaret",
    "current instanceof GroupHead",
    "compiledRequiredLiteral",
    "current.getClass() == Slice.class",
]:
    if fragment not in pattern:
        fail(f"Pattern mandatory literal graph precompute missing: {fragment}")

if "sawSlice && current instanceof GroupHead" not in pattern:
    fail("Pattern mandatory literal planner lost safe GroupHead seam folding")
if "current instanceof GroupTail" in pattern[pattern.find("compiledRequiredLiteral"):pattern.find("private Map<String, Integer> namedGroupsMap")]:
    fail("Pattern mandatory literal planner must not cross GroupTail")

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

# Ordinary StringTable hashing must consume M3's exact Java hash directly. Defensive alternate
# halfsiphash remains UTF-16 based, and intern identity/equality stays with the existing table.
for fragment in [
    "if (!_alt_hash)",
    "return java_lang_String::hash_code_noupdate(val_oop);",
    "jchar* chars = java_lang_String::as_unicode_string_or_null(val_oop, length);",
    "return hash_string(chars, length, true);",
]:
    if fragment not in stringtable:
        fail(f"M3 StringTable hash path missing: {fragment}")

# Expensive donor facts with no JDK21 semantic consumer are intentional NO_PORTs. Adding one of
# these java.lang owners requires an explicit architecture/invariant revision and a real consumer.
for forbidden_path in [
    "src/java.base/share/classes/java/lang/M3StringPrefixZ.java",
    "src/java.base/share/classes/java/lang/M3StringPalindromePrecompute.java",
    "src/java.base/share/classes/java/lang/M3StringSuffixDecision.java",
    "src/java.base/share/classes/java/lang/M3StringLcpPrecompute.java",
    "src/java.base/share/classes/java/lang/M3StringSuffixIndex.java",
]:
    if (ROOT / forbidden_path).exists():
        fail(f"unreviewed no-port String precompute owner appeared: {forbidden_path}")

for required_no_port in [
    "MIndexPrefixZ / MIndexPrefixZCache\tno current M3JDK21 owner\tDONOR_ONLY_NO_JDK21_CONSUMER",
    "MIndexPalindromePrecompute / Manacher facts\tno current M3JDK21 owner\tDONOR_ONLY_NO_JDK21_CONSUMER",
    "MIndexSuffixDecision / suffix DFA facts\tno current M3JDK21 owner\tDONOR_ONLY_NO_JDK21_CONSUMER",
    "LCP range-minimum precompute\tno current M3JDK21 suffix-index owner\tDONOR_ONLY_NO_JDK21_CONSUMER",
]:
    if required_no_port not in port_map:
        fail(f"String donor no-port classification missing: {required_no_port}")

# Standard UTF-8 and modified UTF-8 are different M3 facts. VM/JNI GetStringUTFLength must
# consume the modified UTF-8 fact, with a native regression that covers NUL and surrogate units.
for fragment in [
    "final int modifiedUtf8Length;",
    "modifiedUtf8 = Math.addExact(modifiedUtf8, modifiedUtf8Bytes(unit));",
]:
    if fragment not in facts:
        fail(f"M3 modified UTF8 fact missing: {fragment}")
for fragment in [
    "_facts_modifiedUtf8Length_offset",
    "modified_utf8_length_if_precomputed",
]:
    if fragment not in (java_classes_hpp + java_classes_cpp + inline):
        fail(f"M3 VM modified UTF8 accessor missing: {fragment}")
if "GetStringUTFLength" not in native_encoding_c or "modifiedUtf8Length(s)" not in native_encoding_java:
    fail("M3 modified UTF8 JNI length regression missing")

# VM/JIT experiment boundary: subsystems that still assume String.value payload semantics must
# remain disabled until they become explicitly M3-aware.
for fragment in [
    "if (UseM3StringStorage) {",
    "FLAG_SET_ERGO(OptimizeStringConcat, false);",
    "UseStringDeduplication = false;",
]:
    if fragment not in arguments:
        fail(f"M3 VM experiment boundary missing: {fragment}")
for unsupported in [
    "UseM3StringStorage does not support JVMCI",
    "UseM3StringStorage does not support UseStringDeduplication",
    "UseM3StringStorage does not support CDS archive operations",
    "UseM3StringStorage does not support Flight Recorder",
]:
    if unsupported not in arguments:
        fail(f"M3 unsupported VM subsystem guard missing: {unsupported}")

# VM terminology must describe the actual representation owner. M3 storage is no longer a
# join-only feature; every M3-backed String is covered by the same predicate.
vm_string_bridge = java_classes_hpp + java_classes_inline + java_classes_cpp + hotspot_jni
if "is_m3_joined" in vm_string_bridge:
    fail("stale join-only M3 VM predicate name leaked back into runtime")
for fragment in [
    "is_m3_backed(oop java_string)",
    "java_lang_String::is_m3_backed",
]:
    if fragment not in vm_string_bridge:
        fail(f"M3 VM backed-storage predicate missing: {fragment}")

# HotSpot must not intrinsify contiguous String.value assumptions while M3 storage is active.
for fragment in [
    "if (UseM3StringStorage) {",
    "case vmIntrinsics::_compareToL:",
    "case vmIntrinsics::_indexOfL:",
    "case vmIntrinsics::_equalsL:",
    "case vmIntrinsics::_StringBuilder_toString:",
    "case vmIntrinsics::_StringBuffer_toString:",
]:
    if fragment not in vm_intrinsics:
        fail(f"M3 HotSpot intrinsic guard missing: {fragment}")
if "if (UseM3StringStorage)" not in string_opts or "return;" not in string_opts:
    fail("C2 String concat optimizer is not fail-closed for M3 storage")

# All HotSpot consumers use the canonical M3-backed String predicate. The obsolete
# tuple-era is_m3_joined name must never return.
for label, text in [
    ("StringDedup", dedup),
    ("ArchiveHeapWriter", archive_writer),
]:
    if "is_m3_joined" in text:
        fail(f"{label} reintroduced obsolete is_m3_joined String predicate")
    if "java_lang_String::is_m3_backed" not in text:
        fail(f"{label} lost canonical is_m3_backed String predicate")

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

# codePointCount is executable geometry: when it equals UTF-16 length there are no surrogate
# pairs, so code-point offsets are identical to code-unit offsets. All other cases delegate to
# Character.offsetByCodePoints for exact JDK boundary/exception behavior.
for fragment in [
    "M3StringFacts prepared = storage.factsIfPrepared();",
    "storage.coder() == LATIN1",
    "prepared.codePointCount == storage.length()",
    "long result = (long) index + codePointOffset;",
    "return Character.offsetByCodePoints(this, index, codePointOffset);",
]:
    if fragment not in string:
        fail(f"M3 code-point offset fact route missing: {fragment}")

# Negative filters must reuse existing range facts; they must not force an O(n) fact scan before
# an O(n) exact comparison.
for fragment in [
    "M3StringFacts factsIfPrepared()",
    "owner.rangeFactsIfPrepared(value)",
    "final M3StringFacts rangeFactsIfPrepared(long coordinate)",
]:
    if fragment not in (m3 + owner):
        fail(f"M3 prepared-fact reuse path missing: {fragment}")

# Literal replacement with a flat/bootstrap source and M3 target/replacement must reuse
# direct target search plus builder M3 ingress; M3 operands must not be materialized via value().
for fragment in [
    "M3String targetM3 = trgtStr.m3();",
    "M3String replacementM3 = replStr.m3();",
    "builder.append(replStr);",
]:
    if fragment not in string:
        fail(f"mixed flat/M3 literal replace route missing: {fragment}")

# Mixed flat/M3 prefix checks must not materialize the M3 prefix through value().
for fragment in [
    "M3String prefixM3 = prefix.m3();",
    "if (prefixM3 != null) {",
    "prefixM3.charAt(index)",
]:
    if fragment not in string:
        fail(f"mixed flat/M3 startsWith route missing: {fragment}")

# Range hashes and ASCII case hashes are negative filters only; exact comparison remains in
# String.regionMatches for every surviving candidate.
for fragment in [
    "leftRange.factsIfPrepared()",
    "rightRange.factsIfPrepared()",
    "leftFacts.javaHash != rightFacts.javaHash",
    "leftFacts.asciiLowerHash != rightFacts.asciiLowerHash",
    "leftFacts.asciiUpperHash != rightFacts.asciiUpperHash",
    "leftFacts.asciiTitleHash != rightFacts.asciiTitleHash",
]:
    if fragment not in string:
        fail(f"M3 region precompute filter missing: {fragment}")
if "for (int index = 0; index < len; index++)" not in string:
    fail("exact region comparison loop missing after precompute filter")

# Case conversion is canonical only for ASCII + Locale.ROOT. Locale-sensitive and non-ASCII
# transformations must continue through the stock JDK case engine.
for fragment in [
    "M3StringFacts prepared = storage == null ? null : storage.factsIfPrepared();",
    "prepared != null && locale.equals(Locale.ROOT) && prepared.ascii",
    "storage.asciiCase(false)",
    "storage.asciiCase(true)",
]:
    if fragment not in string:
        fail(f"M3 ROOT ASCII case boundary missing: {fragment}")
if "M3String asciiCase(boolean upper)" not in m3:
    fail("M3String ROOT ASCII canonical case mapper missing")

# Long M3 line traversal reuses the bounded exact position-mask cache for the next CR/LF.
for fragment in [
    "static int indexOfEither(",
    "exact.mask(first) | exact.mask(second)",
    "linearIndexOfEither(",
]:
    if fragment not in position_precompute:
        fail(f"M3 line position precompute missing: {fragment}")
if "int indexOfLineTerminator(int fromIndex)" not in m3:
    fail("M3String line-terminator search owner missing")
for fragment in [
    "private final M3String storage;",
    "storage.indexOfLineTerminator(start)",
    "storage.charAt(end) == '\\r'",
]:
    if fragment not in string:
        fail(f"String.lines lost M3 position-mask traversal: {fragment}")

# Fixed scan-avoidance facts must remain constant-size and have live consumers.
for fragment in [
    "final boolean hasAsciiUpper;",
    "final boolean hasAsciiLower;",
    "final boolean hasBackslash;",
    "final boolean hasLineTerminator;",
]:
    if fragment not in facts:
        fail(f"M3 fixed scan-avoidance fact missing: {fragment}")
for fragment in [
    "if (!prepared.hasBackslash) return this;",
    "(upper && !prepared.hasAsciiLower)",
    "(!upper && !prepared.hasAsciiUpper)",
]:
    if fragment not in m3:
        fail(f"M3 fixed scan-avoidance consumer missing: {fragment}")
if "prepared != null && !prepared.hasLineTerminator" not in string:
    fail("String.lines lost prepared no-terminator fast path")

# Full Unicode/locale case conversion for M3-backed Strings must use the JDK conditional
# casing engine over canonical String access. Source value() materialization is forbidden.
for fragment in [
    "return caseMapM3(locale, false);",
    "return caseMapM3(locale, true);",
    "private String caseMapM3(Locale locale, boolean upper)",
    "ConditionalSpecialCasing.toLowerCaseEx(this, index, locale)",
    "ConditionalSpecialCasing.toUpperCaseEx(this, index, locale)",
    "ConditionalSpecialCasing.toLowerCaseCharArray(this, index, locale)",
    "ConditionalSpecialCasing.toUpperCaseCharArray(this, index, locale)",
]:
    if fragment not in string:
        fail(f"M3 full JDK case-mapping route missing: {fragment}")

# Builder coder selection may use exact M3 range facts locally. This does not change the
# String/HotSpot coder; it only avoids inflating a Latin1 builder for a Latin1-only M3 range.
for fragment in [
    "M3String storage = input.m3();",
    "if (!storage.facts().latin1) {",
]:
    if fragment not in abstract_builder:
        fail(f"AbstractStringBuilder M3 range-coder decision missing: {fragment}")

# Mutable builder sources may reuse immutable M3 target plans, but source bytes are never cached.
for fragment in [
    "static int indexOf(\n            byte[] source,",
    "static int lastIndexOf(\n            byte[] source,",
    "sourceUnit(source, sourceCoder, index)",
]:
    if fragment not in search_precompute:
        fail(f"M3 builder prepared-target search path missing: {fragment}")
for fragment in [
    "M3StringSearchPrecompute.indexOf(",
    "src, srcCoder, srcCount, targetM3, plan, fromIndex",
    "M3StringSearchPrecompute.lastIndexOf(",
    "src, srcCoder, targetM3, plan, fromIndex",
]:
    if fragment not in string:
        fail(f"String builder helper lost prepared M3 target route: {fragment}")

# Shared flat-source substring search (used by AbstractStringBuilder) must not materialize
# an M3-backed target String through value().
for fragment in [
    "M3String targetM3 = tgtStr.m3();",
    "targetM3.charAt(index)",
]:
    if string.count(fragment) < 2:
        fail(f"builder/static search M3 target route missing: {fragment}")

# AbstractStringBuilder must not materialize M3 String.value() while appending String ranges.
for fragment in [
    "M3String storage = s.m3();",
    "s.getBytes(this.value, off, this.count, LATIN1, end - off);",
    "s.getBytes(this.value, i, j, UTF16, end - i);",
    "s.getBytes(this.value, off, this.count, UTF16, end - off);",
]:
    if fragment not in abstract_builder:
        fail(f"AbstractStringBuilder M3 bulk String append route missing: {fragment}")

# Existing M3 storage remains the execution owner for repeat; the admission/join feature gate
# controls creation of M3 storage, not operations on a String that already owns it.
if "M3String storage = m3();" not in string
        or "return new String(storage.repeat(count));" not in string:
    fail("M3-backed repeat no longer executes on canonical storage")

# Once a String is already M3-backed, range preservation is not optional composition.
# substring must keep the same canonical owner+coordinate regardless of the admission/join gate.
if "M3String storage = m3();" not in string
        or "return new String(storage.slice(beginIndex, endIndex));" not in string:
    fail("M3-backed substring no longer preserves canonical owner+coordinate directly")

# Builders may own their mutable byte[] as required by AbstractStringBuilder, but M3-backed
# String ingress must bulk-copy canonical ranges and must not materialize String.value().
for fragment in [
    "M3String storage = s.m3();",
    "s.getBytes(this.value, off, this.count, LATIN1, end - off);",
    "s.getBytes(this.value, off, this.count, UTF16, end - off);",
]:
    if fragment not in builder:
        fail(f"M3 builder bulk String ingress missing: {fragment}")
if "M3StringBuilderInteropTest.java" not in workflow:
    fail("M3 String workflow lost builder interop proof")
if "M3StringBuilderInteropTest.java test/jdk/jdk/internal/mindex/M3TQFactsTest.java" not in workflow:
    fail("M3 String workflow does not execute builder interop proof")

# History-qualified M3 literal split is a conservative multi-unit regex subset only.
for fragment in [
    "static boolean isConservativeLiteralRegex(String expression)",
]:
    if fragment not in m3:
        fail(f"M3 literal split classifier missing: {fragment}")
for fragment in [
    "regex.length() > 1",
    "M3String.isConservativeLiteralRegex(regex)",
    "return split(storage, M3String.canonicalize(regex), limit, withDelimiters);",
    "M3String storage, M3String delimiter, int limit, boolean withDelimiters",
    "storage.indexOf(delimiter, off)",
    "substring(next, next + delimiterLength)",
]:
    if fragment not in string:
        fail(f"M3 literal split route missing: {fragment}")
if "Pattern pattern = Pattern.compile(regex);" not in string:
    fail("general regex split fallback missing")
if (ROOT / "src/java.base/share/classes/java/lang/SubM3String.java").exists():
    fail("donor SubMIndexString taxonomy was copied instead of owner+coordinate reuse")

# M3-backed constructors must store only the empty compatibility sentinel.
if string.count("storage.compatibilityValue()") < 4:
    fail("M3-backed String constructors no longer consistently use the empty sentinel")

if "if (java_lang_String::is_m3_backed(java_string))" not in dedup:
    fail("String deduplication is not fail-closed for M3-backed values")
if "if (UseM3StringStorage)" not in stringopts:
    fail("legacy C2 StringConcat optimization is not disabled for M3 storage")
if "if (java_lang_String::is_m3_backed(string))" not in archive_writer:
    fail("CDS String sizing is not fail-closed for M3 values")


# Escape translation must scan canonical M3 storage and rebuild from slices/unit atoms; the
# stock char[] implementation remains fallback-only for non-M3 Strings.
for fragment in [
    "M3String translateEscapes()",
    "pieces.add(M3StringPool.internUnit(escaped))",
    "return new String(storage.translateEscapes());",
]:
    if fragment not in (m3 + string):
        fail(f"M3 translateEscapes canonical route missing: {fragment}")

if "if (M3String.admissionEnabled())" not in string
        or "return new String(M3StringPool.internUnit(c));" not in string:
    fail("String.valueOf(char) does not use direct M3 unit interning after activation")

# Exact single-byte charset ingress may bypass decoded compact byte[] staging only when the
# mapping is exact: ISO-8859-1, or all-ASCII UTF-8/US-ASCII. Other decoding stays stock-authority.
for fragment in [
    "maybeAdmitDecodedSingleByte(charset, bytes, offset, length)",
    "charset == ISO_8859_1.INSTANCE",
    "charset == UTF_8.INSTANCE || charset == US_ASCII.INSTANCE",
    "StringCoding.countPositives(bytes, offset, length) == length",
    "M3String.admitLatin1Bytes(bytes, offset, length)",
    "M3StringPool.internLatin1Bytes(source, offset, length)",
    "M3StringAtom.localLatin1Bytes(",
]:
    if fragment not in (string + m3 + pool + atom):
        fail(f"M3 exact byte-array ingress missing: {fragment}")

# Unicode code-point array ingress emits canonical UTF-16 directly into native storage.
for fragment in [
    "M3String direct = maybeAdmitCodePoints(codePoints, offset, count);",
    "M3String.admitCodePoints(value, offset, count)",
    "M3StringPool.internCodePoints(source, offset, count)",
    "M3StringAtom.localCodePoints(",
    "throw new IllegalArgumentException(Integer.toString(cp));",
]:
    if fragment not in (string + m3 + pool + atom):
        fail(f"M3 direct code-point ingress missing: {fragment}")

# StringBuilder ingress reads its compact backing directly and snapshots once into M3.
for fragment in [
    "M3String direct =",
    "maybeAdmitCompact(",
    "M3String.admitCompactBytes(value, sourceOffset, length, sourceCoder)",
    "M3StringPool.internCompactBytes(source, sourceOffset, length, sourceCoder)",
    "M3StringAtom.localCompactBytes(",
]:
    if fragment not in (string + m3 + pool + atom):
        fail(f"M3 direct StringBuilder ingress missing: {fragment}")

if "return internCompactBytes(value, 0, value.length >> coder, coder);" not in pool:
    fail("M3 scalar ingress bypasses canonical coder normalization")

# Mutable char[] ingress must snapshot directly into canonical M3 storage after activation.
# Do not compress to a transient compact byte[] first and then copy again into native storage.
for fragment in [
    "M3String direct = maybeAdmit(value, off, len);",
    "M3String.admit(value, offset, length)",
    "M3StringPool.internChars(source, offset, length)",
    "M3StringAtom.localChars(source, offset, length, coder, id, hash64)",
]:
    if fragment not in (string + m3 + pool + atom):
        fail(f"M3 direct char-array ingress missing: {fragment}")

# Empty-target literal replacement is defined at every UTF-16 code-unit boundary, including
# between surrogate halves. M3 preserves that exact contract without StringBuilder flattening.
for fragment in [
    "M3String replaceEmptyTarget(M3String replacement)",
    "pieces.add(slice(index, index + 1));",
    "return new String(storage.replaceEmptyTarget(replacementM3));",
]:
    if fragment not in (m3 + string):
        fail(f"M3 empty-target replacement route missing: {fragment}")

# Canonical single-unit transforms must re-enter the native pool directly rather than create
# temporary one-character String/byte[] payloads.
if "String.COMPACT_STRINGS" not in pool
        or "&& mappedLatin1(address, lengths[row], bigEndian)" not in pool:
    fail("mapped M3 lexicon coder ignores CompactStrings mode")
if "String.COMPACT_STRINGS && StringLatin1.canEncode(unit)" not in pool:
    fail("M3 direct unit interning ignores CompactStrings mode")

for fragment in [
    "static M3String internUnit(char unit)",
    "active.findUnit(unit)",
    "return M3String.whole(active.atom(row))",
    "M3StringAtom.localUnit(unit, coder, id, hash64)",
    "existing.charAt(0) == unit",
    "int findUnit(char unit)",
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
    "MIndexString split/splitWithDelimiters indexed-result repair\tjava.lang.String + java.lang.M3String\tIMPLEMENTED_HISTORY_RECOVERY",
    "DO_NOT_PORT_TO_JAVA_LANG_STRING",
]:
    if fragment not in port_map:
        fail(f"precompute port map missing: {fragment}")

if "M3StringPrecomputeSearchTest.java test/jdk/" in workflow:
    fail("M3 String workflow contains concatenated path entries")

for required_trigger in [
    "src/java.base/share/classes/java/lang/AbstractStringBuilder.java",
    "src/java.base/share/classes/java/util/regex/Pattern.java",
    "src/java.base/share/classes/java/util/regex/Matcher.java",
    "m3/docs/synexia-recipe-application.md",
    "AGENTS.md",
]:
    if required_trigger not in workflow:
        fail(f"M3 String workflow lost trigger path: {required_trigger}")

for required_gate in [
    "M3StringFactsCompositionTest.java",
    "M3StringCodePointPrecomputeTest.java",
    "M3StringPrecomputeSearchTest.java",
    "M3StringPositionPrecomputeTest.java",
    "M3StringInternTest.java",
    "M3StringLiteralSplitTest.java",
    "nativeEncoding/StringPlatformChars.java",
    "M3TQFactsTest.java",
    "M3RegexLiteralTQTest.java",
]:
    if required_gate not in workflow:
        fail(f"M3 String workflow lost verification gate: {required_gate}")

# Synexia owns reusable M3JDK21 recipes; this target keeps only the applied product and receipt.
for fragment in [
    "recipe_name\tcom.synexia.rewrite.M3Jdk21StringHistoryConvergence",
    "synexia_repository\thsoliwal/com.synexia",
    "synexia_commit\t",
    "synexia_java_manifest\tsynexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-string-history-convergence/manifest.tsv",
    "synexia_text_manifest\tsynexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-string-history-convergence/manifest.tsv",
    "synexia_java_manifest_sha256\t",
    "synexia_text_manifest_sha256\t",
    "m3jdk21_applied_result_commit\t",
    "runtime_owner\tM3JDK21",
    "recipe_owner\tSynexia",
    "runtime_dependency_on_synexia\tfalse",
    "target_recipe_disposition\tTHIN_RECEIVER_ONLY",
    "application_mode\tFIXED_POINT_NO_SOURCE_DELTA",
    "verification_status\tSYNEXIA_CANONICAL_PACKET_31_OF_31_TARGETS_HASH_MATCHED",
]:
    if fragment not in recipe_receipt:
        fail(f"Synexia recipe application receipt missing: {fragment}")

for forbidden_recipe_path in [
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jni-newstring-admission.yml",
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-char-boundary.yml",
    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-history-convergence.yml",
    "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-history-convergence",
    "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-string-history-convergence",
    "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/M3StringHistoryConvergenceRecipeTest.java",
    "m3/tooling/tq/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-tq",
    "m3/tooling/tq/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq",
    "m3/tooling/tq/src/test/java/com/m3/tq/TQTest.java",
]:
    if (ROOT / forbidden_recipe_path).exists():
        fail(f"Synexia-owned recipe duplicated in M3JDK21: {forbidden_recipe_path}")

if "M3StringHistoryConvergenceRecipeTest" in workflow:
    fail("M3JDK21 workflow must not execute a local Synexia-owned recipe proof")
if "m3/tooling/migration-recipes/**" in workflow:
    fail("M3JDK21 String workflow must not treat local recipe custody as authoritative")

print("M3_STRING_SOURCE_INVARIANTS_PASS")
