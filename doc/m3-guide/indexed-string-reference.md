<!-- SPDX-License-Identifier: Apache-2.0 -->

# Indexed-string bridge and reusable precompute contracts

[Guide](README.md) · [Package contracts](m3-packages.md) · [Runtime owners](runtime-owners.md)

This reference covers the seven production Java owners in
`m3/ports/indexstring/src/main/java`, their two executable test sources, and the
explicit call boundaries into `M3TextMetrics`, `M3TextSignals` and
`M3EditDistancePlan`. It explains the inspected implementation, including its
limitations. It is not whole-family receiving acceptance, a new public JDK API,
or a claim that the tests or benchmarks passed in this documentation pass.

The bridge source pin is `220eb11788d4039da904491e7d135b2304045659`.
The reused precompute evidence is pinned at
`278b221e8365ebeb6095a3a5643b38a0564258b5`; its file identities are recorded below.
All linked tests here were **read, NOT_RUN**. No project code, build, policy
verifier, native library or benchmark was executed for this page.

## 1. The ownership boundary

The [porting invariant][policy] places canonical runtime String storage in the
existing `java.lang.M3String`/owner/atom/tuple/pool family; public
`java.lang.String` remains authoritative. That intended target architecture is
distinct from the separately built [compatibility port][port-readme] described
here. The [name mapping][mapping] and full-family receiver remain qualification
records; this page does not promote their receiving states.

The concrete bridge is:

1. `FrozenChars` or `FrozenBytes` owns or retains an immutable payload
2. `MIndexJoinedStorageIntern` weakly interns an ordered **segment descriptor
   body**, containing segment references and cumulative ends
3. `MIndexJoinedChars` or `MIndexJoinedBytes` holds that body plus a range
4. `M3Text` exposes a selected stock-JVM text API over the character view
5. Explicit precompute calls derive facts or copy a pattern; they are not
   automatically attached to this body by any of the reviewed bridge owners

The bridge adds no default text-admission policy, persistent dictionary, native
arena or `String.intern()` implementation. Its existing descriptor interner must
not be described as canonicalizing every equal spelling across layouts, files,
processes or class loaders. Do not place these retained compatibility classes and
another copy with the same qualified names on one loader/module path.

The [port POM][port-pom] identifies `com.m3.compat:indexstring-retained-view` and
Java release 21. It wires `ExistingSurfaceTest` and `RetainedViewTest` into the
Maven test phase as executable main classes. The [precompute POM][precompute-pom]
is a separate `com.m3:m3-precompute` artifact. Neither POM declares a dependency
on the other. The cross-API example below requires both artifacts on the caller's
classpath; it is not an existing combined test or bootstrap dependency.

## 2. Admission, equality and descriptor identity

Source: [M3Text][text], [joined characters][joined-chars] and
[descriptor interner][intern].

| Entry point | Concrete behavior | Failure / ownership boundary |
| --- | --- | --- |
| `M3Text.fromJoined(storage)` | New facade retaining the supplied immutable joined view | Null storage: `NullPointerException`; no content admission or payload copy |
| `M3Text.fromFrozen(atoms...)` | Calls `MIndexJoinedChars.of`, then wraps its result | Null directory or element: `NullPointerException`; atoms must already be immutable |
| `M3Text.fromString(text, admission)` | For nonnull inputs, calls the supplied function once, wraps the returned view and compares every UTF-16 unit with the input | Null text/function/result: `NullPointerException`; differing content: `IllegalArgumentException`; callback exceptions propagate |
| `M3Text.storage()` | Returns the original joined view | This is an immutable-view reference, not a mutable buffer, portable identifier or ownership transfer |
| `M3Text.equals(Object)` | Content equality with another `M3Text`, independent of segmentation | False for a `String`, including equal spelling; `String.equals(M3Text)` is also false |
| `M3Text.contentEquals(CharSequence)` | Explicit content comparison across the type boundary | Null: `NullPointerException`; externally mutable/custom inputs must remain coherent throughout comparison |
| `M3Text.compareTo(M3Text)` | Lexicographic unsigned UTF-16-unit order, then length difference, as in `String` | Null: `NullPointerException`; no locale collation, normalization or case folding |
| `canonicalBackingId()` on either joined type | Identifier of the full descriptor body retained by the view | Not an identifier of the slice or its text; character and byte bodies use separate counters |
| `sharesBackingWith(other)` on either joined type | Tests body object identity | Null: `NullPointerException`; two different ranges of one body return true |

