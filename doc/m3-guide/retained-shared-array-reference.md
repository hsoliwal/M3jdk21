<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Retained shared-array ownership and JNI contracts

[Guide](README.md) · [Coverage and source identities](retained-shared-array-reference-coverage.md) · [Runtime owners](runtime-owners.md)

This reference explains the eight retained `com.synexia.indexstring.shared`
Java source bodies and their two retained native companions in public M3JDK21
at `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. The source is **custody-only**.
It is not a complete compilable module, a JDK runtime dependency, an exported
JPMS API, or proof of current runtime admission. No build, test, example,
native-library load, policy verifier or benchmark was run for this reference.
Any hosted CI remains separate exact-commit evidence, not a result asserted
by this source-documentation pass.

The [source provenance][provenance] pins these bodies to the shared-array intake
at donor revision `857c4cc0ddeb73e5ea5269a1d0ef2d30057dcfa8`. These are public
receiver bytes; the provenance is not an independent inspection of the donor
repository. Exact identities and member routes appear in the companion page.

## 1. Keep the ownership domains separate

The existing [shared-atom design][shared-design] explains the intended storage
and composition model, with its own historical evidence limits. The
[indexed-string bridge reference](indexed-string-reference.md) owns the
separately built `ports/indexstring` contracts. The
[bootstrap String ownership guide](bootstrap-string-ownership.md),
[mapped backing reference](mapped-string-storage-reference.md) and
[current String backing guide][backing-guide] own the runtime-side explanations.
This page adds the retained `shared` package's concrete source behavior.

| Domain | Actual responsibility | What it does not establish |
| --- | --- | --- |
| Retained `SharedArrayPool`, `SharedChars`, `SharedBytes` | Append-only `arena.bin` records, read-only literal mappings and immutable logical range directories | Canonical `java.lang.String` storage or a shipped standalone artifact |
| Retained `SharedSegments` | Arrays of owner references, literal IDs, offsets, lengths and cumulative ends | A joined byte/character payload, or permission to add this descriptor cache to the runtime |
| `ports/indexstring` | Separate `M3Text`/frozen/joined-view bridge | SharedArrayPool admission, arena persistence or JNI qualification |
| Frozen-text compatibility fixture | Isolated frozen-character/byte source fixture | A substitute build or test for these eight shared-array bodies |
| `java.lang.String` and its M3 owner family | Public JDK semantics and target-owned runtime representation | Automatic promotion of retained package names or donor native symbols |

The [intake target map][target-map] says `MAPPED_CANDIDATE_NO_TARGET_FAST_PATH`,
`OWNER_MAPPING_ONLY` and `NO_PARALLEL_DESCRIPTOR_OWNER`. In particular, the
retained `ByteBuffer[]` preparation cache must not become a second runtime
payload/descriptor owner. The [name map][name-map] separately leaves future
shared-array target owners pending retention, namespace/generation, lifetime,
replacement/corruption and cross-process proof. Those recorded dispositions
are not runtime facts inferred from a Java package name.

## 2. Handles, content equality and aliases

Sources: [ArrayHandle][handle], [SharedArray][array], [SharedBytes][bytes],
[SharedChars][chars].

`ArrayHandle(namespace, recordOffset, start, length)` is a value record. Its
constructor rejects a null UUID, a record offset below 64, negative coordinates,
or `start + length > Integer.MAX_VALUE`. It does not open a file, verify that a
record exists, identify a width, or validate a range against a stored record.
Record-generated accessors/equality/hash/string rendering concern these four
fields. The offset is a file coordinate, never a native pointer or heap index.
There is no separate generation counter: stable reuse depends on the namespace
and the trusted, never-replaced arena.

`SharedArray` is package-private; its public final accessors are inherited by
the two public value types. `handle()` returns immutable coordinates;
`length()` counts logical units; `languageId()` returns the row's language;
`segmentCount()` counts directory entries; `asReadOnlyBuffers()` returns new
read-only buffer views, described below. Byte values use language 0.
Character language IDs are unrestricted `int` metadata here, not validated
language-catalogue entries.

There are three distinct equality decisions:

- `ArrayHandle.equals` compares namespace, record offset and range
- `SharedArray.equals` requires the same concrete class, the same logical
  length and cached 32-bit hash, then compares every unit exactly. It ignores
  namespace, handle, segmentation and language. Equal character content in two
  language rows or two arenas may therefore compare equal
- Pool admission includes width and language in candidate selection, then
  verifies exact content or exact immutable descriptor tuples. Two different
  language rows need not share a handle even when their values compare equal

A `SharedChars` is not `equals`-equal to a `String` or `SharedBytes`.
`SharedChars.contentEquals(CharSequence)` is the explicit cross-type operation;
it returns false for null or different length, compares units and propagates
exceptions from a custom sequence. The caller must keep that sequence coherent.
No normalization, collation, case folding or surrogate repair occurs in these
comparisons. The optional native `contentEquals` instead delegates to native
`compare` and rejects null operands.

Character hashes use the Java String polynomial, seed 0 and unsigned UTF-16
units. Byte hashes use seed 1 and signed byte contributions, like a Java byte
array content hash. Empty character/byte values thus hash to 0/1 respectively.
The segment implementation can reuse a polynomial contribution for an exact
repeat of the same live buffer object, offset and length; a coincident literal
ID or hash is insufficient for that shortcut. Hash equality alone never proves
content equality. See [segment hashing][segment-hash].

## 3. Arena opening, synchronization and recovery

Sources: [pool fields/open][pool-open], [locking][pool-lock],
[refresh/descriptor validation][pool-refresh].

`open(directory, maxBytes, maxEntries, maxCachedViews)` requires a nonnull path,
`maxBytes >= 64`, `0 <= maxEntries <= 16,777,216` and nonnegative view-cache
capacity. It creates the directory, resolves its real path, and opens
`arena.lock` and `arena.bin`. The directory must be trusted and cooperating
writers must respect these locks. Read-only mappings do not prevent external
writes, replacement or truncation.

A new arena gets a 64-byte version-1 header with magic `SYNARR01`, a random UUID,
`maxBytes`, `maxEntries` and a CRC32C covering the first 56 bytes. The header is
forced to storage. Reopening requires the same magic/version, checksum and
**exact same persistent byte/entry budgets**. `maxCachedViews` is per-instance
and is not part of that header identity. Header mismatch is an `IOException`;
a short header can fail with `EOFException`. For `IOException`,
`RuntimeException` or `Error` during construction, `open` attempts to close
content then lock and suppresses cleanup `IOException`s on the original
failure. An unchecked content-close failure can replace that failure and skip
the subsequent lock-close attempt; the source does not guarantee cleanup on
every possible unchecked exit.

The static `JVM_GATE` serializes pool operations across instances of this loaded
class, even for different directories. The locked path checks openness, drains
cleared mapping references, takes the file lock, refreshes appended records,
then performs its action. This is not a cross-class-loader lock service.
Known-record retrieval is a special case: it uses `JVM_GATE`, checks openness,
and can return an already indexed row without reacquiring the file lock or
refreshing the entire arena. Unknown-offset retrieval takes the locked refresh
path. Consequently a cached read is not a fresh whole-file integrity check.

Each record has an 80-byte header, payload and 8-byte commit trailer. Literal
records have part count -1; descriptor records have nonnegative part count and
16 bytes per tuple `(literalRecordOffset, unitOffset, unitLength)`. Width is 1
for bytes or 2 for UTF-16BE. The header includes language, logical units, Java
hash, payload length, SHA-256 bytes, CRC and a reserved zero long. The trailer is
`COMMIT ^ recordOffset ^ totalRecordBytes`.

Admission writes header and payload, writes the CRC, forces data, writes the
commit trailer, forces again, refreshes, then constructs the result view. There
is no rollback transaction around all of those stages. A write, force, refresh
or mapping failure may occur after a prefix or complete record reached the
file; a retry may rediscover the committed record.

Refresh checks the file is neither shorter than the scanned prefix nor larger
than its budget. It validates record shape, width, nonnegative units, part-count
range, payload/total arithmetic, byte-language zero, reserved field, trailer and
CRC. Descriptor rows must reference an earlier indexed literal of the same
width, with positive in-bounds ranges whose summed lengths equal the logical
length. Leaf language need not equal descriptor language, allowing metadata
relabeling without copying literal payload.

Recovery is deliberately limited. A tail shorter than the record header, or a
structurally valid header whose complete record is not present, is truncated to
the last scanned boundary and forced. A complete bad header, trailer, checksum
or descriptor is refused with `IOException`, rather than silently skipped.
Refresh verifies CRC and geometry but does not recompute the stored SHA-256 or
Java hash from logical content. This is neither hostile-file authentication nor
protection against a writer that can rewrite the trusted arena.

## 4. Admission, lookup and capacity

Sources: [input overloads][pool-input], [join/conversion/get][pool-api],
[admission/content index][pool-intern], [descriptor index][pool-descriptor].

| Operation | Content and ownership behavior |
| --- | --- |
| `internBytes(byte[])` | Reads caller bytes during admission; a miss copies literal bytes to the arena, not into a retained caller array |
| `internBytes(ByteBuffer)` | Admits the source's remaining range through a read-only duplicate and absolute reads; source position, limit and mark are not advanced |
| `internBytes(SharedBytes)` | Same namespace: descriptor admission; foreign namespace: reads and imports the content as a literal input |
| `internChars(languageId, char[])` | Wraps the array as a `CharBuffer` and follows character admission |
| `internChars(languageId, CharSequence)` | Same-namespace `SharedChars`: descriptor admission with the requested language; other sequences, including foreign shared values: reads unit content |
| `lookupChars(languageId, source)` | Refreshes and looks up exact content; returns an optional **record offset**, not an `ArrayHandle`; does not admit a new row |
| `concatChars(languageId, values...)`, `concatBytes(values...)` | Require a nonnull directory and elements from this namespace; construct literal-range geometry, then intern it; foreign namespaces are refused |
| `chars/bytes(recordOffset)` | Resolve a whole known record of the requested width; reject unknown offset or wrong kind with `IllegalArgumentException` |
| `chars/bytes(ArrayHandle)` | First reject a null/foreign handle, resolve the record, then slice its checked range; an in-domain handle can still refer to an absent row or out-of-bounds range |

Mutable arrays, buffers, `CharSequence` content and concat argument directories
must stay valid and stable for the whole call. Admission may read input more
than once, including fingerprinting, collision verification and payload write;
it does not take an upfront snapshot that tolerates concurrent mutation.
After successful admission, ordinary later changes to the original mutable
input cannot rewrite the pool's literal. A direct buffer wrapping externally
managed memory remains the caller's lifetime responsibility during the call.

Descriptor lookup happens first when segment geometry is supplied. Its mixed
signature is a candidate filter; matching width, language, segment count and
**every stored literal-ID/offset/length tuple** are checked. A hit increments
`descriptorIndexHits` and avoids the logical content scan. This relies on the
immutable trusted-owner precondition, not a cryptographic signature.

Otherwise the pool computes SHA-256 over width, language and exact bytes
(character units are emitted high byte first), together with the Java hash.
The content index filters on the first 64 SHA bits, width, language, length and
Java hash, then compares every candidate unit. It does not accept even a full
hash as equality proof. Equal content already represented by a different
segmentation can reuse the existing row, so a concat result is not guaranteed
to retain the requested segmentation or a new descriptor record.

Only a miss tests the append quotas: at most 64 MiB of payload per record,
`maxEntries`, and `maxBytes` including header/trailer. Oversized record payload
throws `IllegalArgumentException`; exhausted arena space/entries throws
`IllegalStateException`. Fingerprinting and candidate work occur **before**
these miss-budget checks, so the byte budget is not a bound on preparation
CPU or temporary encoding memory. Exact hits can still succeed in a full arena.
Literal UTF-16 payload uses two bytes per unit; a descriptor payload is only
16 bytes per segment and can represent a larger logical value within `int`
length limits.

`SharedSegments.join` sums the input segment counts with `Math.addExact` and
rejects more than 4,096 **before coalescing**. Adjacent entries coalesce only
when their literal record IDs match and their ranges are contiguous. It then
constructs checked cumulative ends; length/count overflow can throw
`ArithmeticException`. Empty values add no segment. Joins of joins reuse leaf
literal ranges rather than retaining a nested parent directory. The pool may
still perform whole-content hashing/comparison on a descriptor-index miss;
this is not an unconditional constant-time or allocation-free join.

## 5. Slicing, buffers and caller-owned copies

Sources: [SharedBytes][bytes], [SharedChars][chars],
[segment directory and slice/join][segment-geometry],
[buffer/copy implementation][segment-copy].

Byte indexes count bytes; character indexes count UTF-16 code units. Character
payloads are canonical big-endian byte pairs, including NUL and unpaired
surrogates. Slices may split a surrogate pair. A full-range `slice` or
`subSequence` returns the same object. Other ranges create new directory
metadata and recompute the range hash; their handle retains the original
record offset and adds the relative start. Empty cuts have no participating
owner references. Invalid ranges throw `IndexOutOfBoundsException`.

A nonempty slice keeps only the participating literal owners, not the parent
`SharedSegments` directory. The full mapped literal owner can still be large;
a tiny slice does not promise proportionally tiny retained mapping memory.
The constructor validates positive lengths, nonnegative offsets, capacity and
checked cumulative units for internal callers. It retains the supplied arrays
as internal metadata; it is not a general public constructor accepting hostile
or mutable descriptors.

Indexed access uses one thread-local `LookupCursor` containing a weak reference
to the current directory and the last segment bounds. It tries that segment,
then its immediate next/previous neighbor, then binary-searches cumulative
ends. It retains no strong directory or payload reference. The package-private
`binarySearches` counter is a discriminator, not a public metric, and the
referenced focused proof is not present in the retained test tree.

Every `asReadOnlyBuffers()` call builds a fresh array of sliced read-only
`ByteBuffer` views whose capacity covers exactly the selected bytes. Their
positions/limits/marks are caller state; changing those or replacing array
elements does not mutate the private directory. Their bytes still alias the
same mapped owners, rather than a detached payload copy. `SharedChars`
additionally builds read-only `CharBuffer` views with explicit big-endian order.
Changing a returned byte buffer's order cannot change canonical unit decoding.
No accessor returns a raw address.

| Export | Checks and mutation |
| --- | --- |
| `SharedBytes.copy()`, `SharedChars.copy()` | Allocate a fresh mutable array and copy the whole logical value |
| Array `copyTo(sourceStart, target, targetStart, count)` | Null target, source range, then target range are checked before traversal; only the requested target range is written |
| Buffer `copyTo(sourceStart, target, count)` | Null and source range checked, then remaining capacity, then read-only state; `BufferOverflowException` precedes `ReadOnlyBufferException` if both apply |
| `SharedChars.toString()` | Calls `copy()` and constructs a String; an explicit materialization boundary |

The Java copy loops transfer participating segments directly into the caller's
destination. Buffer exports advance only the target position by the copied
units; internal owner positions remain unchanged. Array outputs have no
cursor. No joined intermediate payload is created by the `copyTo` loops.
Zero-count buffer export still checks read-only state. Preflight errors do not
write the target; unexpected later failures or concurrent caller changes are
not covered by a rollback guarantee. The caller owns target synchronization.

The private `preparedBuffers` cache is different from caller-visible buffers:
it lazily publishes one complete-value descriptor array after construction
succeeds, using volatile publication and synchronization. A construction
failure leaves it empty for retry. It is retained by that segment directory
for synchronous JNI calls, never returned by the public buffer accessor. The
range-copy JNI path instead borrows the existing owner/offset/length/end arrays
through `Utf16RangePlan`, avoiding a newly sliced descriptor per segment.

## 6. Conversion and bounded result-handle caching

Sources: [pool encode/decode][pool-conversion], [conversion facade][conversion].

`encode(SharedChars, Charset)` creates a JDK encoder with malformed and
unmappable input both set to `REPLACE`. It wraps the entire logical sequence
as one `CharBuffer`, so a surrogate pair split across segments or a stateful
charset is not encoded as unrelated fragments. The encoder's result buffer is
then admitted as bytes. This includes conversion allocation; it is not a
zero-copy encoding or a cached payload attached to the source.

`decode(languageId, SharedBytes, Charset)` deliberately calls `source.copy()`,
wraps that flat byte array, applies `Charset.decode`, and admits the decoded
characters. The convenience decoder supplies replacement behavior. A byte
array/character conversion is an explicit flattening boundary, not join
semantics. Neither pool conversion requires source namespace equality.
Source and charset are checked first; conversion work can occur before the
subsequent admission checks that the pool is still open. Charset/provider
errors, allocation failures and admission errors propagate.

`new SharedArrayConversions(pool, maxEntries)` requires a nonnull caller-owned
pool and nonnegative entry limit. It serializes operations per facade and
retains an access-ordered LRU of **result handles**, keyed by source handle,
charset, target language and encode/decode direction. It retains no converted
payload or source buffer. The facade retains its pool reference; it does not
own the pool's close operation.

Caching is enabled only for identity-equal `StandardCharsets` singletons:
UTF-8, UTF-16, UTF-16BE, UTF-16LE, ISO-8859-1 and US-ASCII. A same-name custom or
provider object bypasses it, as does a zero entry budget. This is a source
handle cache, not a general equal-content cache across source handles.

On a hit, the facade resolves the stored result through `pool.bytes(handle)`
or `pool.chars(handle)`, so closing the pool still causes failure on a hit.
The hit counter increments only after successful resolution. A failed hit can
already have touched LRU access order. A miss increments before conversion;
a bypass increments before delegation. Consequently failures can count as
misses/bypasses but failed conversions are never retained. Null arguments or a
closed facade fail before those request counters change. Successful retention
evicts eldest entries until within the count limit.

`stats()` returns `entries`, `hits`, `misses`, `bypasses`, including after facade
close. `clear()` requires an open facade and drops handles without resetting
counters. `close()` clears handles and marks the facade closed, is repeatable,
and never closes the pool. Dropping a conversion-cache entry does not delete
an arena record, release a caller's result view or deterministically unmap its
literal backing.

## 7. Optional JNI: complete call routing and load limits

Sources: [Java native facade][native-java], [native kernels][native-cpp],
[native comparison][compare-cpp], [retained CMake input][cmake].

Only explicit `SharedArrayNative.load(Path)` calls `System.load`, using the
nonnull path's absolute string. Pure Java pool/view operations never load it.
There is no automatic fallback or load-success flag in this facade. A native
operation requires a compatible loaded implementation unless its documented
zero-count Java path returns before JNI.

The retained native target is **not standalone-buildable**: CMake references an
external `synexia-native-interop` `pin.cmake` and `jni/ownership.hpp`, as well as
native arena C sources, headers and tests absent from this slice. The
[intake README][vendor-readme] explicitly records that the wrapper header was
not copied. The exact mapbox ISC [license notice][mapbox-license] is retained;
that does not supply the missing implementation or establish a passing build.

There is also a **source-level registration mismatch**. The Java declaration
and C++ implementation of `copyUtf16RangeTo0` have three `int[]` metadata
parameters. The `JNI_OnLoad` registration descriptor contains four. See the
[exact descriptors and error path](retained-shared-array-reference-coverage.md#native-registration-mismatch).
If a library built from this retained registration path is loaded against this
retained Java class, the requested name/signature does not match that declared
method; registration success must not be assumed. `JNI_OnLoad` returns
`JNI_ERR` on registration failure, with no fallback. This is a source-derived,
conditional load boundary, **not an observed load failure**. The kernel behavior
below describes the retained bodies, not a verified reachable deployment.

| Java route | Native body and result | Validation and limits |
| --- | --- | --- |
| `hash32(SharedBytes)` / `hash32(SharedChars)` → `hash32Segments` | Traverses private complete-capacity descriptors; signed-byte seed-1 or UTF-16BE seed-0 polynomial, with defined low-32-bit wraparound | Nonnull descriptor array/elements, width 1 or 2, at most 4,096 segments, direct capacity divisible by width, nonnull address for nonempty capacity; it does not independently cap summed logical length |
| `compare(left, right)` → `compareUtf16Segments`; `contentEquals` tests zero | Unsigned UTF-16-unit lexicographic difference, then length difference | Validates both complete arrays before reading text, including tails beyond an early mismatch; direct even-capacity buffers, at most 4,096 each, total units fitting `int` |
| `metrics(chars)` → `scanUtf16Segments` → `scanUtf16Segments0` | Returns eight integers: units, UTF-8 bytes, code points, unpaired surrogates, hash, power-of-31, first unit, last unit; Java passes them to `IndexTextMetrics` | Complete preflight, replacement width 1–4, direct even capacities, total units and final metric counts fitting `int`; overflow raises `ArithmeticException` |
| Public character `copy` / `copyTo` → `copyUtf16RangeTo0` | Writes caller-owned `char[]` through 1,024-unit stack blocks and `SetCharArrayRegion` | Java null/range/checkpoint preflight; native complete owner/metadata/range validation, then participating-window traversal |
| Private `copyUtf16SegmentsTo0` | Complete-sequence export with the same bounded stack transfer scheme | Declaration and native implementation are retained, but there is no Java call site in this facade; complete descriptor lengths must equal requested count |

Native descriptor errors generally raise `IllegalArgumentException` through
JNI; pending JNI allocation/lookup exceptions are propagated on checked exits.
Return values such as zero/null on those exits are not successful comparison,
hash or metric results. The scan rejects a UTF-8/count result exceeding Java
`int`; hash/power overflow intentionally wraps instead of throwing.

Native traversal reads complete descriptor capacities and ignores buffer
positions/limits. Java therefore prepares exact slices. UTF-16 is decoded by
explicit high/low byte loads, regardless of host order or address alignment;
these kernels do not inspect `ByteBuffer.order()` or enforce read-only state.
Those properties come from the facade's private immutable owner convention,
not arbitrary native callers. They use ordinary UTF-8 metric semantics, not
JNI modified UTF-8 String-access APIs.

The metrics scanner composes per-segment summaries and repairs high-surrogate /
low-surrogate seams, including across empty components. Empty aggregate first
and last are -1 and power is 1. Its eight-entry invocation-local FIFO summary
cache keys exact borrowed address plus byte length, retains local owner
references and uses a most-recent shortcut. It caches no payload and no pointer
survives the call. This is distinct from the Java conversion-result LRU.

Hash/metric wrappers use acquired local-reference ownership through the
external mapbox wrapper; comparison uses scoped cursors and explicit local-ref
release. Input argument references are borrowed, not adopted. The source uses
no persistent global-reference/native-handle table for these calls. A rooted
buffer object cannot make caller-freed external memory safe: source owners and
native aliases must remain valid and immutable for each synchronous call.

## 8. Native range export and cancellation

Sources: [Java export/checkpoint][native-export],
[native complete export][native-full-copy], [native range export][native-range].

`copy(source)` and `copy(source, monitor)` allocate a fresh mutable `char[]`;
the latter checks cancellation before allocation, then delegates to range copy.
`copyTo` has monitored and unmonitored overloads. Both source and target must
be nonnull and both ranges valid before mutation. A zero count still performs
the Java checkpoint but does not require a loaded library.

`copyCheckpoint` tests `Thread.currentThread().isInterrupted()`, calls the
nonnull monitor's `checkCanceled()`, then tests interruption again. It throws
`CancellationException` for interruption without clearing the interrupt flag;
monitor exceptions propagate. Native copying calls that method before transfer
and before every block. The public range wrapper checks again after JNI
returns, so cancellation can be reported even after all requested units were
written. Hash, compare and metrics have no corresponding monitor/checkpoint
contract in these bodies.

The range plan borrows the directory's owner references and three integer
metadata arrays plus the participating first/last segment indexes and logical
range. Native preflight checks matching array lengths, at most 4,096 entries,
a valid participating window, destination range, positive segment lengths,
nonnegative offsets, strictly increasing cumulative ends with exact deltas,
direct even-capacity owners, capacity bounds, total source range and exact
first/last window coverage. It validates the whole directory even though the
copy pass visits only participating owners.

`jint_array_view` acquires the three metadata arrays with
`GetIntArrayElements` and releases each successful acquisition with `JNI_ABORT`.
The VM may pin or copy **metadata**; this is not a claim that no arrays are ever
acquired. The caller's character destination is not pinned by these routines:
UTF-16BE loads fill a fixed 2 KiB stack block and `SetCharArrayRegion` writes the
bounded target region. There is no `GetPrimitiveArrayCritical` region, joined
payload cache or escaping pointer.

Cancellation or a later native failure may leave a prefix of the requested
range written. Outside that range stays untouched by the intended copy loop;
there is no rollback or snapshot of concurrently modified aliases. The caller
owns destination synchronization and source backing lifetime/immutability.
The private complete-segment copy additionally verifies the sum of all segment
lengths equals `count`, rechecks descriptor availability during transfer and
refuses changed length totals. Neither native export is a transactional copy.

## 9. Geometry, accounting and close

Sources: [literal geometry][pool-geometry], [statistics][pool-stats],
[mapping/view construction and close][pool-view].

`copyLiteralGeometryPage(startRow, maxRows, payloadOffsets, payloadByteLengths,
widths, languageIds, sourceRows)` copies metadata into caller-owned arrays.
All five outputs must be nonnull; `maxRows` must be positive and fit every
output. After locked refresh, `startRow` must be from 0 through the current
row count. Descriptor rows are skipped. The limit bounds **emitted literal
rows**, not total scanned rows; the method can pass many descriptor rows while
filling one page. It reads no literal payload for the returned geometry, though
the preceding refresh can validate new record payloads.

The result packs the next source row into the high 32 bits and emitted count
into the low 32 bits. `geometryNextRow`/`geometryCount` unpack those bits without
validating the packed value. A call starting at the current row count returns zero emitted rows and that
same next row; the final nonempty page can already return next row equal to
the current count. Payload offsets are `recordOffset + 80`; byte lengths are width
times units. Width/language/source-row arrays are separate outputs. Do not
alias the four `int[]` outputs if all fields are needed: the method writes them
in byte-length, width, language, source-row order without an alias check.
This is a page over current metadata, not a durable snapshot cursor or raw
address export. A subsequent page can observe cooperating append activity.

`stats()` refreshes and returns entries, literal payload bytes, descriptor
payload bytes, scanned file bytes and current strongly cached views.
`descriptorIndexHits()` refreshes and returns this instance's exact-tuple hit
counter. Neither measures all heap/native/process residency, including caller
views or OS mappings. Optional mapping entries are weak references indexed by
literal row, with reference-queue cleanup under `JVM_GATE`; a delayed queue
entry cannot erase a newer mapping wrapper. A weak hit is promoted once to a
strong local before view construction.

The view LRU retains up to `maxCachedViews` whole record views. At zero, views
are still returned and can retain mappings in callers. Live slices, joined
values, exported buffer views and private prepared descriptors keep their
necessary owners reachable independently of the LRU. Eviction changes optional
retention, not an existing view's logical content. Different wrappers/mappings
can exist after weak residency ends; object or virtual-address identity is not
the persistent handle.

`close()` synchronizes on `JVM_GATE`, marks closed, clears the view cache, drops
mapping wrappers/queue, then closes data and lock channels. Repeated close
returns immediately, including after a first close failure. The lock-channel
close is attempted in `finally`; no explicit unmapping is performed. Existing
caller-retained views remain usable under the backing-file stability contract.
Pool operations that need `ensureOpen` fail after close, while `namespace()`
still returns its immutable UUID. There is no deterministic deallocation,
reference-counted view release, record deletion or arena compaction API here.

## 10. Reading an example without implying a build

Source-derived scenario, **not executed**: admit two character literals with
one high surrogate at the end of the first and its low surrogate at the start
of the second, then call `concatChars` in their namespace. Character access
preserves the two exact units. Encoding sees one logical sequence, and native
metric summary composition repairs the seam if that native route is available.
A slice selecting only one half preserves that half; encoding it uses the JDK
encoder's replacement policy. `copy()` produces caller-owned mutable output.

Changing a concat operand's language through same-namespace character admission
can create a descriptor alias of its existing literal bytes. Handle identity
and language-aware pool identity can change while `SharedChars.equals` stays
true for the same units. Closing the pool does not grant permission to truncate
its arena while a returned slice or buffer remains live.

These scenarios illustrate inspected paths only. The retained package has no
shared-array test bodies; fixture tests and bridge tests cannot be counted as
proof of them. Missing Java/native dependencies, the conditional registration
mismatch, exact-head build/JNI tests, cross-process/crash/corruption behavior,
GC/lifetime, platform coverage and cold/warm resource measurements remain
separate qualification work. Policy and licenses are unchanged: see the
[public custody README][vendor-readme], [borrowing contract][borrowing] and
[porting invariant][policy].

[array]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArray.java#L1-L34
[backing-guide]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/doc/mindex-string-backing.md
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md
[bytes]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedBytes.java#L1-L35
[chars]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedChars.java#L1-L58
[cmake]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/CMakeLists.txt#L1-L105
[compare-cpp]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_string_compare.cpp#L1-L103
[conversion]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayConversions.java#L1-L74
[handle]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/ArrayHandle.java#L1-L15
[mapbox-license]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/third-party/mapbox-jni-ownership/LICENSE.txt
[name-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/name-mapping.json
[native-cpp]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L1-L472
[native-export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L51-L101
[native-full-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L240-L302
[native-java]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayNative.java#L1-L110
[native-range]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/native/shared-arrays/shared_arrays.cpp#L323-L459
[policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
[pool-api]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L261-L324
[pool-conversion]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L280-L295
[pool-descriptor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L424-L456
[pool-geometry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L125-L187
[pool-input]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L209-L260
[pool-intern]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L332-L423
[pool-lock]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L188-L207
[pool-open]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L32-L123
[pool-refresh]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L561-L617
[pool-stats]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L325-L331
[pool-view]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedArrayPool.java#L618-L682
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/jni-array-address-receiver-20261007/SOURCE_PROVENANCE.tsv
[segment-copy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L169-L238
[segment-geometry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L20-L132
[segment-hash]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/shared/SharedSegments.java#L239-L276
[shared-design]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/shared-atom-concatenation.md
[target-map]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/jni-array-address-receiver-20261007/TARGET_MAP.tsv
[vendor-readme]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md
