<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Bootstrap String-facing contracts and fixed facts

[Guide](README.md) · [Runtime owners](runtime-owners.md) · [Bootstrap ownership](bootstrap-string-ownership.md)

This reference explains the complete `java.lang.M3String` and
`java.lang.M3StringFacts` bodies at source pin
`ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2`, with selected `String`, `System`,
HotSpot and JNI caller boundaries. The [ownership reference](bootstrap-string-ownership.md)
covers the other four fully reviewed bodies: `M3StringOwner`, `M3StringAtom`,
`M3StringPool` and `M3StringTuple`. Together these are a bounded six-body source
review, not a complete `java.lang`, VM, GC or native-platform audit.

These package-private classes implement the existing public `java.lang.String`
contract. Even their `public` interface overrides are internal implementation
members, not a supported application API. They are separate from the stock-JVM
[`M3Text` compatibility bridge](indexed-string-reference.md) and from the
`jdk.internal.mindex` StringBacking family. The six sibling code-text/search/
position/code-point/regex precompute owners are identified as call dependencies;
their full bodies are **DECLARATIONS_ONLY** in this review.

All linked test sources were **read, NOT_RUN**. No JDK build, test, recipe,
policy verifier, benchmark or native library was executed for this documentation.
The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md),
[naming map](../../m3/docs/name-mapping.json),
[lineage](../../m3/docs/m3string-synexia-lineage.md) and
[runtime integration guide](../../m3/runtime-integration/README.md) retain their
ownership, licensing and pending qualification boundaries. Documentation does not
advance the ordered receiving phase or establish universal content interning,
performance gains or zero-allocation execution.

## 1. Admission and composition

Sources: [descriptor and admission][m3-admission],
[constructor and JNI ingress gates][string-admission],
[System activation][system-activation].

`M3String` retains exactly two instance fields: a nonnull `M3StringOwner owner`
and a packed `long value`. The coordinate stores a UTF-16 start and count.
Payload lifetime, pool budgets, mapped admission, tuple canonicalization and
owner/range fact caching belong to the [ownership page](bootstrap-string-ownership.md).
Neither descriptor identity nor an equal spelling promises a globally shared
owner across processes, mappings or all construction histories.

| Entry point | Behavior and boundary |
| --- | --- |
| Private constructor; `whole`, `range`, `empty` | Construct whole-owner/range descriptors or reuse the empty descriptor. The constructor checks the packed range against owner length. These factories are trusted internals: `range` returns the empty descriptor for zero length before doing the constructor checks, so they are not a new uniform public validation API. |
| `activate`, `ready`, `admissionEnabled` | Activation is synchronized and one-time after success; lexicon initialization runs while the thread-local admission-recursion marker is set. `ready` is published only after initialization completes. Admission is enabled only when ready and the current thread is not already admitting. |
| `admitCodePoints`, `admitCompactBytes`, `admitLatin1Bytes`, both `admit` overloads | Require nonnull input, set the recursion marker, delegate to the matching pool admission, and remove the marker in `finally`. Source shape/range rules and refusal belong to that pool operation. These trusted delegates do not themselves repeat all outer bootstrap gates. |
| `canonicalize(String)` | Reuses existing M3 storage, maps an empty spelling to the empty descriptor, or admits a flat spelling for the new composition. It does not attach that descriptor back to the old flat `String`. A refused flat admission returns `null`. |
| `join(String, String)` | Canonicalizes both operands in order, returns `null` if either admission is refused, otherwise delegates to pool concat. |
| `join(String[])` | Requires a nonnull directory and elements, omits empty pieces, and combines adjacent pairs level by level. Its temporary lists contain descriptors, not retained canonical spelling arrays. A required flat admission can refuse with `null`. |
| `joinDesignated`, `joinAddDesignated` | Compose prefix, elements separated by delimiter, then suffix. The public caller has already converted user `CharSequence` values to `String`. The binary carry directory uses O(log piece count) temporary descriptor slots; this bound is not a claim about all caller allocations or retained tuple nodes. |
| `joinValues`, `concat` | Internal composition of already admitted values. `joinValues` folds adjacent pairs; `concat` requires a nonnull operand and delegates to the pool. |

`System` activates this family after module bootstrap when storage is requested.
The reviewed `String.maybeAdmit*` constructor gates also require the VM to be
booted, admission enabled and nonempty input. **`String.m3()` only reads the
field**. It is not an unconditional lazy-admission getter. The separate
`ensureM3`/`m3AdmitNative` ingress path may attach admitted storage after those
gates. Flat/bootstrap strings and refused admissions retain a real flat fallback.
A `null` admission is not an empty result and does not mean every possible failure
is a recoverable budget refusal.

The designated [`String.join` caller][string-join] converts elements before the
M3 adapter; the singleton/no-affixes path creates a fresh `String` wrapper.
[`String.concat`][string-concat] first preserves its empty-argument identity case,
then tries `m3Concat`, and falls back when it returns `null`. The reference does
not treat descriptor reuse as permission to change user callback order or public
wrapper identity.

## 2. UTF-16 geometry, equality and hashes

Sources: [slicing][m3-slice], [accessors and facts dispatch][m3-geometry],
[exact content equality][m3-equality], [String equality][string-equality],
[String comparison][string-order].

All lengths, starts, end-exclusive ranges, substring coordinates, search results
and copy offsets in this family are **UTF-16 code units**. They are not bytes,
Unicode scalar counts or grapheme clusters. `charAt` checks its logical index and
returns one unit, including a surrogate. `slice` checks both endpoints, returns
`this` for the complete range, and otherwise offsets the coordinate into the same
owner. It may deliberately split a valid surrogate pair; the resulting range's
facts must treat its visible edge independently. `subSequence` delegates to
`slice`; `sliceOf` returns `null` for a flat source rather than admitting it.

| Identity or hash operation | Meaning |
| --- | --- |
| `owner`, `coordinate`, `start`, `end`, `length`, `coder` | Describe the visible range and its retained owner. `coder` is the owner's storage coder; a range is not automatically narrowed to a smaller coder. |
| `sameCoordinate` | Exact owner object identity plus identical packed coordinate. Returns false for null. |
| `identityHash64` | In-process mixed hash using owner identity, coordinate and owner structural hash; collisions remain possible. Not a portable content ID. |
| `structuralHash64` | Returns the **owner's** structural hash, including for a slice. It is not a content hash of just the visible range. |
| `hashCodeValue` | Uses the stored owner Java hash for a whole owner; otherwise prepares/uses range facts. |
| `hashKnownToDiffer` | Rejects only a known unequal Java hash. Uses a whole-owner hash or already-prepared range facts and never prepares facts merely to answer this gate. |
| `facts`, `factsIfPrepared` | Dispatch to whole-owner or packed-coordinate range facts. The latter may return null; absence establishes no semantic result. |
| `contentEquals` | Requires a nonnull `String`; checks same coordinate, length and available hash rejection, then compares exact UTF-16 units. Equal hashes do not prove equal text. |

