<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Stable segmented primitive storage

[Guide](README.md) · [Factories and delivery](reconcile-factories-and-delivery-scope.md) · [Primitive sequences](primitive-sequence-reference.md) · [Atomic storage](specify-atomic-primitive-storage.md) · [Coverage](coverage.md)

This reference covers eight **public `com.m3.collections` owners**: address
geometry, ordinary int/long/bit lanes, a reusable-slot allocator, an append
arena, a multi-lane row store and a handle-linked long list. They are separate
from the similarly named `jdk.internal.mindex` owners covered by the
[internal lane reference](internal-metadata-lanes-reference.md) and
[mapped-storage reference](mapped-string-storage-reference.md).

The complete bodies and declared members were reviewed at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`; all eight source blobs and both
direct test blobs match audit pin `1011050da4205a901ad079d7711b6b8efd8a9af7`.
The [source and member map](#complete-declared-member-map) identifies the
bounded review. Tests, examples, builds, scripts, recipes, native libraries
and benchmarks are **NOT_RUN in this documentation pass**. Source observations
are not runtime qualification or performance measurements.

## Choose the identity before the representation

| Owner | Address / ownership | Main use |
| --- | --- | --- |
| [`M3Address28`][address] | Stateless decomposition of a logical int slot | Shared geometry and checked ranges |
| [`M3IntLane28`][ints], [`M3LongLane28`][longs] | Caller-chosen logical slot; zero in absent storage | Sparse primitive values without membership tracking |
| [`M3BitLane28`][bits] | Caller-chosen Boolean slot | Presence/flags, rank, select and set-bit traversal |
| [`M3SlotAllocator28`][slots] | Allocator-scoped slot plus generation | Reusable live slots and stale-handle rejection |
| [`M3LongArena28`][arena] | Arena-local append offset, no generation | Append-growing data with stable offsets |
| [`M3LongLaneStore28`][rows] | Store-local allocator handle plus lane number | Stable rows whose neighbors do not swap-move on release |
| [`M3LongLinkedList28`][list] | List-local handle; order in next/previous links | Primitive linked sequence with known-handle edits |

`M3Collections.longLane/intLane/bitLane/stableSlots/longRows/longArena/longLinkedList`
directly construct these owners; [factory contracts](reconcile-factories-and-delivery-scope.md)
own the wider factory surface. The address utility has a private constructor;
the other seven have public constructors, with only the row store requiring an
argument (`laneCount > 0`, otherwise `IllegalArgumentException`).

The [JPMS descriptor][module] exports `com.m3.collections` and has only implicit
`java.base` dependency. The standalone [POM][pom] declares Java release 21,
`com.m3:m3-collections:0.1.0-SNAPSHOT`, compiler 3.13.0 with
`-Xlint:all,-module` / `-Werror`, JUnit `5.10.2-m3-source-1`, and Surefire 3.2.5
with module-path testing disabled. Dependency resolution is not verified here.
The [control POM][reactor] includes collections through its `m3-extended`
profile, not its unconditional module list. See the factory/delivery reference
for current-source versus installed-manifest versus historical-receipt scope.
These public independent APIs do not establish installed-JDK delivery or
replacement of any `java.util` collection.

All eight owners are unsynchronized. Ordinary `getAndSet`, bitmap updates,
allocator counters and multi-lane edits contain no atomic publication protocol.
Externally coordinate ownership and publication. Atomic lane contracts belong
to the separate [atomic-storage reference](specify-atomic-primitive-storage.md).
None of the eight implements a JDK collection interface, `Iterable`, a stream
API, `AutoCloseable`, serialization, or content-based equality. The nested
`HandleConsumer` is a callback interface, not an iterator.

## Address geometry and range checks

`REGION_BITS/BLOCK_BITS/OFFSET_BITS` are `8/8/12`; `REGION_COUNT` and
`BLOCKS_PER_REGION` are 256, `VALUES_PER_BLOCK` is 4,096, and
`OCCUPANCY_WORDS_PER_BLOCK` is 64. `MAX_SLOTS` is 268,435,456 (`1 << 28`),
while `MAX_SLOT` is 268,435,455. `OFFSET_MASK` is 4,095 and `BLOCK_MASK` is 255.

`region(slot)`, `block(slot)` and `offset(slot)` first call `checkSlot`, then
extract bits 20–27, 12–19 and 0–11. `compose(region,block,offset)` checks every
component, including negative values, before combining it. Component/slot
violations throw `IndexOutOfBoundsException`.

`checkRange(from,to)` validates `0 <= from <= to <= MAX_SLOTS` using
`Objects.checkFromToIndex`. `checkRangeSize(offset,length)` uses
`Objects.checkFromIndexSize`, avoiding unchecked `offset + length` overflow.
An empty range at `MAX_SLOTS` is valid; a scalar access there is not. Neither
an arena offset nor a raw physical slot carries membership or owner identity.

## Ordinary value lanes: sparse zero and checked bulk copies

Each int/long lane eagerly owns a 256-reference root directory. A region
directory of 256 references and a 4,096-value leaf appear lazily on a nonzero
write. `get` returns zero for an absent region or leaf; zero is also a valid
stored value. A lane has no independent live/nonzero count.

`set(slot,0)` and `clear(slot)` leave absent paths absent. Clearing a populated
leaf never frees it. `isBlockAllocated(slot)` reports physical leaf presence,
not whether that slot is nonzero. `allocatedRegionCount` and
`allocatedBlockCount` count allocated arrays, remain after clearing, and are
not occupancy counts. Private `blockOrNull` reads the directory; `ensureBlock`
creates a missing region and then a missing leaf, updating the counters.

`getAndSet` is a plain get followed by set and returns the former value.
`M3IntLane28.incrementAndGet` uses `Math.addExact(old,1)`;
`M3LongLane28.addAndGet` uses `Math.addExact(old,delta)`. Arithmetic overflow
throws `ArithmeticException` before storing the new value. There is no wrapping
increment/add contract and no atomic read-modify-write guarantee.

| Bulk member on both lanes | Behavior and checks |
| --- | --- |
| `fill(from,to,value)` | Validates the half-open logical range first; fills one leaf segment at a time; zero never allocates absent leaves |
| `copyTo(from,output,outputFrom,length)` | Requires nonnull output, checks source logical range and destination array range before writing; absent source pages become output zeros |
| `copyFrom(array,sourceFrom,to,length)` | Requires nonnull source, checks array range then logical destination range before writing; copies values without retaining the caller array |
| `copyFrom(lane,sourceFrom,to,length)` | Requires nonnull source and checks both logical ranges first; supports self-overlap in either direction |

Private `copyPage` allocates a missing destination leaf only if the source
segment contains a nonzero value. A null source page clears an existing
destination segment and leaves a missing destination page absent. Copying zeros
therefore propagates values without filling holes. Existing destination pages
are retained even when the result becomes all zero.

For self-copy with a higher overlapping destination, the loop walks backward;
otherwise it walks forward. Each chunk fits both source and destination leaf
boundaries and uses `System.arraycopy`, providing memmove-like overlap behavior.
No temporary whole-range payload array is created. Null inputs are rejected
even for length zero; valid zero-length operations do not touch a leaf.

Bounds/null failures happen before destination writes. This is a bounded
validation guarantee, **not transactional mutation**: allocation failure can
leave earlier chunks changed, or a newly installed empty region directory.
The same absence of rollback applies to nonzero fill and scalar allocation.
Stable pages mean that growth does not copy old leaves; it does not mean that
reads, writes or bulk operations are safe during concurrent access.

## Bitmap occupancy, rank and select

Each allocated bit leaf has exactly 64 `long` words for 4,096 flags. Absent
regions/leaves read as false. `set(slot)` is `set(slot,true)`;
`clear(slot)` is `set(slot,false)`. True-setting allocates as necessary and
increments `cardinality` only for a false-to-true transition. False-setting
does not allocate, and decrements only for a true-to-false transition.
`flip` composes get with set/clear and is not atomic. No operation frees a leaf.

| Member | Range / result |
| --- | --- |
| `cardinality()` | Maintained total number of set slots |
| `cardinality(from,to)` | Set bits in `[from,to)`; full-space range returns the maintained total |
| `rank(end)` | Exclusive rank, exactly `cardinality(0,end)`; `end == MAX_SLOTS` is valid |
| `select(ordinal)` | Slot of the zero-based set-bit ordinal, or `-1` for negative/out-of-range ordinal |
| `nextSetBit(from)` | First set slot at or after `from`; accepts `0..MAX_SLOTS`; returns `-1` when absent |
| `previousSetBit(from)` | Last set slot at or before `from`; accepts `-1..MAX_SLOT`; returns `-1` for an empty prefix |

Traversal skips absent region/leaf directories. Range count masks first/last
words and uses `Long.bitCount`; select scans allocated regions, leaves and
words in ascending address order, subtracting word populations. There is no
separate rank/select index or constant-time select guarantee. Its final
`IllegalStateException("concurrent mutation of a single-threaded lane")` is
a defensive inconsistent-state check, not reliable concurrent-modification
detection or a synchronization contract. `cardinalityNative` is an explicit
separate JNI path described [below](#optional-jni-counting-boundary).

## Stable handles: generation and owner scope

A handle uses high 32 bits for a generation and low 28 bits for the physical
slot. Low-half bits 28–31 must be zero. `slot(handle)` examines all low 32 bits
and returns `-1` when the unsigned low half is outside the slot space.
`generation(handle)` returns the high half as an `int` bit pattern;
negative generations are allowed. `handle(slot,generation)` checks the slot
and rejects generation zero with `IllegalArgumentException`. It constructs
bits only: it does not allocate, mark live or authenticate ownership.

The allocator owns `generations`, `freeNextEncoded`, `live`, `highWater`,
`freeHeadEncoded` and `size`. Fresh slots are sequential from zero with
generation one. Reusable free slots are a LIFO chain storing `slot + 1`,
with encoded zero as the empty-chain sentinel. `allocate` pops that chain
before extending the high-water mark; it clears the popped free-link value,
checks for an unexpected zero generation, marks the slot live, increments
size and returns its handle. At a full high-water mark with no free slot it
throws `IllegalStateException`.

`release` first validates liveness/generation. It then clears the live bit,
increments the generation, pushes the slot onto the free chain and decrements
size. Generation therefore changes **at release**, not at the later reuse.
Signed integer rollover from `Integer.MAX_VALUE` to `Integer.MIN_VALUE` is
permitted; the bit pattern `0xffffffff` (`-1`) is the last generation.
Releasing that generation throws `IllegalStateException` **before the
allocator mutates state**, refusing wrap back to reserved zero. Exhaustion
does not automatically retire/reclaim the still-live slot.

`isLive` returns false for malformed/out-of-high-water/free/stale handles.
`requireLiveSlot` throws `IllegalStateException` for those categories.
`isSlotLive` returns false for an invalid/out-of-high-water raw slot;
`generationAtSlot` instead throws `IndexOutOfBoundsException` outside
`[0,highWater)` and returns the current generation even for a free slot.
`handleForLiveSlot` throws for a nonlive slot. `nextLiveSlot(from)` checks
`0..MAX_SLOTS`, returns `-1` at/above high water, otherwise searches the live
bitmap inclusively. `size/isEmpty` reflect live occupancy; `highWaterMark`
does not shrink; `freeSlotCount` is `highWater - size`.

**Handles are allocator-scoped by caller discipline, not by encoded identity.**
Two fresh allocators can issue the same numeric handle. A handle from another
allocator/store/list with matching slot and generation can pass validation.
The list's error text about a “foreign” handle does not implement a namespace
check. Raw handle construction, extraction and numeric comparison cannot
establish provenance. Keep the owning allocator/store/list with the handle;
do not treat it as a portable ID, security capability or native address.

Allocator allocation/release is not allocation-failure-atomic. For example,
fresh allocation increments high water before generation/bitmap allocation;
free-list pop occurs before live-bit publication; release clears liveness and
updates generation before a free-next leaf might need allocation. There is
no rollback or recovery API for a partially failed mutation. These are
source-observed failure boundaries, not injected-failure test results.

## Arena offsets and callback mutation

`M3LongArena28` owns one long lane and an int size. `append(value)` returns the
previous size as the offset. `appendAll(long[])` rejects null, preflights total
capacity with a widened `(long) size + source.length`, then copies values in
array order and returns the starting offset. An empty appendAll returns size
without changing it, including at full capacity; exceeding `MAX_SLOTS` throws
`IllegalStateException`. No source array is retained.

`get/set` check against the current arena size, not the entire logical lane
capacity. `set` can overwrite existing values, so append-only describes its
shape/growth, not immutable contents. `size/isEmpty`, `allocatedBlockCount`
and `payloadBytes` expose logical occupancy and lane allocation separately.
Appending zeros consumes valid logical offsets without allocating data leaves.
`copyRange(fromInclusive,toExclusive)` validates against size and returns a
new caller-owned array. There is no delete, release, trim, reset or close API.

`append` increments size before the underlying set; appendAll increments size
on each set argument. Allocation failure may leave size advanced and earlier
values committed. `forEach(LongConsumer)` rejects null, walks ascending offsets
and rereads size in its loop condition. It has no snapshot/fail-fast check:
callback appends can extend the traversal, callback writes affect later reads,
and a throwing callback leaves its earlier effects intact. An endlessly
appending callback need not terminate before capacity/failure stops it.

## Row allocation, initialization and ordered export

`M3LongLaneStore28` owns one allocator and `laneCount` independent long lanes.
`allocate()` creates a live, initially zero row on a fresh/released-clean slot.
`allocate1/2/3` require **exactly** one/two/three lanes, otherwise throwing
`IllegalStateException` before allocation. They allocate a live handle first,
then initialize lanes sequentially. There is no reserve/commit API, hidden
unpublished-row state or atomic multi-lane publication. A failure during
initialization can leave a live partially initialized row without returning
its handle to the caller; `forEachLive` is not a recovery guarantee.

`get(handle,lane)` and `set(handle,lane,value)` check the lane number first,
then require the allocator's live generation. Releasing a row clears every
data lane at its slot and then releases the allocator handle. Other live rows
keep their physical slots and handles. Successful reuse gets the next
generation, with prior payload cleared. `isLive`, `size/isEmpty` and
`highWaterMark` delegate to the allocator.

The order of release matters: at terminal generation `-1`, allocator release
refuses, but the row store has **already cleared all its data lanes**. The row
remains allocator-live with zero payload. This differs from the allocator's
own no-mutation generation-exhaustion refusal; no all-or-nothing store
guarantee should be inferred.

`forEachLive(HandleConsumer)` rejects null and visits ascending **physical
slot order**, reconstructing each current live handle. This is not insertion
order after slot reuse. Callback mutation is neither frozen nor fail-fast:
later-slot allocations may be visited, released later slots may be skipped,
and a reused slot behind/current to the cursor is not revisited. Exceptions
propagate with earlier callback effects intact.

`copyLiveLane(lane,output,outputFrom)` checks lane, nonnull output and capacity
for the then-current size before writing. It copies values in the same
ascending slot order, returns the number written and does not return the
corresponding handles. Under the required nonconcurrent use this is size;
it is not a snapshot under racing mutation. The caller owns the output array.

`allocatedDataBlockCount` sums lane block counts with `Math.addExact`;
`allocatedAllocatorBlockCount` sums the allocator's separate metadata leaves.
`payloadBytes` includes allocator payload plus all data-lane payload with
checked long addition. Counts represent retained leaves across all lanes,
not distinct logical pages or live rows.

## Handle-linked long list

`M3LongLinkedList28` uses a three-lane row store (`VALUE=0`, `NEXT=1`,
`PREVIOUS=2`), plus head/tail handles. Public `NULL_HANDLE` is zero, a null-link
sentinel whose generation cannot be allocated. **Payload zero remains valid.**
All primitive long values are accepted; handles are not positional indexes.

`addFirst/addLast` return the new handle, initialize its value/neighbor links
and adjust the old endpoint or the empty opposite endpoint. `insertAfter` and
`insertBefore` require a currently live handle, allocate a row, then relink
the neighbors/head/tail. Surviving handles remain stable as list order changes.
Known-handle work does not traverse the list; leaf allocation may still occur,
so this source structure is not a latency or allocation-free guarantee.

`firstHandle/lastHandle` throw `NoSuchElementException` when empty;
`firstLong/lastLong` read through them. `value(handle)` reads the payload;
`set(handle,value)` returns the old payload. `nextHandle/previousHandle`
return zero at the list ends, but reject a nonlive input through the store.
`isLive` has the same owner-scope limitation as the allocator.

`remove(handle)` validates, reads its payload and neighbors, rewires
head/tail/neighbors, then releases the row and returns the removed payload.
`removeFirstLong/removeLastLong` use the throwing endpoint methods. `clear`
walks forward, saving each next handle before release, and resets head/tail
only after the walk succeeds. It retains leaf capacity, high water and the
allocator's updated generations; stale handles do not become valid merely
because the list was cleared.

Edits are not transactional. At generation exhaustion, removal may already
have unlinked a node, and row release may already have erased its data before
allocator refusal. Clear can stop after earlier releases while old endpoints
remain. Allocation/resource failure during insertion can similarly leave
partial effects. No public repair guarantee is provided.

`toArray()` allocates a new `long[size()]` and follows links in list order.
It throws `IllegalStateException` if traversal exceeds that length (cycle or
size corruption) or ends short. `forEachLong(LongConsumer)` rejects null,
counts visited nodes against the current size and checks final equality.
It reads the next link **after** the callback: removing the current node can
make that read stale; appending or inserting may affect what is visited.
Cycle/size guards are not mod-count fail-fast iterators. Keep topology stable
during traversal; callback exceptions propagate without rollback.

## Allocation accounting and lifetime

| Retained primitive leaf | Payload reported per allocated block |
| --- | --- |
| Int lane | 4,096 × 4 = 16,384 bytes |
| Long lane | 4,096 × 8 = 32,768 bytes |
| Bit lane | 64 × 8 = 512 bytes |

`payloadBytes` excludes array headers, root/region references, lane/allocator/
store/list objects, alignment, caller output arrays and temporary allocation.
Allocator accounting sums its two int lanes and live bit lane. Arena delegates
to its long lane; store includes metadata and data; list delegates to the store.
List `allocatedDataBlockCount` excludes allocator metadata. Counters do not
measure retained heap, native/process memory, peak workspace or per-live-row
cost. Sparse values can still require whole leaves, and metadata allocations
can dominate a zero-valued row's data payload.

Leaves are heap arrays retained for their owner's lifetime. Clear/release
reuses logical slots but does not free directories/leaves; no trimming,
compaction, mapping, off-heap allocator or explicit close is implemented by
these eight owners. Stable numeric IDs do not root arbitrary Java objects or
provide a GC/lifetime protocol for reference payloads.

## Optional JNI counting boundary

`M3BitLane28.cardinalityNative(from,to)` first performs the same Java range
check, then unconditionally calls private native `cardinality0`. The host
must explicitly load a compatible library; neither construction nor ordinary
Java cardinality loads one. Missing linkage propagates, with no implicit
fallback, even for an empty valid range.

The [C peer][native] implements
`Java_com_m3_collections_M3BitLane28_cardinality0`. It rejects null root/invalid
ranges and a root length other than 256 with `IllegalArgumentException`.
For each visited nonnull region it requires 256 leaves; each visited nonnull
leaf must have 64 words. It skips absent directories/leaves, copies the
needed word interval with `GetLongArrayRegion` into a stack `jlong[64]`, masks
the boundary words and counts with unsigned 64-bit arithmetic. Local JNI
references are deleted; pending JNI exceptions are propagated through early
returns. The zero return accompanying an exception is not a successful count.

Shape checks cover visited paths, not an eager audit of every stored page.
The private Java owner normally supplies the correct array types/shapes;
the C checks are not a general hostile object-deserialization validator.
No Java-heap pointer is retained, no native backing store is created, and
there is no callback or whole-lane flattening. Java/native equality is the
intended sequential behavior; neither implementation snapshots concurrent
mutation or supplies a performance advantage by contract.

## Complete declared-member map

All **eight constructors, 127 explicitly declared methods (including the
native declaration and nested callback), 38 fields and nine named types**
are covered. Counts exclude inherited/generated Object methods. Private
helpers are implementation evidence, not extra public API. Methods with the
same name are distinct overloads.

| Owner | Declared members routed above |
| --- | --- |
| `M3Address28` | Private constructor; 11 public geometry constants; `region`, `block`, `offset`, `compose`, `checkRange`, `checkRangeSize`, `checkSlot` |
| `M3IntLane28` | Constructor; `regions/allocatedRegions/allocatedBlocks`; `get`, `set`, `getAndSet`, `incrementAndGet`, `clear`, `isBlockAllocated`, both allocation counts, `payloadBytes`, `fill`, `copyTo`, both `copyFrom` overloads; private `copyPage/blockOrNull/ensureBlock` |
| `M3LongLane28` | Constructor; same three state fields and method groups as int lane, with `addAndGet(slot,delta)` instead of increment; private `copyPage/blockOrNull/ensureBlock` |
| `M3BitLane28` | Constructor; `regions/allocatedRegions/allocatedBlocks/cardinality`; `get`, both `set` overloads, `clear`, `flip`, both cardinality overloads, both allocation counts, `payloadBytes`, `nextSetBit`, `previousSetBit`, `rank`, `select`, `cardinalityNative`; private `cardinality0/ensureBlock` |
| `M3SlotAllocator28` | Constructor; `generations/freeNextEncoded/live/highWater/freeHeadEncoded/size`; `allocate`, `release`, `isLive`, `requireLiveSlot`, `size`, `isEmpty`, `highWaterMark`, `freeSlotCount`, `generationAtSlot`, `isSlotLive`, `nextLiveSlot`, `handleForLiveSlot`, `payloadBytes`, `allocatedBlockCount`; static `slot/generation/handle` |
| `M3LongArena28` | Constructor; `values/size`; `append`, `appendAll`, `get`, `set`, `size`, `isEmpty`, `allocatedBlockCount`, `payloadBytes`, `copyRange`, `forEach` |
| `M3LongLaneStore28` | Constructor; `allocator/lanes`; `laneCount`, `size`, `isEmpty`, `allocate/allocate1/allocate2/allocate3`, `get`, `set`, `release`, `isLive`, `highWaterMark`, `allocatedDataBlockCount`, `allocatedAllocatorBlockCount`, `payloadBytes`, `forEachLive`, `copyLiveLane`; private `checkLane/requireLaneCount`; nested public `HandleConsumer.accept(long)` |
| `M3LongLinkedList28` | Constructor; `NULL_HANDLE/VALUE/NEXT/PREVIOUS/rows/head/tail`; `size`, `isEmpty`, `firstHandle/lastHandle/firstLong/lastLong`, `addFirst/addLast/insertAfter/insertBefore`, `value`, `set`, `nextHandle/previousHandle`, `isLive`, `remove/removeFirstLong/removeLastLong`, `clear`, `toArray`, `forEachLong`, `allocatedDataBlockCount`, `payloadBytes`; private `requireLive` |

## Test attribution and open qualification

Two complete direct JUnit bodies contain **13 tests**. They were read, not
run. Assertions establish their intended finite scope, not successful execution.

| Test member | Assertion scope |
| --- | --- |
| `SegmentedPrimitiveStorageTest.addressRoundTripsAllBoundaries` | Seven boundary slots round-trip; negative region input and one-past-last offset rejected |
| `longIntAndBitLanesAllocateOnlyTouchedPaths` | Four selected leaves / three regions, sparse reads and scalar clears; bit total after clear |
| `allocatorRejectsStaleHandlesAfterReuse` | Release/reallocate same slot, incremented generation, stale refusal and stable high water |
| `segmentedArenaCrossesLeafBoundaryWithoutCopyingExistingOffsets` | Appends 4,113 values, checks two leaves and boundary reads/range copy; does not instrument whether old arrays moved |
| `linkedListKeepsOrderAcrossStableSlotReuse` | Endpoint/in-between edits, next/previous links, reused-slot new handle, stale value refusal, endpoint removal, traversal and clear |
| `leanFactoryExposesStorageWithoutChoosingSemantics` | Seven factory returns/lane count/empty status; not full factory contracts |
| `multiLaneStoreDoesNotMoveOtherHandlesOnRemoval` | Surviving two-lane handles/values, reused slot and invalidated old handle |
| `SegmentedLaneBulkContractTest.longCopiesMatchArraycopyAcrossBothOverlapDirections` | 1,500 seeded fill/self-copy steps against 17,000-element flat-array oracle |
| `intCopiesAndImportsMatchFlatArrays` | 700 seeded import/zero-fill/self-copy steps against 12,000-element flat-array oracle |
| `sparseZeroPropagationAndExtremeEndpointsDoNotGrowHoles` | Full-space zero fill/copy, extrema near final slot, cross-region import/clear and valid empty end ranges |
| `invalidRangesAreRejectedBeforeWriting` | Selected long-lane bad array/logical ranges and null input leave existing output/data intact |
| `bitmapRankSelectAndTraversalMatchBitSet` | Edge bits, seeded mutations/ranges, total/rank/select/next/previous against BitSet, MAX_SLOT and selected invalid endpoints |
| `sparseRowsRetainAscendingSlotAndGenerationSemantics` | 9,000-row creation, sparse survivors/reuse, stale refusal and ascending-slot lane export/callback order |

The complete [native probe][native-probe] explicitly loads its library, sets
18,000 seeded random bits plus the last slot, compares 66 endpoint ranges
and 1,200 changing random ranges, and checks one Java-side negative-range
rejection. It does not exercise deliberately malformed private directories,
native allocation/JNI failure, concurrent mutation or every platform.

The complete [allocation probe][allocation-probe] warms prepared CAS lanes
and a long-lane self-copy loop, then requires zero current-thread allocated
bytes in five trials on its particular JVM. That is unexecuted probe intent
here, not a result, retained-heap measurement or whole-library zero-allocation
claim. Atomic operations in that probe belong to the separate atomic-storage review.

The [historical verifier][historical-verifier] stages a sealed recipe
candidate and selects three JUnit classes plus the native/allocation probes;
the [current donation verifier][donation-verifier] checks its manifest and
source-set equality before compiling, scanning tests and building JNI with
UBSan / checked JNI. Neither was run. The factory/delivery reference documents
the current 108-source versus 93-declared-src equality-guard discrepancy;
historical README test counts are not a current validation result or authority
to run the old installer against expanded owners.

Direct module callers reviewed are the seven factory methods and the
allocator → lanes, arena → long lane, row store → allocator/long lanes,
list → row-store chains. This is not an exhaustive repository-wide inbound
call graph. Atomic owners reuse address geometry; their behavior is covered
separately, not counted as additional owner closure in this reference.

Not directly covered by the 13 tests: cross-owner numeric handle collisions,
reserved low bits/zero generation, signed and terminal generation transitions,
full-space allocator/arena exhaustion, allocator OOM recovery, ordinary lane
arithmetic overflow, all constructor/lane errors, callback topology mutation,
partial store/list release, corrupt-link refusal and cold/warm memory/performance.
These remain source-derived contracts or explicit qualification gaps. Whole
package/repository coverage, inline-Javadoc completeness, Java SE compatibility,
installed-image proof and ordered receiving acceptance remain open.

## Exact source identities

Primary links below are pinned to the reviewed documentation/source commit;
their blobs also match the earlier audit pin. Hashes identify bytes, not tests.

| Primary Java file under `m3/collections/src/main/java/com/m3/collections/` | Git blob SHA |
| --- | --- |
| [M3Address28.java][address] | `2dda612d7cfdc39d8138c17a85308dc9d084b28b` |
| [M3BitLane28.java][bits] | `bf44fbf34b893f3919fa1919064869c5aac7d0c3` |
| [M3IntLane28.java][ints] | `4e1e7eb7f2212493eecd544c3e7174ac1ca33232` |
| [M3LongLane28.java][longs] | `8616ab397be434e947a3148ea7b02e946c710e99` |
| [M3SlotAllocator28.java][slots] | `3aa639f8fa88c948babfacb5b34511dd2f18442d` |
| [M3LongArena28.java][arena] | `b265551ff952606326e25c0361bc6f35c72adedf` |
| [M3LongLaneStore28.java][rows] | `bc67c1f6d4da60c330c2d130cfa69b9c05d5b814` |
| [M3LongLinkedList28.java][list] | `f92fd3b564b53ee7bee422db4ce7b6b63a0860eb` |

| Supporting source | Git blob SHA / scope |
| --- | --- |
| [SegmentedPrimitiveStorageTest.java][storage-test] | `fd14c6e5e0e56b634592a3eb0ef46e3136091c4a`; seven tests, full body read |
| [SegmentedLaneBulkContractTest.java][bulk-test] | `050fea92fd02950e5cdc653d55234e829d0a7ab3`; six tests, full body read |
| [segmented_bit_lane.c][native] | `011b847f0d22ea96e4b136547083904ac50b1440`; complete JNI body and two static helpers |
| [SegmentedLaneNativeProbe.java][native-probe] | `babb33e8570d4cf455e4ad8665a5d5f3e4a468d0`; complete probe |
| [PreparedLaneAllocationProbe.java][allocation-probe] | `abd09daee4742abc1ff0b182691e9c918c568522`; complete bounded measurement probe |
| [M3Collections.java][factory] | `3d2f5aa53bda55293b3254bed3d526b896cbd044`; seven relevant delegates only in this pass |
| [module-info.java][module] | `e6ca30e5a04a0b7c0f1ada4f0659342cd863d78d` |
| [collections/pom.xml][pom] | `3c408446a3841282754f06d82819c285a23acd8a` |
| [collections/README.md][module-readme] | `622b61d4b719fd6879ef8df72ffbcfbaa7e54918`; source instructions, historical outcomes not rerun |
| [verify.py][historical-verifier] | `55ba6f5c73922881e712850d9bfb9a78bc4a5dcc`; historical build/JNI orchestration read only |
| [verify_donation.py][donation-verifier] | `be9a75e665683014c21e2a884a9ec8bf43636fbd`; relevant current source-set/compile/test/JNI gates read only |

The [porting invariant][policy], [naming map][naming] and
[product-scope document][product] retain canonical ownership, licensing and
the active String → arrays → collections → AST/compiler sequence. This page
changes no implementation, recipe, policy, manifest, source denominator or
receiving phase. Hosted CI, if reported elsewhere, is separate exact-head
evidence and is neither claimed nor erased by this pass's NOT_RUN status.

[address]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Address28.java
[bits]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3BitLane28.java
[ints]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3IntLane28.java
[longs]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3LongLane28.java
[slots]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3SlotAllocator28.java
[arena]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3LongArena28.java
[rows]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3LongLaneStore28.java
[list]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3LongLinkedList28.java
[storage-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/SegmentedPrimitiveStorageTest.java
[bulk-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/SegmentedLaneBulkContractTest.java
[native]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/native/collections/segmented_bit_lane.c
[native-probe]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/verification/SegmentedLaneNativeProbe.java
[allocation-probe]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/verification/PreparedLaneAllocationProbe.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/pom.xml
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/pom.xml
[module-readme]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/README.md
[historical-verifier]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/verify.py
[donation-verifier]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/verify_donation.py
[policy]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/docs/m3jdk21-porting-invariant.md
[naming]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/docs/name-mapping.json
[product]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/M3_COLLECTION_PRODUCT_SCOPE.md
