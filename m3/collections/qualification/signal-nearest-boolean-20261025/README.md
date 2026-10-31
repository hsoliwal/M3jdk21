<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 original-source Boolean-filtered exact nearest-neighbor receiver

**Original M3JDK21 product, not M3 layered on the JDK.** The independent
canonical source-sealed OpenRewrite recipe is in private Synexia
`M3SignalNearestBooleanRecipe` with crate `m3-signal-nearest-boolean-20261025`.
This product receives only Java21 source/test postimages, original compiler/JUnit
CI and a passive data-only qualification receipt. No Synexia runtime dependency,
new compiler/parser, JNI ABI, or new stored per-row Entry/Node/value table.

The original owners remain authoritative: `M3SignalColumns` values,
`M3SignalQualitySchema` typed reliability, `M3SignalDependencyEpochs` invalidation,
`M3SignalBooleanQuery` packed selected-mask, and
`M3SignalExactIntegerDistance` unsigned-128 weighted integer distance.
`M3SignalBooleanQuery.Compiled.evaluateNearest` uses the identical source
schema and passes the mask directly to the existing metric heap. The metric
also gains `nearestByMask` for direct first-party mask consumers, avoiding
an O(number-of-hits) selected-row array. The old `nearest(int[])` API shares
one primitive ranking implementation while retaining duplicates/encounter ties.

**Correctness limits:** numeric distance over declared exact int/long columns
is not a mathematical proof of Java object equality, domain semantics, dynamic
call-graph completeness, or any external producer's semantic correctness.
Reflection/JNI/external changes require the original source owner to
invalidateAll(). A query is an optimistic snapshot, not a persistent lock.
Reentrant source invalidation in progress callbacks prevents partial results;
standalone ranking owns its progress task, embedded ranking does not restart it.

**Evidence:** locally compiled strict OpenJDK21 `--release 21 -proc:none
-Xlint:all -Werror` with byte-identical candidate Java and compatible owner
source, 25,223 independent BigInteger/seeded nearest assertions (normal and
`-Xint`). Earlier exact-integer (22,759), Boolean (61,543), viewport
(32,077), ranked (11,894), cancellation (19) oracles also passed both modes.
These are not actual original M3JDK21 Maven/installed JDK results.

**HOLD** until canonical Maven/OpenRewrite SDK actual first/second pass and
source-drift refusal, original target Java21/JUnit at the same PR commit,
JDK/jmods/jtreg/JCK, native/JNI/HotSpot/GC/JMM, Linux/Windows CPU/memory
and full binary compatibility gates. Do not promote or merge this draft.

## Receiving SHA-256

| Original source path | Git blob after | SHA-256 after |
|---|---|---|
| `m3/collections/src/main/java/jdk/m3/collections/M3SignalBooleanQuery.java` | `06637398e3f1a8bafb7ba760f2286cadd90a4164` | `941f2e9cc5fc6560ce85d9e2b19553046c9bd47049b4ad9b6236fa9a0a23fa2b` |
| `m3/collections/src/main/java/jdk/m3/collections/M3SignalExactIntegerDistance.java` | `3388f30324da5eb3b3529cbfc784a09bf6b6c416` | `68436395afde7e4761ef0771a216e2d50e82b99087fde9d01b6ab38f6b170ea1` |
| `m3/collections/src/test/java/jdk/m3/collections/M3SignalNearestBooleanSelfTest.java` | `c5d4e2133ce13608417caccfb6cc0e9799b6242c` | `2f5d14d4083706e249cd2627d2f448c1d7fb4df8331c042e88b85ad5dcf437c8` |
| `m3/collections/src/test/java/jdk/m3/collections/M3SignalNearestBooleanTest.java` | `6fbcdbb45dea28c4e235c4e1b061cd3eb4f39a12` | `6b724f6f566ae06022ec0f9e8f4162a4f82facbaabb2f37fe9ed4fa70c41c152` |