`String.equals` checks type and wrapper identity before the internal dispatch.
For a mixed flat/M3 pair it uses the flat side's already-cached Java hash only
when known, then performs exact equality. `compareTo` accepts same-coordinate
identity as equality; otherwise an M3-involved pair compares UTF-16 units and
then lengths. Hash collision, equal candidate signals and matching packed edges
never replace the exact comparison.

[`String.codePointAt` and `codePointBefore`][string-codepoints] combine only valid
adjacent high/low pairs. `codePointCount` can use a prepared sibling geometry or
range facts; `offsetByCodePoints` has a proven one-unit-per-code-point shortcut,
an optional geometry path and `Character.offsetByCodePoints` fallback. Those
caller boundaries do not document the sibling geometry's cache or implementation.

## 3. Direct traversal and stream callbacks

Sources: [cursor and spliterators][m3-streams], [String stream dispatch][string-streams].

`CodeUnitCursor` retains its source descriptor, the current scalar owner and
primitive visible-run bounds. `at` checks the requested logical index, then
`seek` descends tuple children and their coordinates only when the index leaves
the cached scalar run. The retained owner references keep the text reachable;
the cursor is not an address-only lifetime token. It is thread-confined state,
not a shared concurrent cursor.

| Stream | Splitting and values | Characteristics and size |
| --- | --- | --- |
| `charsStream` / `CharsSpliterator` | Splits at the UTF-16 midpoint and emits zero-extended units in encounter order. Surrogates remain separate units. | `ORDERED`, `IMMUTABLE`, `SIZED`, `SUBSIZED`; remaining unit count is exact. |
| `codePointsStream` / `CodePointsSpliterator` | Emits a supplementary value for a valid pair and an individual unit otherwise. A midpoint splitting a high/low pair moves back one unit, even when that pair crosses owners. A split that then has no prefix returns null. | `ORDERED`, `IMMUTABLE` only; remaining UTF-16 units are an upper-bound estimate, not an exact code-point count. |

Both factories initially create sequential streams; stream machinery can split
independent instances for parallel traversal. Both `tryAdvance` and
`forEachRemaining` reject a null action before checking exhaustion. Callback
exceptions propagate. The character `tryAdvance` increments its position while
fetching the unit, before invoking the callback; its `forEachRemaining` records
the final fence only after the whole loop succeeds. The code-point variant
advances its one- or two-unit position before calling the action and uses
`tryAdvance` for bulk traversal. Do not infer a supported retry/resume guarantee
after an action throws. Stream/cursor construction and splitting allocate
objects; direct traversal is not a blanket no-allocation or throughput claim.

## 4. Fixed facts and seam composition

Sources: [fields and construction][facts-fields], [scan][facts-scan],
[composition][facts-compose].

One `M3StringFacts` instance contains **23 primitive fields**. It owns no spelling,
search token directory, native buffer or length-proportional payload. That retained
shape does not prohibit temporary work: `scan` allocates a `char[]` of
`min(utf16Length, 1024)` units and bulk-reads successive windows. The class also
has a static 256-entry Latin-1 unit-signal table, a 1,024-unit window constant and
the standard UTF-8 replacement-byte count. These are different allocation and
ownership categories.

| Fields | Exact interpretation |
| --- | --- |
| `utf16Length`, `codePointCount`, `unpairedSurrogateCount` | Visible unit count; valid pairs count once as code points; each isolated surrogate counts once and contributes to the unpaired count. |
| `utf8Length`, `modifiedUtf8Length` | Standard replacement UTF-8 length versus JNI modified UTF-8 length; see the separate encoding section. |
| `javaHash`, `hash31Power` | Java 31-polynomial hash and `31^length` with intentional Java `int` wrapping. |
| `firstUtf16Unit`, `lastUtf16Unit` | First/last visible units; zero for an empty scan. An empty result is distinguished by length, not by these values. |
| `ascii`, `latin1` | Every visible unit is at most U+007F / U+00FF. Both are true for an empty scan. |
| `asciiUpperHash`, `asciiLowerHash`, `asciiTitleHash` | Hashes of ASCII-only upper/lower transforms; title means first unit uppercased and remaining units lowercased. Non-ASCII units pass through these helper transforms. These are not general Unicode or locale case-mapping facts. |
| `prefix4`, `suffix4` | At most four UTF-16 units, left-aligned prefix and right-aligned suffix, interpreted together with length. |
| `bitSignal64`, `bigramSignal64`, `trigramSignal64` | Collision-prone candidate signals. Unit signals fold ASCII uppercase to lowercase; n-gram signals use ordered raw unit groups. A positive signal does not prove a match. |
| `trimStart`, `trimEnd` | UTF-16 bounds under legacy `String.trim`: units at most U+0020 are removed. |
| `stripStart`, `stripEnd` | UTF-16 bounds under `Character.isWhitespace` code-point semantics. Empty scan bounds are all zero; an all-removable nonempty scan uses start equal to length and end zero. |

The scan folds hash, signal, edge and whitespace lanes together. It delays a high
surrogate by one unit so its successor resolves it, including across the 1,024-unit
scratch boundary; a final pending high surrogate remains isolated. Surrogate
units still participate in the unit and n-gram lanes even when combined for
code-point/standard-UTF-8 purposes.

`compose` reuses the nonempty side when the other side is empty. Otherwise it:

- adds checked lengths and combines hashes using the right `hash31Power`
- combines first/last units, ASCII/Latin-1 flags and packed edges
- unions signals and adds the bigram plus up to two trigrams crossing the seam
- offsets or inherits the trim/strip bounds
- detects a left-final high plus right-first low surrogate, reduces summed
  code-point count by one and unpaired count by two, and replaces the two
  isolated-surrogate standard UTF-8 contributions with one four-byte pair
- keeps modified UTF-8 length additive across that same seam

Hash/power/mixing arithmetic intentionally wraps. Checked size arithmetic does
not: `utf8Length` is an `int`, and scan/compose may throw `ArithmeticException`
when its checked sums cannot be represented. Fixed-size metadata does not mean
fact preparation is always fail-open or every legal logical descriptor can be
encoded in a single Java array.