Admission validation happens **after** the callback. A failing content check does
not undo that callback's external effects, prove it interned anything, or check a
namespace/generation policy. Supply a policy whose effects and ownership are
already appropriate for the caller. The bridge does not silently publish a file.

The frozen and joined storage classes themselves do not override `equals` or
`hashCode` for content equality. Their `hash32()` methods, where present, are
separate explicit accessors. `M3Text` is the content-equality facade.

### What the existing interner actually reuses

The interner omits zero-length segments. For every remaining position it keys
segment length, precomputed segment hash and direct-versus-heap kind, then
verifies the segment's exact bytes or UTF-16 units on a candidate hit. Identical
atom references are an immediate equality shortcut. Hash collisions alone cannot
authorize a match in this implementation.

Consequences:

- Separately copied atoms with equal content, the same nonempty segment layout
  and the same backing kinds can reuse one live body
- A one-segment `"ab"` body and a two-segment `"a"`, `"b"` body do not have to
  share identity, although their `M3Text` facades compare equal
- A direct/mapped segment cannot be replaced by an equal heap segment through
  this interner. Equal direct segments can still reuse a previously admitted
  direct descriptor; this API is not a file-origin identity service
- All slices of one body retain its ID, even when they spell different text
- `of(...)` can return the same canonical full-view object while the body is
  live. Each `M3Text.fromJoined(...)` call still creates a facade

The body copies segment references into its own directory on creation; it does
not retain the caller's varargs array. However, fingerprinting, matching and body
creation traverse that caller array without taking an ingress snapshot. Keep the
directory stable during the call. Later mutation of the caller's directory does
not rewrite an already published body.

Character and byte tables have weak body references and reference queues. Queue
cleanup occurs on subsequent admissions in the corresponding table. Live views
strongly retain their body and its segments. Collection and cleanup are not
deterministic, and the implementation has no entry-count or retained-byte budget.
This is neither a bounded cache nor a promise of immediate memory release.

After an unreferenced body is collected, later equal admission can create a new
ID. IDs are monotonically allocated from separate atomic counters until a
nonpositive result is refused with `IllegalStateException`; they are not persisted
or coordinated across loaded interner instances. Do not use an ID alone as a
durable content address or a key for range-specific facts.

## 3. Frozen payloads, ranges and output

Sources: [FrozenChars][frozen-chars], [FrozenBytes][frozen-bytes],
[MIndexJoinedChars][joined-chars], [MIndexJoinedBytes][joined-bytes].

All character indexes and lengths below are UTF-16 **code units**. Byte indexes
are bytes. Both use start-inclusive/end-exclusive ranges. A character slice can
split a surrogate pair, and unpaired surrogates are preserved. No grapheme or
normalization boundary is inferred.

| API family | Retained representation / output | Important boundary |
| --- | --- | --- |
| `FrozenChars.copyOf(char[])`, `FrozenBytes.copyOf(byte[])` | Clone input payload, then compute its hash | Null refused; caller must stabilize the input during copying; allocation failures propagate |
| Frozen `length`, `charAt` / `byteAt`, `isDirect`, `hash32` | Read immutable fields/backing; indexed reads check bounds | `FrozenBytes` has no implicit text encoding |
| `FrozenChars.subSequence`, `FrozenBytes.slice` | New view sharing the entire old backing; full-range request returns `this` | Bounds checked; partial-view hash is recomputed by scanning the selected units/bytes, so this operation is not generally O(1) |
| Frozen `copy`, `copyTo` | New mutable array, or write into caller's array | `copyTo` checks nonnull target and source/target ranges before copying; no returned mutable alias to the stored payload |
| Frozen `asReadOnlyBuffer` | Fresh independent cursor over retained payload | No payload copy, but a buffer object is allocated; cursor position/limit remains mutable |
| `FrozenBytes.retainedBytes` | Entire retained backing byte count supplied to the atom | Not just this slice's length; excludes headers/VM state; shared backing must not be counted repeatedly |
| `FrozenBytes.compact` | Returns `this` if logical length equals `retainedBytes`; otherwise copies to heap | A full-sized mapped/direct atom can remain direct. Use `copyOf(atom.copy())` if an unconditional independent heap copy is required |
| Joined `of` | Reuses or allocates a flat body of frozen segment references and cumulative ends | Null inputs refused; summed lengths use `Math.addExact` and can throw `ArithmeticException`; directory allocation can fail |
| Joined `subSequence` / `slice` | New range over the same body; full-range request returns `this` | O(1) range metadata; even an empty/small slice can retain the full body's segments and payloads |
| Joined `charAt` / `byteAt` | Binary search in cumulative ends, then access the selected atom | Bounds checked; O(log S) directory lookup for S body segments |
| Joined `copy` / character `copyTo` | Traverse participating segments into independent mutable output | Character `copyTo` checks all bounds first; copies payload, not only descriptors |
| Joined `detach` | Copies exactly the visible range into a fresh heap frozen atom | Explicit retained-backing release boundary for the resulting value; old views remain valid |
| Joined `asReadOnlyBuffers` | New array and per-part read-only cursors for just the visible range | Empty range returns an empty array; no payload copy or slice rehashing |

