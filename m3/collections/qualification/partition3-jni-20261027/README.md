<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 primitive JNI three-way partition — product receipt (2026-10-27)

**Native product ownership.** M3JDK21 owns the existing original
`M3LaneAlgorithms` Java 21 and `m3_long_address_kernel.c` JNI implementations.
This is a direct implementation pass inside M3JDK21, not M3 + JDK and not
a separate compatibility layer. The canonical source-sealed Java and C
OpenRewrite recipes, evaluated donor algorithm catalogue, rejected candidates
and source preimages live only in [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224),
commit `d00629cdad1ecfc43df2e7d7a5252aab796b6978`.

## Exact source custody and API behavior

Product predecessor draft PR head:
`82562dc8e393db79603fc620e7ded8b2e86a87a1`.

| Original source owner | Before Git blob | Result Git blob |
|---|---|---|
| `m3/collections/src/main/java/com/m3/collections/M3LaneAlgorithms.java` | `6691be3fcc1d3b3b276dd2feb7aa488dea5c00d2` | `a806a8604efc928053159e5c230b1da4f0f9b6be` |
| `m3/collections/src/main/native/collections/m3_long_address_kernel.c` | `76f68619a78b52ab528f4f16dcf26b510288c7c9` | `37c75d4f2532003b36381597d38d2a4af4812c0c` |

Generated original-module Java 21 differential contract:
`M3LanePartition3PreparedContract.java`, Git blob
`43abd6148088186fbdb901d94232e1b2ecde444e`.
JUnit runner Git blob `6c25241f09af6a91563ba54d8962f3c2c8134795`.
Source-pinned exact PR-head Java 21/C17 JNI workflow:
`.github/workflows/m3-lane-partition3-java21-jni.yml`,
Git blob `f3b9a22953217aad4fb7bd21c44fe361cc218c26`.
No downstream source-changing recipe or Synexia runtime code is imported.

The existing `partition3(int[]/long[], from, to, pivot)` Java methods retain
their full original implementation and mutation order. Additive overloads
`partition3Prepared` compare signed int/long values against an explicit
pivot with the LeetCode #75 three-pointer Dutch National Flag method, using
absolute physical half-open [from,to) indexes and returning the same
packed [low,high) bounds. The native path uses the **existing**
`m3_collections_address` library with exactly two new JNI exports; native
required calls always fail closed, and older native builds may use the Java
oracle only for **non-required** requests. The native transition is enabled
with `-Dm3.collections.lane.native=true` for prepared windows of at least
256 elements; smaller requests continue in Java. No Java object or callback
crosses JNI, no per-element Java object is retained, and all JNI array
elements are released before returning. JNI may internally copy the source
array; zero-copy and a speedup are **not** claimed.

This operation mutates **one value lane only** and is not implicitly valid
for a separate aligned bitmask/sidecar; parents requiring atomic coordinate
and mask permutation must use a coordinated multi-lane algorithm.

## Donor choices and bounded evidence

Algorithm families: [LeetCode #75](https://leetcode.com/problems/sort-colors/)
one-pass 3-way partition, HackerRank Quicksort 1 partition and GFG
three-way partition around pivot. Concepts were compared; no donor
editorial or challenge solution code was copied. The original first-party
Java code remains the ordering/mutation authority.

**Executed locally on an isolated Java 21/JNI fixture**:
`javac --release 21 -Xlint:all -Werror`; `gcc -std=c17 -Wall -Wextra -Werror
-fsanitize=undefined`; generated JNI header matching; Java-only and
forced JNI `-Xcheck:jni` in normal and `-Xint` VM modes.
Deterministic whole-array and packed-bound checks:
**978,782 assertions/pass without JNI** and
**980,599 assertions/pass with JNI**, separately in both modes.
Edge cases include signed extremes, duplicate pivots, empty and length-1
windows, nonzero offsets, exact input-array mutation, missing-native
and prevalidation non-mutation. This was an isolated atom fixture, **not**
a compiled installed product JDK or entire collection module.

**Pending mandatory full qualification:** actual GitHub workflow execution
against exact original-source PR head, Maven/JUnit/Synexia OpenRewrite
SDK replay, complete existing Java and JNI contracts, native variant and
source-build checks, JDK image, jtreg/JCK, cross-platform and measured CPU/
allocation results. Do not merge, deploy or claim full native-JDK replacement
qualification until all these gates pass. This PR remains DRAFT/HOLD.