There is also a source-observed extreme-size limit in the scan loop: its `int`
window base advances by 1,024 rather than by the final window's actual count.
For logical lengths 2,147,482,625 through `Integer.MAX_VALUE`, if execution
reaches and completes the final partial window, the next increment wraps the
base negative and the following bulk-read range fails bounds. This concerns the
`scan` path, not every owner fact-composition path; earlier encoding-length or
other failures may occur first. It is an unexecuted source/arithmetic finding
that requires a matched-runtime regression check, not a claim that maximal-size
runtime behavior was tested.

## 5. Candidate gates and exact search boundaries

Sources: [fact gates and signal helpers][facts-gates],
[ASCII mixed-side hash][facts-ascii], [descriptor search][m3-search],
[flat-needle String search][string-search], [mixed-side case gate][string-case-gate].

`mayContain(M3StringFacts)` and `mayContain(String)` apply length and unit/bigram/
trigram necessary conditions. Flat-needle folding reads the needle's units
without admitting it as canonical storage. `prefixMayMatch` and `suffixMayMatch`
compare up to four packed edge units; callers still enforce the full length/range
and exact match. `mayContainCodeUnit` and `codeUnitSignal` use the same two-bit
unit signal. `unitSignal`, `latin1UnitSignals`, `addSignal`, n-gram helpers and
`mix32`/`mix64` implement those derived filters, not collision-free identities.

`M3String.mayContain` returns true when either fact record is absent. A false
candidate gate can avoid impossible work under its preconditions; a true result
only permits the next exact operation. `startsWith` checks offset bounds,
optionally rejects using prepared signals/edges, then compares every unit.

| Internal search | Dispatch and exactness boundary |
| --- | --- |
| `indexOf(char, from, end)`, `lastIndexOf(char, from)` | Optional prepared unit-signal rejection, then `M3StringPositionPrecompute`. |
| `indexOfCodePoint`, `lastIndexOfCodePoint` | Invalid code point returns -1; a BMP value uses the unit search. A supplementary value locates high-surrogate candidates and checks the following low surrogate inside the permitted range. |
| Both `indexOf(M3String, ...)` overloads and `lastIndexOf(M3String, ...)` | Handle empty/single-unit needles, apply candidate gates, then ask `M3StringSearchPrecompute` for a plan. A missing plan retains the direct exact UTF-16 search. |
| Flat needle on an M3 `String` receiver | The reviewed `String` loops optionally use prepared facts and then exact unit comparison; the flat needle is not admitted just to search it. |

The public bounded `String.indexOf(str, begin, end)` checks bounds before the M3
helper, while its unbounded-from-index form applies its own normalization.
Internal helper overloads are not a replacement public validation surface; in
particular their clamps/early returns assume the caller's appropriate range
contract. No cache-budget, concurrency or complexity guarantee for the sibling
precompute implementations is inferred from these call edges.

`asciiLowerHashOrNegative` returns an unsigned 32-bit folded hash in a `long`,
or -1 if any inspected unit is non-ASCII. The mixed-side ignore-case gate only
uses it for a whole M3 region with already-prepared ASCII facts. Non-ASCII cannot
be rejected merely because the other spelling is ASCII: Unicode ignore-case
relationships can cross that boundary. Any successful gate still leaves the
exact `String` case comparison authoritative.

## 6. Copy, encoding and compatibility shadows

Sources: [copy and encoding implementations][m3-output],
[checked no-replacement bridge][string-no-repl],
[native final-array entry points][native-shadows],
[String output dispatch][string-output], [String value accessor][string-value].

| Boundary | Ownership and behavior |
| --- | --- |
| `getChars(sourceBegin, sourceEnd, destination, destinationBegin)` | Checks source bounds and destination extent, then fills the caller's existing `char[]` through the owner. |
| Internal `getBytes(destination, sourceBegin, destinationBegin, destinationCoder, count)` | Requires a destination, accepts only Latin-1/UTF-16 coders, checks source and scaled destination extent, and delegates the copy. `destinationBegin` is a logical unit offset before coder scaling. It is not a public Charset encoder. |
| `encode(Charset)` | Requires a Charset and returns independent output bytes. Uses specialized UTF-8, ISO-8859-1 and US-ASCII paths, otherwise a CharsetEncoder. |
| `encodeUtf8` | Allocates the fact-sized result, emits valid pairs as four bytes and uses the JDK 21 single-byte `?` replacement for isolated surrogates. ASCII can use a direct unit copy. The non-ASCII path checks its final byte count and throws `InternalError` on a mismatch. |
| `encodeSingleByte` | Direct copy when the ASCII/Latin-1 fact proves representability. Otherwise allocates by code-point count, emits representable units and one `?` per unmappable code point, consuming a valid surrogate pair together. |
| `encodeWithEncoder` | Configures malformed/unmappable replacement. An ASCII-compatible `ArrayEncoder` can take the direct ASCII output path. Otherwise wraps the descriptor using `CharBuffer.wrap(this)` and encodes/flushes into a byte buffer; it may trim the allocated byte result with a copy. |
| `encodeNoRepl`, `encodeUtf8NoRepl`, `encodeWithEncoderNoRepl`, `unmappable` | Preserve the internal `IllegalArgumentException`/coding-exception bridge. UTF-8 rejects isolated surrogates; Latin-1 rejects nonrepresentable units; the generic path also reads `CharBuffer.wrap(this)`. The `String.getBytesNoRepl` checked wrapper unwraps the coding-exception cause. |
| `materialize`, `charShadow`, `nativeByteShadow`, `nativeCharShadow` | Explicit JNI entry points produce fresh Compact-String byte or UTF-16 char arrays. They are outputs, not cached canonical spellings. Native byte-shadow allocation checks scaled length against `INT_MAX`; JNI allocation/callback errors still propagate. |
| `compatibilityValue` | Lazily caches one shared zero-length byte-array **layout sentinel**, allocated through the empty native shadow boundary. It is never the spelling of a nonempty String. |

The generic Charset path does **not** first manufacture a caller-owned UTF-16
projection. This observation does not promise that arbitrary Charset providers,
encoders, buffers, trimming or their implementation internals allocate nothing
else. Output bytes are independent of canonical storage; temporary capacity may
exceed the final byte count. The replacement path wraps a coding exception that
should not arise under its replacement setup in `Error`; custom provider
behavior is not silently converted to pool refusal.

For an M3-backed String, the internal `String.value()` requests a fresh
materialized byte shadow, while public `toCharArray()` requests a fresh char
shadow. The ordinary `value` field's empty sentinel must never be read as text.
Constructor/bootstrap flat arrays, caller result arrays, caller-provided copy
buffers, transient fact scratch, temporary descriptor directories and static
lookup tables all have distinct roles. The supported invariant is no retained
canonical spelling array in the descriptor/owner/fact family, not “all arrays
exist only through JNI.”

### Standard UTF-8 is not JNI modified UTF-8

