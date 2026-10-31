<!-- SPDX-License-Identifier: Apache-2.0 -->

# Precompute batch and similarity reference

[Guide](README.md) · [Base text facts](m3-packages.md#precompute-api-contracts) · [Indexed-string bridge](indexed-string-reference.md) · [Coverage](coverage.md)

This reference covers eight complete Java owner bodies in `com.m3.precompute`:
`M3CodeTextSignals`, the lexical batch interface and its Java/JNI providers,
`M3SimilarityPrecompute`, and the similarity batch interface and its Java/JNI
providers. It traces their progress, text-fact, regex-shape, C/JNI and build
dependencies. It deepens the existing precompute package; it does not establish
complete package coverage or runtime acceptance.

Source inspection pin: `c038343521587d96c9fa190715ce2f4b9f8c490a`,
14 October 2026. Linked implementation and test bodies were read in full.
Examples, Java/native tests, builds, recipe/policy verifiers, benchmarks and
runtime qualification are **NOT_RUN**. Allocation and control-flow descriptions
are source observations, not measured performance or stress-test results.

## Choose facts, lexical features or candidate scores

The [module README][readme] remains the overview and build entry point. This is
an isolated Apache-2.0 library, not a replacement for `java.lang.String`, a
bootstrap component or an automatic native accelerator. Existing
[base-fact contracts](m3-packages.md#precompute-api-contracts) explain
`M3TextMetrics`, `M3TextSignals`, `M3Histogram`, exact edit-distance workspaces
and the progress interface. The [indexed-string reference](indexed-string-reference.md)
explains the separate bridge/materialization boundaries.

| Need | Entry point | Meaning of the result |
| --- | --- | --- |
| Code-looking/regex-looking features for one text | `M3CodeTextSignals.compile(text)` | Immutable feature record; heuristics nominate candidate/test routes |
| The same features for many texts | `M3CodeTextSignals.compileBatch(texts, provider, limits, monitor)` | Immutable row-ordered list; provider computes only the packed lexical part |
| Lexical bits/counts over prepacked UTF-16 ranges | `M3CodeTextSignalBatch.analyze(...)` | New `long[]`, one packed result per row |
| Pairwise safe filters and approximate facts | `M3SimilarityPrecompute.compare(left, right)` | Record derived from two `M3TextSignals` objects; each field keeps its own exactness boundary |
| Cheap approximate ranking of precomputed candidates | `M3SimilarityBatch.scores(...)` | New row-aligned `int[]`; lower is closer under this formula; no sorting |

Use `M3CodeTextSignalBatchJava.INSTANCE` or `M3SimilarityBatchJava.INSTANCE`
for the Java reference implementation. Native providers require an explicit
`loadRequired()` call. All four providers return `name()` as `"java-cpu"` or
`"jni-c11"`; `accelerated()` is respectively false or true. That boolean
identifies the implementation, not a measured speedup. Provider constructors
are private; the interfaces can also be implemented by callers.
[Sources: lexical API][lexical-api], [Java lexical][lexical-java],
[native lexical][lexical-native], [similarity API][similarity-api],
[Java similarity][similarity-java], [native similarity][similarity-native].

Candidate scores, hashes, lexical flags and shape labels do not establish text
equality, Java syntax, regex validity/matches or a passed edit threshold.
Retain the original text for the authoritative operation. The
[porting invariant][policy] and [name map][mapping] keep ordinary JDK APIs and
target ownership unchanged; documentation does not advance the active String
qualification phase or introduce a new source/recipe owner.

## Code-text compilation and snapshot provenance

[M3CodeTextSignals][code-signals] has no public constructor. Its two public
factories differ from the raw lexical provider:

| Public factory | Input, result and effects | Refusal and source-observed edge |
| --- | --- | --- |
| `compile(CharSequence text)` | Copies `length()`/`charAt()` into a new `char[]`, runs the Java lexical kernel, then finishes the record from the original input | Null input: `NullPointerException`; no batch limit or monitor parameter; caller-method, arithmetic and allocation failures propagate |
| `compileBatch(List<? extends CharSequence> texts, M3CodeTextSignalBatch batch, Limits limits, M3Progress monitor)` | `List.copyOf` freezes the list container, packs all row characters, invokes the selected provider once, finishes each original element and returns `List.copyOf` of the records | Null list/elements/provider/limits: `NullPointerException`; packed-length overflow beyond JVM `int` geometry: `IllegalArgumentException`; non-null provider result with wrong row count: `IllegalStateException` |

**The input must stay stable for the whole call.** Scalar compilation snapshots
characters only for lexical packing. Finishing reuses an input `String`, or calls
the original object's `toString()`, then derives text signals, keywords and
regex shape. Batch compilation copies element references, reads each element's
length repeatedly, copies characters and later calls the same finishing path.
It does not create an atomic snapshot of mutable/custom `CharSequence`s. A
custom `toString()` must describe the same text as its earlier `charAt` view.
Mutation can produce mismatched features or indexing/other caller exceptions;
there is no synchronization or retry protocol.

**Provider limits are not whole-call budgets.** Batch offsets/lengths and the
complete packed `char[]` are allocated before `analyze` validates limits.
Finishing occurs after the provider completes and is outside its progress
callbacks. It can materialize strings, allocate/sort the base histograms,
scan/lowercase keyword tokens and build regex-shape strings. Those costs and
the returned list/records are not bounded by the provider's transfer expression.
Even a call refused by the provider may already have paid for packing.
[Sources: compilation/finishing][code-signals], [base signals][text-signals],
[histograms][histogram], [regex shape][shape].

A custom provider's result is trusted except for row count: arbitrary packed
values are accepted. A null result produces `NullPointerException` when
`lexical.length` is read, rather than the wrong-row-count exception. Provider
exceptions propagate and no partial list is returned. With the built-in
providers, an empty list returns an empty immutable list after the provider's
empty-batch progress path. Callers implementing their own provider also own
its monitor, copying, lifetime and failure behavior.

### `Snapshot`: stored values are not an authenticity seal

The public nested record constructor is
`Snapshot(M3TextSignals text, long lexicalPacked, int javaKeywordHits, int codeScore, int regexScore, M3RegexShape.Kind regexShape)`.
It rejects null `text`/`regexShape` and negative integer scores/hits. It does not
recompute features, reject unknown packed bits or prove that these components
describe the same text. `compile`/`compileBatch` derive the components; direct
construction accepts caller-asserted facts. [Source][code-signals].

| Public record operation | Contract | Inspected test evidence / gap |
| --- | --- | --- |
| `text()`, `lexicalPacked()`, `javaKeywordHits()`, `codeScore()`, `regexScore()`, `regexShape()` | Return the stored components; no original spelling is stored by this record | `[CT1–CT3]` inspect selected compiled components; direct inconsistent construction is not covered |
| `lexicalFlags()` | Returns `(int) lexicalPacked`, including counter bits in positions 16–31; it is **not** a low-16-bit mask | No direct accessor-mask assertion in the selected tests |
| `semicolonCount()`, `braceCount()`, `parenCount()`, `backslashCount()`, `regexMetaCount()`, `punctuationCount()` | Extract the corresponding unsigned byte lane, each in 0–255 | `[CT1]` checks selected counts; `[C1]` checks selected C lanes; saturation boundaries are not covered |
| `likelyCode()`, `likelyRegex()` | Respectively `codeScore >= 8`, `regexScore >= 8`; both may be true | `[CT1]` covers three examples, not all threshold edges |
| `simHashDistance(Snapshot other)` | Null other: `NullPointerException`; delegates to the two stored text-signal objects | `[CT1]` checks a positive distance on one pair |
| `estimatedJaccard(Snapshot other)` | Null other: `NullPointerException`; delegates to the stored text-signal objects | `[CT1]` checks the sample result is in [0,1] |

The record is immutable and exposes an immutable `M3TextSignals` reference,
primitive fields and an enum. The generated record `equals`, `hashCode` and
`toString` use those components; they do not compare original text. In
particular, `M3TextSignals` does not override `equals`, so independently compiled
snapshots are not promised record equality even for identical spellings. Use
the intended feature comparison or verify the actual text instead.
[Sources: record][code-signals], [text-signal owner][text-signals].

## Packed lexical layout and heuristics

The Java reference examines **UTF-16 code units** in each supplied row. It does
not parse Java, regex syntax, comments, literals or escapes. Pairs such as `->`
and `::` are recognized only when both units lie within the row. Each result
packs 16 flag bits below six unsigned byte counters. Count lanes saturate at
255 in the Java-array/JNI input domain. [Java kernel][lexical-java], [C kernel][c].

| Public flag constant | Bit | Observed trigger |
| --- | ---: | --- |
| `HAS_BRACE` | 0 | `{` or `}` |
| `HAS_PAREN` | 1 | `(` or `)` |
| `HAS_SEMICOLON` | 2 | `;` |
| `HAS_BACKSLASH` | 3 | Backslash |
| `HAS_REGEX_META` | 4 | `.`, `*`, `+`, `?`, pipe, `^`, `$`, `[` or `]` |
| `HAS_ARROW` | 5 | `-` followed by `>` in the same row |
| `HAS_METHOD_REFERENCE` | 6 | `:` followed by `:` in the same row |
| `HAS_ASSIGN` | 7 | `=` |
| `HAS_COMPARISON` | 8 | `<`, `>` or `!` |
| `HAS_QUANTIFIER` | 9 | `*`, `+` or `?` |
| `HAS_CLASS_BRACKET` | 10 | `[` or `]` |
| `HAS_DOT` | 11 | `.` |
| `HAS_COMMA` | 12 | `,` |
| `HAS_COLON` | 13 | `:` |
| `HAS_AMP_PIPE` | 14 | `&` or pipe |
| `HAS_WHITESPACE` | 15 | Java `Character.isWhitespace(char)` in the Java kernel; explicit table in C |

`regexMetaCount` also counts braces and parentheses, even though those cases do
not set `HAS_REGEX_META`. The flag and the counter therefore answer different
questions. `punctuationCount` counts only switch-recognized punctuation, not a
Unicode punctuation category. Backslash increments its own and punctuation
counts, not the regex-meta count. Hyphen contributes punctuation even when no
arrow follows. [Source][lexical-java].

| Counter accessor | Shift | Counted units |
| --- | ---: | --- |
| `semicolonCount()` | 16 | `;` |
| `braceCount()` | 24 | `{`, `}` |
| `parenCount()` | 32 | `(`, `)` |
| `backslashCount()` | 40 | Backslash |
| `regexMetaCount()` | 48 | Braces, parentheses, brackets, `.`, `*`, `+`, `?`, pipe, `^`, `$` |
| `punctuationCount()` | 56 | All switch-recognized punctuation, including `=`, comparisons, comma, colon, ampersand and hyphen |

Bit-test named constants, for example
`(snapshot.lexicalFlags() & M3CodeTextSignals.HAS_ARROW) != 0`.
If a consumer needs only the packed flag field, mask explicitly with `0xffff`.
A negative packed `long` or `lexicalFlags()` value can be a valid result because
high counter bits can be set; neither is an error sentinel at the Java API.

Finishing computes these integer heuristics, capped at `Integer.MAX_VALUE`:

- `codeScore`: four times keyword hits, four times semicolons, three times braces, plus parentheses; add five each for arrow/method reference and two each for assign/comparison
- `regexScore`: twice regex-meta count plus twice backslashes; add four each for class bracket/quantifier and two for the regex-meta flag

Keyword recognition collects `Character.isJavaIdentifierPart(char)` tokens,
lowercases with `Locale.ROOT`, and compares with a fixed list: `class`,
`interface`, `enum`, `record`, `if`, `else`, `for`, `while`, `do`, `switch`,
`case`, `try`, `catch`, `finally`, `throw`, `throws`, `return`, `new`, `public`,
`private`, `protected`, `static`, `final`, `synchronized`, `native`, `void`,
`int`, `long`, `boolean`, `double`, `float`, `char`, `byte`, `short`.
This is neither a complete Java keyword taxonomy nor a language parser.
Matches inside comments or quoted text still count. [Source][code-signals].

`regexShape()` exposes only `M3RegexShape.Kind`: `LITERAL`, `STRICT_PREFIX`,
`STRICT_SUFFIX`, `STRICT_EXACT`, or `GENERAL`. Finishing calls `analyze(text)`
with no separate budget. The recognizer specializes nonempty fully decoded
literal bodies with optional `\A`/`\z` anchors; unsupported constructs, empty
bodies and rejected surrogate/escape shapes become `GENERAL`. A `GENERAL`
label does not mean invalid syntax. This record stores the label, not a
compiled `Pattern` or an exact match result. The shape/trial/oracle family
needs its own broader API review. [Shape source][shape], [module overview][readme].

## Raw lexical batch contract and accounting

`M3CodeTextSignalBatch.Limits(long maxTransferBytes, long maxUtf16Units, int maxRows)`
rejects negative byte/unit limits and `maxRows < 1` with
`IllegalArgumentException`. Its `DEFAULT` is 64 MiB (`64L << 20`), 16,000,000
UTF-16 units and 1,000,000 rows, in that constructor order. The record's three
component accessors return these stored values; it supplies ordinary generated
record value operations. [Interface and limits][lexical-api].

For `analyze(char[] units, int[] offsets, int[] lengths, Limits limits, M3Progress monitor)`,
let `U = units.length` and `N = offsets.length`. Both built-in providers use
the same Java validation:

1. Null `units`, `offsets`, `lengths` or `limits` is refused with `NullPointerException`
2. `offsets.length` must equal `lengths.length`, and `N <= maxRows`
3. `U <= maxUtf16Units`
4. Nominal transfer `2*U + 4*N + 4*N + 8*N = 2*U + 16*N` must not exceed `maxTransferBytes`
5. Every row must have nonnegative offset/length and `(long) offset + length <= U`

Violations in steps 2–5 produce `IllegalArgumentException`. Transfer sums use
`Math.addExact` with long operands; the expression is accounting, not bytes
actually transferred by every implementation. Empty arrays/zero-length rows
are allowed; the positive `maxRows` constructor requirement does not require
a nonempty batch. A zero-length row may start at `U`. No well-formed-surrogate
check, row sort/disjointness check or copy of the input arrays is performed.
[Validation source][lexical-java].

Repeated and overlapping ranges are valid. The budget counts the full packed
image, including unused units, **not the sum of row lengths**. Repeating one
large range many times repeats its scan and, natively, its copies. There is no
lexical `maxWork` field. The caller must keep all three arrays stable until
return; validation and subsequent reads are not one coherent concurrent
snapshot. This also applies to mutations made by a progress callback.

Results preserve row order and are new caller-owned arrays. The built-in
providers do not mutate or retain input arrays and do not retain returned arrays.
They provide no close/release method. The Java path allocates `long[N]` after
validation and before progress begins. The native path additionally allocates
chunk results and native scratch described below. Allocation failure can occur
despite a passing transfer check. `[CT2]`, `[NP1]` and `[C1]` cover selected
stable-input results; budget edges, overlapping-row accounting, mutation and
throwing callbacks remain source-only observations in this pass.

## Pairwise similarity record

[M3SimilarityPrecompute][pair] is a public record with the constructor
`M3SimilarityPrecompute(int utf16LengthDelta, int editLowerBound, int simHashDistance, double estimatedJaccard, boolean mayEqual, boolean anagram)`.
The six same-named component accessors and generated record value operations
return/compare the stored values. No spelling, mutable workspace or native
resource is retained.

| Entry/component | Derived meaning from `compare(left, right)` | Boundary |
| --- | --- | --- |
| `compare(M3TextSignals left, M3TextSignals right)` | Creates a new record from the two supplied fact objects | Null operand: `NullPointerException`; it does not compile or verify original text |
| `utf16LengthDelta()` | Absolute UTF-16 length difference, computed with a long subtraction | UTF-16 units, not Unicode code points or encoded bytes |
| `editLowerBound()` | Delegates to the safe unit-cost UTF-16 Levenshtein lower bound | A lower bound above a threshold can reject; a bound below it does not prove a match |
| `simHashDistance()` | Hamming distance between the two 64-bit sketches | Derived range 0–64; zero is not equality |
| `estimatedJaccard()` | Equality fraction of the 16 MinHash lanes, with explicit empty-text handling | Approximate feature-set similarity; both empty gives 1, exactly one empty gives 0 |
| `mayEqual()` | Length and two hash checks from `M3TextSignals.mayEqual` | True still requires original-text equality verification |
| `anagram()` | Exact equality of UTF-16-unit histograms | No order, code-point or normalization-equivalence claim |

The constructor checks only that the three integer fields are nonnegative and
that Jaccard is not less than 0 or greater than 1. It does **not** cap a supplied
SimHash distance at 64, reconcile fields, or authenticate their origins.
`Double.NaN` passes the two Jaccard comparisons; infinities do not. The derived
`compare` path and arbitrary direct construction therefore have different
guarantees. No direct constructor/`compare` assertions occur in the three Java
test files selected for this packet. Delegated fact semantics remain covered
by the [earlier reference](m3-packages.md#precompute-api-contracts), not a new
pairwise execution claim. [Record][pair], [delegated operations][text-signals].

## Approximate similarity batch contract

`M3SimilarityBatch.Limits(int maxRows, long maxTransferBytes, long maxWork)`
rejects `maxRows < 1` or either negative long with `IllegalArgumentException`.
`DEFAULT` is 1,000,000 rows, 64 MiB and 64,000,000 work units. The three
component accessors and generated record value operations expose those limits.
[Interface][similarity-api].

`scores(int queryUtf16Length, long querySimHash, int[] candidateUtf16Lengths, long[] candidateSimHashes, Limits limits, M3Progress monitor)`
uses arbitrary 64-bit hash patterns and nonnegative UTF-16 lengths. Callers
must provide matching, stable lengths/hashes; no text is retained or hashed by
this API. Both built-in providers call the Java validator:

| Check | Refusal |
| --- | --- |
| Query length is negative | `IllegalArgumentException`, before array/null validation |
| Candidate array or limits is null | `NullPointerException` |
| Candidate arrays have different lengths, or `N > maxRows` | `IllegalArgumentException` |
| Nominal transfer `4*N + 8*N + 4*N = 16*N` exceeds the byte budget | `IllegalArgumentException` |
| Nominal work `64*N` exceeds `maxWork` | `IllegalArgumentException` |
| Any candidate length is negative | `IllegalArgumentException` |

Zero candidates are allowed, even with zero transfer/work budgets. The transfer
sum uses exact long additions; the work formula counts 64 hash-bit positions
per row, not elapsed time or every validation/copy/callback/allocation step.
Input arrays are not cloned or synchronized by Java validation. Concurrent
mutation can invalidate the relationship between validation and scoring; native
copies taken separately are not an atomic paired snapshot either.
[Java validator/scorer][similarity-java], [native wrapper][similarity-native].

For each candidate, both implementations compute:

```text
hamming = bitCount(querySimHash XOR candidateSimHash)
delta = abs((long) queryUtf16Length - candidateUtf16Length)
score = hamming * 1024 + min(delta, 1023)
```

The result is in 0–66,559 for accepted stable inputs. One Hamming bit outweighs
any length penalty; length differences of 1,023 or more all receive the same
penalty. The returned array stays in candidate order: the provider neither
sorts it nor chooses a tie-break. A zero score means identical supplied length
and SimHash, not necessarily identical text. Scores are not edit distances or
safe edit lower bounds. [Java formula][similarity-java], [C formula][c].

Java allocates a new `int[N]` before progress begins. JNI allocates the Java
result and full native arrays during the native call. Returned arrays belong
to the caller, and these providers retain no candidate/output arrays after
return. `[SB1]` checks one deterministic ranking fixture and `[SB2]` two
invalid-input cases; budget, mutation, callback and cancellation edges are not
covered by those assertions.

## Progress, exceptions and cancellation

[M3Progress][progress] supplies `NONE`, `none()` and default no-op `begin`,
`subTask`, `worked`, `checkCanceled`, `done`. Null monitor selects that no-op
instance. These four providers do not call `subTask`. Cancellation is defined
by the caller throwing from `checkCanceled`; there is no built-in token,
deadline, interruption check or exception subtype guarantee.

| Built-in operation | Progress sequence after validation | Cancellation granularity |
| --- | --- | --- |
| Java lexical `analyze` | Allocate result; `begin("M3 code-text lexical signals", N)`; `worked(1)` after each row; final check; `done()` in finally | Check before rows 0, 256, 512, … and after the loop; none inside one row |
| Native lexical `analyze` | Allocate full result; `begin("M3 JNI code-text lexical signals", N)`; compute/copy chunks of at most 256 rows; `worked(chunkRows)`; final check; finally `done()` | Before each native chunk and after all chunks; no callback during C scanning/copying |
| Java similarity `scores` | Allocate result; `begin("M3 similarity candidate scores", N)`; `worked(1)` per row; final check; finally `done()` | Before rows 0, 1024, 2048, … and after the loop |
| Native similarity `scores` | `begin("M3 JNI similarity candidate scores", N)`; precheck; one native call; validate result; `worked(N)`; postcheck; finally `done()` | Only before/after the entire native call |

`begin` is outside each `try/finally`: if it throws, `done` is not called.
Validation/loading failure and some result allocations happen before `begin`.
Once the try body is entered, `done` runs on normal completion, failure or
cancellation; a throwing `done` can replace an earlier exception. Other callback
exceptions propagate without conversion. A failure/cancellation returns no
partial array, although earlier callbacks may already have run.

An empty Java batch still calls `begin`, final `checkCanceled` and `done`,
without a row `worked` call. Empty native lexical behaves similarly and makes
no JNI call. Empty native similarity still performs its precheck/native call,
`worked(0)`, postcheck and `done`. None of these paths promises timely
cancellation during allocation, validation, one very long row, native work,
or the `compileBatch` packing/finishing stages. Do not use progress as a
universal resource acquisition/release protocol. [Four implementations][lexical-java],
[native lexical][lexical-native], [Java similarity][similarity-java],
[native similarity][similarity-native]. The selected tests do not exercise
these throwing/cancellation paths.

## Native opt-in, loading and resource lifetime

`M3CodeTextSignalBatchNative.loadRequired()` loads once behind a volatile flag
and synchronized double check, then returns a new provider instance.
`isLoaded()` reports that class's flag without attempting a load.
`M3SimilarityBatchNative.loadRequired()` delegates to the same loader and
returns its own new provider; it has no separate public `isLoaded()` method.
No public unload/close operation or automatic Java fallback exists.
[Lexical loader][lexical-native], [similarity loader][similarity-native].

Loading chooses the first applicable route:

1. Nonblank `m3.precompute.native.path`: normalize an absolute path and call `System.load`
2. Otherwise nonblank `m3.precompute.native.pathFile`: read the named file, strip its contents, normalize the contained path and call `System.load`
3. Otherwise `System.loadLibrary("m3_precompute")`

An empty path-file body produces `IllegalStateException`; an `IOException`
reading it is wrapped in `IllegalStateException`. Invalid-path, security or
linkage failures otherwise propagate. Failure does not fall through to the
next route and does not set the loaded flag, so a later explicit load can retry.
Once loaded, changing properties does not trigger a reload in that class.
Relative library paths are made absolute against the process working directory,
including a relative path read from a path file; they are not resolved against
the path file's parent directory. The direct path property is not stripped.
The flag is class-local state, not a cross-class-loader ABI/version or parity
certificate. Each native operation checks the flag before Java argument
validation; a missing/incompatible native symbol can still fail on invocation.

### JNI copying is not zero-copy

The [JNI body][jni] uses `GetIntArrayRegion`, `GetLongArrayRegion` and
`GetCharArrayRegion` into native `malloc` storage, followed by `Set*ArrayRegion`
for the Java result. It does not acquire a retained Java heap pointer or create
a persistent native handle. All observed scratch cleanup paths free the
allocated native arrays, including range-copy/validation failure paths; pending
JNI exceptions propagate. Failed scratch allocation attempts to throw
`OutOfMemoryError`; Java result allocation can independently fail.

| JNI entry | Allocation/copy boundary | Revalidation and result handling |
| --- | --- | --- |
| `M3CodeTextSignalBatchNative.nativeAnalyzeRange` | Java chunk `long[]`; native offsets/lengths/values for the chunk; one reusable char buffer sized to its maximum row length; each row's units copied separately | Checks null arrays, equal metadata lengths, row interval, nonnegative row geometry and packed-image bounds; Java wrapper rejects null/wrong-sized chunk output with `IllegalStateException` before copying into the full result |
| `M3SimilarityBatchNative.nativeScores` | Java full `int[]`; native full-length lengths/hashes/scores arrays; copies both input arrays and writes all scores back | Checks nonnegative query, nonnull arrays, equal lengths and each copied candidate length; Java wrapper rejects null/wrong-sized output with `IllegalStateException` |

For lexical calls through the public provider, chunk metadata is bounded by
256 rows, but a single row buffer can be as long as the largest row. The full
Java output, original input arrays and temporary chunk results also exist.
Similarity scratch is proportional to the whole candidate count, not chunked.
The nominal Java byte budgets omit array/object headers, allocator overhead,
these extra copies and the broader caller work. No allocation-failure,
class-loader, GC/lifetime stress or peak-native-memory measurements were run.

At extreme custom row limits, passing Java validation is not a guarantee that
native lexical chunk indexing can represent the final interval: `from + 256`
and `from += 256` use signed `int` arithmetic. If arrays/allocation permit more
than 2,147,483,392 rows, the next chunk-end addition overflows and JNI rejects
the resulting interval. This source-observed edge lies far above the default
row limit and is not exercised by the inspected tests. [Wrapper][lexical-native],
[JNI range validation][jni].

### C/header boundary

The [header][header] exposes C linkage (with an `extern "C"` guard for C++):

- `uint64_t m3_precompute_code_text_signal(const uint16_t *units, size_t length)` scans borrowed units and returns packed features; `(NULL, 0)` returns zero, while null with nonzero length returns `UINT64_MAX`
- `int m3_precompute_similarity_score(int query_length, uint64_t query_hash, int candidate_length, uint64_t candidate_hash)` computes the same score without allocating or retaining inputs

These C functions do not implement Java limits, callbacks, array bounds or
ownership checks. Direct C callers must supply valid readable storage and
suitable lengths. In particular, the score kernel assumes the nonnegative
length domain enforced by Java/JNI: it subtracts C `int`s before conversion
and is not a validating API for arbitrary signed inputs. The lexical kernel's
counters are unsigned and only saturated when packed; the Java-array domain
does not justify an unlimited `size_t`-length saturation guarantee for direct
C callers. The null-input sentinel is not a Java-level error code because JNI
validates its own inputs. [C implementation][c].

Java uses the runtime's `Character.isWhitespace(char)`; C embeds a table of
selected UTF-16 values/ranges. `[NP1]` and `[C1]` include selected Unicode
whitespace examples, not exhaustive parity across all UTF-16 values and JDK
versions. Neither C nor JNI executes regex matching or Java parsing.

## Source-checked usage examples, NOT_RUN

These fragments use Java 21 library types and were inspected against the
source signatures, not compiled or executed. Native opt-in changes only the
chosen provider; authoritative text/regex verification remains the caller's
separate operation.

```java
import com.m3.precompute.M3CodeTextSignalBatch;
import com.m3.precompute.M3CodeTextSignalBatchJava;
import com.m3.precompute.M3CodeTextSignals;
import com.m3.precompute.M3SimilarityBatch;
import com.m3.precompute.M3SimilarityBatchJava;
import com.m3.precompute.M3SimilarityPrecompute;
import java.util.List;

List<String> values = List.of("x -> x + 1", "Type::method", "plain text");
List<M3CodeTextSignals.Snapshot> facts = M3CodeTextSignals.compileBatch(
        values, M3CodeTextSignalBatchJava.INSTANCE,
        M3CodeTextSignalBatch.Limits.DEFAULT, null);
boolean hasArrow =
        (facts.getFirst().lexicalFlags() & M3CodeTextSignals.HAS_ARROW) != 0;

int[] lengths = new int[facts.size()];
long[] hashes = new long[facts.size()];
for (int row = 0; row < facts.size(); row++) {
    lengths[row] = facts.get(row).text().metrics().utf16Length();
    hashes[row] = facts.get(row).text().simHash64();
}
int[] scores = M3SimilarityBatchJava.INSTANCE.scores(
        lengths[0], hashes[0], lengths, hashes,
        M3SimilarityBatch.Limits.DEFAULT, null);
// scores[0] is expected to be zero; the array is still in input order.
M3SimilarityPrecompute pair = M3SimilarityPrecompute.compare(
        facts.get(0).text(), facts.get(1).text());
// pair.mayEqual(), if true, would still require original-text verification.
```

For raw row geometry, the two rows below are permitted to overlap and repeat:

```java
char[] image = "a->b".toCharArray();
long[] packed = M3CodeTextSignalBatchJava.INSTANCE.analyze(
        image, new int[] {0, 0}, new int[] {image.length, image.length},
        M3CodeTextSignalBatch.Limits.DEFAULT, null);
// Nominal transfer: 2 * 4 + 16 * 2 = 40 bytes, not total process memory.
// Keep image and both metadata arrays unchanged until analyze returns.
```

## Test evidence, build defaults and open qualification

The identifiers below link claims to **assertions inspected, NOT_RUN**. A test
method's name does not establish that the associated behavior passed here.

| ID / file | Assertions actually inspected | What they do not establish |
| --- | --- | --- |
| `[CT1]` [M3CodeTextSignalsTest][code-tests], `separatesNaturalCodeAndRegexText` | Natural/code/regex fixtures; selected likelihoods/counts/keywords; positive sample SimHash distance; Jaccard range | Complete flag taxonomy, saturation, exact threshold edges, syntax validity or direct-constructor consistency |
| `[CT2]` same file, `batchCompilationMatchesScalarPacking` | Seven immutable strings; same row count, packed lexical values, code/regex scores and content hashes between scalar and Java batch | Mutable/custom inputs, budget refusals, null/wrong custom-provider results or every component/accessor |
| `[CT3]` same file, `regexShapeIsExposedAsCandidateEvidenceOnly` | `\Aabc\z` gives `STRICT_EXACT`; `a+` gives `GENERAL` | Whole regex-shape API or regex engine equivalence |
| `[SB1]` [M3SimilarityBatchTest][similarity-tests], `canonicalBatchRanksPrecomputedCandidatesDeterministically` | Four candidate strings; first score zero; one relative ranking; one score equals the helper formula | Sorting, all ties, large deltas, every hash/length boundary or pairwise-record constructor semantics |
| `[SB2]` same file, `invalidGeometryAndNegativeLengthsFailClosed` | Mismatched candidate arrays and a negative candidate length throw `IllegalArgumentException` | Every null, query length, limit, empty or cancellation case |
| `[NP1]` [M3NativeParityTest][native-tests], `nativeLexicalAndSimilarityBatchesMatchCanonicalJava` | Java/JNI array equality on nine stable strings, including selected Unicode whitespace and arrows/method references after a 1,023-character prefix | Exhaustive Unicode/JDK-version, chunk-boundary, budget, allocation, class-loader or cancellation proof |
| `[C1]` [m3_precompute_signals_checks.c][c-tests] | Selected lexical flags/counts and whitespace; `(NULL,0)` and `(NULL,1)` behavior | C similarity-score assertions, unbounded direct-C input geometry or a measured native-memory bound |

`[NP1]` requires a nonblank `m3.precompute.native.path` or
`m3.precompute.native.pathFile` via a JUnit assumption. Without those properties
it is skipped, even if a library could be found via the system library path;
skip is not pass. `[C1]` undefines `NDEBUG` before including `assert.h`, so its
assertions remain enabled in the configured Release build. [Tests][native-tests],
[C checks][c-tests].

The [standalone POM][pom] declares `com.m3:m3-precompute:0.1.0-SNAPSHOT`,
Java release 21 and JUnit Jupiter 5.10.2 as a test dependency. The compiler
plugin is 3.13.0 with `proc:none`, `-Xlint:all`, `-Werror`; Surefire is 3.2.5
with `useModulePath=false`. These are declarations, not evidence of the JDK
installed in a runner or of a successful build.

The module README already supplies the reproduction commands. In particular:

- Default `mvn -B -ntp -f m3/ports/precompute/pom.xml test` does not activate native compilation
- Adding `-Pnative` configures CMake Release with `BUILD_TESTING=ON`, builds, runs `ctest` and passes the generated `native-path-Release.txt` to Surefire
- [CMake][cmake] requires 3.20+, JNI discovery and C11; it enables warning-as-error flags (`/W4 /WX` on MSVC, otherwise `-Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic`)
- The POM's two Synexia OpenRewrite profiles are opt-in authoring/verification entries, not default runtime dependencies

None of those commands ran for this reference. Open gaps include tests of all
limits/refusals, counter and scoring boundaries, throwing monitors/cancellation,
custom provider misbehavior, mutable input inconsistency, loading retries and
ABI errors, allocation failure and peak/cold/warm resource cost. Broader
regex-shape/trial/oracle owners, the separate
[CPU-tool DAG/image/runtime family][cpu-tools], full package coverage and exact
target compiler/JNI/jtreg/runtime qualification remain outside this packet.

## Pinned source index

All source links below resolve within the public M3JDK21 tree at the inspection
pin. Existing base references are reused; the eight-owner count does not mean
eight complete packages. The documentation changes no source, test, native ABI,
build profile, receiving manifest, sealed foundation guide or policy authority.

[code-signals]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignals.java
[lexical-api]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatch.java
[lexical-java]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchJava.java
[lexical-native]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchNative.java
[pair]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityPrecompute.java
[similarity-api]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatch.java
[similarity-java]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatchJava.java
[similarity-native]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatchNative.java
[progress]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java
[text-signals]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java
[histogram]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java
[shape]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexShape.java
[jni]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/native/src/m3_precompute_signals_jni.c
[header]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/native/include/m3_precompute_signals.h
[c]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/native/src/m3_precompute_signals.c
[code-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/test/java/com/m3/precompute/M3CodeTextSignalsTest.java
[similarity-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/test/java/com/m3/precompute/M3SimilarityBatchTest.java
[native-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/test/java/com/m3/precompute/M3NativeParityTest.java
[c-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/native/tests/m3_precompute_signals_checks.c
[readme]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/README.md
[pom]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/pom.xml
[cmake]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/src/main/native/CMakeLists.txt
[cpu-tools]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/ports/precompute/docs/M3_CPU_TOOLS_PRECOMPUTE_20261014.md
[policy]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/docs/m3jdk21-porting-invariant.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/docs/name-mapping.json
