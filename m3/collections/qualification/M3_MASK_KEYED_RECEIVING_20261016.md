<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 primitive keyed mask receiving — no intermediate element objects

## Inherited runtime and target authority
Base: [M3JDK21 #971](https://github.com/hsoliwal/M3jdk21/pull/971),
head source `abadfdc3c0679d41b6fba506fb98834f91b8c3af`.
Canonical recipe: [Synexia #11870](https://github.com/hsoliwal/com.synexia/pull/11870),
commit `b0cc4bdd936fefdf80ed216d871508c1e45faa26`. No additional runtime dependency or storage hierarchy.
The public Java `java.util` packages and existing physical arrays are unchanged.

## Actual target runtime
Two existing M3-owned primitive collections gain source-sealed methods:
`M3LongHashSet.retainByStateMask(byte[],int)` and
`M3LongLongHashMap.retainByStateMask(byte[],int)`.

* Input mask is the caller's byte[] aligned by **current occupied-slot
  encounter ordinal**: Set iterator/toArray order; Map keyIterator/keysToArray
  order. It is not indexed by hash slot, key value or pointer address.
* The original long[] key lane, Map's parallel long[] value lane and byte[]
  occupancy lane remain the exclusive resident primitive storage.
* Rejected slots become reusable tombstones, keys and values are zeroed.
  No per-entry object, boxed Long/ID, Map.Entry, copied selection array,
  auxiliary collection, or hidden rehash.
* Invalid masks/null/length mismatch fail before touching live lanes. Zero mask
  retains all, changes to modCount only when members are actually removed.
  Source/masks cannot be concurrently mutated; no synchronized snapshot claim.
* Complexity O(backing capacity) time, O(1) additional heap scratch; the
  future insertion path retains the existing tombstone/rehash policy.
  Public JDK HashSet/HashMap backends are **not replaced** by this proving
  module and remain authoritative for their own contract.

## Verification gates
Manifest `mask-keyed-postimages-20261016.tsv` hashes four exact generated
source outputs. `verify_mask_keyed.py` refuses drift/missing paths.
A focused Java21 workflow recompiles the actual whole existing M3 core and
executes seeded Set/Map/JDK differentials plus the inherited List/Deque
mask contract. The tests use JDK HashSet/HashMap and boxed keys only as
**independent test oracles**, not runtime data representations.

Pending until actual receipts: OpenRewrite SDK/JUnit, CI conclusion, ABI,
JNI/native platforms, installed-JDK/jtreg, complete 5-library API census,
memory-pressure or throughput benchmarks, independent review.
String → arrays → collections → AST/compiler remains the ordered migration
policy; do not promote this partial receiver as all collections complete.
