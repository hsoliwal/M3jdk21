# Collection lane invariants

The invariants that the array-lane collections of `jdk.m3.collections` keep, one row each, with where the code
keeps them and the test that checks them. A change to one of these classes keeps every row of its table or
changes the row in the same pull request. Recorded 2026-11-17 from the arrayify, leetcodify, strategy and DOOP
leaves (pull requests #3220-#3236).

Paths are relative to `src/jdk.m3.collections` (sources) and `test/jdk` (tests).

## WeightedLRUCache and WeightedSegmentedLRUCache (`share/classes/jdk/internal/m3/collections/...`)

| Invariant | Kept by | Checked by |
|---|---|---|
| Every resident is counted once and is on the recency list | `admit`, `release`, `linkLast`/`unlink` atoms | `lanesSound` in both differential tests |
| No two residents have equal keys (same equals and hashCode) | `find` before `admit`; CME when equals or hashCode adds or removes a resident meanwhile | `lanesSound`; `firstAdmissionHashReentry` |
| A resident's stamp is the never-reset structure counter at admission (unique, nonzero); a free slot's stamp is 0 | `admit`, `release` | `lanesSound` |
| The index length is the power of two at or above max(2 min(lanes, maxEntries), lanes + 1) | `grow` | `lanesSound` (runs at maxEntries 2 and 8) |
| An operation that empties the cache drops the lanes (not a trimTo that throws ArithmeticException) | `dropLanes` | "an emptied cache drops its lanes" check per step |
| The index never calls a resident's hashCode again (hashes are kept); evictions and removeIf call neither hashCode nor equals | `hashLane` | hashCode counts against the node reference |
| Segmented: each segment's entry count and weight equal the sums over its residents | `changeSegment` computes the new total before moving | `sound`, `lanesSound` |
| Segmented: after every put, also one that throws ArithmeticException, size <= maxEntries and probation <= maxWeight - protected | `trimAfterOverflow` (overflow-free comparison) | `sound` after every overflow; `overflowThenContinue` |
| Segmented: a get only moves weight between segments and cannot overflow | the put invariant above | (follows from it) |

## LinkedHashMultimap (`providers/classes/jdk/m3/collections/guava/collect/LinkedHashMultimap.java`)

| Invariant | Kept by | Checked by |
|---|---|---|
| One lane per field indexed by the slot (key, value, hash, bucket, two value-set links, two multimap links, stamp) | field lanes, `resize` allocates all before publishing | `LinkedHashMultimapLanesTest` |
| Freed slots wait in a first-in-first-out queue; a fresh slot is taken while one exists | `takeSlot`, `freeSlot` | `fifo`, `freshFirst` |
| A freed slot keeps its forward links, its key and value are null, its stamp is even | `freeSlot` | `keyRetention`, `retention`, suspended-iteration scenarios |
| Emptying the multimap bumps the epoch, so suspended iterations and forEach end | `emptied` | `clearedAndRefilled`, `forEachEmptied`, `emptiedByRemove` |
| Capacity is kept until the multimap empties; at most 64 slots are kept when it empties | `onEmpty` | `growth` (58 slots kept, more dropped) |
| create(Multimap) reserves exactly the copied size | `reserve` | `copyReserved` |
| Iteration orders, serialized form and public members equal upstream Guava | | upstream differential (verifier), `LinkedHashMultimapLanesTest` model |

## LongRangeSet (`share/classes/jdk/m3/collections/range/LongRangeSet.java`)

| Invariant | Kept by | Checked by |
|---|---|---|
| Ranges are sorted, disjoint and non-adjacent (touching ranges merge), each start < end | `add`, `remove`, `union` | `verifyInvariants` |
| addAll gives the ranges, capacity and payload of adding the source ranges one by one | `mergeAll` capacity replay through `grownCapacity` | `LongRangeSetAddAllMergeTest` |
| An addAll that adds nothing keeps the lanes and the revision | `mergeAll` change detection | `LongRangeSetAddAllMergeTest` |

## PackedLongLongSortedMap (`share/classes/jdk/internal/m3/collections/packed/PackedLongLongSortedMap.java`)

| Invariant | Kept by | Checked by |
|---|---|---|
| Keys are strictly increasing in [0, size) | `put`, `mergeBackward` | `PackedSortedMapPutAllTest` |
| putAll equals sequential puts: last value wins, the same return value and capacity; the caller's arrays are untouched | `sortPairs` (stable), `lastOfEachKey`, capacity replay | `PackedSortedMapPutAllTest` |

## RoaringIntSet (`share/classes/jdk/internal/m3/collections/mat/RoaringIntSet.java`)

| Invariant | Kept by | Checked by |
|---|---|---|
| Chunk keys increase; prefix[i] is the number of values before chunk i | `flatten`, `FlatLanes.place` | `RoaringIntSetDecodeTest` |
| A chunk's container is the smallest of 2 bytes a value, 8 a run, 8 KiB; ties go to ARRAY, then RUNS | `kindOf` (shared by both chunk builders) | `RoaringIntSetDecodeTest` (1,024 runs of 5) |
| RUNS data are maximal ascending (start, length) pairs; ARRAY data ascending lows | `runsOfSorted`, `runsOfWords`, `lowsOf`, `lowsOfWords` | lanes compared with master's class |
| Every method is at most 15 lines | atomization | `method_lengths.py` |

## PackedLongBatchSortedSet and PackedLongLongBatchSortedMap (`share/classes/jdk/internal/m3/collections/packed/PackedLongBatchSortedSet.java`, `PackedLongLongBatchSortedMap.java`)

| Invariant | Kept by | Checked by |
|---|---|---|
| The original private lanes (sorted and pending keys/values, sizes, capacities, garbage tails) equal the master class after every public call; results, exceptions, iteration and callback order are unchanged | the index lives in its own lanes (`pendingSlots`, `pendingIndexed`) | `BatchSortedIndexTest` against `BatchSetReference` and `BatchMapReference` |
| pendingIndexed implies: `pendingSlots.length` is a power of two of at least 128, its nonzero slots are exactly 1..pendingSize once each, load at most 1/2, and the slot found for lane value i is i + 1 (MASK, OFFSET_ARRAY) | `PackedHashAtoms.laneIndexLength`, `findLaneIndex`, `placeLaneIndex`, `rebuildLaneIndex` | `BatchSortedIndexTest` (index checked after every operation) |
| The index is dropped before anything that can fail or permute the pending lane: flush (before the sort and the capacity check), clear, trimToSize | `flush`, `clear`, `trimToSize` | mutants set-flush-keeps-index, set-clear-keeps-index, map-flush-invalidates-after-required |
| Read paths never allocate or mutate; the index is built or grown only on the append path after the pending capacity is ensured | `pendingIndex`, `appendPending`, `preparePendingIndex` | `BatchSortedIndexTest` forced OutOfMemoryError cases |
| Removal keeps lane order; slots above the removed lane index are rebased by `s -= (i + 1 - s) >>> 31` | `removePendingAt`, `deleteLaneIndex`, `shiftLaneIndexes` | mutants set-remove-skips-rebase, map-remove-skips-slot-delete, atoms-rebase-signed-shift |
| Every new or changed method is at most 15 lines | atomization | `method_lengths.py` |

## ArraySizingStrategy and its adoptions (`providers/classes/jdk/m3/collections/hppc/...`)

| Invariant | Kept by | Checked by |
|---|---|---|
| grow(capacity, elements, additions) >= elements + additions | each factory; adopters refuse a short result with IllegalStateException before changing | `ArraySizingStrategySupersetTest`, adoption tests |
| The four library factories keep the capacity when it already holds elements + additions; hppc() grows anyway | `ArraySizingStrategies` | `ArraySizingStrategySupersetTest` |
| A list with a strategy grows only by it, at every growth point, and refuses serialization | FastList, GapList, the primitive array and gap lists | `ArraySizingStrategyAdoptionTest`, `ArraySizingStrategyPrimitiveAdoptionTest` |
