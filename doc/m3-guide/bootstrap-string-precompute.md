<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Bootstrap String precompute contracts

[Guide](README.md) · [String ownership](bootstrap-string-ownership.md) · [String operations and facts](bootstrap-string-contracts.md) · [Twelve-owner source coverage](bootstrap-string-source-coverage.md)

This reference covers all six remaining P01 implementation source bodies at public
source pin `68bfe667943f4a231f3627b56d34b0be53a4e4af`: `M3CodeTextFacts`,
`M3CodeTextSignalBatch`, `M3StringCodePointPrecompute`,
`M3StringPositionPrecompute`, `M3StringRegexDp` and
`M3StringSearchPrecompute`. Their Git blobs are unchanged from the original
`278b221e8365ebeb6095a3a5643b38a0564258b5` inventory. All are package-private
`java.lang` implementation owners in `java.base`, including the nested types
and interface overrides documented here. They are not new public application APIs.

The companion references cover canonical payload ownership, native/mapped lifetime,
admission, tuples, fixed facts and String operations. Together the three references
cover the twelve inventoried M3-prefixed source files, not all `java.lang`,
all public String observables, the StringBacking family, or runtime acceptance.
The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md),
[naming map](../../m3/docs/name-mapping.json), and existing provenance and per-file
licenses remain authoritative. No private source was used. Ordered receiving
remains String → arrays → collections → AST/compiler → remaining families.

