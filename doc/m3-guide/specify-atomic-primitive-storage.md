<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Atomic primitive storage: locations, publication and traversal

This reference covers every declared member and complete body of
`M3ConcurrentIntLane28`, `M3ConcurrentLongLane28` and `M3ConcurrentBitSet` in
`com.m3.collections` at `7abdf87cb497d0d94abeb723d70c50f856ecc381`. Their blobs are
unchanged from audit snapshot `1011050da4205a901ad079d7711b6b8efd8a9af7`.
It describes source behavior, not an executed concurrency or performance result.

These are separately built Apache-2.0 storage owners, not replacements for Java
arrays, `java.util.BitSet`, or an entire concurrent collection. The
[module descriptor][module] exports `com.m3.collections`; the
[standalone POM][pom] targets Java 21. The repository's
[ordered receiving policy][policy] still requires concrete target qualification.
See [ordinary segmented storage](specify-stable-segmented-storage.md) for the
address/dependency owners, [primitive maps](primitive-map-reference.md) for the
distinct immutable-snapshot map, and
[residency](specify-residency-lifecycle.md) for monitor-serialized object access.

## Choose the ownership boundary

| Owner | Address space and allocation | Consistency boundary |
| --- | --- | --- |
| `M3ConcurrentIntLane28` | Fixed 2^28 logical `int` slots; lazy 4,096-element pages | One primitive location per scalar operation |
| `M3ConcurrentLongLane28` | Same addresses; lazy 4,096-element `long` pages | One primitive location per scalar operation |
| `M3ConcurrentBitSet` | Constructor-fixed nonnegative `int` bit capacity; one eagerly allocated `long[]` | One word per actual mutation; scans span independently observed words |

The lane address dependency is [M3Address28][address]: 256 regions, 256 blocks
per region, 4,096 values per block. Valid scalar slots are `0..268435455`.
Half-open ranges permit `0 <= from <= to <= 268435456`, including the empty
end range. Invalid scalar/range inputs throw `IndexOutOfBoundsException` before
storage work. Every `int`/`long` bit pattern, including zero and both signed
extremes, is a payload. Zero is not an ownership, occupancy or generation marker.

Neither lane allocates IDs, reserves a logical row, tracks live entries, or
coordinates multiple lanes. CAS compares the value only. An owning collection
must supply any occupancy/publication protocol, object-handle retention,
generation/ABA defense and multi-index consistency. A correct scalar atomic does
not make a check-then-act sequence, parallel tuple update, or range snapshot atomic.
The two factory methods `M3Collections.concurrentIntLane()` and
`concurrentLongLane()` each return a fresh owner; `M3Collections.bitSet()` returns
a JDK `BitSet`, not this concurrent owner ([factory, lines 76–80 and 118–119][factory]).

## Atomic lane members

The int and long implementations have matching structure and line ranges;
replace the primitive return/value/delta type as appropriate. The complete
implementations are [int][int] and [long][long].

| Declared API | Source lines in each lane | Implemented effect |
| --- | --- | --- |
| Public no-argument constructor | 34–39 | Allocates the outer 256-entry region directory; no region or value page is populated |
| `getAcquire(slot)` | 42–46 | Validates; acquire-loads directory/page references, then the value if present; absent storage reads as zero without allocation |
| `setRelease(slot, value)` | 49–52 | Ensures a page even for zero, then performs a release value store |
| `compareAndSet(slot, expected, update)` | 55–62 | Ensures an actual page even if expected is nonzero or update is zero; executes strong `VarHandle.compareAndSet`, returns its boolean result |
| `getAndSet(slot, value)` | 65–68 | Ensures the page; atomic exchange returns the previous value |
| `getAndAdd(slot, delta)` | 71–74 | Ensures the page even for delta zero; atomic addition returns the previous value; overflow wraps as Java primitive arithmetic |
| `prepare(from, to)` | 77–83 | Ensures every intersecting page without changing values; empty ranges do nothing |
| `allocatedBlockCount()` | 86–96 | Acquire-scans populated pages; weakly consistent count, not an atomic allocation snapshot |
| `payloadBytes()` | 99–101 | Count multiplied by 4,096 and primitive width; excludes all array headers, directories and losing allocation candidates |
| `spliterator(from, to)` | 104–107 | Validates immediately; creates a fixed-slot-range traversal |
| `stream(from, to)` / `parallelStream(from, to)` | 110–117 | Sequential/parallel primitive stream over that range, with no copied payload snapshot |

The actual CAS/exchange/add operations have the VarHandle volatile read/write
memory effects; a failed lane CAS has the corresponding read effects without a
successful write. In particular, an absent zero-to-zero lane CAS allocates and
executes an actual CAS. It is not replaced by a successful plain comparison.
Release/acquire correctness still depends on the caller's matching synchronization
protocol. An absent-page zero read does not establish that a producer published a
value, and equal values do not identify a generation.

`getAndAdd` does not use `Math.addExact`; this intentionally differs from the
ordinary checked-add lanes. None of these scalar members calls user code.

