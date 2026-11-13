<!-- SPDX-License-Identifier: Apache-2.0 -->
# Primitive binary sorted-set algebra M3JDK21 receiver

Existing `com.m3.collections.M3LongCompositeViews` owner is refined
additively with three factories: `sortedIntersection`, `sortedDifference`,
and `sortedSymmetricDifference`. They preserve signed-long natural
ordering and work as live read-only views of existing M3LongSortedSet
backing owners. No new collection index or element store is created.

Runtime receives exact source-generation templates from canonical Synexia
[PR #11937](https://github.com/hsoliwal/com.synexia/pull/11937)
at `c58aade6b9b5439705fad71e789e61419dc7f268`; source preimage is the unchanged PR #987 module composite
source. Target manifest and `verify_sorted_set_algebra.py` refuse
missing, linked or modified Java source or test outputs.

`BinarySetAlgebra` retains two source references and an enum;
`BinarySetCursor` holds two original primitive M3LongIterator controls
and scalar signed long/boolean state. Each iteration is O(n+m) total
and has O(1) scratch; no per-row `Long`, `Map.Entry`, key/value
array, node or new JNI callback. Explicit `toArray` allocates one
exact result `long[]` and refuses cardinality drift across its two
passes. Set semantics are unique-key, deliberately distinct from
Guava Multiset/Commons Bag cardinality behavior.

Natural signed-long ascending sorted source is a precondition. Wrong
descending or repeated-key inputs fail closed when advanced. Navigation
is linear, never falsely advertised logarithmic. Spliterator is
ORDERED/SORTED/DISTINCT/NONNULL, unsized while backing is live.

Independent JDK `TreeSet` regression probe uses 128 seeded scenarios
times eight live mutation phases, includes Long.MIN/MAX, navigation
fallback boundaries, live filtered sorted-view composition and explicit
mutation refusal. Boxed objects in JDK test oracle are test-only.

The focused CI verifies all inherited source hashes, actual Java21
module compile and standalone behavior, and recompiles the existing C17
JNI mask kernel. This task does not create a duplicate JNI set engine,
modify `java.base` or claim complete provider API compatibility.

Hold draft until exact-head CI, Maven/OpenRewrite/JUnit fixed point,
full installed M3JDK/jtreg, native portability and independent review.
