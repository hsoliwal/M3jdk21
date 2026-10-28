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
| `m3/collections/src/main/java/com/m3/collections/M3SignalBooleanQuery.java` | `af858f0618087b3284b1ba2e62ade59cac8f4e28` | `d6b26340655fe084397a9665c10aa0f56500e3dacb0231cb6cc57e90fa3d2207` |
| `m3/collections/src/main/java/com/m3/collections/M3SignalExactIntegerDistance.java` | `dfee78788ef527a3f1f340e5b87a5680463c02f1` | `25d4b22a944930cb51739221febf790d3ae54f752245a21a54d5b45951bcd07a` |
| `m3/collections/src/test/java/com/m3/collections/M3SignalNearestBooleanSelfTest.java` | `d22ab6157deb99c359a31495938c466dfd60872c` | `211696edd42c336153ec964bde0425ccd15e64706fa37f0ec206abe482fa02bd` |
| `m3/collections/src/test/java/com/m3/collections/M3SignalNearestBooleanTest.java` | `243df18ca528466bebe683c92f5e7fa10196dddf` | `7bd478aff58e86ab628de29322463d8cd8238e59f9062ee4e158955e3e3e9dd2` |