Sources: [fact scan/composition][facts-scan], [whole-owner cached VM field][vm-mutf8-cache],
[VM fallback][vm-mutf8-walk], [JNI ingress/egress guide](../../m3/docs/mindex-jni-string-ingress.md).

| UTF-16 input | Standard UTF-8 replacement length | JNI modified UTF-8 length |
| --- | --- | --- |
| NUL | 1 | 2 |
| Nonzero ASCII unit | 1 | 1 |
| Unit U+0080 through U+07FF | 2 | 2 |
| Non-surrogate BMP unit above U+07FF | 3 | 3 |
| Valid high/low pair | 4 | 6, encoded as two surrogate units |
| Isolated surrogate | 1 with this JDK's `?` replacement | 3 |

The fact scan accumulates modified length in a `long` and stores **-1** when the
result exceeds `Integer.MAX_VALUE`. Composition propagates that sentinel or a
sum too large for the cached `int`. It means “not cacheable in this field,” not
an encoding error, an empty string or standard UTF-8 length.

HotSpot's cached-length reader uses `modifiedUtf8Length` only for a whole owner
with prepared owner facts. A range, absent facts or negative cached length causes
the reviewed caller to walk actual units in bounded native scratch. That fallback
still returns an `int`; this source path is **not proof that every oversized
modified-UTF-8 result fits a JNI `jint` or succeeds**. Standard UTF-8's checked
`int` fact has its separate overflow boundary.

The reviewed [ordinary JNI entry points][jni-ordinary], [region copies][jni-regions]
and [critical acquire/release branch][jni-critical] support the existing guide:
construction may admit after the gates; ordinary char/UTF access allocates copies
and matching release frees them; regions fill caller storage; critical access
copies when M3 storage is enabled, otherwise the stock branch can pin non-Latin-1
arrays. These selected ranges were read, not exercised. They do not close the
broader JIT/GC/CDS/SA, allocation-failure, platform or ABI test matrix.

## 7. Transformations and String wrapper semantics

Sources: [line transforms and repeat][m3-transforms],
[escape/case/replace implementations][m3-replace],
[String replacement wrappers][string-replace],
[String case wrappers][string-case], [String line wrappers][string-lines],
[String escape wrapper][string-escapes].

| Operation | Concrete internal behavior and outer boundary |
| --- | --- |
| `indent` | Scans CR, LF and CRLF logical lines. Positive amounts prepend a reusable space composition; negative amounts remove up to that many leading whitespace units, with `Integer.MIN_VALUE` removing all leading whitespace. Each emitted line ends in LF. Positive logical output size is checked before composition. Empty storage returns empty. |
| `stripIndent` | Computes common incidental indentation from nonblank lines, considering a final blank line; a final CR/LF opts out of common outdent. Trims trailing whitespace per line and composes normalized LF separators, with a final LF in the opt-out case. |
| `repeat` | Rejects negative repetitions, returns empty for zero/empty input and `this` for one; otherwise checked multiplication followed by repeated doubling/concat. It shares descriptors/owners rather than promising one flat output. |
| `translateEscapes` | Composes unchanged slices and units for `b`, `f`, `n`, `r`, `s`, `t`, quotes and backslash; uses the JDK octal-width and CR/LF/CRLF line-continuation cases, subject to the extreme-size limit below. Invalid or trailing escapes throw `IllegalArgumentException`; this is not a Unicode-escape parser. When there is no escape, the helper reuses the descriptor. |
| `asciiCase` | Calls `facts()` and requires ASCII, throwing `IllegalStateException` otherwise. Maps only ASCII letters and composes unchanged slices plus changed units. The `String` caller restricts this lane by locale and ASCII facts; full Unicode/locale mapping remains in the existing String implementation. |
| `replace(char, char)` | Same character, proven absent candidate or no exact match reuses the descriptor. Otherwise finds nonoverlapping unit occurrences and composes source slices plus the replacement unit. |
| `replaceEmptyTarget` | Inserts replacement before/between/after every UTF-16 unit. An empty replacement can reuse the descriptor while the `String` wrapper creates the required fresh result for this path. It is not a code-point-boundary insertion. |
| `replaceAt`, `replaceFlatTarget`, `replaceMatches` | Nonempty targets only; start from the already-found match, advance past each whole match and compose unchanged slices plus replacement. The flat target drives exact search without entering the pool. The shared helper checks logical output length and honors `firstOnly`. Its caller supplies coherent target/source/first-match state. |
| `isLiteralRegex`, `isLiteralRegexReplacement`, `replaceLiteralRegex` | Admit only a nonempty BMP literal regex with no surrogate, metacharacter or escape; replacement must contain neither `$` nor backslash syntax. Locate the first match before canonicalizing replacement. No match returns the original; refusal returns null for the caller's Pattern fallback; a match creates a String wrapper even if its descriptor aliases an input. |

A second source-only extreme-size limit concerns `translateEscapes`: its octal
lookahead computes `index + 2` in an `int` when the first digit is 0–3. At logical
length `Integer.MAX_VALUE`, a backslash at `Integer.MAX_VALUE - 3` followed by such
a digit and a final octal digit leaves `index` at `Integer.MAX_VALUE - 1` after
reading the first digit; adding two wraps negative.
If that path is reached, the remaining octal digit is not consumed as part of the
escape. This is a source/arithmetic finding, **NOT_RUN**, rather than a passing
maximal-size escape contract; no source fix is included here.

The reviewed `String.replace(CharSequence, CharSequence)` calls `target.toString()`
and then `replacement.toString()` before inspecting/admitting the result pieces.
For a nonempty general target it searches first; absence preserves receiver
identity without admitting target or replacement. A flat search target is never
admitted by that lane. A needed flat receiver/replacement may be admitted only
for the result, with the original flat path retained on refusal. The one-character
case has its own char-replacement dispatch.

`String.replaceFirst`/`replaceAll` use the conservative literal lane only when
both predicates allow it; all other regex/replacement syntax remains in
`Pattern`/`Matcher`. The separately reviewed [`String.matches` call edge][string-matches]
tries a literal comparison or sibling `M3StringRegexDp` result before its
`Pattern.matches` fallback. This is not a full audit or new acceptance claim for
the regex precompute body.

The ASCII case wrapper excludes Turkish, Azeri and Lithuanian from its simple
mapping lane. [`trim`, `strip`, `stripLeading`, `stripTrailing`][string-trim]
consume the corresponding fact bounds, preserve unchanged receiver identity and
return empty or a new range wrapper when appropriate. For nonempty receivers,
`indent`, `stripIndent` and `translateEscapes` wrap their internal result in a new
`String`; empty receivers return the empty String before dispatch. A reused
descriptor therefore does not imply a reused public object.