Eight relevant Java test bodies were read. All compilation, repository tests,
native builds, policy/recipe checks, benchmarks and matched-runtime qualification
are **NOT_RUN by this authoring pass**. Hosted CI is separate exact-commit
evidence and is not relabeled by this statement. In particular, the [source-observed gaps](#source-observed-gaps)
below are not executed compiler/test results. No implementation or test was changed.

## Shared identity, preparation and memory boundaries

| Owner | Preparation and reuse | Result authority |
| --- | --- | --- |
| `M3StringSearchPrecompute` | Pattern plan eagerly prepares prefix/reverse-prefix, low-byte skips and optional exact trigram membership on a cache miss; source trigram facts prepare on demand | Search compares exact UTF-16 units; trigram membership only rejects impossible candidates |
| `M3StringPositionPrecompute` | Cache entry first allocates empty block tables; each 64-unit signal and exact position block prepares lazily | Negative signals may skip blocks; positive signals need exact code-unit lookup |
| `M3StringCodePointPrecompute` | A miss scans the exact range for paired low-surrogate offsets | Exact code-point count/offset geometry for that range |
| `M3StringRegexDp` | Pattern tokens and two DP rows are temporary, recomputed each call | Exact whole-region decision only within its admitted subset; `UNKNOWN` delegates |
| `M3CodeTextFacts` | Explicit scalar scan or typed precomputed import; no automatic owner cache | Heuristic code/regex/similarity signals, never syntax/equality/match proof |
| `M3CodeTextSignalBatch` | Explicit synchronous packed-row scan; no cache | Lexical flags and capped counters, never a parser or regex engine |

The three global search/position/code-point caches are **direct mapped**, not LRU.
Each entry weakly references the canonical owner and checks owner reference
identity, the exact packed coordinate and length before reuse. Slot hashes only
choose a slot; an equal hash, equal text or equal numeric coordinate on another
owner is not cache identity. The packed cache coordinate encodes owner-relative
start and range length in UTF-16 units. Search results and geometry offsets are
relative to the supplied `M3String` range, including ranges within larger owners.
They do not identify a file, generation, persistent artifact or process-wide text ID.

Atomic slot publication permits duplicate preparation on simultaneous misses.
The slot can be replaced without invalidating a plan already used by a caller.
Weak keys do not retain the canonical owner graph; derived arrays can remain in
their slot after a key clears until replacement. Temporary cursors and active
method arguments retain their sources during exact reads. There is no explicit
cache clear, reference-queue cleanup, persistence, provider selection or cancellation
API in these six owners. Trusted internal constructors take ownership of their
array arguments without defensive cloning or independent identity validation,
except for the explicit `M3CodeTextFacts` MinHash copy.

Published `maximumRetainedPrimitiveBytes()` values describe bounded cache lane
payloads, not total heap/process memory or all simultaneously live allocations:

| Cache | Fixed admission | Published formula and value |
| --- | --- | --- |
| Search | 256 pattern slots, lengths 2–8,192; 64 source-trigram slots, lengths 256–32,768 | `256 × (8192 × 16 + 256 × 4) + 64 × 32768 × 8` = 50,593,792 bytes (48.25 MiB) |
| Position | 64 slots, source lengths 256–32,768, 64-unit blocks | `64 × 512 × 8 + 64 × 32768 × (1 + 8)` = 19,136,512 bytes (18.25 MiB) |
| Code point | 64 slots, nonempty source lengths at most 32,768 | `64 × 16384 × 4` = 4,194,304 bytes (4 MiB) |

These formulas count retained primitive array lanes, excluding headers/references,
scalar fields, transient scratch, duplicate concurrent work and caller-held plans
after eviction. They are not an allocation admission budget or an OOM guarantee.
The constructors/static initialization and exact caller work have their own
allocation paths. The precise catches, including an uncaught position-signal
allocation, are described below. No cold/warm speedup or memory measurement is claimed.

## Search plans and exact UTF-16 matching

Source: [complete M3StringSearchPrecompute][search]. Callers:
[M3String search dispatch][m3-search], [String and builder byte-source dispatch][string-search].

`Plan` contains `patternLength`, forward `prefix`, `reversePrefix`, `skip256`
and optional `M3TQ.Facts trigrams`; it retains no `M3String` or cursor. The
private constructor of the outer class prevents instantiation. Constants fix
slot masks and admission limits; the two static `AtomicReferenceArray` fields
own the pattern/source tables. Pattern identity remains a caller precondition
when invoking the helpers with a `Plan`: the plan itself has no owner identity
field and those helpers do not revalidate that it belongs to the supplied pattern.

| Declared member/type | Contract and effects |
| --- | --- |
| `prepare(M3String)` | Returns null outside lengths 2–8,192. Within that range, catches `OutOfMemoryError` from `prepareAllocated` and returns null; it does not catch arbitrary errors or wrap null input. |
| `prepareAllocated(M3String,int)` | Checks weak owner/coordinate/length identity, then prepares forward and reversed KMP prefix lengths through a temporary `CodeUnitCursor`. Builds a 256-entry skip table and, for length ≥3, trigram facts. Publishes the completed plan with `CACHE.set`. |
| `slot(M3StringOwner,long)`, `sourceSlot(M3StringOwner,long)` | Mix owner identity hash, structural hash and coordinate into separate fixed table indices. They are routing functions, not equality or persisted keys. |
| `Plan(...)`, `Plan.retainedPrimitiveBytes()` | Trusted constructor stores its primitive lanes/facts. Report is four bytes per prefix/reverse-prefix/skip entry plus eight bytes per trigram key; scalar fields and object overhead are excluded. |
| `maximumRetainedPrimitiveBytes()` | Returns the fixed conservative lane bound in the preceding table, using checked addition. |
| `mayContain(M3String,Plan)` | Returns true if no pattern trigrams, source length outside 256–32,768, or source-fact preparation throws OOM. Otherwise asks whether the source's exact trigram set contains every required key. True is permission to continue exact matching, not proof of a match. |
| `sourceFacts(M3String)` | Reuses or fully scans the exact source range and atomically replaces one weak-key source slot. Source facts cover the whole supplied range even for a bounded search; this can weaken rejection but does not justify accepting a match outside the requested interval. |
| `precomputeTrigrams(M3String,int)` | Passes a temporary cursor-backed `CharSequence` to `M3TQ.precompute`. Facts contain unique packed 48-bit UTF-16 trigram keys and bounded prefix/suffix scalar metadata, no source/cursor reference. They discard occurrence order and multiplicity. |
| `CursorCharSequence(M3String)`, `(CodeUnitCursor,int,int)` | Private temporary readers hold a cursor plus start/length. `length()` returns the local range length; `charAt(int)` and `subSequence(int,int)` intend checked local bounds and translate to cursor offsets. Subsequences share the temporary cursor. `toString()` explicitly builds a new String; the inspected trigram scanner uses `length`/`charAt`, not this materializing export. See the source-resolution caveat below. |
| `indexOf(M3String,M3String,Plan,int,int)` | Dispatches to adaptive BMH for plan length ≥8, KMP otherwise; interval is half-open `[fromIndex,endIndex)`. This trusted helper assumes valid bounds, a compatible nonempty plan and stable sources. |
| `kmp(M3String,...)` and `kmp(byte[],byte,int,M3String,Plan,int)` | Forward exact matching with fallback through `prefix`. The M3 form uses cursors; the byte form decodes source units and reads the canonical needle. Returns first match or −1. |
| `adaptiveBmh(M3String,...)` and `adaptiveBmh(byte[],byte,int,M3String,Plan,int)` | Compare full UTF-16 units right-to-left. The skip table is indexed only by the trailing source unit's low byte; collisions reduce skip opportunities, never authorize equality. After advancing, excessive failed comparison work (`patternLength + 2 × distance advanced`) switches to KMP at the next candidate. |
| `indexOf(byte[],byte,int,M3String,int)` | Clamps the start to `[0,sourceCount]`; empty pattern returns that start, too-long pattern returns −1, one-unit pattern scans directly. Otherwise prepares a plan and uses BMH/KMP; refusal falls back to exact candidate comparison. It does not snapshot a mutable byte source or validate the whole coder/count/array geometry as a public API. |
| `lastIndexOf(M3String,M3String,Plan,int)` | Starts at `maximumStart + patternLength − 1`, runs reverse-prefix KMP and returns the largest admitted start or −1. Caller has already normalized the start and rejected empty/one-unit patterns. |
| `lastIndexOf(byte[],byte,int,M3String,int)` | Uses `min(fromIndex,sourceCount−patternLength)`; negative maximum returns −1, empty pattern returns maximum, one unit scans backward. Prepared longer patterns use reverse-prefix KMP; refusal uses exact candidate comparison. |
| `sourceUnit(byte[],byte,int)`, `reverseUnit(M3String,int)` | Decode LATIN1 through `StringLatin1`, otherwise through `StringUTF16`, or read `pattern[length−1−reverseIndex]`. Source coder/count are trusted internal inputs. Needle owner coder is not a rejection criterion. |
| `Entry(WeakReference,long,Plan)`, `SourceEntry(WeakReference,long,int,Facts)` | Private immutable holders store weak owner, coordinate and plan/facts (with explicit source length for `SourceEntry`). No payload is copied into either entry. |

The [M3TQ implementation][tq] reads each admitted unit once while building keys,
sorts and deduplicates them, and performs exact set inclusion. A source may
contain every pattern trigram without containing the pattern. The long
trigram-superset test below exercises precisely that distinction. No fuzzy
matching, normalization, code-point folding, regex interpretation or hash-only
acceptance occurs in these exact search helpers.

`M3String` handles null needles, empty/single-unit cases, start normalization and
prepared fixed-fact rejection before this plan path. Public bounded
`String.indexOf(String,begin,end)` validates its range separately. A flat needle
on an M3 source follows the separate exact loop in `String`; a canonical M3
needle on a byte-backed String/builder uses the byte-source overloads here.
The source's coder decodes its bytes. A Latin1-only needle range inside a
UTF16 owner can still match a Latin1 source. Do not reject solely from the
backing owner's coder, or claim that the mutable builder is snapshotted here.

OOM recovery is localized: pattern preparation returns null and source-trigram
preparation returns a permissive true. Exact search still allocates temporary
cursor objects in some paths; failures outside those preparation catches
propagate. No global allocation-free or universal fail-open guarantee follows.

## Lazy block-position precompute

Source: [complete M3StringPositionPrecompute][position];
[M3String BMP/supplementary callers][m3-search].

The position table searches **UTF-16 code units**, including isolated surrogate
halves. Supplementary-code-point search remains in `M3String`: it searches for
the high surrogate and checks the adjacent low surrogate, including across tuple
leaves, within the permitted search interval. No Unicode normalization or
case-insensitive matching is performed here.

| Declared member/type | Contract and effects |
| --- | --- |
| Private `M3StringPositionPrecompute()` | No instances; static cache/constant owner. |
| `indexOf(M3String,char,int,int)` | Clamps start upward to zero and end downward to source length; returns −1 if empty/reversed. Prepares optional blocks, skips negative signals, masks exact positions below start/at-or-above end and returns the least set bit. |
| `lastIndexOf(M3String,char,int)` | Clamps start to the last source index; negative returns −1. Walks blocks backward, masks positions above start and returns the greatest set bit. |
| `prepare(M3String)` | Returns null outside lengths 256–32,768. On a miss allocates block containers only, then publishes weak owner/coordinate/length. It does not eagerly read all text. |
| `blockSignal(M3String,Blocks,int)` | Zero means unprepared. Copies at most 64 units into temporary `char[]`, ORs the conservative two-bit `M3StringFacts.codeUnitSignal`, and publishes with CAS. A computed zero is `InternalError`; a racing winner's value is reused. |
| `exactBlock(M3String,Blocks,int)` | Lazily copies one block; sorts distinct units in temporary construction scratch while accumulating position masks. Retains only first-occurrence offsets and masks, trimmed to distinct-unit count; publishes by CAS. No retained char lane. |
| `linearIndexOf(...)`, `linearLastIndexOf(...)` | Exact forward/backward unit traversal for unavailable block preparation, or the corresponding local loop when exact-block allocation fails. Return −1 if absent. |
| `slot(M3StringOwner,long)` | Hash routing only; the weak identity/coordinate/length check governs reuse. |
| `maximumRetainedPrimitiveBytes()` | Counts maximum signal `long` lanes and one `byte` plus one `long` for every potential distinct unit in each cache slot. |
| `Blocks(int)` | Allocates `AtomicLongArray signals` and `AtomicReferenceArray<ExactBlock> exact`; signals and exact blocks publish independently. |
| `ExactBlock(byte[],long[])`, `mask(M3String,int,char)` | Trusted constructor stores `firstOffsets` and `masks`. Lookup binary-searches distinct-unit order by reading each candidate at `blockStart + firstOffsets[mid]` from canonical text. Offsets are 0–63, so the stored byte is nonnegative. Equality then returns its position mask, else zero. The source/block identity is a caller precondition. |
| `Entry(WeakReference,long,int,Blocks)` | Holds weak owner, coordinate, length and block tables; does not retain spelling or an owning source descriptor. |

Signals fold ASCII case and can collide, so a positive signal is intentionally
inconclusive. Exact-block masks identify exact case-sensitive units, with no
second spelling store: their keys are source offsets, not retained characters.
The caller's live source is needed to resolve those offsets.

**Allocation fallback has a hole in its scope.** The two search methods catch
OOM from `prepare` and from `exactBlock(...).mask(...)`, using exact scans in
those cases. Their `blockSignal(...)` call is outside both catches; its small
temporary `char[]` allocation can propagate OOM. This is a source observation,
not an injected-failure result. Do not generalize the cache's optional role to
“every position precompute allocation failure falls back.” Other exceptions and
the `InternalError` also propagate.

## Exact code-point geometry

Source: [complete M3StringCodePointPrecompute][codepoint];
[public String code-point callers][string-codepoint].

The retained geometry stores only the relative UTF-16 offsets of low surrogates
whose immediately preceding high surrogate is **inside this exact range**.
Unpaired halves each count as one code point. At a requested subrange boundary,
a pair crossing into/out of the range must not be collapsed.

| Declared member/type | Contract and effects |
| --- | --- |
| Private `M3StringCodePointPrecompute()` | No instances; owns constants and atomic cache. |
| `prepare(M3String)` | Returns null for empty or >32,768-unit inputs. On a miss scans adjacent UTF-16 units, collecting paired-low offsets in growable temporary `int[]`, trims it, constructs geometry and publishes the weak-key entry. OOM within the preparation try block returns null. |
| `slot(M3StringOwner,long)` | Routes owner/coordinate/structural-hash information to 64 slots; exact identity checks still apply. |
| `maximumRetainedPrimitiveBytes()` | Assumes at most half the maximum source units are paired lows: 4 MiB of cached int lanes, excluding other memory. |
| `Geometry(int,int[])` | Trusted constructor retains length and owned sorted paired-low offsets without cloning or checking sort/content. These are internal derived offsets, not a generally validated imported artifact. |
| `Geometry.codePointCount(int,int)` | Checks `0 ≤ begin ≤ end ≤ utf16Length`; empty returns zero. Computes `(end−begin) − (lowerBound(end)−lowerBound(begin+1))`. The `begin+1` excludes a low surrogate whose high surrogate lies before the requested range. |
| `Geometry.offsetByCodePoints(int,int)` | Checks index in `[0,utf16Length]`; zero returns index. Checks enough code points in the requested direction, then binary-searches a UTF-16 endpoint with the appropriate boundary treatment. Negative offset is negated as `long`, so `Integer.MIN_VALUE` is rejected rather than overflowing. Insufficient range throws `IndexOutOfBoundsException`. |
| `Geometry.pairedLowCount()`, `Geometry.retainedPrimitiveBytes()` | Return stored pair count and four times that count respectively. No array export. |
| `lowerBound(int[],int)` | Returns the first sorted offset not less than the key; private binary-search primitive. |
| `Entry(WeakReference,long,Geometry)` | Holds weak owner, coordinate and geometry; geometry length participates in reuse checks. |

`String.codePointCount` first checks public bounds, tries geometry and, on
refusal, gets exact facts for the selected M3 slice. Thus the actual fallback
is not always a byte-array traversal and may prepare fixed facts.
`String.offsetByCodePoints` can first answer by long-valued unit arithmetic for
a Latin1 owner or already-prepared one-unit-per-code-point facts. Otherwise it
tries geometry; refused/unavailable geometry or an invalid public index goes
to `Character.offsetByCodePoints(this,...)`. Cache absence cannot license a
different code-point result, but does not guarantee all fallback work avoids
allocation or succeeds under arbitrary memory exhaustion.

## Conservative dot/star full-match DP

Source: [complete M3StringRegexDp][regex]; [String.matches dispatch][string-matches].

For the compiled-node adapter and the separate Pattern Boolean plan reached
through some fallback calls, see [Pattern query contracts](bootstrap-pattern-query-contracts.md).
Its admission and work limits differ from this String-local helper.

This is a temporary whole-input decision helper for `String.matches(String)`
with an existing M3 receiver. It is separate from `M3PatternQuery`,
Pattern/Matcher state, [standalone regex precompute](regex-precompute-reference.md),
and any serialized regex program. It has no flags parameter, provider, cache,
captures, match positions, region API, cancellation, `hitEnd` or `requireEnd` state.

| Declared member/constant | Contract |
| --- | --- |
| Private `M3StringRegexDp()` | No instances. |
| `UNKNOWN = −1`, `NO_MATCH = 0`, `MATCH = 1` | Three distinct decisions; UNKNOWN must call the ordinary JDK engine, never be read as false. |
| `tryMatch(M3String,String)` | Accepts regex length 2–128, ASCII letters/digits or dot atoms, optional single following star, and at least one star. Rejects leading/doubled star, all other syntax and no-star patterns with UNKNOWN. Source length >8,192 or `(textLength+1) × (atomCount+1) > 1,000,000` also returns UNKNOWN. |
| `isAsciiAlphanumeric(char)` | Exactly A–Z, a–z and 0–9; no underscore, escape, class, Unicode property, group, alternation, anchor or inline flag. |
| `isJdkLineTerminator(char)` | Default dot excludes LF, CR, NEL (U+0085), U+2028 and U+2029. No DOTALL or UNIX_LINES mode. |
| `STAR`, `CODE_UNIT`, length/work constants | A bit in each temporary int atom marks repetition; its low 16 bits hold the literal/dot. Work is bounded by the stated text/pattern/cell limits, not a timeout. |

The nullable-prefix row allows zero repetitions of consecutive starred atoms.
For each source unit, the algorithm updates two boolean rows: a star either
uses zero copies from the current row or consumes another accepted unit from
the previous row. It reads the canonical source directly. Any encountered
surrogate, paired or unpaired and even across tuple seams, returns UNKNOWN so
the JDK engine preserves Unicode code-point regex behavior. Non-surrogate BMP
characters may match dot even though literal atoms are restricted to ASCII.

Pattern-token allocation occurs before the source/work checks; boolean rows
allocate after them. OOM is **not caught** in this helper or its inspected
`String.matches` dispatch. Null handling is a caller contract: public
`String.matches` checks regex first. The helper does not promise a normalized
error for arbitrary null internal arguments.

`String.matches` first tries its separate conservative literal-equality route,
then this DP when M3 storage exists, then `Pattern.matches(regex,this)` on
UNKNOWN. Invalid syntax is refused rather than diagnosed by this mini-parser;
the fallback remains responsible for `PatternSyntaxException`. One-character
patterns, star-free dot expressions, unsupported flags and oversized candidates
also delegate. No claim is made about general Pattern/Matcher optimization.

## Scalar code-text receiver and approximate signatures

Source: [complete M3CodeTextFacts][codefacts]. This owner is separate from
`M3StringFacts`: it is not the fixed-fact cache consulted by the preceding
String operations. No call from the inspected `String`/`M3String` dispatches
to this receiver was found; the direct receiver test is the demonstrated caller
in this review. That bounded inspection is not a repository-wide absence proof.

The [public target field map][field-map] records eleven donor-to-receiver field
rows; it is provenance metadata, not runtime validation or consumer admission.
The immutable result has ten primitive scalar fields (`utf16Length`,
`codePointCount`, `textFlags`, `contentHash64`, `presence64`, `simHash64`,
`lexicalPacked`, `javaKeywordHits`, `codeScore`, `regexScore`) plus a private
16-long MinHash lane. It retains no original source or snapshot characters.
Its static keyword list is algorithm metadata, not input spelling.

| Declared member/type | Contract and effects |
| --- | --- |
| Private `M3CodeTextFacts(...)` | Stores supplied scalars and clones MinHash. Only the checked import and scanned construction paths call it. |
| `fromPrecomputed(...)` | Requires nonnull MinHash with exactly 16 lanes; nonnegative lengths, counts and scores; code-point count no greater than UTF-16 length. Invalid geometry/counts throw `IllegalArgumentException`; null MinHash throws NPE. It does not validate text/lexical flag bits, hash correctness, score consistency, source equality, snapshot identity, a schema version or a producer's trustworthiness. |
| `scan(CharSequence)` | Requires nonnull source; allocates a full-length `char[]`, reads each `charAt` and rechecks length afterward, throwing `IllegalArgumentException` if length differs. Same-length mutation and inconsistent reads are not detected; caller must keep the source stable. Index/callback/allocation exceptions propagate. |
| Private `scan(char[])` | Eagerly derives all scalar and signature facts from that local snapshot. Allocates character/bigram key arrays, histograms, sort/maps and signatures. There is no input-length budget, cancellation or OOM recovery here; fixed-size retained output does not imply bounded transient work. |
| `minHash()` | Returns a defensive clone. Mutating input or export cannot alter an existing receiver's lane. |
| `lexicalFlags()` | Low 16 bits of `lexicalPacked`. |
| `semicolonCount()`, `braceCount()`, `parenCount()`, `backslashCount()`, `regexMetaCount()`, `punctuationCount()` | Unsigned eight-bit lanes at shifts 16, 24, 32, 40, 48 and 56 respectively; saturated at 255 during scanning, so they are not arbitrary exact counts. Imported lanes are trusted. |
| `likelyCode()`, `likelyRegex()` | Compare their respective scores with 8. Neither invokes a parser or Pattern compilation. |
| `simHashDistance(M3CodeTextFacts)` | Requires nonnull peer and returns Hamming distance `bitCount(left XOR right)` in 0–64. This is not edit distance or semantic similarity. |
| `estimatedJaccard(M3CodeTextFacts)` | Requires nonnull peer and equal nonempty MinHash lane counts; otherwise IAE. Returns equal-lane fraction, not an exact set Jaccard or substring/equality proof. It does not validate matching producer versions or feature domains. |
| `lexicalPacked(char[])` | Performs the exact lexical switch described in the next section over the full snapshot, with bounded two-unit lookahead. It does not skip comments, quoted strings or escapes. |
| `keywordHits(char[])` | Collects runs of `Character.isJavaIdentifierPart(char)`, lowercases tokens with `Locale.ROOT`, and checks a fixed Java-looking word list. This is deliberately not Java's case-sensitive grammar or full keyword set; comments/strings contribute too. |
| `addSignal(long,char)`, `mix32(int)` | ASCII-folds A–Z and ORs two mixed bits into the 64-bit presence signature. Collisions and case folding make positive membership inconclusive. |
| `simHash(Histogram,boolean)` | Uses bigram features when present, otherwise character features; occurrence counts weight per-bit votes. Only strictly positive votes set a bit. Empty input yields zero. |
| `minHash(Histogram,boolean)`, `mix(long,long)` | Uses 16 deterministic salted unsigned minima of distinct feature keys; frequency does not weight MinHash. Bigram hashes carry a separate marker. Empty input leaves every lane at unsigned all-ones, so two empty signatures estimate 1.0. Comparisons across character-only/bigram-producing inputs remain heuristics. |
| `lane(long,int)`, `saturate(long)` | Extract an unsigned eight-bit lane, or cap a nonnegative derived score at `Integer.MAX_VALUE`. |
| `Histogram`, private `Histogram(long[],int[])`, `Histogram.of(long[])` | Count feature values with `HashMap`, sort keys through `TreeMap`, and emit parallel owned key/count arrays. These histograms are construction scratch and not retained by the result. |
| `M3TextFlags`, private constructor | Internal constant namespace for text classification, not lexical punctuation bits; no instances. |

Content hashing uses `hash = hash × 1,000,003 + UTF16Unit` with Java long
wraparound. It is a candidate identity signal with collisions, not exact text
identity. Character/bigram features are UTF-16 units, while classification and
`codePointCount` combine valid surrogate pairs and preserve unpaired units.
Text flag bits 0–9 are ASCII, LATIN1, BLANK, ALL_LETTERS, ALL_DIGITS,
HAS_LETTER, HAS_DIGIT, HAS_UPPER, HAS_LOWER and WELL_FORMED_UTF16. Empty scans
set ASCII/LATIN1/BLANK/ALL_LETTERS/ALL_DIGITS/WELL_FORMED_UTF16 by the actual
zero-count comparisons; do not read these as evidence of a nonempty token.

The code score is `4×keywordHits + 4×semicolonLane + 3×braceLane + parenLane`,
plus five each for arrow/method-reference and two each for assignment/comparison.
The regex score is `2×regexMetaLane + 2×backslashLane`, plus four each for
class-bracket/quantifier and two for the regex-meta flag. Both saturate to int
maximum; their lexical counters were already capped at 255. These formulas
describe ranking heuristics, not language membership.

## Packed lexical rows and the separate provider boundary

Source: [complete bootstrap M3CodeTextSignalBatch][batch].

The bootstrap class has only `analyze`, `validate`, `pack`, its private outer
constructor, and nested `Limits`. It is **not** the same-named public interface
in `com.m3.precompute`, and does not call that interface or native library.
The header's possible future JNI ABI is not evidence of an implemented bootstrap
JNI provider. The [separate batch reference](precompute-batch-reference.md)
covers the standalone Java/JNI library in depth.

| Declared member/type | Contract and effects |
| --- | --- |
| Private `M3CodeTextSignalBatch()` | Static utility only. |
| `Limits(long maxTransferBytes,long maxUtf16Units,int maxRows)` | Compact constructor rejects either negative long limit or row limit <1 with IAE. Record component accessors/equality/hash/toString are implicit record projections, not extra explicitly declared methods. `DEFAULT` is 64 MiB transfer, 16,000,000 UTF-16 units and 1,000,000 rows. |
| `validate(char[],int[],int[],Limits)` | Nulls produce NPE. Requires equal offsets/lengths sizes, row count within max, whole packed-array length within unit max, and `2×units.length + 4×offsets.length + 4×lengths.length + 8×rowCount` within transfer max. Uses checked long addition. Each row needs nonnegative offset/length and long-valued end ≤ units length; violations produce IAE. |
| `analyze(char[],int[],int[],Limits)` | Validates before allocating the result, then packs rows in order into a new `long[]`. Empty batches are allowed. It retains no caller arrays and returns no partial array if a later failure escapes. There are no callbacks or polling; input arrays must stay stable for the whole call. |
| `pack(char[],int,int)` | Nonnull plus `Objects.checkFromIndexSize` range check; invalid direct range throws an index exception, unlike `validate`'s IAE. Scans only the half-open row, never looking beyond its end for `::` or `->`. Direct calls do not apply batch limits. |

Rows may overlap, repeat, be empty, be out of source order or leave unused gaps.
The budget charges the full packed array and metadata/result lanes once, not
sum-of-row scan work. Repeating a long row many times can therefore multiply
CPU work without increasing `maxUtf16Units`; it is not a total visited-unit or
deadline bound. The transfer formula charges the supplied character, offset
and length array payloads plus the result lane. It excludes object overhead,
unrelated caller allocations and additional transient work/storage. Validation does not lock
or clone mutable arrays, so it is not an atomic snapshot or a corruption seal.

Both bootstrap lexical implementations use this layout:

| Low-16 flag bit | Meaning |
| --- | --- |
| 0–3 | Brace, parenthesis, semicolon, backslash |
| 4–7 | Regex meta, arrow, method reference, assignment |
| 8–11 | Comparison, quantifier, class bracket, dot |
| 12–15 | Comma, colon, ampersand/pipe, whitespace |

Six capped count lanes follow: semicolons/braces/parentheses/backslashes/
regex-meta/punctuation at bits 16/24/32/40/48/56. Braces and parentheses
increment the regex-meta counter but set their own flags, not the regex-meta
flag. Backslash increments backslash/punctuation only. `. * + ? | ^ $` set
the regex-meta flag/counter; `* + ?` also set quantifier, dot/pipe their own
flags. Brackets set regex-meta plus class-bracket. `=`, `< > !`, comma, colon,
ampersand and hyphen increment punctuation with the respective flags where
defined; colon detects `::`, hyphen detects `->`. Default units only test
`Character.isWhitespace(char)`. This selected switch is not Unicode punctuation
classification, syntax-aware escaping, brace balance or regex validation.

### Provider, cancellation and error semantics stay outside java.lang

The separately inspected [public interface][provider-api], [Java provider][provider-java],
[native provider][provider-native], [progress interface][progress] and
[JNI lexical entry][provider-jni] establish the following boundary:

- The public interface adds `name()`, `accelerated()` and an `M3Progress` argument.
  Java identifies as `java-cpu`/false; native as `jni-c11`/true. These are provider
  labels, not performance results or bootstrap dispatch decisions.
- Java validates, allocates, calls `begin`, then polls before rows 0,256,... and
  after all rows, with `worked(1)` per row. Native validates, allocates, begins,
  polls before each ≤256-row JNI batch and after all batches, validates returned
  part length and reports worked part size. Neither polls inside one row or a
  running JNI call. Empty batches still reach the final cancellation check.
- Null monitor means `M3Progress.none()`. Its callbacks default to no-ops;
  cancellation is whatever exception the caller's `checkCanceled` throws.
  Callback/native failures propagate. `done()` is in the processing `finally`,
  but `begin` is before that try: a failing begin does not trigger done, and a
  throwing done can replace another failure. No partial result is returned.
- Native loading is explicit and synchronized/volatile: explicit library path,
  then path-file, otherwise `System.loadLibrary("m3_precompute")`. Read/load
  errors propagate or are wrapped as documented; no automatic Java fallback is
  installed by `loadRequired()` or `analyze()`. Native `analyze` refuses if not
  loaded, and a null/wrong-size returned part is `IllegalStateException`.
- The JNI entry copies a batch's offset/length arrays and each selected UTF-16
  row with `Get*ArrayRegion`, uses temporary native arrays and max-row scratch,
  writes a new Java long array, and frees scratch on its checked paths. It has
  no direct canonical owner/coordinate handle and no asynchronous retained Java
  array access. Geometry errors use IAE; native scratch failure requests OOM;
  existing JNI exceptions propagate. Java caller validation owns the transfer
  limit; the native entry independently checks row ranges, not a Limits object.

Those provider contracts must not be attributed to the bootstrap receiver.
Likewise, provider-level transfer limits do not bound every higher-level
`compileBatch` allocation or finishing operation. Reuse the standalone reference
for that separate pipeline; no provider parity/runtime result is asserted here.

## Inspected tests and precise open gates

All entries below are **source read, NOT_RUN**. Test source labels and declared
run configurations are not proof that the tested branch executed or passed.

| Test | Assertions actually present | Remaining limits |
| --- | --- | --- |
| [M3CodeTextFactsReceiverTest][test-facts] | Stores all eleven supplied values, checks MinHash input/export defensive copying; rejects negative UTF-16 length, code-point count above length, 15 lanes and negative keyword hits | Does not test scalar `scan`, exact flags/hash algorithms, threshold formulas, empty signatures, same-length mutation, every null/negative score case or producer identity; no jtreg run directive |
| [M3CodeTextSignalBatchTest][test-batch] | Checks two rows, brace/semicolon/backslash bits/counts, and out-of-image/mismatched-row/negative-offset rejection | One expected backslash count contradicts the inspected row; see below. No saturation, row overlap, limit boundaries, empty/null batches, mutation or exhaustive switch parity; no jtreg directive |
| [M3StringCodePointGeometryTest][test-codepoint] | Deterministic paired/unpaired/seam examples, 512 seeded random values of length <128 with many range/count/offset comparisons, and 33,000-unit fallback | Large ranges sample end positions; does not assert cache identity, eviction/GC/concurrent publication, exact 32,768 threshold, retained formula or injected OOM. Declares M3-enabled othervm |
| [M3StringPositionInvariant6Test][test-position] | Reflects exact-block `byte[] firstOffsets`/`long[] masks`, atomic container and weak-entry field shapes; checks published memory formula | Structure/formula test does not populate or traverse blocks, prove reclamation, inject allocation failure or measure total memory. Declares module-open main, not M3 feature-enabled othervm |
| [M3StringPrecomputeSearchTest][test-search] | Deterministic sparse and block-boundary search, low-byte BMH collisions, adaptive/reverse/overlap cases, tuple/surrogate seams; 2,000 seeded small composition cases against UTF-16 oracles; also many non-search String contracts | Oracle agreement alone does not prove which cache/algorithm path ran; no direct eviction/race/OOM tests, no exhaustive admission thresholds or performance evidence. Declares M3-enabled othervm with java.lang open |
| [M3StringCompositeLiteralSearchCursorTest][test-cursor] | 1,200 seeded composed/ranged UTF-16 cases, seam/repetition cases, and length-32,768 source/8,192 pattern with complete trigram superset but no exact match, plus an inserted real match | Strong finite range/seam evidence, not exhaustive path/identity/concurrency/retention validation; no injected failures or measurements. Declares M3-enabled othervm |
| [M3StringByteSourceNeedleTest][test-byte] | Builder searches for Latin1-only ranges from UTF16 owners, one-unit ranges, UTF16 source, a 403-unit prepared needle and 8,800-unit refusal/fallback | No mutable-source race, all coder/geometry-invalid internal inputs, exact 8,192 boundary, cache-path instrumentation or fault injection. Declares M3-enabled othervm |
| [M3StringRegexDpTest][test-regex] | Direct reflective decisions plus String/Pattern parity; 4,096 seeded ASCII dot/star cases with independent memoized oracle; five default line terminators, BMP dot, surrogate refusal, unsupported/malformed syntax and large input/pattern refusal | Declares both compact-string settings. No OOM injection, exhaustive 128/8,192/cell-edge matrix, every flag or general Matcher state proof. The 140-unit pattern refusal reaches the pattern-length cap, not an isolated DP-cell-cap test |

The search/geometry tests are useful acceptance inputs for a complete matched
M3 Java/HotSpot image. The [code-text host workflow][host-workflow] separately
selects Java 21, compiles just the two receiver classes and their two
`m3/core/test/java/lang` tests using `--patch-module java.base`, then runs the
two test mains. It also checks the field-map row count and receiver-schema
marker. That isolated host route does not patch String or qualify the modified
Java/HotSpot runtime; its definition is not evidence that its jobs ran or passed
for this documentation head. Follow [contributor build/test selection](../../CONTRIBUTING.md#first-build-prerequisites-outputs-and-test-selection)
and the [runtime guide](../../m3/runtime-integration/README.md); no stock-JVM
String.class transplant or unverified reproduction command is proposed here.

### Source-observed gaps

1. In [SearchPrecompute's imports and cursor methods][search], the unqualified
   `Objects.checkIndex` and `Objects.checkFromToIndex` calls have no
   `java.util.Objects` import, wildcard import or nested `Objects` declaration.
   The saved complete path inventory contains no `java.lang/Objects.java`, and
   the inspected Object/CharSequence boundaries supply no inherited type of that
   name. This is an unresolved source-level name-resolution concern, **compile
   NOT_RUN**; this review did not scan every other java.lang file for an unusual
   package-private declaration under another filename.
2. [Bootstrap batch test][test-batch] line 16's source literal decodes to
   `[U+0061,U+007B,U+003B,U+005C,U+005C]`. Row offset 1/length 4 selects `[1,5)`,
   namely brace, semicolon and **two** backslashes. The packed loop increments
   the backslash lane per selected unit, so source arithmetic gives 2, whereas
   line 25 expects 1. This is an independently source-checked assertion
   contradiction, **test NOT_RUN**, not a claimed runtime failure.
3. Position-signal OOM is outside the documented local catches, and DP does not
   catch OOM at all. The inspected tests do not inject these failures. Generic
   “optional precompute can always fail open” wording would overstate the source.

No source/test changes accompany these findings. Compilation, focused tests and
target resource/error qualification remain necessary before resolving them.
Full GC/JIT/CDS/SA/native-platform coverage, public API compatibility,
inline/package-Javadoc completeness, provider qualification and the ordered
receiving gates are still open. The [source coverage map](bootstrap-string-source-coverage.md)
separates this twelve-file documentation milestone from those outcomes.

[search]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java#L1-L508
[position]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java#L1-L303
[codepoint]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java#L1-L174
[regex]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3StringRegexDp.java#L1-L110
[codefacts]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3CodeTextFacts.java#L1-L493
[batch]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/java/lang/M3CodeTextSignalBatch.java#L1-L169
[tq]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/src/java.base/share/classes/jdk/internal/mindex/M3TQ.java#L186-L364
[m3-search]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/M3String.java#L1059-L1174
[string-search]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L2964-L3192
[string-codepoint]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L1743-L1802
[string-matches]: https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/String.java#L3411-L3424
[provider-api]: https://github.com/hsoliwal/M3jdk21/blob/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatch.java#L1-L27
[provider-java]: https://github.com/hsoliwal/M3jdk21/blob/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchJava.java#L1-L178
[provider-native]: https://github.com/hsoliwal/M3jdk21/blob/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchNative.java#L1-L95
[progress]: https://github.com/hsoliwal/M3jdk21/blob/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L1-L17
[provider-jni]: https://github.com/hsoliwal/M3jdk21/blob/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute/src/main/native/src/m3_precompute_signals_jni.c#L7-L113
[host-workflow]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/.github/workflows/m3-code-text-host-receiver.yml#L1-L49
[field-map]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/m3/lexicon/synexia-code-text-precompute-field-map.tsv
[test-facts]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/m3/core/test/java/lang/M3CodeTextFactsReceiverTest.java
[test-batch]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/m3/core/test/java/lang/M3CodeTextSignalBatchTest.java
[test-codepoint]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/test/jdk/java/lang/String/M3StringCodePointGeometryTest.java
[test-position]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/test/jdk/java/lang/String/M3StringPositionInvariant6Test.java
[test-search]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java
[test-cursor]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/test/jdk/java/lang/String/M3StringCompositeLiteralSearchCursorTest.java
[test-byte]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/test/jdk/java/lang/String/M3StringByteSourceNeedleTest.java
[test-regex]: https://github.com/hsoliwal/M3jdk21/blob/68bfe667943f4a231f3627b56d34b0be53a4e4af/test/jdk/java/lang/String/M3StringRegexDpTest.java
