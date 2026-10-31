<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Internal primitive lanes and metadata helpers

[Guide](README.md) · [Runtime owners](runtime-owners.md) · [Mapped storage and identity](mapped-string-storage-reference.md) · [Class index](class-index-reference.md) · [Class/member views](class-member-view-reference.md) · [Package source coverage](internal-mindex-source-coverage.md)

This reference reviews all declared bodies in seven `java.base` owners in `jdk.internal.mindex`: `M3IntLane28`, `M3LongLane28`, `M3BitLane28`, `M3CB`, `M3Descriptor`, `M3PC` and `M3VI`. The public source pin is `e7d21edf0f998c3c53ab0d0909972a4a97dfc471`; all seven blobs match the earlier 24-file package inventory at `278b221e8365ebeb6095a3a5643b38a0564258b5`. These are implementation-internal classes, not seven newly exported Java SE APIs or separate modules.

The three lane bodies are mutable heap storage. The class bridge is a mutable synchronized session. The version index retains a caller-supplied comparison scheme. Their different ownership rules must not be collapsed into a blanket immutable-image contract. None of these seven owners implements a persistent format, Java serialization, file mapping or native-address export. The bit lane has one explicit native read operation, described below.

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md), [naming map](../../m3/docs/name-mapping.json), [borrowing policy](../../SYNEXIA_APACHE_BORROWING_POLICY.md), [full-family pin](../../m3/compatibility/synexia-full-family-pin.tsv) and [receiver plan](../../m3/synexia-import/current-full-borrow/README.md) remain authoritative. This documentation neither changes sealed source nor advances String → arrays → collections → AST/compiler → remaining-family promotion. Per-file OpenJDK, third-party and first-party rights remain unchanged. Only public target source is used here.

## Coordinates, widths and ownership

| Coordinate or value | Actual scope |
| --- | --- |
| Lane slot | Integer in `[0, 2^28)`, split into 8 region bits, 8 block bits and 12 value-offset bits; it is not a process address, text handle or class row |
| Lane payload | Full signed Java `int` or `long`, or one bit; 28 limits the logical address, not the stored numeric width |
| `M3CB` class row | Coordinate in its particular `M3CI`; class identity additionally requires the live defining-loader/mirror boundary |
| `M3Descriptor` result | VM descriptor spelling; equal nominal descriptors can name different classes in different loaders |
| `M3PC` input lanes | Ordered references to existing immutable Strings; content comparison is explicit through `sameInputs` |
| `M3VI.originalRow(position)` | Original input position inside this prepared index, selected by a sorted position; neither coordinate is a cross-index version ID |
| `M3Release` attached to a bridge row | Exact caller-declared release provenance, including raw version spelling; comparison-equivalent versions need not be equal release records |

The [address/bit arithmetic reference](mapped-string-storage-reference.md) covers `M3Address28`, package-private `M3Bits`, weak `M3Boundary` and `M3Release` in full. A slot is `(region << 20) | (block << 12) | offset`: 256 regions, 256 blocks per region, 4,096 values per block and 268,435,456 possible slots. `MAX_SLOT` is 268,435,455. Half-open ranges may use `MAX_SLOTS` as an endpoint, including an empty range there; scalar access may not. Range-length validation uses `Objects.checkFromIndexSize` rather than an overflowing unchecked `offset + length` test. Invalid coordinates throw `IndexOutOfBoundsException`.

These are logical integers and Java primitive arrays; there is no byte-order flag or serialized endian convention. Native bit counting receives numeric `jlong` values through JNI rather than interpreting a persisted byte stream. Do not apply the big-endian mapped-text format to these heap lanes or treat their numeric IDs as interchangeable with mapped-store handles.

## Segmented int and long lanes

The complete [int lane][ints] and [long lane][longs] have the same paging, copying and ownership algorithm. Each implicit public no-argument constructor eagerly creates a 256-reference root directory. Region directories and primitive leaf arrays are allocated only when a nonzero write requires them. Reads from absent regions/pages return zero. A zero scalar write or clear does not allocate; an already allocated all-zero page remains allocated. There is no trim, close, delete-page or whole-owner reset method.

