# J423 — G1 Region Pinning inventory

Status: native/runtime candidate; not materialized.

JEP 423 is a transparent HotSpot/G1 runtime improvement for JNI critical regions. It does not add a
Java language or public Java API surface, but its implementation crosses GC, JNI/WhiteBox,
serviceability-agent, JFR and jtreg boundaries. It therefore requires MULTI_MODULE native authority.

## Required upstream lineage

A safe intake cannot stop at the initial JEP commit:

1. JDK-8318706 — implement JEP 423 Region Pinning for G1.
2. JDK-8323610 — widen HeapRegion pin count to size_t to prevent overflow.
3. JDK-8322484 — repair the post-JEP performance regression with G1RegionPinCache.

Later mass-renaming/refactoring commits are not prerequisites merely because they touch the same
files; they must be reconciled separately if a materialization packet is authored.

## Why this is not yet materialized

The initial JEP changes more than fifty product/test files and deliberately removes the old
TestJNIBlockFullGC/GCLocker stress path. It affects:

- G1 collection-set, evacuation-failure, full/young collection and policy code;
- HeapRegion state and VMStructs;
- JNI pin/unpin behavior exposed through CollectedHeap/WhiteBox;
- SA HeapRegion representation;
- JFR/GC cause and phase tests;
- native/GC stress tests.

The current execution environment cannot complete an OpenJDK Linux image build because required
ALSA/CUPS/Freetype/Fontconfig/GTK development headers are unavailable and package-network resolution
is blocked. No fake headers or configure bypass is permitted.

## Admission before materialization

A later recipe packet must:

- reconcile all three required commits against the exact M3JDK21 HotSpot owners;
- create source-sealed FILE atoms for C/C++/headers/Java tests, then explicit MULTI_MODULE DAG;
- build an actual fastdebug/slowdebug image;
- run pinned-object, evacuation-failure, JNI critical, full-GC, JFR and SA tests;
- verify pin-count overflow and pin-cache fast path;
- compare JNI Get/ReleasePrimitiveArrayCritical behavior against Java21 baseline;
- preserve collector behavior for non-G1 collectors;
- run platform-specific tests before default enablement.

No performance/correctness claim is made from source inspection.
