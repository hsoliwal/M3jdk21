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

## JNI older-address-library ABI admission successor

A review of the additive LC75 optional JNI path identified a compatibility
trap: a legacy `libm3_collections_address.so` may load successfully while
having neither new `partition3Long0` nor `partition3Int0` JNI export.
Availability based only on `System.loadLibrary` would be a false claim until
a native symbol was actually resolved.

Canonical Synexia #13224 source-sealed Java/CI successor:
`ed64d9089f685692b4c09fb11354a3c1f23320e0`.
Existing M3 Java candidate blob `a806a8604efc928053159e5c230b1da4f0f9b6be`
is the exact SHA-pinned preimage; final reviewed Java blob is
`9f512b267485bc1b68cf97eacadb42beada78372`.
Old exact-head workflow blob `f3b9a22953217aad4fb7bd21c44fe361cc218c26`
is replaced by `95040d48b727f1cd6ea4789271cbd2884d7aed75`.
The prior canonical recipes and intermediate source images remain in Synexia
as rejected/intermediate evidence; this product contains only execution
results, never the OpenRewrite transformation engine.

The new `PartitionNative.load()` probes **both** export symbols using two
fresh detached zero-length primitive lanes after an opt-in library load.
A missing symbol raises `UnsatisfiedLinkError`, causing optional use to
stay entirely on the original Java partition oracle, with no original
caller array mutation. Explicit native-required requests still throw
`UnsatisfiedLinkError` if the library or either needed ABI is missing.
No new JNI C source change or additional JNI export is required; original
`partition3` entrypoints and swap order are unchanged.

The exact-head workflow now compiles a temporary **legacy** address library
without either new symbol, runs the current original-module Java oracle
with `m3.collections.lane.native=true` in both normal and `-Xint` modes,
and requires Java fallback `native=false` and JNI-required refusal.
It retains the new-native mandatory parity cases and all original source
SHA pins. The isolated JDK21 smoke suite passed: Java-only/old-ABI
**978,782 assertions** and new native **980,599 assertions**, each in
normal and interpreted modes. JNI is checked with `-Xcheck:jni`;
the native C kernel is compiled with strict C17 and UBSan.

Actual original full-source GitHub Actions and full M3JDK21 Maven/JNI,
installed JDK/jtreg/JCK and CPU/heap benchmarks remain pending.
**DRAFT/HOLD.** No default public API behavior, JNI library ownership,
original source-manifest, protected branch or release gate was changed.

## Native workflow YAML syntax correction, 2026-10-27

The first ABI-fallback CI workflow candidate at exact product head
`cd093b18d59bd34b452463a41631e2fcdc4a1ad2` failed on
[Actions run 40539147663](https://github.com/hsoliwal/M3jdk21/actions/runs/40539147663)
**before any job was instantiated**. The source was malformed: a literal
`$'` in the shell `grep` expression was interpreted by JavaScript
`String.replace` as a replacement-suffix metasequence while preparing
the intermediate template. This embedded a duplicate upload-artifact
YAML block inside the legacy native ABI test's `grep` line.

Canonical **Synexia #13224** fixed-text recipe
`com.synexia.m3.M3LanePartition3WorkflowSyntax20261027`,
commit `6d5a69d31ceb3b1f3ba12bdb2f7308d33636dc00`,
pins and replaces exactly one workflow:
malformed original Git blob
`95040d48b727f1cd6ea4789271cbd2884d7aed75`,
SHA-256 `f54506aa24678a623e1819390e1ddca67f508833a3c66672f286edd64abb5f82`,
with corrected Git blob `e5d1f2b4a112ba55593baa34aecb3ef8173a4fad`,
SHA-256 `2ccbb81df480db5dc4505d2932d99946289751252e16be9bdf2b5f44e79b404d`.

The exact corrected workflow preserves **all original Java21/C17/JNI checks**
and the legacy shared-object fallback, with one complete
`grep ... native=false` line and precisely one artifact-upload step.
Synexia retains the malformed source for regression evidence, a Python
structural/refusal oracle, and a real OpenRewrite Text SDK replay test.
No Java runtime owner, JNI native source, public API or source-provenance
manifest was modified in this corrective pass. The original failure was
YAML validation, **not** a Java/JNI runtime assertion.

**Current-head original-source GitHub workflow, full collection/native
Maven/JUnit, installed-JDK jtreg/JCK and measured performance remain
unqualified until their actual run succeeds.** Keep DRAFT/HOLD; do not
bypass requirements or merge based on isolated harness tests.