### Public exceptions versus trusted helpers

The public [`String.repeat`][string-repeat] checks the logical unit limit before
calling `M3String.repeat` and throws `OutOfMemoryError` when that required length
is too large. The helper's `Math.multiplyExact` can instead throw
`ArithmeticException` if called directly outside that wrapper. Similarly, the
public empty-target replacement checks its result length and converts checked
arithmetic failure to `OutOfMemoryError`; the helper relies on that caller.

Keep these layers distinct from `NullPointerException`, range checks, invalid
coder/escape errors, coding failures, native shadow-size limits, actual allocation
failure and pool admission's `null` refusal. A source-level fallback or bounded
metadata layout is not a global no-error guarantee.

## 8. Source tests and proof limits

The following are source assertions and intended test configurations, all
**NOT_RUN** in this documentation pass. The linked jtreg headers remain the
execution authority; descriptions here are not fresh passing receipts.

| Test | Source coverage | Important limit |
| --- | --- | --- |
| [M3StringFactsCompositionTest][test-compose] | Primitive fact fields across tuple/range versus scalar facts, deterministic surrogate seams and 5,000 randomized trials. | Scalar facts share implementation; this is not an independent oracle for every field. |
| [M3StringFactsScanWindowTest][test-scan] | 1,024/2,048-window shapes, surrogate edges, whitespace and 4,000 randomized inputs/ranges; compares single-unit composition and public geometry/hash/trim/strip definitions. | Finite source cases; no runtime pass or resource measurement. |
| [M3StringMixedEqualityGatesTest][test-equality] | Known versus unknown hashes, unprepared ranges, ASCII gates, cross-script case examples and consumer parity. | Does not assert every generated flat-labeled operand stays flat under enabled M3 storage; not exhaustive mixed-representation branch proof. |
| [M3StringModifiedUtf8DpTest][test-mutf8] and [native helper][test-mutf8-c] | JNI modified bytes/length versus `DataOutputStream.writeUTF`, warm facts, seams and an independent long-valued overflow model. Header requires a 64-bit VM and native helper, with a 128 MiB heap. | `verify(input)` calls `fresh(input)`, re-admitting joined input as scalar. The overflow model is a separate `LengthFacts` record, not an oversized runtime owner. This alone does not establish tuple/range JNI cached-fact reuse or oversized `jint` success. |
| [M3StringStreamsTest][test-streams] | Atom/range/tuple unit/code-point traversal, malformed and cross-owner pairs, sequential/parallel order and 500 randomized tuple/range trials. | Not a throughput or zero-allocation benchmark. |
| [M3StringJniShadowLifetimeTest][test-shadows] | Fresh, isolated whole/range/tuple arrays, zero-length compatibility sentinel and unchanged owner/coordinate under churn. | Uses Java-reflective shadow entry points; not all external JNI acquire/release functions. |
| [M3StringConcatLengthOverflowTest][test-overflow] | Large logical DAG concat/join and public overflow under 512 MiB heap, with and without compact strings. | Does not prove all fact/encoding operations at maximal logical size. |

The [ownership reference](bootstrap-string-ownership.md) additionally maps
`M3StringCanonicalDagTest`, `M3StringPoolBoundsTest` and
`M3StringOwnerRangeLifetimeTest`. In total the bounded review read ten complete
Java test bodies and the modified-UTF-8 native helper. Source assertions and older
receipts remain separate from executed evidence. No broad native lifetime,
compiler-tier, provider, concurrency or all-input correctness claim follows from
the finite tests listed here.

## 9. Declared-member coverage

The following index accounts for every directly declared constructor/method in
the two reviewed production bodies, including nested traversal helpers. It records
implementation surface, not new public API or inherited-member coverage.
`M3String` directly declares 80 methods and one constructor; its cursor and two
spliterators add 12 methods and three constructors. `M3StringFacts` declares
23 methods and one constructor. Each overload has its own source range.

Fields are accounted for in sections 1–4 and 6: the descriptor's owner/coordinate,
`SPAN_SHIFT`/`SPAN_MASK`, `EMPTY_OWNER`/`EMPTY`, `ready`/`ADMITTING` and
`emptyCompatibilityShadow`; cursor source/scalar/run fields; both spliterators'
source/cursor/index/fence fields; all 23 primitive fact fields and the three static
fact constants/table. The private facts constructor assigns the field bundle;
the descriptor constructor validates its owner and packed range; the trusted
factory short-circuits are explained above. No sibling body gains full coverage through this index.

### M3String and traversal helpers

