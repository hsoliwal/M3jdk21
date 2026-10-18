<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# PR 1325 collection storage admission invariant

## Absolute representation law (DOOP)

No collection row is represented by a stored `Map.Entry`, `Node`, `Cell`, `Bucket`,
or a disguised per-row carrier. Canonical mutable storage is an array owner:
`Object[]` for GC-traced reference payloads, primitive arrays for offsets, hash
buckets, order, free links, tree topology, generation, masks and optional
precomputed signals. An absent slot and a present null value are separate states.
Operations use physical slot numbers, logical-to-physical index projections,
relative offsets, masks, stable permutations and array compaction. There is no
allocation of row carriers during put/get/remove/compute/filter/sort/reorder.

A Java API returning `Map.Entry` may materialize an outward projection only at
that API boundary. Existing `M3SlotMap.LiveEntry` references the owning map and
a **fixed key**; it does not hold or mutate an independent stored mapping.
`M3LFUCache` retains a bounded set of those already-created API projections in
parallel arrays indexed by (physical slot, generation). It uses LFU count and
LRU tie-breaking, never repurposes a projection for a different row, and drops
strong references on eviction/pressure so the ordinary JVM GC may reclaim them.
The cache is not canonical storage. Detached snapshot endpoints remain fresh and
are not cacheable. In a weak-key owner, caching an API entry would strongly pin a
weak key; therefore `M3WeakHashMap` refuses API-entry caching.

The current hash owner `M3LinkedHashMap` now retains an `int[]` generation
plane and an `int[]` slot-mask plane, in addition to parallel keys/values,
hashes, index offsets, before/after order offsets and free-list offsets. Slot
reuse advances generation, including reuse after clear. Bitmasks encode live,
linked, touched and free state; traversal never materializes an Entry. A retained
API entry is not rebound when the physical slot is reused.

## Lifetime, secondary storage and concurrency limits

Canonical Java reference payloads must remain reachable through GC-scannable
arrays or a separately verified rooted-handle owner. Cold primitive planes may
be paged/swapped through independently qualified storage owners; eviction may
discard **reconstructible API projections**, not live logical mappings. Dirty
pages, pin counts, generation-safe handles, bounded IO failure semantics and
stable logical indexes are required before introducing transparent swap into a
concrete collection. No JNI pointer into moving Java heap memory may survive
its call. This patch does not claim transparent paging for all 59 source files.

An LFU cache synchronized method does not make unsynchronized HashMap/TreeMap
mutations thread-safe. Existing concurrent/weak/identity structures retain their
separate consistency and GC contracts. No global allocation- or CPU-speedup claim
has been measured.

## Qualification and receiving policy

PR 1325 remains **source custody only**: 59 complete JDK donor implementations
remain untouched under `implementations/jdk21-package-renamed/` with their
original licenses and historical source bindings. The full copied-source build
had 78 errors and one warning before this change; it is not an admitted
`java.base` collection replacement. Existing `m3/collections` is an
independent proving-ground module and preserves the ordered target gate
String -> arrays -> collections -> AST/compiler.

This additive stacked change implements the shared array-backed *API projection
boundary* and one hash storage-state refinement; it does **not** assert that
ArrayList, HashMap, ConcurrentHashMap, TreeMap, TreeSet, skip-lists or the entire
59-source inventory have been converted. Pending gates include a checked
source-by-source storage conversion, contract-differential tests, JDK jtreg,
CI compile, memory/allocation/throughput evidence, JNI/native parity where used,
and a qualified Synexia recipe lineage. Keep every gate pending until evidenced.

## Test boundary

New regressions cover slot-generation reuse, array-mask validation, LFU/LRU
reclamation, nonrebinding of escaped API objects, weak-key exemption and detached
snapshot freshness. The new isolated LFU class was compiled and self-tested
locally with Java 21 `javac -Xlint:all -Werror`. A full reactor or jtreg run
was not performed by this change.
