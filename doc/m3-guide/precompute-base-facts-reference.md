<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Precompute base text facts and exact-distance contracts

[Guide](README.md) · [Package overview](m3-packages.md#precompute-api-contracts) · [Source/member/test ledger](precompute-base-facts-source-coverage.md) · [23-owner routing](precompute-source-coverage.md)

This reference deepens exactly five existing owners in the isolated
`com.m3.precompute` port: `M3TextMetrics`, `M3TextSignals`, package-private
`M3Histogram`, `M3EditDistancePlan` with `Workspace`, and `M3Progress`.
It does not introduce a new precompute abstraction or move these objects into
ordinary `String` storage. The [batch](precompute-batch-reference.md),
[regex](regex-precompute-reference.md) and
[CPU-tool](cpu-tool-precompute-reference.md) guides retain their own owners.

The complete five bodies, two direct test bodies and selected callers were read.
Inspection date: **2026-10-15 UTC**. Public source pin:
[`db49925d43d4854707911868e603426db7055664`](https://github.com/hsoliwal/M3jdk21/tree/db49925d43d4854707911868e603426db7055664/m3/ports/precompute).
All five bodies and the packet's support sources are byte-identical to their
previous `1de629d26ea82d2729a5993bfc88099f83221923` evidence. Exact Git blob SHA-1,
raw SHA-256, complete member routes and seven test-method scopes are in the
[ledger](precompute-base-facts-source-coverage.md). Builds, tests, examples,
policy/recipe verifiers, native work, benchmarks and runtime qualification are
**NOT_RUN in this documentation authoring/review pass**. Hosted CI is separate
exact-commit evidence, not established by this static review. Source deductions below are not observed
regressions, executed proofs or speedup claims.

## Scope and units

The [module README][module-readme] describes an independently built,
Apache-2.0 target-owned port outside the bootstrap path. Its
[POM][pom] declares `com.m3:m3-precompute:0.1.0-SNAPSHOT`, Java release 21,
JUnit 5.10.2 and Surefire without the module path. The scoped complete source
tree contains no `module-info.java` or `package-info.java`; that is a physical
listing statement, not an effective-model or runtime-module proof.

| Need | Existing owner | Authority and unit |
| --- | --- | --- |
| Small composable geometry | `M3TextMetrics` | UTF-16-unit length, encoded UTF-8 byte length, code-point/unpaired counts and hashes; no retained spelling |
| Exact histograms and candidate facts | `M3TextSignals` | UTF-16-unit and contiguous q-gram counts; code-point classification; hashes/sketches only filter or rank |
| Histogram implementation | `M3Histogram` | Package-private sorted primitive key/count arrays, not another public API |
| Exact edit verification | `M3EditDistancePlan` | Unit-cost insertion/deletion/substitution over UTF-16 units; no transposition, code-point, grapheme or normalization semantics |
| Caller-defined observation/cancellation | `M3Progress` | Callbacks whose invocation and granularity belong to each operation |

A supplementary character occupies two edit/histogram units, but one classified
code point when its surrogate pair is intact. Byte length is neither of those
counts. ASCII folding occurs only in the presence filter; it is not a global
case-insensitive mode. No normalization or locale option exists in these owners.
The [porting invariant][policy] and [name map][mapping] retain existing payload
owners, per-file licensing and String → arrays → collections → AST/compiler →
remaining-family qualification order. This reference advances no receiving phase.

## Metrics: components, construction and composition

[`M3TextMetrics` lines 13–43][metrics-shape] is a public record with eight `int`
components. The generated component accessors expose exactly the stored values:

| Accessor | Meaning for facts produced from a stable sequence |
| --- | --- |
| `utf16Length()` | Number of `char` units |
| `utf8Length()` | UTF-8 encoded byte count, with the Java UTF-8 encoder's replacement-byte width for each unpaired surrogate |
| `codePointCount()` | Each valid surrogate pair counts once; each unpaired surrogate also counts once |
| `unpairedSurrogateCount()` | Number of unpaired high or low surrogate units |
| `javaHashCode()` | Ordinary String-style `h = 31*h + unit` with wrapping `int` arithmetic |
| `hash31Power()` | `31^utf16Length` with wrapping `int` arithmetic |
| `firstUtf16Unit()` / `lastUtf16Unit()` | Boundary unit values, or `-1` for empty text |

Generated record `equals`, `hashCode` and `toString` describe the eight stored
components. In particular the generated record `hashCode()` is not the
`javaHashCode()` component. Component equality does not establish equality of
original spellings. These generated methods and accessors are not handwritten
methods or extra physical source owners.

The compact constructor rejects negative length/count fields; an empty
`utf16Length` requires both boundaries `-1`, and a nonempty length requires both
boundaries in `0..65535`. It does **not** check relationships among lengths,
code-point/unpaired counts, boundary pairing or hash/power values. An empty direct
record can have positive other counters or arbitrary hashes; a nonempty record
can have zero code points or incompatible boundaries/counts. Direct construction
therefore establishes local shape, not authenticity. `isEmpty()` tests only
stored UTF-16 length; `isWellFormedUtf16()` tests only stored unpaired count.
[Constructor and constants][metrics-shape], [predicates/helper][metrics-tail].

`EMPTY` is `(0,0,0,0,0,1,-1,-1)`. The private
`UTF8_REPLACEMENT_BYTES` is initialized from
`StandardCharsets.UTF_8.newEncoder().replacement().length`; the algorithm does
not hard-code three bytes or store an encoded byte array. The private
`utf8Bytes(char)` returns 1 for units through `0x7f`, 2 through `0x7ff`, otherwise
3; `of` handles surrogates before this helper.

`of(CharSequence)` rejects null and returns the shared `EMPTY` for an observed
zero length. Otherwise it repeatedly reads the original `length()`/`charAt()`
view, including the boundaries at result creation. It does not call `toString`,
copy, lock or retry. Keep the input stable for the whole scan. A valid pair adds
four UTF-8 bytes and one code point; an unpaired surrogate adds the replacement
width and one unpaired count. UTF-8 accumulation uses `Math.addExact`, so an
encoding-length overflow throws `ArithmeticException` even if UTF-16 length fits
an `int`. Code-point/unpaired loop increments are ordinary `++`; with stable,
contract-conforming input they cannot exceed the `int` sequence length.
Caller-method exceptions and allocation/initialization failures propagate.
[Full scan][metrics-of].

`combine(left,right)` rejects either null **before** its empty checks. If the left
stored length is zero it returns the exact right object; otherwise an empty right
returns the exact left object. It does not revalidate or canonicalize the returned
operand. Nonempty composition adds the stored counts and keeps the outer
boundaries. A high-surrogate left tail followed by a low-surrogate right head
repairs the seam: subtract one code point and two unpaired units; add
`4 - 2*UTF8_REPLACEMENT_BYTES` to the byte sum. Length/count sums and seam
adjustments use checked arithmetic; an overflow can occur before the final
record is constructed. Inconsistent direct records can also yield a negative
adjusted count rejected by that constructor. Hash composition is
`left.javaHashCode * right.hash31Power + right.javaHashCode`, and powers multiply,
intentionally wrapping. Neither spelling is reread. [Composition][metrics-combine].

For example, separately computed facts for `"\uD83D"` and `"\uDE42"` compose to
two UTF-16 units, one code point, zero unpaired units and four UTF-8 bytes.
This is a source-derived example; the existing
[usage example](m3-packages.md#precompute-usage-and-test-linkage) remains NOT_RUN.

## Signals: stable facts and candidate boundaries

### Preparation, flags and stored state

`compile(CharSequence)` rejects null. A `String` is used directly; every other
input is observed through **one `toString()` call**, then all facts are derived
from that resulting String. It does not check equivalence with the original
`length`/`charAt` view or make a mutable object's `toString` atomic. A custom
implementation returning null fails when the returned value is used. No caller
input or whole spelling is retained in the signal object. [Compile][signals-compile].

Preparation allocates three `long[]` work arrays with lengths `n`, `max(0,n-1)`
and `max(0,n-2)`, records all UTF-16 units and overlapping contiguous bigrams and
trigrams, then sorts/collapses each through `M3Histogram.of`. Keys pack two units
into 32 bits and three into 48 bits; no Unicode normalization or hash compression
is used for histogram keys. A separate code-point loop counts `Character.isLetter`,
`isDigit`, `isWhitespace`, `isUpperCase`, and `isLowerCase`. Metrics perform another
scan. This preparation has no public length/work/cancellation budget and is not
an allocation-free or fixed-memory operation.

The private constructor stores metrics, three histograms, wrapping polynomial
hash/power, presence mask, packed first/last two units and five classification
counts, then eagerly computes `flags`, SimHash and 16 MinHash lanes. There is no
lazy cache, global interner or eviction service. Its private field references
are final; the public API does not expose the histogram arrays. See the separate
[internal mutability boundary](#histogram-internal-ownership-and-arithmetic).
[All fields and constructor][signals-shape].

| Public constant | Value | Predicate used in construction |
| --- | ---: | --- |
| `ASCII` | 1 | Maximum UTF-16-unit key below 128; empty uses maximum 0 |
| `LATIN1` | 2 | Maximum unit key below 256 |
| `BLANK` | 4 | Whitespace code-point count equals total code-point count |
| `ALL_LETTERS` | 8 | Letter code-point count equals total |
| `ALL_DIGITS` | 16 | Digit code-point count equals total |
| `HAS_LETTER` | 32 | At least one letter code point |
| `HAS_DIGIT` | 64 | At least one digit code point |
| `HAS_UPPER` | 128 | At least one uppercase code point |
| `HAS_LOWER` | 256 | At least one lowercase code point |
| `WELL_FORMED_UTF16` | 512 | Stored unpaired-surrogate count is zero |

For empty input, ASCII, LATIN1, BLANK, ALL_LETTERS, ALL_DIGITS and
WELL_FORMED_UTF16 are set; all HAS flags are clear. `BLANK` uses
`Character.isWhitespace`, not a byte-space test. `hasAll(mask)` is
`(flags & mask) == mask`; `hasAny(mask)` is `(flags & mask) != 0`. Thus
`hasAll(0)` is true and `hasAny(0)` false. Unknown bits are accepted without
validation: any requested unknown bit makes `hasAll` false, while `hasAny` may
still succeed on another known set bit. [Flags/accessors][signals-accessors].

### Composition and observation ownership

`combine(left,right)` first rejects null operands. Empty operands use the same
object-identity return rules as Metrics. For nonempty inputs it merges unigram,
bigram and trigram multisets, adds one cross-seam bigram and up to two cross-seam
trigrams, composes the first/last two units, and ORs the presence masks.
The 64-bit polynomial composes as `left.hash64*right.power64 + right.hash64`;
its base is 1,000,003 and hash/power arithmetic intentionally wraps.
[Composition][signals-combine], [packing/composition helpers][signals-packing].

Classification counts use `Math.addExact` to combine children. If a formerly
unpaired high/low boundary becomes one supplementary code point, its applicable
classifications are added with ordinary `++`; the separate metrics composition
repairs code-point/unpaired/byte counts and checks combined length. These are
facts derived from existing valid children, not a public arbitrary-histogram
constructor. The constructor recomputes flags and both sketches from the merged
histograms; composition does not combine already-finalized sketch bits alone.
It rereads no child spelling and retains no child signal object or input String.

Composition still allocates merged and trimmed primitive arrays, a temporary
boxed seam list/array, metrics and sketches; several histogram steps precede
final metrics-length validation. Checked-count or size failures can therefore
follow earlier allocation. No partial result is returned, and there is no
operation-level rollback/cancellation or whole-heap cap.

### Public queries and exactness

| Query | Exact meaning and boundary |
| --- | --- |
| `metrics()` / `flags()` | Return the stored immutable record reference / bit mask |
| `contentHash64()` / `presence64()` | Return the cached wrapping polynomial / 64-bit lossy presence mask |
| `characterCount(char)` | Exact multiplicity of one UTF-16 unit |
| `bigramCount(char,char)` / `trigramCount(char,char,char)` | Exact count of that overlapping contiguous unit sequence, including any surrogate units |
| `mayContain(needle)` | True iff receiver length is at least needle length and receiver presence bits contain every needle bit; no order or multiplicity check |
| `mayEqual(other)` | True iff UTF-16 lengths, Java-style hashes and 64-bit content hashes agree; no histogram or original-text comparison |
| `isAnagramOf(other)` | Exact equality of the UTF-16-unit multiset; does not preserve order, surrogate pairing or normalization |
| `editLowerBound(other)` | Safe nonnegative lower bound for unit-cost UTF-16 Levenshtein on the represented stable texts, not an exact distance |
| `simHash64()` / `simHashDistance(other)` | Stored 64-bit sketch / Hamming distance `bitCount(xor)` in `0..64` |
| `minHash()` | Fresh clone of all 16 stored lanes |
| `estimatedJaccard(other)` | Fraction of equal MinHash lanes, with explicit empty rules; approximate feature-set similarity |
| `histogramPayloadBytes()` | Sum of retained histogram key/count payloads only |
| `labels()` | Unmodifiable list of set flag names in the constant-table order; no fresh-identity guarantee |

All public methods accepting another signal object reject null, including the
empty cases.
Neither `M3TextSignals` nor `M3EditDistancePlan` declares value `equals` or
`hashCode`; ordinary object identity is distinct from `mayEqual` or content
comparison. [Accessors/counts/filters][signals-accessors],
[lower bound and sketches][signals-queries].

`addSignal` folds only ASCII `A..Z` to lowercase, hashes each unit twice with
`mix32`, and ORs two selected bits (`& 63`); the two may coincide. This is a
lossy set-like presence filter, not exact membership. Missing needle bits rule out
containment of the represented stable texts; a positive answer cannot prove it.
For example, source-derived `compile("A").mayContain(compile("a"))` is true despite
the case-sensitive spelling mismatch. Empty needles pass `mayContain`, and two
empty compilations pass `mayEqual`/`isAnagramOf`. Hashes are noncryptographic and
not durable content identities. [Presence/hash helpers][signals-mixing].

The lower bound takes the maximum of:

- absolute UTF-16-length difference, computed after widening to `long`
- `(unigram L1 + 1)/2`, the ceiling of L1/2
- `(bigram L1 + 3)/4`, the ceiling of L1/4
- `(trigram L1 + 5)/6`, the ceiling of L1/6

A single unit-cost edit changes at most `2q` q-gram multiset counts; taking the
maximum does not sum these overlapping bounds. `Math.toIntExact` converts the
maximum to `int`. A zero bound need not mean equal spellings. This bound is not
licensed for a code-point, transposition, weighted-edit or normalized-text metric.
[Exact implementation][signals-queries].

Both sketches select the highest available feature domain: trigrams when any
exist, otherwise bigrams, otherwise unigrams. A domain tag 3, 2 or 1 is mixed
into every feature so different q values are distinguished. SimHash uses each
feature's multiplicity as its signed vote; a bit is set only when the total vote
is strictly positive, so ties clear it. Empty SimHash is zero. MinHash uses each
distinct key once, ignoring counts; each lane begins at `-1L` (unsigned maximum)
and takes minima using `Long.compareUnsigned`, not signed ordering. Empty lanes
remain `-1L`. `estimatedJaccard` explicitly returns 1 for empty/empty and 0 for
exactly one empty; other values are multiples of 1/16 in `0..1`. This is neither
weighted Jaccard nor word/token similarity. Small texts can use different q
domains, and neither sketch gives a semantic-equivalence or error-bound guarantee.
[Sketch construction][signals-sketches].

`histogramPayloadBytes()` is `12 * distinct-key-count` summed over three
histograms. It excludes array/object headers, metrics, fields, the 16-lane sketch,
String materialization and temporary arrays. `minHash` clones per query; `labels` allocates temporary arrays/a builder and
returns an unmodifiable copy, with no distinct returned-list identity promise. These figures are accounting expressions, not measured
retained heap or full-operation resource limits.

## Histogram: internal ownership and arithmetic

[`M3Histogram`][histogram] is package-private and final. Its `keys: long[]` and
`counts: int[]` fields are package-visible final references. Final prevents
reference reassignment, **not element mutation**. The implementation's immutable
usage convention is sound for its internal construction paths but is not a broad
defensive-immutability guarantee against arbitrary same-package code. There is
no public API constructor or array getter; the private constructor stores its
arrays without cloning or validating them.

| Declared operation | Behavior and limits |
| --- | --- |
| `of(long[] owned)` | Sorts the supplied work array **in place**, counts distinct keys, allocates fresh retained key/count arrays, increments multiplicities and does not retain the supplied work array |
| `plus(other)` | Allocates merge capacity using `Math.addExact` on distinct-key lengths; sorted signed-long merge copies unequal keys and adds duplicate counts with `Math.addExact`; then copies both arrays down to actual size |
| `l1(other)` | Walks the sorted arrays and sums absent-key counts plus absolute differences for shared keys; shared differences widen to `long` before subtraction/absolute value |
| `count(long key)` | `Arrays.binarySearch` of sorted keys; zero if absent, stored count if present |
| `same(other)` | Exact `Arrays.equals` for both key and count arrays |
| `payloadBytes()` | `12L * keys.length`; counts key and count element storage, no headers or workspace |

Normal factory-created counts fit the owned array length. `plus` can throw for
capacity or duplicate-count overflow; actual array allocation may fail earlier
than Java's theoretical length limit. `l1` accumulates ordinary `long` additions
without a separate overflow check. These methods assume matching array shapes,
sorted distinct keys and nonnegative counts, and do not repair or revalidate
same-package corruption. Null arrays/operands fail through array or field use,
not a named public validation contract. There is no monitor, lock, serialization,
native resource or close operation here. Public signals do not return these arrays.

## Edit-distance plan and workspace

### Pattern preparation and public surface

`compile(pattern)` rejects null and an observed length above 1,048,576 UTF-16
units, allocates a `char[]` using another length observation and copies each unit.
A changing/custom input is not atomically snapshotted or synchronized; stabilize
it before calling. For accepted stable input, the retained private pattern array
is independent of later caller mutation. Empty and 64-or-more-unit patterns have
no Myers arrays. Lengths **1 through 63** additionally retain sorted distinct
units and a bit mask of pattern positions for each unit, plus a high bit/domain
mask. `mask(char)` binary-searches that table and returns zero for absent units.
[Compilation/state][edit-compile], [mask][edit-mask].

| Explicit public outer method | Contract |
| --- | --- |
| `patternLength()` | Stored pattern-unit count |
| `usesMyers()` | Whether the precomputed unit table is non-null; false for empty and lengths at least 64 |
| `primitivePayloadBytes()` | `2*pattern.length + 8*maskCount + 2*myersUnitCount`, with checked long sums; only retained array element payload |
| `workspace()` | New mutable workspace bound to this exact plan object |
| `distance(value)` | Calls the workspace-taking overload with a **new** workspace and no-op monitor |
| `distance(value,workspace,monitor)` | Exact UTF-16 Levenshtein after checks and algorithm selection below |
| `within(value,maxDistance)` | Calls the workspace-taking overload with a **new** workspace and no-op monitor |
| `within(value,workspace,maxDistance,monitor)` | Threshold decision after length/fact rejection and selected exact verifier |
| `length()` | Same stored unit count as `patternLength()` |
| `charAt(index)` | Stored unit after `Objects.checkIndex`; invalid index throws `IndexOutOfBoundsException` |
| `subSequence(start,end)` | Checks the half-open range with `Objects.checkFromToIndex`, then creates an ordinary String; invalid range throws `IndexOutOfBoundsException` |
| `toString()` | Creates an ordinary String containing the copied pattern |

Together with `compile`, those are **13 explicitly declared public outer
methods**. `Workspace.previousRowCopy()` is a separate nested public method.
[Inspection/query methods][edit-public], [CharSequence methods/workspace][edit-tail].

The implemented `CharSequence` defaults are separate inherited surface:
`isEmpty()` uses `length()==0`; `chars()` streams unsigned UTF-16 unit values;
`codePoints()` combines valid surrogate pairs and passes other units through.
These are stream operations, not cached histogram access. The pinned
[`CharSequence` body][charsequence] establishes those defaults; its static
`compare` is not inherited, and the plan does not implement `Comparable`.
Inherited Object identity/monitor methods are not plan declarations or a workspace
synchronization protocol. The plan does not override `equals`/`hashCode` to compare
pattern contents. `subSequence` and `toString` materialize, rather than returning
an alias of the private pattern array.

### Query order, algorithms and refusal

For the workspace-taking overloads, null value is checked first, then null
workspace and identity ownership. Equal pattern contents do not authorize
another plan's workspace. A null monitor selects `M3Progress.none()`.
`within` also rejects a negative threshold, before source-length checks.
Explicit null argument checks throw `NullPointerException`; foreign workspace,
negative threshold and length/work-budget refusals throw `IllegalArgumentException`.
Checked-arithmetic overflow throws `ArithmeticException`. Caller sequence, monitor
and allocation failures propagate without wrapping or a partial-result return.
The convenience overloads create a workspace before entering those argument
checks, so even an invalid query may have paid preparation/allocation cost.
[Query dispatch][edit-queries].

`distance` checks observed source length (`0..1,048,576`), then:

1. An empty pattern returns another `value.length()` observation without callbacks
2. A Myers plan runs the single-word verifier, with no DP-cell gate
3. Any other plan checks full-DP work and runs the two-row recurrence

`within` checks source length, rejects an absolute length gap exceeding the
threshold, and returns the empty-pattern decision before preparing source facts.
Otherwise it reads workspace-cached pattern signals, compiles fresh value signals
and rejects when their edit lower bound exceeds the threshold. Positive filters
continue to verification. Myers is selected only when it is available **and**
`maxDistance >= min(patternLength,value.length())`; otherwise even a short plan
uses the banded DP path after its work check. A very large threshold does not
universally bypass verification or the banded budget. [Query dispatch][edit-queries].

Full DP uses the ordinary insertion/deletion/substitution recurrence over two
rows of width `patternLength+1`. Banded DP clamps its band to
`min(maxDistance,max(sourceLength,patternLength))`, initializes outside-band cells
to `band+1`, and rejects when a row's recurrence minimum exceeds the threshold.
It still fills the **entire current row** with the sentinel each iteration and
can reread `value.charAt(row-1)` for every recurrence cell. Full DP reads one
source unit per row; Myers reads one per iteration. All algorithms repeatedly
observe `value.length()` and do not snapshot the source for the whole query.
[Myers][edit-myers], [full DP][edit-dp], [banded DP][edit-band].

That observation distinction matters: `within` prepares its filter from
`value.toString()` for a non-String, then verifies the original live `charAt`
view. A custom `toString` inconsistent with that view, or mutation between those
stages, can make the filter describe a different text. Initial length validation
also is not a lock against later growth. These are source-derived reasons to
require stable, contract-conforming input, not an executed counterexample or a
claim that the API repairs arbitrary concurrent mutation.

### Budgets, preparation and lifetime

The private guards use checked `long` arithmetic and reject only when the
calculated value is **greater than** 64,000,000:

- Full DP: `sourceLength * max(1,patternLength)`
- Banded DP: `sourceLength * max(1,min(patternLength,2*maxDistance+1))`

Both source and pattern have their separate 1,048,576-unit ceilings. The compile
check only tests above the pattern ceiling; a contract-violating negative custom
length can instead fail array allocation. Source validation explicitly tests
negative values too. Exception messages distinguish pattern, source, full-DP and
banded-work refusal. `within` prepares source histograms before its banded guard;
length/fact rejection may avoid it. No algorithm deadline is enforced.
[Budget helpers][edit-budgets].

These guards bound the selected recurrence-cell estimate, not all work or memory.
Pattern copying/sorting, workspace construction, temporary String materialization,
histogram/sketch preparation, whole-row fills, binary searches, callbacks and
final copies are outside that expression. There is no automatic streaming/spill,
cache eviction, native provider or accelerator selection in these five owners.
No CPU/heap/native-memory measurement was made.

Each workspace stores its owner, two new `int[patternLength+1]` arrays and
`M3TextSignals.compile(owner)`. This compiles the plan via its `toString`, including
String materialization and histogram/sketch work **for every new workspace**,
even when subsequent calls use Myers or only `distance`. Pattern signals are
cached in the workspace, not shared once per plan. `within` recompiles value
signals per call. No source-result cache or invalidation protocol exists.
The plan payload estimate excludes both rows, pattern/value signals, object
headers and temporary/caller storage. All state is ordinary heap state; these
owners declare no close/unmap/free protocol. [Workspace construction][edit-tail].

### Workspace reuse and diagnostic rows

`Workspace` is public, static and final, but its constructor is private. Its
private `require(expected)` checks `owner != expected` and throws
`IllegalArgumentException` for a foreign plan. Use one workspace sequentially,
and separate workspaces for concurrent or reentrant queries on a safely published
immutable plan. There is no locking, busy flag or reentrancy detection. A monitor
callback that reenters a query with the same workspace can interfere with rows;
separate-workspace use also requires stable inputs and appropriately managed
monitor state. This is source-based usage guidance, not a stress-test guarantee.

`previousRowCopy()` clones the stored `previous` array, not a complete DP matrix
or a promised latest result for every path:

- A new workspace starts with zero-filled rows
- Empty-pattern, initial rejection and Myers paths need not refresh rows
- Full DP initializes the previous row before `begin`; on its normal result path
  it copies the final row back to the stored previous array when necessary,
  then performs the final cancellation check
- Banded DP initializes/fills rows, swaps local references and may return early;
  it does **not** normalize the final local row into stored `previous`, so the
  copy may represent a preceding/partially updated row, not the returned decision
- Exceptions/cancellation may leave row mutations and earlier callbacks visible;
  there is no rollback, diagnostic freshness marker or partial-result object

[Full DP][edit-dp], [banded DP][edit-band], [diagnostic clone][edit-tail].

## Progress: defaults and actual caller cadence

[`M3Progress`][progress] is an interface. Its implicitly public/static/final
`NONE` field holds one anonymous implementation with no additional declarations;
`none()` returns that same object. All five public defaults are no-ops:
`begin(String,long)`, `subTask(String)`, `worked(long)`, `checkCanceled()` and
`done()`. The defaults neither validate task/null/work values nor keep counters.
There is no token, cancellation status, deadline, implicit interruption check,
automatic accounting, callback serialization or exception type prescribed for
cancellation. A custom monitor owns its state and may throw unchecked exceptions.

| Inspected operation | `checkCanceled` cadence after entering algorithm | Work and completion |
| --- | --- | --- |
| Myers edit distance | Before indices 0, 1024, 2048, … and after the loop | `begin` total is observed source length; `worked(1)` per unit; `done` in finally |
| Full dynamic edit distance | Before rows 1024, 2048, … and after row normalization | `worked(1)` per source row; no row-zero precheck |
| Banded edit decision | Before rows 1024, 2048, … and on normal loop completion | `worked(1)` per row; row-minimum rejection precedes the final check |
| Java similarity batch | Before rows 0, 1024, 2048, … and after the loop | Validates and allocates output before `begin`; `worked(1)` per score |
| Regex/String fact batch | Before indices 0, 256, 512, … and after the loop | Copies/validates pair list before `begin`; `worked(1)` per fact |

The edit algorithms' `begin` calls are outside the `try/finally`, as are the
selected similarity/regex clients' begin calls. If `begin` throws, `done` is not
called. Once the try body is entered, `done` executes on success, body failure
or early return, but a throwing `done` may replace an earlier exception or return.
There is no universal balanced pair: query validation/budget refusal, empty-pattern
and length/lower-bound rejection can precede all callbacks. Empty Myers/full-DP
source loops can still reach begin/final-check/done when their dispatch allows
it. None of these selected paths calls `subTask`.

Cancellation can be delayed through preparation, validation, allocation, full
row initialization and an individual long row. The 1,024-row rule is not a
1,024-cell rule. `done` is an observation callback, not an automatic release of
workspace or application resources. The [batch guide](precompute-batch-reference.md#progress-exceptions-and-cancellation)
keeps Java/native provider distinctions; the
[regex guide](regex-precompute-reference.md#ordered-snapshots-parity-and-hashing)
keeps its own cadence. Do not impose a single cadence across this module.
[Edit bodies][edit-myers], [similarity client][similarity-batch],
[regex client][regex-signals].

## Callers, read tests and open qualification

The selected direct chain is Metrics/Histogram → TextSignals → EditDistancePlan
or code-text/pairwise facts. `M3CodeTextSignals.finish` compiles String-based base
signals after lexical preparation; batch finishing occurs after the provider
returns. This does not make the whole facade a single immutable snapshot of a
mutable original. `M3SimilarityPrecompute.compare` derives the length delta,
lower bound, sketch distance, estimated Jaccard, candidate equality and anagram
fields; it does not run exact edit verification. `M3SimilarityBatchJava` ranks
supplied lengths/SimHashes and is a progress client, not an edit-distance caller.
`M3RegexStringSignals` is another progress client. The inspected CPU runtime has
no direct lexical reference to these five types and is not counted as a proven
direct client. This is a bounded sample, not a whole-repository call graph.
[Code-text client][code-signals], [pairwise client][similarity-pair],
[batch guide](precompute-batch-reference.md), [CPU guide](cpu-tool-precompute-reference.md).

The [test ledger](precompute-base-facts-source-coverage.md#read-test-methods-and-limits)
records all four TextSignals tests and three EditDistancePlan tests, including
finite seam matrices, seeded composition/lower-bound/independent-DP comparisons,
and selected ownership/budget refusals. Their bodies were read, **not run**.
The scoped source tree has no separately named Metrics, Histogram or Progress
test file; indirect assertions do not establish exhaustive coverage.

Open qualification includes direct-record inconsistencies, hostile/mutable custom
sequences, every flag/count/query edge, arbitrary collision behavior, exact
threshold/band/budget boundaries, allocation failure, internal-array corruption,
throwing callbacks, cancellation order/latency, row freshness, concurrent/reentrant
workspace misuse, runtime/build/provider/platform parity and measured cold/warm
full-operation cost. No finite test-source loop, matching hash, callback count,
source-route closure or candidate signal closes those gates. The broader package,
module, project and repository retain their existing incomplete qualifications.


## Pinned source routes

[module-readme]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/README.md#L1-L71
[pom]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/pom.xml#L1-L44
[policy]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/m3jdk21-porting-invariant.md#L1-L499
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/name-mapping.json
[metrics-shape]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L13-L43
[metrics-of]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L45-L91
[metrics-combine]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L93-L119
[metrics-tail]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L121-L134
[signals-shape]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L15-L91
[signals-compile]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L93-L146
[signals-combine]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L149-L205
[signals-accessors]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L207-L258
[signals-queries]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L260-L318
[signals-sketches]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L320-L355
[signals-packing]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L357-L391
[signals-mixing]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L393-L416
[histogram]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L1-L83
[edit-compile]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L14-L68
[edit-mask]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L162-L165
[edit-public]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L70-L130
[edit-queries]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L89-L130
[edit-myers]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L132-L160
[edit-dp]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L167-L202
[edit-band]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L204-L245
[edit-budgets]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L247-L268
[edit-tail]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L270-L314
[charsequence]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/src/java.base/share/classes/java/lang/CharSequence.java#L59-L305
[progress]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L1-L17
[code-signals]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignals.java#L107-L179
[similarity-pair]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityPrecompute.java#L25-L41
[similarity-batch]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatchJava.java#L22-L91
[regex-signals]: https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringSignals.java#L153-L180