| Declaring owner and exact signature | Contract section | Source |
| --- | --- | --- |
| `M3String`: `private M3String(M3StringOwner owner, long value)` | [1](#1-admission-and-composition) | [70–76](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L70-L76) |
| `M3String`: `static M3String whole(M3StringOwner owner)` | [1](#1-admission-and-composition) | [78–80](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L78-L80) |
| `M3String`: `static M3String range(M3StringOwner owner, int start, int length)` | [1](#1-admission-and-composition) | [82–86](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L82-L86) |
| `M3String`: `static M3String empty()` | [1](#1-admission-and-composition) | [88–90](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L88-L90) |
| `M3String`: `static void activate(String lexiconFile)` | [1](#1-admission-and-composition) | [92–104](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L92-L104) |
| `M3String`: `static boolean ready()` | [1](#1-admission-and-composition) | [106–108](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L106-L108) |
| `M3String`: `static boolean admissionEnabled()` | [1](#1-admission-and-composition) | [110–112](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L110-L112) |
| `M3String`: `static M3String admitCodePoints(int[] source, int offset, int count)` | [1](#1-admission-and-composition) | [114–122](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L114-L122) |
| `M3String`: `static M3String admitCompactBytes( byte[] source, int sourceOffset, int length, byte sourceCoder)` | [1](#1-admission-and-composition) | [124–133](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L124-L133) |
| `M3String`: `static M3String admitLatin1Bytes(byte[] source, int offset, int length)` | [1](#1-admission-and-composition) | [135–143](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L135-L143) |
| `M3String`: `static M3String admit(char[] source, int offset, int length)` | [1](#1-admission-and-composition) | [145–153](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L145-L153) |
| `M3String`: `static M3String admit(byte[] compactValue, byte coder)` | [1](#1-admission-and-composition) | [155–163](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L155-L163) |
| `M3String`: `static M3String join(String first, String second)` | [1](#1-admission-and-composition) | [170–176](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L170-L176) |
| `M3String`: `static M3String join(String[] parts)` | [1](#1-admission-and-composition) | [179–204](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L179-L204) |
| `M3String`: `static M3String joinDesignated( String prefix, String suffix, String delimiter, String[] elements, int size)` | [1](#1-admission-and-composition) | [218–247](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L218-L247) |
| `M3String`: `private static boolean joinAddDesignated(M3String[] levels, String source)` | [1](#1-admission-and-composition) | [250–268](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L250-L268) |
| `M3String`: `static M3String canonicalize(String source)` | [1](#1-admission-and-composition) | [274–280](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L274-L280) |
| `M3String`: `private static M3String joinValues(ArrayList<M3String> values)` | [1](#1-admission-and-composition) | [282–294](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L282-L294) |
| `M3String`: `static M3String sliceOf(String source, int beginIndex, int endIndex)` | [2](#2-utf-16-geometry-equality-and-hashes) | [296–300](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L296-L300) |
| `M3String`: `M3String slice(int beginIndex, int endIndex)` | [2](#2-utf-16-geometry-equality-and-hashes) | [302–307](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L302-L307) |
| `M3String`: `M3String concat(M3String other)` | [1](#1-admission-and-composition) | [309–311](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L309-L311) |
| `M3String`: `M3String indent(int amount)` | [7](#7-transformations-and-string-wrapper-semantics) | [313–384](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L313-L384) |
| `M3String`: `M3String stripIndent()` | [7](#7-transformations-and-string-wrapper-semantics) | [386–467](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L386-L467) |
| `M3String`: `M3String repeat(int repetitions)` | [7](#7-transformations-and-string-wrapper-semantics) | [469–484](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L469-L484) |
| `M3String`: `M3StringOwner owner()` | [2](#2-utf-16-geometry-equality-and-hashes) | [486–488](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L486-L488) |
| `M3String`: `long coordinate()` | [2](#2-utf-16-geometry-equality-and-hashes) | [490–492](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L490-L492) |
| `M3String`: `int start()` | [2](#2-utf-16-geometry-equality-and-hashes) | [494–496](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L494-L496) |
| `M3String`: `int end()` | [2](#2-utf-16-geometry-equality-and-hashes) | [498–500](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L498-L500) |
| `M3String`: `public int length()` | [2](#2-utf-16-geometry-equality-and-hashes) | [502–505](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L502-L505) |
| `M3String`: `byte coder()` | [2](#2-utf-16-geometry-equality-and-hashes) | [507–509](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L507-L509) |
| `M3String`: `long structuralHash64()` | [2](#2-utf-16-geometry-equality-and-hashes) | [511–513](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L511-L513) |
| `M3String`: `boolean sameCoordinate(M3String other)` | [2](#2-utf-16-geometry-equality-and-hashes) | [515–517](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L515-L517) |
| `M3String`: `long identityHash64()` | [2](#2-utf-16-geometry-equality-and-hashes) | [519–527](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L519-L527) |
| `M3String`: `public char charAt(int index)` | [2](#2-utf-16-geometry-equality-and-hashes) | [529–533](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L529-L533) |
| `M3String`: `public M3String subSequence(int beginIndex, int endIndex)` | [2](#2-utf-16-geometry-equality-and-hashes) | [535–538](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L535-L538) |
| `M3String`: `IntStream charsStream()` | [3](#3-direct-traversal-and-stream-callbacks) | [540–542](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L540-L542) |
| `M3String`: `IntStream codePointsStream()` | [3](#3-direct-traversal-and-stream-callbacks) | [544–546](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L544-L546) |
| `M3String`: `int hashCodeValue()` | [2](#2-utf-16-geometry-equality-and-hashes) | [548–550](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L548-L550) |
| `M3String`: `boolean hashKnownToDiffer(int hash)` | [2](#2-utf-16-geometry-equality-and-hashes) | [556–560](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L556-L560) |
| `M3String`: `M3StringFacts facts()` | [2](#2-utf-16-geometry-equality-and-hashes) | [562–566](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L562-L566) |
| `M3String`: `M3StringFacts factsIfPrepared()` | [2](#2-utf-16-geometry-equality-and-hashes) | [568–572](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L568-L572) |
| `M3String.CodeUnitCursor`: `CodeUnitCursor(M3String source)` | [3](#3-direct-traversal-and-stream-callbacks) | [586–588](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L586-L588) |
| `M3String.CodeUnitCursor`: `char at(int index)` | [3](#3-direct-traversal-and-stream-callbacks) | [590–596](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L590-L596) |
| `M3String.CodeUnitCursor`: `private void seek(int index)` | [3](#3-direct-traversal-and-stream-callbacks) | [598–621](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L598-L621) |
| `M3String.CharsSpliterator`: `CharsSpliterator(M3String source, int index, int fence)` | [3](#3-direct-traversal-and-stream-callbacks) | [630–635](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L630-L635) |
| `M3String.CharsSpliterator`: `public OfInt trySplit()` | [3](#3-direct-traversal-and-stream-callbacks) | [637–644](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L637-L644) |
| `M3String.CharsSpliterator`: `public boolean tryAdvance(IntConsumer action)` | [3](#3-direct-traversal-and-stream-callbacks) | [646–652](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L646-L652) |
| `M3String.CharsSpliterator`: `public void forEachRemaining(IntConsumer action)` | [3](#3-direct-traversal-and-stream-callbacks) | [654–659](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L654-L659) |
| `M3String.CharsSpliterator`: `public long estimateSize()` | [3](#3-direct-traversal-and-stream-callbacks) | [661–664](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L661-L664) |
| `M3String.CharsSpliterator`: `public int characteristics()` | [3](#3-direct-traversal-and-stream-callbacks) | [666–672](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L666-L672) |
| `M3String.CodePointsSpliterator`: `CodePointsSpliterator(M3String source, int index, int fence)` | [3](#3-direct-traversal-and-stream-callbacks) | [681–686](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L681-L686) |
| `M3String.CodePointsSpliterator`: `public OfInt trySplit()` | [3](#3-direct-traversal-and-stream-callbacks) | [688–702](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L688-L702) |
| `M3String.CodePointsSpliterator`: `public boolean tryAdvance(IntConsumer action)` | [3](#3-direct-traversal-and-stream-callbacks) | [704–719](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L704-L719) |
| `M3String.CodePointsSpliterator`: `public void forEachRemaining(IntConsumer action)` | [3](#3-direct-traversal-and-stream-callbacks) | [721–727](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L721-L727) |
| `M3String.CodePointsSpliterator`: `public long estimateSize()` | [3](#3-direct-traversal-and-stream-callbacks) | [729–732](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L729-L732) |
| `M3String.CodePointsSpliterator`: `public int characteristics()` | [3](#3-direct-traversal-and-stream-callbacks) | [734–737](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L734-L737) |
| `M3String`: `private boolean isWholeOwner()` | [2](#2-utf-16-geometry-equality-and-hashes) | [740–742](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L740-L742) |
| `M3String`: `boolean contentEquals(String other)` | [2](#2-utf-16-geometry-equality-and-hashes) | [744–767](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L744-L767) |
| `M3String`: `void getChars(int sourceBegin, int sourceEnd, char[] destination, int destinationBegin)` | [6](#6-copy-encoding-and-compatibility-shadows) | [769–777](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L769-L777) |
| `M3String`: `void getBytes(byte[] destination, int sourceBegin, int destinationBegin, byte destinationCoder, int count)` | [6](#6-copy-encoding-and-compatibility-shadows) | [779–796](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L779-L796) |
| `M3String`: `byte[] encode(Charset charset)` | [6](#6-copy-encoding-and-compatibility-shadows) | [798–810](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L798-L810) |
| `M3String`: `private byte[] encodeUtf8()` | [6](#6-copy-encoding-and-compatibility-shadows) | [812–848](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L812-L848) |
| `M3String`: `private byte[] encodeSingleByte(boolean asciiOnly)` | [6](#6-copy-encoding-and-compatibility-shadows) | [850–877](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L850-L877) |
| `M3String`: `byte[] encodeNoRepl(Charset charset)` | [6](#6-copy-encoding-and-compatibility-shadows) | [879–902](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L879-L902) |
| `M3String`: `byte[] encodeUtf8NoRepl()` | [6](#6-copy-encoding-and-compatibility-shadows) | [904–942](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L904-L942) |
| `M3String`: `private byte[] encodeWithEncoderNoRepl(Charset charset)` | [6](#6-copy-encoding-and-compatibility-shadows) | [944–964](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L944-L964) |
| `M3String`: `private static IllegalArgumentException unmappable(int offset, int length)` | [6](#6-copy-encoding-and-compatibility-shadows) | [966–969](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L966-L969) |
| `M3String`: `private byte[] encodeWithEncoder(Charset charset)` | [6](#6-copy-encoding-and-compatibility-shadows) | [971–1002](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L971-L1002) |
| `M3String`: `byte[] materialize()` | [6](#6-copy-encoding-and-compatibility-shadows) | [1007–1009](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1007-L1009) |
| `M3String`: `byte[] compatibilityValue()` | [6](#6-copy-encoding-and-compatibility-shadows) | [1011–1024](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1011-L1024) |
| `M3String`: `char[] charShadow()` | [6](#6-copy-encoding-and-compatibility-shadows) | [1027–1029](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1027-L1029) |
| `M3String`: `boolean mayContain(M3String needle)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1031–1038](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1031-L1038) |
| `M3String`: `boolean startsWith(M3String prefix, int offset)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1040–1057](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1040-L1057) |
| `M3String`: `int indexOf(char unit, int fromIndex, int endIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1059–1063](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1059-L1063) |
| `M3String`: `int lastIndexOf(char unit, int fromIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1065–1069](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1065-L1069) |
| `M3String`: `int indexOfCodePoint(int codePoint, int fromIndex, int endIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1071–1093](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1071-L1093) |
| `M3String`: `int lastIndexOfCodePoint(int codePoint, int fromIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1095–1116](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1095-L1116) |
| `M3String`: `int indexOf(M3String needle, int fromIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1118–1120](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1118-L1120) |
| `M3String`: `int indexOf(M3String needle, int fromIndex, int endIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1122–1146](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1122-L1146) |
| `M3String`: `int lastIndexOf(M3String needle, int fromIndex)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [1148–1171](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1148-L1171) |
| `M3String`: `M3String translateEscapes()` | [7](#7-transformations-and-string-wrapper-semantics) | [1173–1231](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1173-L1231) |
| `M3String`: `M3String asciiCase(boolean upper)` | [7](#7-transformations-and-string-wrapper-semantics) | [1233–1256](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1233-L1256) |
| `M3String`: `M3String replace(char oldChar, char newChar)` | [7](#7-transformations-and-string-wrapper-semantics) | [1258–1277](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1258-L1277) |
| `M3String`: `M3String replaceEmptyTarget(M3String replacement)` | [7](#7-transformations-and-string-wrapper-semantics) | [1279–1293](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1279-L1293) |
| `M3String`: `static boolean isLiteralRegex(String regex)` | [7](#7-transformations-and-string-wrapper-semantics) | [1299–1307](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1299-L1307) |
| `M3String`: `static boolean isLiteralRegexReplacement(String regex, String replacement)` | [7](#7-transformations-and-string-wrapper-semantics) | [1310–1317](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1310-L1317) |
| `M3String`: `String replaceLiteralRegex(String original, String regex, String replacement, boolean firstOnly)` | [7](#7-transformations-and-string-wrapper-semantics) | [1324–1331](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1324-L1331) |
| `M3String`: `M3String replaceAt(M3String target, M3String replacement, int found)` | [7](#7-transformations-and-string-wrapper-semantics) | [1334–1340](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1334-L1340) |
| `M3String`: `M3String replaceFlatTarget(String receiver, String target, M3String replacement, int found)` | [7](#7-transformations-and-string-wrapper-semantics) | [1347–1352](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1347-L1352) |
| `M3String`: `private M3String replaceMatches(M3String target, String receiver, String flatTarget, M3String replacement, int found, boolean firstOnly)` | [7](#7-transformations-and-string-wrapper-semantics) | [1355–1389](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1355-L1389) |
| `M3String`: `static int pow31(int length)` | [4](#4-fixed-facts-and-seam-composition) | [1391–1399](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1391-L1399) |
| `M3String`: `private static long span(int start, int length)` | [2](#2-utf-16-geometry-equality-and-hashes) | [1401–1404](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1401-L1404) |
| `M3String`: `private static int start(long coordinate)` | [2](#2-utf-16-geometry-equality-and-hashes) | [1406–1408](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1406-L1408) |
| `M3String`: `private static int count(long coordinate)` | [2](#2-utf-16-geometry-equality-and-hashes) | [1410–1412](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1410-L1412) |
| `M3String`: `private static native byte[] nativeByteShadow( M3String value, int start, int length, byte coder);` | [6](#6-copy-encoding-and-compatibility-shadows) | [1414–1415](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1414-L1415) |
| `M3String`: `private static native char[] nativeCharShadow( M3String value, int start, int length);` | [6](#6-copy-encoding-and-compatibility-shadows) | [1417–1418](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1417-L1418) |

### M3StringFacts

| Declaring owner and exact signature | Contract section | Source |
| --- | --- | --- |
| `M3StringFacts`: `private M3StringFacts( int utf16Length, int utf8Length, int modifiedUtf8Length, int codePointCount, int unpairedSurrogateCount, int javaHash, int hash31Power, char firstUtf16Unit, char lastUtf16Unit, long bitSignal64, boolean ascii, boolean latin1, int asciiUpperHash, int asciiLowerHash, int asciiTitleHash, long prefix4, long suffix4, long bigramSignal64, long trigramSignal64, int trimStart, int trimEnd, int stripStart, int stripEnd)` | [4](#4-fixed-facts-and-seam-composition) | [57–104](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L57-L104) |
| `M3StringFacts`: `static M3StringFacts scan(M3String value)` | [4](#4-fixed-facts-and-seam-composition) | [126–271](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L126-L271) |
| `M3StringFacts`: `static M3StringFacts compose(M3StringFacts left, M3StringFacts right)` | [4](#4-fixed-facts-and-seam-composition) | [273–360](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L273-L360) |
| `M3StringFacts`: `boolean mayContain(M3StringFacts needle)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [362–371](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L362-L371) |
| `M3StringFacts`: `boolean prefixMayMatch(M3StringFacts prefix)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [373–378](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L373-L378) |
| `M3StringFacts`: `boolean suffixMayMatch(M3StringFacts suffix)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [380–385](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L380-L385) |
| `M3StringFacts`: `boolean mayContain(String needle)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [392–411](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L392-L411) |
| `M3StringFacts`: `boolean prefixMayMatch(String prefix)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [414–423](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L414-L423) |
| `M3StringFacts`: `boolean suffixMayMatch(String suffix)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [426–436](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L426-L436) |
| `M3StringFacts`: `boolean mayContainCodeUnit(char unit)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [438–441](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L438-L441) |
| `M3StringFacts`: `static long codeUnitSignal(char unit)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [443–445](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L443-L445) |
| `M3StringFacts`: `private static long unitSignal(char unit)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [447–449](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L447-L449) |
| `M3StringFacts`: `private static long[] latin1UnitSignals()` | [5](#5-candidate-gates-and-exact-search-boundaries) | [451–457](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L451-L457) |
| `M3StringFacts`: `private static int utf8Bytes(char value)` | [4](#4-fixed-facts-and-seam-composition) | [459–463](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L459-L463) |
| `M3StringFacts`: `private static int pow31(int length)` | [4](#4-fixed-facts-and-seam-composition) | [465–473](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L465-L473) |
| `M3StringFacts`: `private static long addSignal(long signal, char value)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [475–480](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L475-L480) |
| `M3StringFacts`: `private static long addBigramSignal(long signal, char first, char second)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [482–484](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L482-L484) |
| `M3StringFacts`: `private static long addTrigramSignal(long signal, char first, char second, char third)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [486–489](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L486-L489) |
| `M3StringFacts`: `private static long addNgramSignal(long signal, long value)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [491–496](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L491-L496) |
| `M3StringFacts`: `private static long mix64(long value)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [498–505](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L498-L505) |
| `M3StringFacts`: `private static int mix32(int value)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [507–514](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L507-L514) |
| `M3StringFacts`: `private static char asciiUpper(char value)` | [4](#4-fixed-facts-and-seam-composition) | [516–518](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L516-L518) |
| `M3StringFacts`: `static long asciiLowerHashOrNegative(String value, int from, int to)` | [5](#5-candidate-gates-and-exact-search-boundaries) | [525–533](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L525-L533) |
| `M3StringFacts`: `private static char asciiLower(char value)` | [4](#4-fixed-facts-and-seam-composition) | [535–537](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L535-L537) |

## 10. Pinned source identities and remaining work

| Fully reviewed body | Git blob at the source pin |
| --- | --- |
| [M3String.java][m3-source] | `42399d411c1516a56a47dcdb10242960157b4677` |
| [M3StringFacts.java][facts-source] | `d8dd7dc30db6a9e3897146572bd4a7755c954ffb` |

The companion page records the four ownership bodies and their blobs. Selected
caller/native ranges above establish only the boundaries they show. The next
unreviewed production bodies in this `java.lang` family are `M3CodeTextFacts`,
`M3CodeTextSignalBatch`, `M3StringCodePointPrecompute`,
`M3StringPositionPrecompute`, `M3StringRegexDp` and `M3StringSearchPrecompute`.
Their declaration inventory is not full contract, cache, concurrency, budget or
consumer evidence. The `jdk.internal.mindex` StringBacking family is an independent
runtime slice. The broader public String contract, native platform matrix and
matched-runtime tests remain open beyond this bounded source reference.

[m3-admission]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L41-L294
[string-admission]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L5556-L5642
[system-activation]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/System.java#L2235-L2242
[string-join]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L4035-L4090
[string-concat]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L3313-L3323
[m3-slice]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L296-L311
[m3-geometry]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L486-L572
[m3-equality]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L740-L767
[string-equality]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L1994-L2026
[string-order]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L2233-L2259
[string-codepoints]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L1660-L1802
[m3-streams]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L574-L738
[string-streams]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L5005-L5040
[facts-fields]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L20-L117
[facts-scan]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L126-L271
[facts-compose]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L273-L360
[facts-gates]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L362-L514
[facts-ascii]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java#L520-L537
[m3-search]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1031-L1171
[string-search]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L2964-L3054
[string-case-gate]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L2137-L2151
[m3-output]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L769-L1029
[string-no-repl]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L999-L1053
[native-shadows]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/native/libjava/String.c#L51-L131
[string-output]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L1893-L1977
[string-value]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L5653-L5665
[vm-mutf8-cache]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/hotspot/share/classfile/javaClasses.inline.hpp#L193-L203
[vm-mutf8-walk]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/hotspot/share/classfile/javaClasses.cpp#L707-L743
[jni-ordinary]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/hotspot/share/prims/jni.cpp#L2137-L2273
[jni-regions]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/hotspot/share/prims/jni.cpp#L2767-L2803
[jni-critical]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/hotspot/share/prims/jni.cpp#L2831-L2879
[m3-transforms]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L313-L484
[m3-replace]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1173-L1389
[string-replace]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L3478-L3631
[string-case]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L4204-L4320
[string-lines]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L4638-L4787
[string-matches]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L3411-L3424
[string-trim]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L4375-L4503
[string-repeat]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L5369-L5401
[m3-source]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java
[facts-source]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3StringFacts.java
[test-compose]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringFactsCompositionTest.java
[test-scan]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringFactsScanWindowTest.java
[test-equality]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringMixedEqualityGatesTest.java
[test-mutf8]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringModifiedUtf8DpTest.java
[test-mutf8-c]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/libM3StringModifiedUtf8DpTest.c
[test-streams]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringStreamsTest.java
[test-shadows]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringJniShadowLifetimeTest.java
[test-overflow]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java
[string-escapes]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L4889-L4896