`FrozenChars` has no public retained-byte counter or `compact` method. Do not infer
whole-heap size from its length. `M3Text.toCharArray` delegates to joined `copy`;
`getChars(start,end,target,targetStart)` delegates to validated `copyTo`.
`M3Text.asString`/`toString`, `FrozenChars.toString`, and
`MIndexJoinedChars.toString` deliberately materialize ordinary Strings. There is
no retained flattened-String cache in these owners.

### Concatenation has a descriptor cost

`MIndexJoinedChars.concat` first checks that the combined UTF-16 length fits an
`int`; excess is refused with `OutOfMemoryError`. It returns the nonempty operand
when the other is empty. Adjacent ranges of the same body can reuse that body and
can recover its canonical full view. Other joins assemble the visible segment
directory and pass it through the existing interner.

No joined spelling array is created by that general path, but partial frozen
atoms are rehashed and interner equality can scan content. Descriptor work is
linear in participating segments; repeated one-segment appends can cause
quadratic total descriptor work. A flat directory avoids recursive rope depth,
not all preparation cost. `M3Text.concat` delegates here and wraps a nontrivial
result; it does not promise facade object identity. `MIndexJoinedBytes` exposes no
corresponding public `concat` method at this pin.

## 4. Traversal, exact search and regex behavior

Source: [M3Text][text]. Let N be visible input units, M pattern units, and S the
number of retained body segments. These are algorithmic observations from the
source, not latency or throughput measurements.

- `length`/`isEmpty` are stored-length reads. `charAt`, `codePointAt` and
  `codePointBefore` use indexed view access. Code-point operations combine valid
  surrogate pairs, including pairs across segment seams; indexes remain UTF-16
- `chars` and `codePoints` create independent sequential streams backed by a
  linear segment cursor. They allocate descriptor/cursor/stream metadata rather
  than copying spelling. `codePointCount(start,end)` uses the same seam-aware
  cursor over a validated range
- `compareTo`, content equality, first `hashCode`, and `writeTo` use sequential
  cursors. `writeTo` appends units directly to the supplied `Appendable`; null is
  refused and `IOException` propagates. A failing append can leave partial output
- The first `M3Text.hashCode` scans units with the Java String recurrence; a
  volatile ready flag publishes the cached result, including hash zero. Later
  calls read it without rescanning. Concurrent first callers may both compute it;
  no single-computation guarantee is made

### Exact literal search and its budget

`indexOf(String[,fromIndex])`, `lastIndexOf(String[,fromIndex])`, `contains`,
`startsWith`, and `endsWith` reject null search strings. Results are UTF-16 offsets.
For `indexOf`, a negative start becomes zero; the empty pattern returns the
smaller of the clamped start and input length. For `lastIndexOf`, a negative start
returns -1 even for the empty pattern; otherwise an empty pattern returns the
smaller of the start and input length. Nonempty impossible ranges return -1.
Prefix/suffix tests compare the selected range exactly.

Patterns of at most **65,536 units** use a newly allocated KMP prefix array on
each search. Its primitive payload is at most **262,144 bytes**, excluding array
headers, the range view, cursors and segment descriptor array. Matching traverses
the selected input sequentially; prefix construction and matching take O(M + N)
work after descriptor setup. The prefix array is not a retained compiled index.
`lastIndexOf` scans the eligible prefix forward and remembers the last match.