### Page publication and retained memory

The private `REGIONS`, `PAGES` and `VALUES` VarHandles operate on the outer array,
region arrays and primitive pages respectively. The final `regions` field owns
the directory. Private `page(slot)` (119–122) acquire-loads the two directory
levels. `ensurePage(slot)` (124–140) creates a region/page candidate when absent
and uses `compareAndExchange(null, candidate)` to install it. A losing publisher
uses the winning reference; its unused candidate becomes garbage.

Regions and pages are never replaced, removed, compacted or reclaimed by this
API. Clearing a value does not release its page. Each retained int page holds
16,384 primitive payload bytes; each long page holds 32,768. Full logical
population would retain 1 GiB or 2 GiB of primitive payload respectively, before
directories/headers. This is a source-derived size, not a measured memory budget
or an allocation guarantee. `prepare` is physical page preparation, not logical
reservation; a failure can leave already prepared pages in place.

Prepared-page scalar bodies contain no explicit lock, wait or retry loop. First
writes can allocate, and the VM/GC/scheduler controls progress. No wait-free,
universal lock-free, allocation-free-stream, fairness or latency claim follows.
There is no timeout, interruption or cancellation API; allocation failures are
not converted to a storage-specific exception.

### Range traversal and callbacks

Private `Range` (142–177) holds the owner, mutable cursor and fixed fence. Its
constructor stores those three fields. `trySplit()` returns the lower half and
advances this cursor to the upper half, or returns null for fewer than two
remaining slots. It allocates a new range descriptor, without copying values.
`estimateSize()` is exactly `fence - cursor`: it counts slots including zeros,
not populated pages or nonzero entries.

`tryAdvance` and `forEachRemaining` reject a null action even on an exhausted
range. Each value uses `getAcquire`, and the cursor advances before the callback.
A callback exception propagates; the consumed slot is not replayed, and earlier
callback effects remain. Callbacks may observe/mutate later slot values because
there is no snapshot or owner-wide traversal lock. Characteristics are
`ORDERED | SIZED | SUBSIZED | NONNULL | CONCURRENT`; neither `IMMUTABLE` nor
`SORTED` is supplied. Encounter order is increasing slot index, not numeric value
order. Splitting permits disjoint traversal partitions; it does not make one
mutable spliterator safe for simultaneous unsynchronized calls by several threads.
Parallel execution does not promise callback execution order or isolate callback
side effects.

## Concurrent bit-set members

[M3ConcurrentBitSet][bits] does not use the segmented lanes. The constructor
(24–29) rejects a negative capacity with `IllegalArgumentException`, permits
zero, widens before computing `ceil(bitCapacity / 64)`, and allocates that many
zeroed words immediately. Valid bits are `[0, bitCapacity)`; private `check`
(161–163) throws `IndexOutOfBoundsException` otherwise. Private `mask` (165–167)
uses `1L << (bit & 63)`. Capacity is fixed, with no grow/shrink or default
constructor. The largest accepted numeric capacity is `Integer.MAX_VALUE`,
subject to actual allocation limits.

| Declared API | Source lines | Implemented effect |
| --- | --- | --- |
| `bitCapacity()` / `wordCount()` | 31–32 | Returns requested bit capacity / allocated word count |
| `get(bit)` | 34–38 | Acquire-loads the containing word and tests the bit |
| `set(bit)` | 41–50 | Returns false if an acquire observation already has the bit; otherwise retries word CAS until a 0-to-1 change succeeds or an observation finds it set |
| `clear(bit)` | 53–62 | Analogous 1-to-0 change; true means this call changed it |
| `toggle(bit)` | 65–76 | Retries CAS of the containing word and returns this operation's new bit value |
| `compareAndSet(bit, expected, update)` | 84–96 | False on mismatching acquire observation; true immediately for matching same-value request; otherwise retries actual word CAS |
| `cardinalityApprox()` | 98–104 | Sums `Long.bitCount` of separately acquire-loaded words; exact without mutation, weakly consistent with mutation |
| `snapshotWords()` | 107–113 | Allocates a detached `long[]` and copies each word with acquire semantics; not a single-time whole-set snapshot under mutation |
| `clearAll()` | 115–119 | Exchanges each word with zero in sequence; no multiword atomic clear |
| `setBitStream()` / `parallelSetBitStream()` | 125–132 | Sequential/parallel primitive traversal of observed set indices using `SetBits` |

**Matching no-op bit CAS has no release/volatile-write effect.** Line 92 returns
true after `getAcquire`, without executing a CAS. Likewise already-set `set` and
already-clear `clear` return without a write. Do not use those success/no-change
paths as the publication operation offered by actual lane CAS. Actual changing
CAS preserves the other bits from the observed word, retrying when any concurrent
word update defeats CAS. Bits sharing a word therefore share a contention unit.
The loops have no retry bound, backoff, interruption check or fairness mechanism;
there are no explicit Java locks, but source alone does not establish a progress
or throughput guarantee.