| Declared operation | Observable contract |
| --- | --- |
| `get(slot)` | Checked read; absent storage is zero |
| `set(slot,value)` | Checked overwrite, allocating a region/block for nonzero data if absent; accepts every value of the primitive type |
| `getAndSet(slot,value)` | Calls `get` then `set`, returns the previous value; this is not an atomic exchange |
| Int `incrementAndGet(slot)` | Computes `Math.addExact(old,1)` before writing; overflow throws `ArithmeticException` and leaves the slot unchanged |
| Long `addAndGet(slot,delta)` | Same checked arithmetic with a caller-supplied signed long delta; both overflow and underflow are refused before writing |
| `clear(slot)` | Equivalent to writing zero, retaining allocated storage |
| `isBlockAllocated(slot)` | Checks whether the containing page exists, not whether the slot is nonzero |
| `allocatedRegionCount()`, `allocatedBlockCount()` | Count directories/pages allocated so far; zeroing does not reduce either count |
| `payloadBytes()` | Primitive leaf payload only, using long multiplication; excludes root/region references and all array/object headers |
| `fill(from,to,value)` | Checked half-open range; nonzero fill touches/allocates every intersected page, zero fill skips absent regions/pages |
| `copyTo(from,output,outputFrom,length)` | Checks nonnull output, source-lane range, then output-array range before writing; absent source spans become zeros in caller-owned output |
| `copyFrom(sourceArray,sourceFrom,to,length)` | Checks nonnull source, source-array range, then lane destination range; copies payload without retaining the input array |
| `copyFrom(sourceLane,sourceFrom,to,length)` | Checks nonnull source and both lane ranges before writing; supports same-owner overlap in either direction without a flattened temporary payload array |

Both `copyFrom` overloads consider each destination page separately. `copyPage` scans the corresponding source span when the destination is absent and allocates only if at least one copied value is nonzero. An absent source lane page clears the corresponding span of an existing destination page. A distinct lane never lends its page array to the destination; this is copying, not page aliasing. Modifying the source array or source lane after the copy does not mutate the copied values.

For self-overlap with destination inside and above the source range, copying proceeds backward; otherwise it proceeds forward. Each chunk respects both source and destination page boundaries and uses `System.arraycopy` within its page. Logical overlap therefore has `memmove` semantics. `copyTo` and array import use page-bounded chunks as well. A zero-length operation still checks null and ranges; valid empty endpoint copies do no payload work.

The private `fillZero` helper returns immediately when there are no allocated blocks, but the public range check happens first. It otherwise skips absent region directories and zeroes only existing pages. `blockOrNull` reads the directory, `ensureBlock` allocates and publishes missing directory/page storage, and `copyPage` owns the sparse-write decision. Their bounds assumptions come from the checked public entry points and address helpers.

Bounds and checked-arithmetic failures occur before destination writes. Allocation failure is not transactional: an earlier page may already have been copied/filled, and a new region directory can remain published if the later page allocation fails. There is no rollback or capacity reservation. Repeated writes keep existing page objects stable, but this is neither a no-allocation claim for new pages nor an ownership guarantee for reflective/internal array extraction.

Each int leaf has 16 KiB primitive payload; each long leaf has 32 KiB. A fully allocated logical domain would contain 1 GiB or 2 GiB of primitive payload respectively, plus directories/headers. Those are layout calculations, not measured retained-heap sizes or a promise that allocation will succeed. The logical maximum is independent of the heap budget.

Every operation is unsynchronized, including reads, counters and read-modify-write methods. The owners provide no volatile publication, atomicity, concurrent growth protection or snapshot iterator. Callers must confine a lane or provide appropriate external synchronization. A method name containing `AndGet` does not make it a concurrent collection. The separately named `com.m3.collections` owners are another package and are not reviewed as interchangeable implementations here.

## Segmented bit lane

The complete [bit lane][bits] uses the same 28-bit domain. Each allocated logical page contains 64 long words, 512 bytes for 4,096 bits. Offset `o` selects word `o >>> 6` and mask `1L << (o & 63)`: low-numbered offsets use low-order bits. This is bit arithmetic, not a persisted byte layout. The root directory is eager; pages are sparse and retained after clearing.