Larger patterns use the no-prefix fallback, comparing each possible start through
`charAt`. Worst-case work is O(N × M × log S), with O(1) segment access when there
is only one segment. This fallback limits prefix allocation, not total work. No
search cancellation, wall-time budget, native dispatch or automatic promotion to
a prepared search plan exists in this method.

### Regex remains the caller's Java regex engine

`matcher(Pattern)` passes the facade directly to `Pattern.matcher`.
`matches(regex)`, `replaceAll(regex,replacement)` and `split(regex,limit)` delegate
to `java.util.regex`; the latter two return ordinary String outputs. Pattern and
replacement syntax, capture behavior and their exceptions remain those of that
engine. The facade adds no trigram filter, approximate acceptance, regex cache or
regex work limit. Reuse a supplied `Pattern` when its caller-controlled lifetime
is appropriate; `matches`/`replaceAll`/`split` compile their string pattern anew.

## 5. Bytes, encoding, I/O and native lifetime

Sources: [M3Text][text], [frozen characters][frozen-chars],
[joined characters][joined-chars] and [joined stream implementations][streams].

`MIndexJoinedChars.encode(Charset)` runs one whole-input encoder with replacement
for malformed and unmappable input, then copies output into a new `FrozenBytes`.
It does not encode each segment independently: encoder state and surrogate pairs
must survive seams. A `CharacterCodingException` despite the replacement policy
is wrapped in `IllegalStateException` by this implementation.

`M3Text.encode(charset,malformed,unmappable)` instead takes explicit nonnull error
policies and returns an independent read-only `ByteBuffer`. `REPORT` may throw
`CharacterCodingException`; `IGNORE` or `REPLACE` changes the exported bytes.
Output length is charset/policy-dependent and is not the UTF-16 length. Neither
entry point caches encoded output or guarantees allocation-free encoding.

### Direct descriptors are a borrowing boundary, not an unmap API

Public `copyOf` factories create heap atoms. The package-private mapped admission
methods trust already validated immutable backing:

- `FrozenBytes.mapped` checks direct/read-only status and a retained size at least
  as large as the slice; it accepts a supplied verified hash
