<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained regex programs, images and candidate facts

[Guide](README.md) · [Coverage and exact identities](retained-regex-reference-coverage.md) · [Runtime owners](runtime-owners.md) · [Indexed-string bridge](indexed-string-reference.md)

This reference explains the seven retained root Java owners in
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring`
at public M3JDK21 commit `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`.
It covers their complete bodies, including nested types and support paths.
Tests, builds, policy verifiers, native libraries and benchmarks were **NOT_RUN**
in this documentation authoring/review pass. Hosted CI, if subsequently run,
must be reported separately for its exact commit; it is not included in these
NOT_RUN statements. Historical evidence is identified separately below.

## 1. Custody, package and runtime boundaries

These original-package files are a retained source slice, **not a complete
compilable module or JDK runtime dependency**. The retained `synexia-indexstring`
subtree has no `pom.xml` or `module-info.java`. [Vendor custody policy][vendor]
and [delivery policy][delivery] describe independently pinned snapshots outside
normal source roots. The [control reactor][reactor] includes the separate
[import verifier][import-pom]; it does not make this directory a regex runtime
module. The [foundation build script][build] compiles `m3/core/src`, not these
seven files. No standalone build command is supplied for this incomplete slice.

| Owner | Responsibility |
| --- | --- |
| `MIndexRegexProgram` | Conservative boolean regex compiler, immutable primitive NFA and optional kernels; caller-owned compilation session and execution workspace |
| `MIndexRegexProgramImageCodec` | Bounded binary encoding/decoding and file transport for portable, execution, accelerated and precomputed images |
| `MIndexCompactAsciiDfaImage` | Immutable transition-equivalent ASCII class projection |
| `MIndexRegexAcceleratedImage` | Execution image plus validated compact anchored/search projections |
| `MIndexRegexPrecompute` | Exact request, persisted positive/negative cache flow and restore/compile boundary |
| `MIndexRegexPrecomputeKey` | Package-private deterministic request identity, including historical owner literals |
| `MIndexRegexTrigramQuery` | Necessary-condition boolean query and immutable UTF-16 membership facts |

The original `MIndexString`, `IndexLexiconWordView`,
`IndexUtf16SearchImage.RowView`, `IndexTextMetrics`, `IndexCharacterFlags`,
`IndexCharacterFlagsResolver` and `MIndexStringBitOps` dependencies referenced
by Program are not root source owners in this retained subtree. Neither
`com.synexia.job.IProgressMonitor` nor the session's
`com.synexia.common.collections.PackedLongLongOrderedMap` belongs to this
seven-file scope. Cold compilation also uses `java.lang.management.ThreadMXBean`.
The [porting invariant][policy] explicitly requires a bootstrap-safe split and
notes that `java.base` cannot read `java.management`. Same-named types elsewhere,
a public oracle receipt, or a successful importer test do not close that dependency
gap. The `fs` and [generic typed precompute](retained-precompute-reference.md) siblings
have separate ownership;
only the calls needed to explain this regex boundary are traced here.

Three other surfaces must stay distinct:

- The isolated frozen-text fixture has its own [provenance][frozen-fixture]
- The separately built `m3/ports/indexstring` compatibility bridge exposes
  `M3Text` and retained-view storage; see the [bridge reference](indexed-string-reference.md)
- Canonical runtime String storage remains the existing
  `java.lang.M3String`/owner/atom/tuple/pool family under
  [runtime ownership](runtime-owners.md), with public `java.lang.String` and
  `java.util.regex.Pattern`/`Matcher` contracts authoritative

The [name map][mapping] treats these regex/search families as candidate/internal
planning inputs where justified. The [String/precompute port map][port-map]
keeps full program, image, capture, matcher-state and provider admission separate.
The retained boolean APIs do not provide captures, match spans, repeated Matcher
iteration, `hitEnd`, `requireEnd`, regions or transparent/anchoring-bound state.
A captured-group count is metadata, not a capture-result implementation. No
RE2/J runtime dependency, general Pattern replacement, native/GPU execution,
performance gain or whole-family acceptance follows from these files.

The public [reuse][reuse] and [borrowing][borrowing] records require preserving
applicable Apache-2.0 copyright, LICENSE, NOTICE and attribution, including
`Copyright 2026 Hitesh Soliwal and contributors`, while retaining independent
OpenJDK and third-party rights. This page describes that project policy; it
creates no new relicensing or admission authority. The full-family catalogue
and [current receiving plan][full-borrow] retain their qualification boundaries.

## 2. Inputs, compilation and refusal

### Exact expression and supported language

[Compilation][compile] requires nonnull expression and semantics and a positive
`maxStates`; it snapshots the supplied `CharSequence` by `toString()`. The caller
must make that observation stable and faithful. Program retains the resulting
String, the selected `Semantics`, primitive lanes and derived metadata. Runtime
matching reads the original input's `length()`/`charAt()` without creating a
substring or String snapshot; mutable/custom inputs must remain stable for the
whole operation, including any metadata reads.

`compile(expression[, monitor])` requests `RE2` semantics with 100,000 states and
throws `IllegalArgumentException` if `tryCompile` returns null. The two
`tryCompile` overloads accept an explicit `RE2` or `JDK` mode and state ceiling.
They catch the internal `UnsupportedRegex` refusal and return null. This covers
unsupported syntax/features **and the compiler's state/repetition limits**;
null does not prove that an expression is invalid in another backend. Other
exceptions, including monitor cancellation, propagate. These methods do not
invoke another backend themselves.

The [parser and class builder][parser] implement the following bounded language:

| Surface | Inspected behavior |
| --- | --- |
| Literals, grouping, alternation | BMP non-surrogate literals; ordinary and `(?:...)` groups; group count only; empty alternatives allowed |
| Repetition | `*`, `+`, `?`, `{n}`, `{n,}`, `{n,m}`; a following lazy `?` is consumed because these APIs return only boolean language truth; possessive `+` is refused |
| Repeat restrictions | A repeated atom with zero minimum length is refused; numeric bounds must fit the state ceiling, and RE2 bounds above 1,000 are refused; actual lowering can exhaust the ceiling even when the numeric bound fits |
| Quoted text | `\Q...\E`, or quote through end of expression; a following quantifier binds only to the last quoted literal (`\Qab\E+` has the boolean language of `ab+`) |
| Classes | Positive/negated BMP classes, literal/range pieces and the supported escaped classes; reversed ranges, unsupported nested classes and JDK `&&` intersections are refused |
| Character families | ASCII `\d`/`\w` and complements; `\s` is tab, LF, FF, CR, space in RE2, with vertical tab additionally in JDK |
| Dot | RE2 excludes LF; JDK also excludes CR, NEL U+0085, line separator U+2028 and paragraph separator U+2029 |
| Anchors/boundaries | `^` and `\A` mean absolute start; `\z` absolute end; `$` accepted only in RE2 and means absolute end; `\b`/`\B` accepted only in RE2 and use ASCII word membership |
| POSIX class names | RE2 only: `alnum`, `alpha`, `ascii`, `blank`, `cntrl`, `digit`, `graph`, `lower`, `print`, `punct`, `space`, `upper`, `word`, `xdigit`, including `^` complement syntax inside the named class |
| Other escapes | `\n`, `\r`, `\t`, `\f`, two-digit `\x`, four-digit `\u`, and the explicitly escaped punctuation in the parser |
| Refused scope | Inline flags, lookarounds, backreferences, unsupported escapes/Unicode properties, possessive constructs, malformed syntax, literal or escaped surrogate atoms and other surfaces outside the parser |

These are implementation boundaries, not exhaustive equivalence certification.
The public historical oracle snapshots give finite examples, not an independent
proof of every accepted input or all Pattern/Matcher observable state.

### Progress, tables and bounded optimization

Compilation reports parse, lowering, class freezing, bit-kernel preparation and
bounded ASCII determinization phases to the supplied monitor. Null selects the
shared no-op monitor. Cancellation checks occur before phases and within selected
loops. The `finally` after `beginTask` calls `done()` on success, unsupported
refusal or exceptions arising inside that block; exceptions during input
validation, snapshotting or `beginTask` precede that block.

The [table builders][tables] retain optional structures under separate ceilings:
ASCII state masks 4 MiB; epsilon closures 4 MiB; ASCII subset transitions 8 MiB
(preferring eight-bit then four-bit subsets); BMP masks 4 MiB and at most
4,000,000 assignments; each ASCII DFA 2 MiB retained, 8 MiB construction accounting,
and at most 4,096 states. DFA construction also bounds signed-short targets.
Assertions disable static epsilon/DFA paths. Missing masks, too many states,
resource refusal or unsuitable structure may omit an optimization while leaving
the NFA usable. These are primitive-table bounds, not a cap on total compilation,
heap, native or process memory.

### Caller-owned `CompilationSession`

[Session implementation][session] is a bounded syntax-sharing arena, not a
compiled-program cache. `Limits(expressions, nodes, childLinks, classes, ranges,
accountedBytes)` rejects negative capacities. Construction allocates fixed
arrays and four packed-map indexes, then rejects a byte budget below their
reserved accounting; the budget is not a pre-allocation total-memory guarantee.
`Policy.LRU` or `MRU` chooses which retained expression entry is evicted.

An exact expression/semantics/`maxStates` hit is confirmed after hash routing and
reconstructs local parser nodes/classes from canonical primitive IDs. Lowering
and derived-table construction still happen on every compile. `admit=false`
permits existing hits but bypasses new retention. An oversized expression,
zero expression capacity or exhausted node/class/link/range capacity also
bypasses retention without changing compilation truth. Unsupported results are
not a separate negative-cache entry; parsing may have been retained before a
later lowering refusal or cancellation.

Expression eviction releases its source/class-ID references. Interned
node/class IDs remain append-only until `clear()`: eviction does not reclaim the
arena, and a partially completed interning attempt can leave reusable entries.
`statistics()` reports retained expressions/nodes/classes, accounted bytes,
hits, bypasses and evictions. `accountedBytes()` includes spare primitive array
capacity, eight bytes per reference slot, two bytes per retained source UTF-16
unit, four per retained source class ID and all four primitive indexes. It
excludes object/array headers, scalar fields, monitor/clock objects and transient
compilation storage.

Calls are synchronized. Reentrant compilation and `clear()`/`close()` while
busy throw `IllegalStateException`, including from a progress callback.
`clear()` empties indexes/references, resets arena sizes/counters/timings, but
keeps reserved capacity. `close()` clears then permanently marks the session
closed; clearing it again does not reopen it. Returned programs own their
arrays and remain valid after eviction, clear or close.

Optional CPU measurement records `CompilationTimings(parseCpuNanos,
loweringCpuNanos, tableCpuNanos, totalCpuNanos, completed)` for an attempt that
reaches the timing/finally path. Preparation includes parse/remap and the
snapshot/progress setup covered by the clock. Unsupported or disabled current
thread CPU timing yields `-1`; measurement does not enable CPU timing globally.
Without measurement, `lastTimings()` remains null after construction/clear.
The `completed` flag is set after table preparation but before final Program
construction; that construction or the subsequent monitor `done()` can still
throw. Timing is finalized before `done()`. The flag is not a guarantee that the
call returned successfully. These are per-attempt thread-CPU observations, not
wall-clock benchmarks or performance acceptance.

## 3. Boolean execution and source lifetime

[Input admission][input] first tests whether the program can consume any
surrogate code unit. If not, input is admitted without scanning for surrogates:
those atoms cannot consume either half of a pair. Otherwise:

- `MIndexString` and `IndexLexiconWordView` use their metrics
- A row view uses metrics only when it advertises precomputed regex facts
- Other sequences are scanned and **any** surrogate unit, paired or unpaired,
  causes refusal

The metrics predicate requires UTF-16 length equal to code-point count and zero
unpaired surrogates. A false `supportsInput` makes each boolean operation throw
`IllegalArgumentException` before its fast rejection/matching paths. The caller
must select a suitable semantic backend; the retained program does not do so.

`matches` requires full-input acceptance; `lookingAt` accepts a prefix beginning
at zero, including an admitted empty match; `find` asks whether any position can
start a match, including the end position for empty matches. No match object,
span or captured text is returned. [Execution][execution] checks length and
available required facts, then uses the suitable ASCII DFA if present or falls
through to bitset/closure NFA execution. A search DFA reinjects the NFA start.
Unknown ASCII knowledge checks encountered units; trusted ASCII metadata allows
a path without repeated unit-range checks. Correct and stable resolver/row
facts are therefore a caller/dependency contract, not optional decoration.

Required presence bits can collide: a missing required bit rejects, but present
bits prove nothing. Character flags have the same necessary-condition role.
Concatenation unions requirements; alternation intersects them; optional
repetition contributes none. Unknown metadata is conservative. `firstAsciiAllows`
is also conservative for non-ASCII input or a nullable program.
`structurallyDeterministic()` means only that the recorded split-state count is
zero. `nullable()` tests stored `minLength == 0` only; it is a structural signal,
not proof that `matches("")` succeeds. A source-only example is RE2 `\b`: its
minimum is zero, but its word-boundary assertion fails on empty input.
`minLength`/`maxLength` use UTF-16 units, with `-1` for an unbounded/overflowed
maximum; minimum arithmetic saturates at `Integer.MAX_VALUE`.

A Program can be shared; its lazily memoized trigram query is published with a
volatile field and synchronization. [Workspace][workspace] retains that Program
and mutable primitive scratch and is deliberately thread-confined. Each call
resets NFA active/next lanes; closure stamps avoid revisiting states and reset on
wraparound. Direct Program calls allocate scratch when the NFA is needed;
a reusable workspace owns scratch from construction even if a DFA later answers.
Neither retains the matching input. The caller must keep a mapped/resolver-backed
source and its exact owner/range valid throughout access. This slice does not
supply the missing original text-owner lifecycle or a persistent source resolver.

`primitivePayloadBytes()` and the individual kernel/workspace payload accessors
count their listed primitive arrays only. They exclude expression Strings,
object/header overhead, lazy query objects, source payload, temporary copies and
mapped/native/process memory. They are not retained-heap measurements.

## 4. Immutable image forms and trust

[Portable images][image] copy all supplied arrays on construction and clone every
array accessor. Non-array record components are returned normally. Their default
record equality is not a deep-array content identity; use the documented roots
where a portable image identity is needed, with the surrounding admission contract.

| Form / entry points | Stored state and restore work |
| --- | --- |
| `Image`, `image()`, `fromImage(image[, monitor])` | Expression, semantics, primitive NFA/class lanes and scalar metadata; no parse on restore, but bounded masks, epsilon closures, subset/BMP tables and DFAs are rebuilt; dispatch facts are derived |
| `ExecutionImage`, `executionImage()`, `fromExecutionImage` | Adds every optional execution table and dense DFA present at freeze time; restore copies lanes without parsing or reconstructing those tables, but still derives first-ASCII masks and split count |
| `PrecomputedImage`, `precomputedImage()`, `fromPrecomputedImage` | Execution image plus first-ASCII low/high masks and branching-state count; restore also avoids first-character traversal/opcode counting |
| `AsciiDfaImage`, anchored/search snapshots | Start state, dense short transitions and byte acceptance lane; absent projection returns null |
| `MIndexCompactAsciiDfaImage.from(dense)` | Groups ASCII values only after equal-column hash candidates pass full transition-column equality; verifies transition equivalence against dense input |
| `MIndexRegexAcceleratedImage.from(program)`, `restoreProgram()` | Execution plus matching compact anchored/search projections; restore uses the execution form, not a new native matcher |

The constructors validate the following [geometries][execution-image]:

- `Image`: nonempty state lanes of equal length, valid start/match coordinates,
  equal class lanes, bounded range offsets/counts, low ≤ high, recognized opcodes,
  transition bounds, CHAR class/target requirements, SPLIT targets, epsilon/assert
  targets, non-CHAR class ID `-1`, MATCH at `matchState`, nonnegative minimum and
  capture count, maximum at least `-1`
- `ExecutionImage`: empty-or-exact ASCII mask geometry; absent/present epsilon
  row geometry and row IDs; absent-or-four/eight-bit subset layout and expected
  lane length; absent/present BMP word/page/mask geometry, bounded distinct page
  IDs; dense DFA objects validate their own geometry
- `PrecomputedImage`: nonnull execution; branching count between zero and the
  underlying state count
- Dense DFA: nonempty acceptance lane, valid start, exactly `128 × states`
  transitions, targets `-1` or a valid state, acceptance bytes only 0 or 1
- [Compact DFA][compact]: 128 map entries, 1–128 classes, every class represented,
  valid unsigned class IDs, exactly `states × classes` short transitions, valid
  start/targets and 0/1 acceptance. `nextState` checks both state and ASCII value
- [Accelerated image][accelerated]: dense/compact presence must match for each
  lane; acceptance, start, state count and every transition must be equivalent

Required nonnull inputs fail with `NullPointerException`; invalid geometry normally fails
with `IllegalArgumentException`; exact arithmetic can throw `ArithmeticException`.
Copying occurs before several geometry checks, so validation is not an allocation
quota. Geometry does **not** establish that arbitrary supplied metadata or tables
were derived from the claimed expression. In particular, the execution form does
not recompute table truth, the precomputed form does not prove its first-character
masks/count, and portable validation does not prove range sorting or length/fact
metadata against the language. A valid checksum/root is integrity/identity
information, not authenticity or expression-equivalence proof. Use images from
an appropriately qualified producer and bind its implementation/dependencies.

### Roots are separate contracts

[Program root helpers][roots] compute SHA-256 over ordered, length-framed lanes.
`Image.rootHash()` uses domain `M_INDEX_REGEX_IMAGE_V1` and UTF-8 expression bytes
when round-tripping preserves the String, otherwise
`M_INDEX_REGEX_IMAGE_UTF16_V4` with raw UTF-16 units. Numeric framing is big-endian;
its char/short helper values are widened to int for this root scheme. This root
is not the wire envelope digest.

`ExecutionImage.rootHash()` uses `M_INDEX_REGEX_EXECUTION_IMAGE_V2`, the base
root, all derived lanes, scalars and each optional DFA's presence/content.
Compact roots use `MINDEX_COMPACT_ASCII_DFA_V1`, including the map, class count,
start, short-width transitions and acceptance. Accelerated roots use
`MINDEX_REGEX_ACCELERATED_IMAGE_V3`, the execution root and each compact root
(or an empty string for absence), with UTF-8 length framing. `PrecomputedImage`
has no rootHash method. Content-store IDs and the complete v7 envelope bind its
encoded dispatch metadata separately. None of these hashes is `String.hashCode`,
canonical text-owner identity, or an authorization to execute a native provider.

## 5. Codec families, cursor ownership and file effects

The [codec][codec] is explicit binary serialization, not Java object serialization.
All envelopes are big-endian: magic `0x534e5852` (`SNXR`), int version, int payload
length, 32-byte SHA-256 of the payload, then exactly that payload. Header size is
44 bytes. Each nested image has its own envelope/checksum. Each `encode` call
returns a fresh caller-owned byte array; changing that array does not mutate the
immutable image.

| Version | Family | Expression / nested contract |
| --- | --- | --- |
| 1 | Portable `Image` | Strict round-trippable UTF-8 metadata |
| 2 | `ExecutionImage` | Nested v1 plus execution lanes and optional dense DFAs |
| 3 | Accelerated | Nested v2 plus optional compact DFAs |
| 4 | Portable exact-UTF-16 | Raw big-endian 16-bit expression units |
| 5 | Execution exact-UTF-16 | Nested v4, otherwise the v2 lane layout |
| 6 | Accelerated exact-UTF-16 | Nested v5, otherwise the v3 lane layout |
| 7 | `PrecomputedImage` | Nested execution image (v2 or v5), first low/high longs and branching-count int |

`encode` selects v4/v5/v6 only when expression metadata contains an unpaired
surrogate; representable expressions retain the legacy version. A well-formed
surrogate pair remains UTF-8. This preserves arbitrary image metadata exactly;
it does not extend parser or runtime input support. The outer v7 is unchanged
by its nested expression encoding.

Portable envelopes are limited to 32 MiB; execution, accelerated and precomputed
envelopes to 64 MiB, including headers/nested images. Expression metadata is
limited to 1 MiB encoded bytes, or 524,288 raw UTF-16 units. Portable lane counts
are bounded to 100,000 states, 65,536 classes and 1,000,000 ranges. Other counted
int/long/short/byte lanes are bounded by the execution-image byte limit and
checked against remaining bytes before allocation/read. Encoding may allocate
payload/copy buffers before its final total-envelope bound; this is not a whole
operation allocation budget.

[Decode transport][decode] accepts byte arrays, heap/direct/mapped buffers and
buffer subranges. Each byte-array overload first clones the full supplied array.
A ByteBuffer overload reads an independent read-only view of the caller's current
position-to-limit window with explicit big-endian order. The caller's cursor,
limit and order are not changed. The view is not a snapshot: the backing bytes
must remain stable throughout checksum and lane reads. Nested frames are bounded
slices, and only the decoder's private cursor advances past them. No nested
payload byte-array copy is needed at those handoffs, but primitive lanes and
immutable image defensive copies are still allocated. Returned images retain
neither the input buffer nor a mapping.

For v1/v4, v2/v5 and v3/v6 entry points, `imageVersion` first requires eight bytes
and checks the allowed version pair. Full `payload` validation then checks the
44-byte/minimum and maximum size, magic, expected version, exact payload length
and checksum before parsing lanes. The v7 entry goes directly through `payload`.
Nested v2/v5 and v3/v6 forms enforce the corresponding base version; v7 delegates
to the execution decoder's allowed pair. Invalid enums/presence flags/counts,
non-round-trippable UTF-8, truncated lanes, trailing payload and constructor
geometry failures are refused. This ordering matters when several defects coexist:
not every invalid buffer fails first on magic, and all failures are not collapsed
into a single exception type.

[File helpers][files] exist for portable, execution and accelerated forms, with
ordinary and `Mapped` readers for each. There is no v7-specific path `write` or
`readPrecomputed` overload; persistent v7 flow uses the content store below.
`write` encodes before calling `Files.write(CREATE_NEW, WRITE)`: it does not replace
an existing target or create parent directories. I/O failure can leave a newly
created partial file; these helpers provide no transaction, fsync or rollback.
Ordinary reads inspect file size, then read all bytes and pass the resulting
buffer directly to decode. Mapped reads open a read-only channel, check size,
map and decode inside try-with-resources. The channel closes, but no explicit
unmapping is performed. Mapping is an encoded transport, not zero-allocation
restoration or a portable bound on resident memory. Callers must prevent external
file changes while reading. File-system failures propagate as `IOException`;
malformed image/size failures ordinarily remain `IllegalArgumentException`.

## 6. Persisted request, cache identity and publication

[Request and Result][precompute] are explicit caller contracts.
`Request(expression, semantics, maxStates, compilerRevision, codecRevision)`
requires a nonnull String/mode, positive state limit and revision strings of
1–256 visible ASCII units (`!` through `~`, so no spaces). The revisions must bind
the compiler/codec implementation **and all behavior-bearing dependencies**.
A `Compiler` returns a Program or null for explicit unsupported; thrown exceptions
are not cached by this wrapper. `Result(program, reused, output)` requires nonnull
Optionals and matching program/output presence; `reused=true` includes a cached
negative result and does not imply a nonempty program.

The [key][key] uses FS type `regex-execution`, algorithm
`mindex-regex-full-tables`, version 1, and one expression-content input. That input
is [sha256Utf16][utf16-hash]: a big-endian long UTF-16 length followed by each raw
unit's high/low bytes. Configuration includes semantics, decimal state ceiling,
the two revisions, fixed historical owner strings, and the same raw-UTF-16 hash
of `java.vendor + NUL + java.runtime.version` (missing properties default empty).
FS canonical encoding preserves input order and sorts configuration keys.
Expression units, mode, limits, revisions and runtime properties contribute to
request identity. Expression/runtime values and the final key use hashes; this
is not a collision-free identity guarantee. The image-byte budget is intentionally
not in the key: it governs each attempt. The key is not automatically a complete
environment fingerprint.

### Historical owner literals versus current implementation

`PROGRAM_OWNER` remains
`9999e8afe1ec44de58edd644c0ca278415fb7a379f6cdacfe9dbf7011ed77131`, and
`CODEC_OWNER` remains
`09f4ef3d52a432b5a46bd784a91728352d121241dca9ab010df7a06f38d01dc7`.
They are fixed v7 lineage inputs, **not hashes recomputed from current files**.
At this page's pin, actual Program SHA-256 is
`4e217b9aad286aa2c3f1bc77d2900fe2e3ff1889550bfa399792fb999807732c`
and actual codec SHA-256 is
`f7ad944462b279135d82c93e5490d3d6a88bf0e4dfb1de9bbb232990b9c6dc44`.
The exact successor records in section 8 explain that evolution; it is not
unexplained corruption. Reusing a revision label for changed behavior/dependencies
can reuse stale entries because no runtime source hash is computed. Callers must
supply accurate revision identities; this reference does not alter the historical
constants or silently repair key compatibility.

### `restoreOrCompile` sequence

Both overloads require nonnull store/request, and the custom overload requires a
nonnull compiler; `maxImageBytes` must be positive. The default compiler delegates
to `tryCompile` with the request's expression/mode/limit.

1. Construct the key and `M3PrecomputeCache`; its constructor creates the
   `precompute-index` directory. Lookup verifies the persisted descriptor through
   the FS content store and checks that it belongs to this key
2. A negative hit returns empty program/output with `reused=true`, without invoking
   the compiler or mapping an image
3. A positive hit requires `PRECOMPUTE_OUTPUT`, maps within this call's byte budget,
   verifies object integrity, then decodes v7. Codec `IllegalArgumentException`
   is wrapped as `IOException("invalid persisted regex image", cause)`
4. Require exact expression String equality, identical semantics and image state
   count no greater than request `maxStates`; then `fromPrecomputedImage` restores
   the stored lanes. Corrupt, wrong-kind, malformed or mismatched hits fail;
   there is no catch-and-recompile repair path
5. On a miss, invoke the compiler once. Null publishes an explicit negative
   descriptor, then returns `reused=false` with empty Optionals
6. A Program result is frozen to v7 precomputed form, compatibility-checked,
   encoded, and only then compared with `maxImageBytes`. A too-large image throws
   `IOException` before the output put, though compiler/freeze/encode work already
   occurred
7. Put encoded bytes as immutable `PRECOMPUTE_OUTPUT`, publish the positive cache
   descriptor/pointer, then return the original compiled Program and its output
   reference with `reused=false`

Compatibility checks do not recompile the expression or independently verify
its machine/derived facts. Compiler identity and trustworthy production remain
necessary. Exceptions before publication do not produce a wrapper-created
negative result. Storage/publish failure can leave content objects already put;
there is no cross-step rollback or in-flight compilation deduplication here.
The FS publisher rejects a conflicting result for an existing deterministic key.
Concurrent misses can compile more than once before publication arbitrates.

The current [FS dependency paths][fs-cache] verify hashes and use bounded mapped
views; [mapped lifetime][fs-mapped] requires an externally immutable store and
has no deterministic Java-21 unmap. Decoded regex lanes are detached and remain
usable independently of mapped transport after return. The budget limits encoded
bytes per invocation, not simultaneous mappings, decoded array memory, retained
heap or process RSS. FS bundle/catalog/native-content mechanics are separate
sibling review, not additional primary owners of this reference.

## 7. Trigram queries and exact membership facts

[Queries][query] are necessary conditions over UTF-16 triples. A key packs three
unsigned 16-bit units into the low 48 bits of a long. `ALL` is unconstrained true;
`NONE` is impossible false; `AND` requires every direct trigram and subquery;
`OR` needs one. `hasConstraints()` is false only for ALL; `termCount()` recursively
adds direct terms with exact integer arithmetic. It is not a regex-state or
callback-count bound.

The construction/composition helpers flatten matching operators, apply
ALL/NONE identities and absorbing cases, sort/deduplicate direct trigram terms,
and preserve a list of child queries. `fromExact` ORs per-string AND requirements;
an empty exact language becomes NONE and any legal string shorter than three
units makes the query ALL. Public `trigrams()` clones its array and `subqueries()`
returns an immutable list. Package-private read-only-kernel access exposes the
internal trigram array under a no-mutation contract.

`test(LongPredicate)` requires a nonnull predicate even for ALL/NONE. It evaluates
sorted direct terms before children and short-circuits; callback exceptions
propagate. A positive result never proves matching order, multiplicity, position,
count, capture or equality. For example, the `abc.*def` query can accept the
membership of `def---abc` while the regex rejects it. `toString()` is diagnostic
rendering with raw quoted code units, not a stable serialization/parser format.

Program's [lazy query derivation][query-build] reparses its retained expression
once and fails open to ALL on internal unsupported refusal, including when a
restored image outlives parser-surface evolution. It does not serialize or restore
this query in v7. Finite expansion is limited to 64 strings, 256 UTF-16 units per
string, 16 class literals and a bounded repeat expansion of eight, with the
single-exact-string fixed-repeat special case. Adjacent finite-language children
are combined until unknown content or a bound forces a flush. Losing a planning
constraint does not alter authoritative Program execution.

### Facts preparation, range identity and composition

[Fact preparation][facts] does not retain or materialize the source. Whole-value
`precompute(value, budget)` reads length, validates nonnegative length/budget and
admission, then scans every admitted unit once. Range
`precompute(value, from, to, budget)` first checks nonnull value and the half-open
range against `value.length()`, then the range-length budget, before key allocation
or character reads. It reads no units outside that range. The unit budget is not
bytes or whole-backing length; scan temporarily holds at most `max(0, length-2)`
keys, sorts/deduplicates them and retains boundary units.

`Facts` retains UTF-16 length, up to two leading/trailing units and sorted unique
keys. `utf16Length`, `keyCount`, defensive `keys`, and exact binary-search `test`
expose metadata. Multiplicity and arbitrary ordering are lost. Facts are not a
second canonical spelling store, persistent resolver ID, or sufficient material
to derive an arbitrary subrange. The caller must associate them with the exact
immutable source/owner/range observed; no automatic source-identity check is
performed by `testPrecomputed`.

`left.concat(right, budget)` requires a nonnull right side, adds lengths with
`Math.addExact`, then checks the combined budget **before** empty identity returns
or allocation. Overflow throws `ArithmeticException`; negative/exceeded budget
throws `IllegalArgumentException`. Nonempty composition merges the two sorted
sets plus at most two seam trigrams using retained suffix/prefix units, without
reading either original source. Empty operands may return the existing immutable
Facts object. Work and temporary/output space are linear in retained key counts;
there is no global cache or all-pairs precomputation. The result describes ordered
concatenation, even across surrogate-unit seams, not Unicode normalization or
code-point segmentation.

`testPrecomputed` evaluates the same necessary condition against those facts.
The isolated-range contract matters: transparent lookarounds in some external
regex backend may inspect outside that range, so using facts for such a query
requires separately justified scope. The retained parser itself refuses
lookarounds. A prepared positive candidate still needs its authoritative verifier.

## 8. Exact public provenance and evidence limits

The [coverage companion](retained-regex-reference-coverage.md) records current
source hashes, every member group, dependency ranges, oracle methods and provenance
hashes. The relevant chain is additive:

1. The original [v7 export][export] records donor revision
   `80d288951148548ef6aa4c95df9cb31a438aac16`: 22 nonlicense payloads plus preserved
   LICENSE/NOTICE, including six root regex Java files. The export's recorded
   manifest root is `d1889319f72f5e3ab9fb4804e27e8ec58d2bfc75e112539f5322f5f95e3d658b`.
   [PROVENANCE][provenance] binds the export and [SOURCE_CUSTODY][source-custody];
   that historical receipt explicitly says source-complete closure and java.base
   runtime qualification are false
2. The [adjacent-finite-runs manifest][successor-manifest] replaces Program's
   `9999e8af…` preimage with current `4e217b9a…` and adds TrigramQuery from ABSENT
   with current `428cb5cd…`. Its [actual authority record][successor-authority]
   distinguishes later executed recipe evidence from the earlier frozen metadata
   snapshot and leaves runtime/provider/full-family gates open
3. [Copy-recovery qualification][copy-recovery] records codec
   `09f4ef3d…` → `c7f17a7b…`, canonical recipe commit
   `970b2a40b4d6df391027f1bcd59bb6362d62276b`, exact receiver codec output only
4. [Nested-slice qualification][nested-slice] records codec
   `c7f17a7b…` → current `f7ad9444…`, canonical recipe commit
   `c41b6adb6b64f6f0432a0599e09a6c6dae99c93e`, exact pinned codec preimage/SDK
   postimage. Both codec records retain `NOT_ADMITTED`; complete receiving Program
   and JDK/jtreg qualification are not claimed

The current Compact, Accelerated, Precompute and PrecomputeKey bodies still match
their original v7 export rows. TrigramQuery is a later seventh root owner, not an
unlisted original v7 source. Successor hashes must not be compared to historical
rows as if those rows promised to describe every later revision.

Public historical [Program oracle text][program-tests] and [Query oracle text][query-tests]
were read, not executed. They exercise finite boolean/parser/table/input cases and
adjacent-run candidate behavior; they also reference text/resolver/hybrid/native
owners outside the retained slice. A provider test that returns when unavailable
is not proof that the provider ran. The original [qualification receipt][qualification]
describes bounded host/importer replay work and explicitly leaves source-complete
build, consumer wiring, cold-image wrong-geometry cases, Maven coverage, JNI/native,
matched JDK/jtreg, performance and full-family completion unqualified.

No root regex `src/test` bodies are physically present in the retained subtree.
The two oracle snapshots do not supply direct tests for CompilationSession,
current v7 persistence/codec validation, or Facts whole/range/concat methods.
Those contracts above come from the current source bodies, with missing current
execution evidence kept explicit. Diagnostic allocation numbers in successor
receipts do not establish a general speedup, retained-heap bound or native/provider
acceptance. This is a documentation boundary, not a runtime promotion.

[vendor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md
[delivery]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-delivery.md
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml#L1-L35
[import-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml#L1-L99
[build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh
[policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
[frozen-fixture]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/compatibility/synexia-reference/PROVENANCE.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json
[port-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-string-precompute-port-map.tsv#L47-L63
[reuse]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache-reuse.md
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md
[full-borrow]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/README.md
[compile]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L648-L798
[parser]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2940-L3593
[tables]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1857-L2523
[session]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L800-L1013
[input]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1465-L1496
[execution]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L1498-L1855
[workspace]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2525-L2593
[image]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L104-L232
[execution-image]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L234-L357
[compact]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCompactAsciiDfaImage.java#L19-L199
[accelerated]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexAcceleratedImage.java#L18-L80
[roots]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L360-L462
[codec]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L30-L183
[decode]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L185-L258
[files]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgramImageCodec.java#L260-L342
[precompute]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L18-L111
[key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java#L11-L41
[utf16-hash]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L49-L59
[fs-cache]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L220-L280
[fs-mapped]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L19-L93
[query]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L17-L170
[query-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexProgram.java#L2595-L2724
[facts]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexTrigramQuery.java#L172-L369
[export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/export.tsv
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/PROVENANCE.json
[source-custody]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/SOURCE_CUSTODY.json
[successor-manifest]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/recipe/templates/manifest.tsv
[successor-authority]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md
[copy-recovery]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-copy-recovery-20261011.json
[nested-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-nested-slice-20261011.json
[program-tests]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexProgramTest.java.txt
[query-tests]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/evidence/donor-oracles/MIndexRegexTrigramQueryTest.java.txt
[qualification]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json