| Declared operation | Observable contract |
| --- | --- |
| `get(slot)` | Checked bit read; absent region/page yields false |
| `set(slot)`, `set(slot,boolean)`, `clear(slot)` | Set/clear the bit; redundant writes do not change cardinality, false writes do not allocate absent storage |
| `flip(slot)` | `get` followed by set/clear; not an atomic toggle |
| `cardinality()` | Maintained total set-bit count |
| `allocatedRegionCount()`, `allocatedBlockCount()`, `payloadBytes()` | Storage counts and bitmap payload; excludes rank/select metadata and all directories/headers |
| `nextSetBit(from)` | First set slot at or after `from`, or `-1`; accepts `from == MAX_SLOTS` as an empty suffix |
| `previousSetBit(from)` | Last set slot at or before `from`, or `-1`; accepts `-1` as an empty prefix but rejects `MAX_SLOTS` |
| `cardinality(from,to)` | Count in a checked half-open range, skipping absent directories/pages; the whole-domain range returns the maintained total |
| `rank(end)` | Exclusive count below `end`; accepts both endpoints from zero through `MAX_SLOTS` |
| `select(ordinal)` | Slot of the zero-based ordinal set bit; returns `-1` for negative or too-large ordinals rather than throwing |
| `cardinalityNative(from,to)` | Checks the Java range, then explicitly invokes the native peer; no automatic Java fallback |

Traversal masks the first partial word, then scans remaining words/pages, skipping absent regions. `previousSetBit` masks in the opposite direction. Range cardinality masks each partial word; after preparation it can use cached counts for complete pages and regions. Counts are in `[0,2^28]` and fit an int; traversal/select sentinels are described above.

### Lazy rank/select facts

`prepareCounts` scans allocated bitmap words into a temporary `int[256]` of region totals and a `short[256][]` page-count directory. A 256-short page-count array exists only for an allocated region. Each page count is in `[0,4096]`, which fits a signed short; a region total is at most 2^20 and the global total at most 2^28. The maximum primitive metadata payload is 1 KiB of region counts plus 128 KiB of page counts, excluding the reference directory and headers. There is no second bitmap copy.

`rank(0)`, rank on an empty lane, and `rank(MAX_SLOTS)` return without preparing counts. Out-of-range `select` also returns without preparation. A valid select or other nontrivial rank prepares once. `cardinality(from,to)` can use existing counts but does not create them by itself.

`changeCardinality` maintains total and, once prepared, region/page facts only when an actual bit changes. `ensureBlock` allocates page-count metadata before publishing a new region when the count index already exists. The initial `prepareCounts` publishes its two field references only after all allocations/scans succeed, so failure during that preparation leaves the prior owner state unchanged under the single-threaded contract. Later growth can leave an allocated zero region after page-allocation failure; it does not set a bit before storage exists.

Select subtracts region, page and word populations until it reaches the requested ordinal, then clears lower set bits locally to locate that bit. Its terminal `IllegalStateException("concurrent mutation of a single-threaded lane")` is an invariant failure, not reliable concurrent-modification detection. Reads can race with writes or lazy preparation; there is no thread-safe rank/select publication protocol. No fill, copy, reset or serialization method is declared on this owner.

### Native counting boundary

[`M3BitLane28.c`][native] implements private static `cardinality0(long[][][],int,int)` in `libjava`. It checks a nonnull root of exactly 256 elements and a valid range. For visited nonnull regions it requires 256 page entries; for visited nonnull pages it requires 64 words. Malformed directory/range input to this native boundary raises `IllegalArgumentException`. The public Java wrapper instead rejects an invalid range first with `IndexOutOfBoundsException`. Unvisited subtrees are not exhaustively validated.

The C code obtains local references, copies the needed word interval with `GetLongArrayRegion` into a stack `jlong[64]`, applies unsigned 64-bit endpoint masks and a portable population count, and deletes its local references. The final count fits `jint`. It does not flatten the logical domain, pin a Java array across calls, retain a native pointer, call back into Java user code, or mutate the bitmap. Pending JNI exceptions are checked and propagated; a zero C return on that path is not an ordinary successful zero count.

