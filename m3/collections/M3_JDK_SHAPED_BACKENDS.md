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

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- Modified 2026 by Hitesh Soliwal and contributors: restore exact collection receiving provenance and verification while retaining current owners and String phase authority. -->

## Receiving repair on 2026-10-08

Current primitive, identity and enum owners stay byte-identical. The same existing
factory gains the qualified ordered, sorted, weak, blocking, lazy and progress families;
no second primitive payload hierarchy is introduced. The existing M3LongList interface adds M3LongSequence as its superinterface;
the latter retains the current boolean add/addAll signatures.
The exact local mapping and installed manifest are named in synexia-donation.json.
The broader receiving phase remains STRING; Java/JNI module qualification does not
complete the family catalogue or promote a public JDK backend.
