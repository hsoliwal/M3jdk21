<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 JDK-shaped primitive collection backends

This receiver follows the existing Synexia/M3JDK ownership law.

- Synexia is the canonical donor/convergence and reusable-recipe workspace.
- M3JDK21 owns runtime integration, JDK/HotSpot/JNI qualification and public-contract decisions.
- There is no Synexia runtime dependency.
- Public JDK classes keep their JDK names. M3-prefixed names are internal/proving owners.
- Concrete first-party sources and the recipe packet received here are Apache-2.0 and retain
  copyright/provenance. Abstract algorithms are not relabeled as copyrighted source expression.

The v1 set adds primitive-specialized ArrayList, HashSet, HashMap, ArrayDeque and PriorityQueue
backends in the existing `com.m3.collections` package. Existing segmented linked-list, lane,
bitset and read-heavy concurrent owners remain additive. The exact canonical recipe is
`com.synexia.rewrite.M3Jdk21PrimitiveCollectionsV1` at Synexia commit
`38ea4f44572b4711489d1291c898f2aac52bb054` (PR #9775). M3JDK's thin named receiver is
`com.m3.M3Jdk21PrimitiveCollectionsReceiverV1`.

## Selection law

Do not call one data structure universally "best." Select the lowest-cost proven implementation
for the concrete workload and contract: dense vs sparse, ordered vs unordered, mutation-heavy vs
read-heavy, single-threaded vs concurrent, primitive vs object/identity/weak-reference semantics.
Keep credible alternatives additive in this package until benchmarks and the exact consumer
contract establish a winner. A fast microbenchmark never overrides equality, order, iterator,
view, serialization, null, subclassing, GC or JMM requirements.

The deterministic differential test compares the v1 candidates with the corresponding JDK
collections across mutation sequences, extrema, duplicate operations, tombstone reuse, growth and
fail-fast iteration. It also locks the non-structural rule: duplicate set insertion and existing-key
map value replacement do not invalidate iterators merely because the table has cleanup pressure.

See `JDK_FAMILY_MAP.tsv` for existing/pending names and
`m3/compatibility/synexia-to-m3-family-status-20261007.tsv` for the wider MIndex/M3Index
receiving ledger. No PENDING row is implied complete by this PR.


<!-- Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: document the combined collection donation and preserved primitive contracts. -->

## Combined collection donation

The subsequent Synexia packed-collection reconciliation retains the three primitive
hash/set/heap owners above byte-for-byte. It extends the same primitive list/deque
owners with donor operations and retains their fail-fast iterators. The receiving
list keeps its `boolean add(long)` and `boolean addAll(long[])` contracts. Synexia's
own `PackedLongList` and `LongSequence` APIs remain unchanged.

The combined module has one `M3Collections` factory and uses the existing
`com.m3.collections` package. See `name-mapping.json` for the donor-to-receiver names
and `synexia-donation.json` for exact provenance, interface adaptations, and the
active installed-source manifest. Generic object collection coverage does not
complete the primitive-specialization PENDING rows in `JDK_FAMILY_MAP.tsv`.

Run `python3 m3/collections/verify_donation.py --cost-probe` with Java 21 and a JUnit
console JAR in `JUNIT_CONSOLE_JAR`. The dedicated collection workflow runs this
acceptance host and the frozen historical lane SDK test as independent jobs.