There is no library loader or unavailable-native catch in the lane. If the symbol is not linked into the selected runtime, normal linkage failure propagates. The [lane tooling README][lane-tooling] distinguishes stock-JDK patch tests with a test-only loader from a rebuilt image's production `libjava`; neither invocation was executed for this documentation pass. The native operation still needs a stable single-threaded owner and has no speed guarantee or cross-platform qualification implied by its presence.

## Class bridge M3CB

The complete [class bridge][cb] owns a particular `M3CI` reference, fixed `Mode`, two index-sized reference arrays for declared releases/keys, counters, an epoch and validity/closed flags. Its constructor allocates arrays even for `OFF`. `over(index,mode)` reuses prepared metadata; `prepare(roots,options,mode,monitor)` delegates admission to `M3CI.compile` and then creates the bridge. It does not create a process-wide live-class registry or attach itself automatically to `java.lang.Class` dispatch.

See [class snapshot admission](class-index-reference.md#snapshot-admission-and-closure) and [identity/live-boundary rules](class-index-reference.md#identity-and-live-boundary). In the inspected `M3CI.fromClass` body, a defining-loader coordinate and name nominate a row, then the weak boundary must contain the exact input `Class` object. Equal spelling alone is insufficient. Compact snapshots have no live mirror handles, so bridge lookups requiring them cannot reuse a candidate even though direct frozen metadata remains usable.

### Modes and read operations

`Mode` declares exactly `OFF`, `OBSERVE`, `ENABLED`. Their effect is on `name` and `isAssignableFrom`, not every bridge method:

| Operation | Mode and lifecycle behavior |
| --- | --- |
| `epoch()` | Returns current epoch, including after close |
| `fromClass(type)` | Requires open and nonnull type; delegates to the index while valid, otherwise returns empty; it does not test `Mode.OFF` |
| `name(type)` | OFF, invalid or missing live row: increment fallback and use `Class.getName`; ENABLED: return prepared name and increment reused; OBSERVE: compare prepared name with authoritative JDK result, increment observed, return authoritative result |
| `isAssignableFrom(target,candidate)` | Same policy; both classes must resolve live before indexed hierarchy is used; fallback delegates to `Class.isAssignableFrom` |
| `statistics()` | Returns a fresh `Statistics(reused,observed,fallback,mismatches)` record, including after close |

An OBSERVE mismatch increments mismatches and invalidates the whole session before returning the authoritative result. The implementation does not roll back counters or silently rebuild. Counter increments are ordinary long arithmetic, not checked arithmetic. Statistics count these two accelerated operations; metadata lookup, declaration and provenance projection do not increment those operation counters.

### Declaration, freshness and retirement

`DeclaredAttachment` is a public record with components `session`, `type`, `release`, `candidateKey`, `epoch`. Its compact constructor rejects null in the first four components. The record's constructor/accessors and generated record operations are not authentication. A caller can construct a projection; freshness is separately checked by the owning bridge. `Statistics` is a second public record with four long components and no custom validation.

`declare(type,release,key,expectedEpoch)` requires an open, valid bridge, current epoch, nonnull release/key/type, and a class found in the live snapshot. A stale/invalid session throws `IllegalStateException("stale session")`; a class outside the live snapshot throws `IllegalArgumentException`. If that row is already declared, the supplied release must be value-equal and its key must satisfy `sameInputs`, otherwise an `IllegalStateException` rejects the conflict. Equivalent repeated declarations retain the original stored release/key references and return a fresh attachment carrying those references. This publication is synchronized, including in OFF mode.

`effective(type)` returns a fresh current projection only for a valid bridge, live indexed class and populated declaration row; otherwise it returns empty. `effectiveVersion(type)` maps that projection to its release. They do not use the mode as a gate. The release is caller-declared provenance, not an authenticated class-file digest, permission to redefine a class, resolution result or selected implementation.

`isCurrent(attachment)` rejects null, then returns false for a closed/invalid bridge, a different session object, wrong epoch or a view owned by another index. It requires the row's stored release and key to be the exact references carried in the attachment and the row's weak class mirror still to exist. An equal but separately constructed release/key is not enough for this check. It tests freshness at that instant; it does not grant a lease against later invalidation or collection.

`invalidate()` requires an open bridge. On its first valid-to-invalid transition it increments epoch and clears both declaration arrays. Repeated invalidation does nothing further, and no method restores validity. The epoch therefore distinguishes that bridge's original live session from retirement; it is not a universal generation identifier. `close()` synchronizes and is idempotent, invalidating once before marking closed. It does not close or erase the immutable index, explicitly unload classes, or revoke metadata views/attachments held elsewhere. Such projections retain their bridge/index references, so they can extend metadata lifetime even though the boundary weakly references classes/loaders.

After close, `fromClass`, `name`, `isAssignableFrom`, `declare`, `effective`, `effectiveVersion` and `invalidate` fail via `requireOpen` with `IllegalStateException("session closed")`; `epoch`, `statistics`, `isCurrent` and repeat `close` remain callable. The order of checks matters: for example, closed `name(null)` fails as a closed session before checking the null parameter.

All instance operations above synchronize on the bridge, making its own array/counter/state changes serialized. This does not synchronize the VM's redefinition/retransformation, guarantee that a weak reference survives after return, or automatically detect every lifecycle event. Callers must invalidate before unsupported lifecycle changes and explicitly prepare a replacement. `prepare` invokes `M3CI` callbacks before the session exists; its synchronization cannot protect that admission process. Null, budget, reflection, callback and allocation failures from index preparation propagate according to that owner.

## Descriptor writer M3Descriptor

The complete [descriptor helper][descriptor] is a stateless static utility with a private constructor. `type(Class<?>)` null-checks and calls `Class.descriptorString()` directly. It does not derive a descriptor by replacing dots in `Class.getName`, parse descriptor text, resolve classes, inspect generic signatures or establish loader identity.

`field(Field)` and `recordComponent(RecordComponent)` encode the reflected value type. `method(Method)` concatenates the parameter descriptors inside parentheses and then the return descriptor. `constructor(Constructor<?>)` uses the same private `executable(Class<?>[],Class<?>)` helper with `void.class` as the return type. Every public method null-checks its argument with the corresponding name (`type`, `field`, `method`, `constructor`, `component`) and returns an ordinary String. The helper's `StringBuilder(16 + parameters.length * 8)` capacity is a growth hint, not a descriptor-length budget or fixed-width record.

The VM owns primitive, array, nominal and hidden-class spellings. A hidden descriptor need not be acceptable to nominal `ClassDesc.ofDescriptor`, resolvable by name, or portable to another VM session. Distinct loader definitions with the same nominal name may have equal descriptor text. A descriptor does not encode a defining-loader namespace, generic types, annotations, parameter names, declared exceptions or executable behavior.

The inspected `M3CI` admission and live-member search excerpts use these descriptors together with owner/name/kind information; the descriptor is not an independently universal member ID. Reflection/linkage/security/allocation failures are not caught by this helper. `M3CI.admitMembers` separately wraps `LinkageError` and `SecurityException` at its own admission boundary. Encoding metadata here does not invoke the reflected method or constructor; non-initialization is exercised by the dedicated test described below.

## Candidate context key M3PC

The complete [context-key owner][pc] exposes `of(contentSeal,analysisRevision,environmentSeal,orderedDependencies)`, `dependencyCount()` and `sameInputs(other)`, plus a private constructor. `of` rejects a null dependency list and lists larger than 65,536 entries. It allocates a new String reference array of `dependencyCount + 3`, writes the three required nonnull context Strings, then copies nonnull dependency references in order. It does not retain the list itself or deep-copy immutable String payload.

The positional lanes are content seal, analysis revision, environment seal, then ordered dependencies. No field is parsed as a digest or checked for emptiness, character format or maximum String length. The dependency-count budget bounds only the reference count, not total referenced String payload or external work. A concurrently modified dependency list is outside the stable-input assumption; repeated `size`/`get` calls do not form an atomic input snapshot.

`sameInputs` returns false for null and otherwise uses `Arrays.equals` over the complete String arrays: String value equality, exact lane count, exact order and exact spelling. Reordered dependencies, changed seals or changed analysis/environment text change the comparison. It does not override `Object.equals`/`hashCode`; putting two separately constructed equal-input keys in an ordinary hash map does not automatically deduplicate them. `dependencyCount` excludes the three fixed lanes.

The owned array is private and never returned, so ordinary callers cannot mutate a completed key through the factory's original list. There is no retained result cache, parser, work execution, signature verification, linker/native-code admission or portable serialization format. A complete effective dependency/environment context is the caller's obligation; the helper cannot detect omitted inputs. `M3CB.declare` uses `sameInputs` for an idempotent declaration, then `isCurrent` uses the retained key's reference identity when checking a projection.

## Version index M3VI

The complete [version-index owner][vi] is generic in parsed version type `T`. Its nested `Scheme<T>` declares `revision()`, `parse(String)` and `compare(T,T)`. The scheme owns grammar and precedence; the kernel does not silently impose SemVer, Maven comparison, module resolution, dependency selection or class implementation routing. `jpms21()` creates a scheme whose revision is `jpms-version-java21`, parser is `ModuleDescriptor.Version.parse`, and comparator is `Version.compareTo`.

Its nested `Progress` provides no-op defaults for `begin(task,total)`, `checkCancelled()`, `worked(work)` and `done()`, plus `none()` returning a shared no-op instance. Null monitor means this no-op monitor. Callbacks are synchronous caller code; cancellation is expressed by throwing, not a boolean flag or a background worker. The same callback interface is reused by `M3CI.compile`/`M3CB.prepare`, not evidence that they share the same progress totals or preparation algorithm.

### Preparation and budgets

`prepare(versions,scheme,maxVersions,monitor)` performs these steps:

1. Require nonnull versions and scheme; require `maxVersions` in `[1,1_000_000]` and the input list size no larger than that maximum. An empty input is permitted with a positive maximum.
2. Read a nonnull, nonempty scheme revision, copy String references into a new label array, and allocate an `ArrayList<T>` plus boxed `Integer[]` temporary order.
3. Call `begin("M3VI.prepare",labelCount)`. Inside the subsequent try/finally, call `checkCancelled` before each label, reject null labels and lengths above 16,384 UTF-16 units, parse once into a required nonnull `T`, initialize its original-row index, then call `worked(1)`.
4. Sort temporary rows by the scheme comparator, breaking comparison equality by original input row. The comparator path calls `checkCancelled`; another cancellation check follows sorting.
5. Copy sorted rows into an `int[]`, require the scheme's revision still to equal the initial revision, and retain `List.copyOf(parsed)` with the label/order arrays and scheme. `done()` executes in the finally block.

Validation, revision lookup, input copy/allocation and `begin` occur before that try/finally. A failure there does not promise a `done` call. Once the try is entered, parsing/comparison/cancellation/revision failures reach `done`; an exception from `done` can itself replace a result or pending failure because it is not suppressed/caught. No partial index is returned. No rollback is provided for side effects of caller callbacks or schemes.

The fixed version count and per-label length are admission budgets, not a maximum total heap estimate. Preparation holds labels, parsed objects, boxed ordering values, a primitive output order and sorting/list storage; parsed representations and comparator work are scheme-dependent. The scheme revision itself has no maximum length here. Input lists must be stable during copying; there is no concurrent-list snapshot protocol.

### Rows, ranges and equality

| Declared query | Meaning |
| --- | --- |
| `size()` | Number of input rows, retaining duplicates |
| `schemeRevision()` | Captured revision String, not a fresh scheme query |
| `originalRow(sortedPosition)` | Checks sorted-position bounds and returns the corresponding original row |
| `label(originalRow)` | Checks original-row bounds and returns the original immutable String reference |
| `between(lower,includeLower,upper,includeUpper)` | Parses two required bounds and returns a fresh `int[]` of original rows in stable precedence order |

Comparison equality never removes rows or normalizes labels: `"1"`, `"1.0"` and `"1.0.0"` can compare equal in JPMS while remaining distinct raw labels and original coordinates. Exact duplicate labels also remain separate input rows. The stable tie-break is original row, not lexical spelling or content identity.

`between` first calls `requireRevision`, then `parseBound` for each endpoint. Null bounds or null parsed values throw `NullPointerException`; each spelling over 16,384 UTF-16 units throws `IllegalArgumentException`. The scheme may reject its own grammar. If comparison says lower is greater than upper, the method rejects reversed bounds. These checks/parses happen even for an empty index.

The private `bound(value,afterEquals)` performs binary search over the sorted row lane. The lower search skips equals only when the lower endpoint is excluded; the upper search skips equals when the upper endpoint is included. If the resulting upper position is not greater than the lower, an empty array is returned. Otherwise `Arrays.copyOfRange` copies the matching original rows. Stored versions are not reparsed by a warm query; the two query bounds are. Search uses logarithmically many comparisons plus output-size copying, subject to the scheme's own parse/compare costs. There is no query progress/cancellation callback or result-count limit beyond the prepared row count.

### Conditional immutability and concurrency

The container arrays/list are privately owned and not exposed for mutation. The input String objects, caller scheme and parsed `T` objects are retained by reference; `List.copyOf` is a structural copy, not a deep copy of parsed values. Schemes and their semantic revisions must remain deterministic and immutable, with stable comparison semantics. The helper cannot enforce those properties or make a mutable `T` safe.

Revision equality is checked at the end of preparation and at the start of each `between`. It is not checked on `size`, `label` or `originalRow`, nor continuously during a range query. A scheme can change behavior without changing its revision String, or change after the initial query check; neither is a supported immutability guarantee. There is no internal synchronization. Concurrent readers require a safely published index and a thread-safe immutable scheme/parsed-value contract; the revision check is not a lock. There is no close, refresh, automatic reprepare or persistence operation.

## Source evidence and scope boundaries

| Full owner body | Exact blob |
| --- | --- |
| [`M3IntLane28.java`][ints] | `513a1e8f5d55c074250d3b2747f041c19a1128e7` |
| [`M3LongLane28.java`][longs] | `325e8e90ace21e71069665b74585c522b2c627b4` |
| [`M3BitLane28.java`][bits] | `5942b1f46eedf83ad8c4ef8edee91f75e7424e7a` |
| [`M3CB.java`][cb] | `0be655b7020f13f3df4265f68daf189bd039d86b` |
| [`M3Descriptor.java`][descriptor] | `92f4d7da32a3b8265675a413311fd304c6dc2dd2` |
| [`M3PC.java`][pc] | `b63fd9e457f7ac16bca8bf07dcb564069fdfef79` |
| [`M3VI.java`][vi] | `087aa55bef9b57189d03f3388fb2e1f360320b02` |

All explicit public/private methods, overloads, constructors, nested interfaces, enum and records in these seven bodies were reviewed. The no-argument lane constructors are implicit; record/enum generated members retain Java language behavior. Fully read supporting bodies were `M3Address28`, `M3Bits`, `M3Boundary`, `M3Release` and the native `M3BitLane28.c`; these supporting reads do not enlarge this page's seven-owner denominator.

Scoped `M3CI` reads covered compile/progress and basic owner methods (149–188), indexed assignability (298–322), live class identity (360–376), descriptor-based member resolution (626–669), member admission and exception descriptors (953–1035). Scoped `M3TQ` reads covered its independent array/list query representation (1–130); those fields must not be mislabeled `Lane28` storage. These excerpts are not full reviews of either owner; their full references are [class index](class-index-reference.md) and [trigram queries](trigram-query-reference.md). No automatic `java.util` backend substitution or global `Class` dispatch consumer is established by these helper declarations.

## Actual tests read and remaining verification

These six complete Java test/probe bodies were inspected. Their assertions describe intended checks, not newly executed passing results:

| Test or harness | Assertions and limits visible in the body |
| --- | --- |
| [`M3Lane28Test`][lane-test] | Address round trips/refusals; seeded int/long operations against primitive arrays and `System.arraycopy`; overlap, input-array detachment, zero allocation, overflow/no-write refusals, stable page identity, sparse last/page/region slots; bitmap checks against `TreeSet`/`BitSet`, lazy and maintained rank/select counts, Java/native endpoint parity, and seven malformed native calls. Declares an `othervm` run with internal-package opening. Its optional `lane28.native` loader path is test-only. |
| [`M3SparseFillTest`][sparse-test] | Zero/nonzero bounds, empty end ranges, page/region/end seams, retained allocation counts after clearing, seeded sparse ranges, overlap and checked overflow for both numeric lanes. Its argument-selected timing path is separate from the default correctness entry point and supplies no new measurement here. |
| [`CBProbe`][cb-test] | All three modes, name/assignability parity, defining-loader separation, hidden/primitive/array classes, OFF-mode live lookups/declarations, idempotent/conflicting/stale declarations, concurrent declarations, effective versions, compact fallback, repeated close, prepare cancellation and a bounded GC-unloading attempt. Also tests version range/stability/parse counts/revision/cancellation and `M3PC` dependency order/length/value comparisons. Declares `-Xint` and `-Xmixed` runs with class unloading. |
| [`DescriptorTest`][descriptor-test] | VM descriptor and `MethodType` comparisons for primitives, nominal/local/anonymous/hidden/lambda classes, members/records and array ranks through 255; nominal/hidden distinction, same-name different-loader definitions, all five null diagnostics and dormant-class non-initialization. Declares default, interpreted and non-compact-String modes. |
| [`VIProbe.java.txt`][vi-probe] | A generated-probe resource, not an independently installed jtreg test. Exercises JPMS range oracles, empty input, preserved label references after caller list clearing, stable equal precedence, bound-only parsing, owned output arrays, range/null/budget errors, cancellation cleanup and revision drift during preparation/query. It also checks exact release identity. |
| [`VITest`][vi-test] | Recipe harness that generates VI/release/probe source, checks fixed point and retained target equality, prepares strict Java-21 patch-module compilation and interpreted/mixed probe execution, and checks dependency/source/license pins, refusal/tamper cases and bootstrap inventory. Reading this harness is not running its subprocesses or validating its historical logs. |

Precise remaining gaps in these inspected tests include allocation-failure injection for lane growth/copies/count preparation; full-domain dense-memory limits; negative long-add underflow as a dedicated assertion; concurrent lane mutation (unsupported); native behavior across architectures and a rebuilt selected image; `M3CB` forced OBSERVE mismatch and exhaustive post-close/null ordering; actual redefinition/retransformation invalidation; `M3PC` maximum/null-element/empty-seal cases and factory-list races; mutable parsed version values or a changed scheme that keeps the same revision String; callback exceptions from `begin`/`done`; and concurrent revision changes within a range query. No test read here authenticates release seals or proves all class lifecycles.

The [lane tooling][lane-tooling] and [VI tooling][vi-tooling] pages provide their existing build/proof entry points. Use a matching runtime and the [native build/test routes](../../m3/docs/native-build.md#current-checkout-verification-routes) for fresh qualification. This pass executed only documentation/source-evidence checks. Repository builds, compilation, Maven/JUnit, jtreg, recipe/policy verifiers, native runtime calls, GC tests, image linkage and resource/performance measurements were **NOT_RUN**. Full-body documentation coverage does not by itself close module, runtime, license-admission or repository verification gates.

[ints]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3IntLane28.java
[longs]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3LongLane28.java
[bits]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java
[cb]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3CB.java
[descriptor]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3Descriptor.java
[pc]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3PC.java
[vi]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/classes/jdk/internal/mindex/M3VI.java
[native]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/src/java.base/share/native/libjava/M3BitLane28.c
[lane-test]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/test/jdk/jdk/internal/mindex/M3Lane28Test.java
[sparse-test]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/test/jdk/jdk/internal/mindex/M3SparseFillTest.java
[cb-test]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/test/jdk/jdk/internal/mindex/cb/com/m3/cb/CBProbe.java
[descriptor-test]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/test/jdk/jdk/internal/mindex/descriptor/com/m3/descriptor/DescriptorTest.java
[vi-probe]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/m3/tooling/vi/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-vi/VIProbe.java.txt
[vi-test]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/m3/tooling/vi/src/test/java/com/m3/vi/VITest.java
[lane-tooling]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/m3/tooling/lane28/README.md
[vi-tooling]: https://github.com/hsoliwal/M3jdk21/blob/e7d21edf0f998c3c53ab0d0909972a4a97dfc471/m3/tooling/vi/README.md