A concurrent writer can repopulate a word already processed by `clearAll` before
it returns. `snapshotWords` and `cardinalityApprox` can combine different moments
across words. Returned arrays do not alias internal words. Unused trailing bits
remain zero because all mutators validate the bit range and the whole-word clear
only writes zero. Primitive payload is `8 * wordCount()` bytes, excluding headers;
copying a snapshot allocates another word array. There are no per-bit wrappers
and no native/JNI/store dependency in this owner.

### Set-bit traversal internals

Private `SetBits` (134–159) stores word cursor/fence, `currentBase` and a local
`pending` word. Its constructor sets the word interval. `tryAdvance` rejects null,
loads each word at most once when needed, removes the least-significant set bit
from `pending` before calling the consumer, and yields ascending indices.
Once a word is loaded, subsequent changes to that word do not alter its pending
local bits. It is a sequence of per-word observations, not a copied bitmap or
whole-set snapshot. Exceptions consume the bit just selected and propagate;
there is no callback rollback.

`trySplit` partitions remaining whole words and returns the prefix; it returns
null while `pending != 0` or fewer than two words remain. `estimateSize` is an
upper bound, `64 * remainingUnloadedWords + bitCount(pending)`, which may include
unused tail capacity. Characteristics are
`ORDERED | DISTINCT | NONNULL | CONCURRENT`, without `SIZED`, `SUBSIZED`,
`IMMUTABLE` or `SORTED`. Ascending encounter order does not add the `SORTED`
characteristic or order parallel side effects. No custom `forEachRemaining`
implementation is declared; the inherited traversal uses advancement.

## Evidence and remaining qualification

All of [M3ConcurrentLaneContractTest][test] was read, including its latch/executor
helper. Tests were **NOT_RUN** in this documentation pass.

| Exact test method | Assertions read | Boundary of that evidence |
| --- | --- | --- |
| `atomicValuesWrapAndZeroWritesKeepPublicationLocations` | Sparse zero read, zero release allocation, long wrap, CAS success/failure, exchange, retained page count, bad slot/reversed range | Allocation/value assertions, not a JMM message-passing proof; its zero-to-zero CAS uses an already allocated page |
| `racingPublicationAndAtomicAddsLoseNoUpdates` | Eight actors contend on first lane CAS; one winner; 120,000 int/long additions; page/region crossings and page counts | Finite test program, not a proof of all schedules, ABA safety or progress |
| `parallelStreamsSplitFixedRangesWithoutLosingOrDuplicatingSlots` | Int/long ordered arrays, long sum, split-size conservation, characteristics, empty end range, null and throwing callback consumption | Values are populated before traversal; no concurrent-writer snapshot assertion |
| `intAtomicBoundaryAndSparsePreparation` | Last logical slot, one page and 16,384 payload bytes, wrapping int add, exchange and CAS mismatch | Small bounded allocation, not full-space memory qualification |
| `concurrentCollectionStreamsPreserveCapturedSnapshotsAndIndexOrder` | Quiescent BitSet sequential/parallel index arrays, zero-capacity traversal, final valid bit, missing `SIZED` | Map/set captured snapshots in the same method do not make the BitSet stream a snapshot |

The helper starts eight actors using latches, waits on futures with timeouts and
shuts the pool down. Reading it does not establish that any race test completed.
The inspected complete collection source/test tree contains the two lane factory
callers and the above direct tests; no repository-wide absence-of-callers claim
is made. The constructor/mutation details of BitSet, no-op CAS memory effects,
whole-word clear/copy races, callback concurrency, failed first allocation,
zero-to-zero CAS on absent storage, native/runtime integration, exhaustive JMM
behavior and resource/latency measurements remain separate unexecuted gates.

### Exact source identities

| Source | Git blob at the pinned revision |
| --- | --- |
| [M3ConcurrentIntLane28.java][int] | `bc2004e2c4802c3338c0099d537b94fb49dc4363` |
| [M3ConcurrentLongLane28.java][long] | `176991710420d96aace2774c59fe0137d2b1ba4e` |
| [M3ConcurrentBitSet.java][bits] | `feb93280cbc1481c40c52156ffc50e2be5e35b54` |
| [M3Address28.java][address] (dependency) | `2dda612d7cfdc39d8138c17a85308dc9d084b28b` |
| [M3ConcurrentLaneContractTest.java][test] | `86f5ae48e363b14a00e62ed6fd6d74fe572cc2c3` |

Only the first three owners are primary coverage here. Address/factory/build
inspection and the existing map reference do not count again as new primary
owners. Source review, local editorial checks, exact-head hosted CI, and target
runtime qualification are distinct evidence levels.

[int]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ConcurrentIntLane28.java
[long]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongLane28.java
[bits]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3ConcurrentBitSet.java
[address]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Address28.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3ConcurrentLaneContractTest.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/pom.xml
[policy]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/docs/m3jdk21-porting-invariant.md