- `FrozenChars.mapped` requires a direct atom of even byte length and accepts a
  supplied verified UTF-16 hash. Payload order here is **UTF-16BE**, distinct from
  the UTF-16LE local foundation described in the [text/storage contracts](m3-packages.md#textstorage-api-contracts)
- Neither method proves that a direct buffer is file-backed, seals a file against
  external mutation, authenticates the caller's hash, or validates a whole image

`FrozenChars.directUtf16Bytes` returns a fresh read-only descriptor in **bytes**,
position zero, without copying payload. A heap-backed atom throws
`IllegalStateException`. `MIndexJoinedChars.directUtf16Buffers` applies that rule
to each participating segment: an empty range returns an empty array; a mixed
heap/direct range throws rather than silently flattening. Already allocated
descriptor metadata is not an exposed partial result.

These APIs expose Java buffers, not a native pointer owner, pin lease or
`AutoCloseable` mapped image. A read-only view does not prevent another owner from
mutating/truncating its backing file or writable alias. Such changes violate the
immutable-admission premise and can invalidate cached hashes/facts. There is no
repair/invalidation mechanism here. The external image owner must establish and
preserve immutability; native consumers need their own proven JNI reachability,
acquire/release and thread-lifetime contract. Directness alone is not that proof.

The tests include a mapped view outliving its file channel. This is not a promise
of deterministic unmapping, safe concurrent truncation, or cross-platform file
deletion behavior. No close/free operation on a bridge value releases a mapping.

### Reader and InputStream cursor contracts

`asReader`/`asInputStream` create independent mutable cursors over read-only
segment buffers. Reads can cross seams without creating a flat payload. Individual
overridden stateful operations are synchronized on that stream/reader; sharing
one cursor still consumes one position, rather than giving each thread an
independent traversal. Use separate cursors for independent readers.

| Operation | Byte InputStream | Character Reader |
| --- | --- | --- |
| Scalar read | Returns 0–255, or -1 at EOF | Returns one UTF-16 unit, or -1 at EOF |
| Bulk read | Validates target/range, then open state; zero count returns 0 only when open | Same validation order and zero-count behavior |
| `skip` | Negative amount is treated as zero after open-state check | Negative amount throws `IllegalArgumentException` before open-state check |
| Availability | `available()` is remaining byte count | `ready()` returns true while open, including EOF; it does not mean characters remain |
| Mark/reset | Mark supported; read limit ignored; mark after close is a no-op | Mark supported; negative read-ahead limit refused; mark requires open state |
| Reset before explicit mark | Restores the initial position | Restores the initial position |
| Close | Idempotently drops this cursor's buffer-array attachments | Same; does not close other cursors or invalidate the source view |

Subsequent reads/reset/skip/availability checks on a closed cursor throw
`IOException`, subject to the argument-validation precedence above. Mark/reset
uses position metadata rather than a replay payload; reset traverses the segment
directory and is O(S), not constant time. Closing is not a native unmap request.

## 6. Precompute reuse without a hidden second spelling store

Sources: [M3TextMetrics][metrics], [M3TextSignals][signals],
[M3Histogram][histogram] and [M3EditDistancePlan][distance]. Full argument, monitor
and DP-budget contracts remain in the [precompute API guide](m3-packages.md#precompute-api-contracts).

The following are explicit caller compositions, not calls made by `M3Text` itself:

| API → representation | Precompute/index retained | Exact path / fallback and cost boundary | Test evidence |
| --- | --- | --- | --- |
| `M3TextMetrics.of(view)` reads `length`/`charAt` | Fixed-size lengths/counts, boundary units, Java hash and 31-power; no text reference | No flattening in `of`; for joined `M3Text`, per-unit binary search adds directory lookup cost, giving O(N log S) scanning rather than cursor-linear access | Metrics and seam composition in `M3TextSignalsTest`; combined bridge call is not tested there |
| `M3TextMetrics.combine(left,right)` consumes stored facts | Fixed-size composed metrics; repairs a high/low-surrogate seam | Constant-size arithmetic, with checked counters and deliberately wrapping Java hash arithmetic; no text reread | Flat-versus-composed metrics tests |
| `M3TextSignals.compile(view)` calls `toString` for non-String input | Exact UTF-16-unit histograms, boundary facts, presence filter, hashes and sketches | `M3Text.toString` materializes a String first; three temporary q-gram arrays are sorted. This is a cold allocation/preparation boundary | Histogram, sketch and lower-bound tests on ordinary String inputs |
| `M3TextSignals.combine(left,right)` merges derived histograms | Adds cross-seam q-grams and code-point classification adjustments | Avoids rereading spelling but allocates merged arrays and recomputes sketches; cost depends on distinct features, not just two input references | Composition tests include surrogate seams |
| `M3EditDistancePlan.compile(patternView)` reads `charAt` into a char array | Immutable pattern snapshot and masks for lengths 1–63 | Pattern copy is deliberate; joined-view lookup adds directory cost. Above 63 units, exact DP remains the plan's path | Plan tests cover Myers/DP and snapshot-shaped String inputs; no bridge-specific integration case |
| `plan.workspace()` | Two mutable int rows plus pattern signals, bound to this exact plan object | Allocates even for Myers; signal compilation materializes the plan's String projection. Sequential reuse avoids rebuilding this workspace | Wrong-plan workspace refusal is tested |
| `plan.distance(view,work,monitor)` | Reuses plan/masks/rows | Exact UTF-16 Levenshtein; no whole-source `toString` call in this path. `charAt` cost still depends on source representation | Independent finite DP comparisons |
| `plan.within(view,work,k,monitor)` | Reuses pattern facts; compiles fresh source signals after early length/empty checks | Negative filters can reject; otherwise exact Myers or banded DP verifies. A surviving `M3Text` input is materialized for signal compilation, and banded DP repeatedly calls its `charAt` inside the recurrence | Finite threshold comparisons and budget/ownership refusal tests |

The DP cell budget does not include signal sorting, buffer creation, source lookup
cost, full-row filling or monitor effects. Reusing a workspace does not imply an
allocation-free `within` call. Neither a nonnegative candidate signal nor matching
hashes establishes an exact match.

### Keep the hash domains separate

- Frozen character hashes and `M3Text.hashCode` follow the 32-bit Java String
  recurrence over UTF-16 units; the facade computes its own first hash rather
  than retrieving a precomposed body hash
- `M3TextMetrics` composes that hash using the stored power of 31:
  left hash × right power + right hash, with ordinary Java integer overflow
- Frozen byte `hash32` is FNV-1a over unsigned bytes, including arbitrary binary
  bytes; it is not the Java String hash or evidence of UTF-8 encoding
- `M3TextSignals.contentHash64` is another composable polynomial over UTF-16
  units. SimHash/MinHash are similarity/ranking signals, and the presence filter
  permits false positives
- Descriptor fingerprints are interner bucket selectors. Exact segment
  comparison follows a hit. These hashes are not cryptographic digests,
  signatures or authenticated provenance

### Binding, invalidation and budgets

These immutable result objects expose no versioned persisted cache, mutation
notification, eviction budget or native resource lifecycle. Keep facts associated
with the exact immutable text/range and semantic options that produced them. The
reviewed APIs do not implement that external association for the caller. A joined
body ID alone cannot distinguish slices, and a content hash alone cannot prove
identity. An external persistent cache would also need explicit format/algorithm
versioning and admission rules; those are not supplied by these classes.

An immutable heap snapshot needs no content invalidation during its own lifetime.
Changing a caller's original array after `copyOf` does not change that snapshot.
A different range, different newly admitted content, or changed encoding/error
policy requires the corresponding facts to be recomputed or explicitly combined
under the documented operation. Loss of a weak descriptor-cache entry changes
reuse/identity availability, not the text semantics of surviving views.

Metrics compose with fixed-size work. Histograms can retain O(N) distinct q-grams,
and compilation can allocate O(N) work arrays in addition to String projections;
there is no general signal-compilation admission limit. The 65,536-unit literal
prefix cap, the edit plan's 1,048,576-unit pattern/source caps, and its 64,000,000
DP-cell guard are separate limits. None is a whole-process memory bound or a
guarantee that an arbitrarily large corpus fits in memory. Measure cold
preparation, warm reuse, retained heap and native/process memory separately before
claiming a performance gain.

### Source-checked usage example, not compiled or run

```java
import com.m3.text.compat.M3Text;
import com.synexia.indexstring.FrozenChars;
import com.m3.precompute.M3TextMetrics;
import com.m3.precompute.M3TextSignals;
import com.m3.precompute.M3EditDistancePlan;

M3Text left = M3Text.fromFrozen(FrozenChars.copyOf(new char[] {'A', '\uD83D'}));
M3Text right = M3Text.fromFrozen(FrozenChars.copyOf(new char[] {'\uDE42', '!'}));
M3Text whole = left.concat(right);
M3Text face = whole.substring(1, 3); // two UTF-16 units, one code point

M3TextMetrics facts = M3TextMetrics.combine(
        M3TextMetrics.of(left), M3TextMetrics.of(right));
// Expected: four UTF-16 units, three code points, six UTF-8 bytes
// The split surrogate pair is repaired by the metric composition rule

M3TextSignals sourceSignals = M3TextSignals.compile(whole); // materializes here
M3TextSignals needleSignals = M3TextSignals.compile("!");
boolean hasBang = sourceSignals.mayContain(needleSignals)
        && whole.indexOf("!") >= 0; // exact verification after candidate filtering

M3EditDistancePlan plan = M3EditDistancePlan.compile("A\uD83D\uDE42!");
M3EditDistancePlan.Workspace work = plan.workspace();
int edits = plan.distance(whole, work, null); // expected: zero
// Reuse work sequentially with this plan only; it is mutable scratch state
```

Here `face.storage().sharesBackingWith(whole.storage())` is true, although the
two values have different text. A cached fact keyed only by that shared body
would therefore be wrong for at least one range. The example is illustrative
cross-API usage, not evidence that a runtime integration or cache exists.

## 7. Tests, source pins and remaining gaps

| Test source | Assertions read | What this pass does not establish |
| --- | --- | --- |
| [ExistingSurfaceTest][existing-test] | Seeded arbitrary UTF-16 splits/slices; all 65,536 code units in hash/copy checks; mutable-input isolation; five charsets including ISO-2022-JP; reader mark/reset; byte streams | No execution result, universal charset proof, hostile-file safety or performance evidence |
| [RetainedViewTest][retained-test] | Reflection checks of retained payload sharing; slice/concat identity; all single UTF-16 units; surrogate seams; seeded equality/search/encoding cases; oversized-pattern fallback; admission callback count/refusal; regex capture/region checks; mapped/direct refusal; concurrent live-body lookup | No automatic proof of race-free arbitrary callers, forced weak eviction, ID exhaustion, allocation failure, Windows unlink semantics, or JNI acceptance |
| [M3TextSignalsTest][signals-test] | Flat/composed metrics and histograms, seam handling, finite edit lower bounds and self-consistent sketches | No bridge-specific integration test, cryptographic guarantee or exhaustive semantic proof |
| [M3EditDistancePlanTest][distance-test] | Seeded Myers/DP/threshold comparisons with an independent recurrence; wrong-plan workspaces; pattern/source/full-DP limits | No concurrent/reentrant workspace qualification, throwing monitors, allocation-failure injection or whole-operation cost measurement |

Further targeted work includes direct-buffer authenticity and immutable-file
publication, retained-mapping release across platforms, deliberate hash-collision
and descriptor-eviction cases, concurrent caller-directory mutation refusal or
contract clarification, all stream error-order branches, and cross-API tests
using actual `M3Text` inputs. These remain gaps, rather than implicit passes.
The separate native probes, wider canonical String owners and all other
precompute/native/image APIs are outside this seven-owner bridge review.

The following Git blob identities make the reviewed source bodies explicit.
The first nine rows belong to the bridge pin above; the remaining rows belong to
the precompute pin above. They are source identity evidence, not runtime receipts.

| Source | Git blob |
| --- | --- |
| [M3Text.java][text] | `46aecc2fb0b1e5f516c1db8b5e93476eb70444f2` |
| [FrozenBytes.java][frozen-bytes] | `b624dc8dbb23eac6b3147ea888ac69df60f971e1` |
| [FrozenChars.java][frozen-chars] | `a8f016a1764aea6e970bc60e38f345804e82a84e` |
| [MIndexJoinedBytes.java][joined-bytes] | `9db0c6363480ccb0f38bba7b2218e151dd75ee68` |
| [MIndexJoinedChars.java][joined-chars] | `6d7d06cc24c7c42c0aa2e6d21b82a53c9b4e1566` |
| [MIndexJoinedStorageIntern.java][intern] | `95a576da9e60fb830a443034d6403e84740233e8` |
| [MIndexJoinedStreams.java][streams] | `6f350a84b954b2f80929252254a007652fc1d0d0` |
| [ExistingSurfaceTest.java][existing-test] | `d660ec518b35c944280b6165adf6138e7fff6b36` |
| [RetainedViewTest.java][retained-test] | `22ad8979e7124c34e5095235b958c03ad1636e4d` |
| [M3TextMetrics.java][metrics] | `e67ad2157aacb8b31b293e522294d4c7f657adff` |
| [M3TextSignals.java][signals] | `631145b68358970e6a7d9a792580e7a100eebb20` |
| [M3Histogram.java][histogram] | `e8abc6db52f3032f415f5316167a169dec043438` |
| [M3EditDistancePlan.java][distance] | `84bc435ddfa6a84d5c41dc5ae3ddc2711d609938` |
| [M3TextSignalsTest.java][signals-test] | `2defb5e9a66749dec38384dccd6999107c94fb32` |
| [M3EditDistancePlanTest.java][distance-test] | `47c363ad6ae3cb1d2d24ce7c5549c1cfe0609117` |

[policy]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/docs/m3jdk21-porting-invariant.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/docs/name-mapping.json
[port-readme]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/README.md
[port-pom]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/pom.xml
[precompute-pom]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/pom.xml
[text]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/m3/text/compat/M3Text.java
[frozen-bytes]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenBytes.java
[frozen-chars]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenChars.java
[joined-bytes]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedBytes.java
[joined-chars]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedChars.java
[intern]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStorageIntern.java
[streams]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/main/java/com/synexia/indexstring/MIndexJoinedStreams.java
[existing-test]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/test/java/ExistingSurfaceTest.java
[retained-test]: https://github.com/hsoliwal/M3jdk21/blob/220eb11788d4039da904491e7d135b2304045659/m3/ports/indexstring/src/test/java/RetainedViewTest.java
[metrics]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java
[signals]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java
[histogram]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java
[distance]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java
[signals-test]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java
[distance-test]: https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java
