<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0
-->

# M3JDK21 sorting and collection-view convergence candidate

This is the independently qualified collection-side candidate for the existing M3JDK21 runtime. The separate java.util sorting packet and these existing com.m3.collections owners belong to that one runtime. This candidate composes published behavior refinements in existing owners; it does not introduce a second collection framework.

The Java templates and source fixtures are **candidate inputs**, not a canonical recipe receipt or a published target change. Product changes must be produced by the existing canonical Maven/OpenRewrite owners after exact admission and replay. No original target source, Git index, owning POM, or prior recipe packet was edited by this work.

## Exact inputs and selected owners

The baseline is hsoliwal/M3jdk21 commit 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49. The isolated baseline contains 313 exact Git-derived source/legal/build inputs: 134 main Java files, 158 test Java files, existing native sources, and the unchanged owning POM and notices. BASELINE_INPUTS.json records each Git blob, SHA-256, byte length and mode.

Four immutable published candidates supply the selected Java atoms and their unchanged oracles:

| Published PR | Exact head | Selected contribution |
| --- | --- | --- |
| #1294 | 2027d2f4e9810e5f0e78c91c079cfa0130c24695 | Bounded and nested view navigation, strict read-only boundaries, deep private-filter continuations, long-row masks, stable group reversal, physical mask paths |
| #1314 | d11f07d78d1705d325272e2dfd5b87e72ab100ab | Comparator-equivalent sorted-map entry membership and additional navigation/mask oracles |
| #1328 | 548642eae6579bae33982fef45749f1cd7c8fef0 | Exact direct-owner null probe sequence and the stronger independent null/alias oracle |
| #1303 | e70058e73e4f42036815a24c2c22dac9dd948074 | Recursive split comparator metadata, native bounded payload spans, loaded/missing signal oracle |

Seven main Java owners change: M3FilteredViews, M3MutableFilteredViews, M3FilteredNavigableSet, M3SlotSpliterators, M3IndexPathMasks, M3LongIndexPathMasks and M3LongIndexProjection. Two native owners change: m3_index_path_masks.c and m3_index_mask_window.c. Native templates and their independent evidence remain under the sibling native-candidates directory.

The 40 Java templates comprise those seven runtime files and 33 exact published oracle files. Thirty-two oracle files are added; M3SignalDimensionsContract is the single existing oracle changed, using exact published #1303 bytes to test loaded JNI correctly while retaining explicit missing-native refusal. No assertions were relaxed. M3DequeSpanCopyTest and its unrelated deque source change are not selected.

JAVA_CANDIDATE_INPUTS.json is the authoritative local Java candidate receipt. PER_FILE_INVARIANT.tsv gives every selected Java target an exact preimage or absence requirement, postimage hash, template path, donor pins, license and behavior decision. Every published donor was reconstructed from its exact GitHub diff and verified against its Git blob before use. The 303 original inputs outside the nine runtime changes and one existing oracle change were rehashed unchanged.

## Contracts and representation

The existing M3ArrayList, M3FilteredLists, M3CollectionViews, index snapshots, M3LongStateArray, M3StateMaskArrays and Java M3IndexMaskWindow remain the representation owners. Arrays hold payload and dynamic capacity; primitive directories describe physical indices, logical slices, strides, offsets and state masks. The selected code adds no retained per-element Node/Entry store. Existing private filter continuations describe predicate nesting, not a replacement payload structure.

Live filtered views continue to observe backing changes and follow their existing write-through or strict read-only permission. Sorted snapshots remain detached, stable and unmodifiable, retaining duplicate occurrences and stored references. Comparator-equivalent aliases are tested against the actual stored representative. Empty no-op mutations on strict read-only views still refuse before evaluating user callbacks.

The null-navigation reconciliation intentionally retains two qualified paths. Direct read-only owners use the exact #1328 comparator/isEmpty/first/conditional-last sequence to distinguish an interior stored null from absence. Nested reviewed M3/tree owners retain #1294's separate owner-boundary lookup, then traverse the original wrapped range. Opaque owner exceptions and comparator/predicate exception identity remain observable. Replacing all paths with a generic membership or eager admission probe was rejected because it changes callback demand outside the query range.

Recursive split children keep the same comparator or equivalent ordering through the existing M3SlotSpliterators helper. This retains the current sorted-set iterator batching and size demand. Guava's licensed streaming-filter helper was reviewed but not copied: its unsized streaming policy changes this existing contract.

Long index projections retain immutable captured values and physical index identity. Opposite known sort directions reverse equal-value groups in O(n), preserving the order within each equal group and allocating one int directory. Encounter-order input still uses the existing stable merge. Physical path masks preserve signed long row IDs, separate int physical offsets, full 32-bit stage masks, signed logical stride and exact validation order. Output cannot alias borrowed mask metadata. Native-required selection refuses when its library is unavailable; Java object callbacks remain in Java.

## Executed evidence

The unchanged owning Maven POM compiled with Java 21, -Xlint:all,-module and -Werror using the real cached JUnit dependencies. No stand-in classes or dependency stubs were used.

| Gate | Actual result |
| --- | --- |
| Unchanged full module clean verify | 232 tests; zero failures, errors or skips |
| Initial published union | 263 tests; one null-probe compatibility failure; two explicit native-lane assumptions skipped |
| Reconciled Java full module verify | 263 tests; zero failures or errors; two explicit native-lane assumptions skipped |
| Full native module verify | 263 tests; zero failures, errors or skips; all nine libraries required; checked JNI and fatal UBSan |
| Declared public API and JVM descriptors | All baseline rows retained across 172 public compiled classes; 4,544 baseline rows, 4,554 candidate rows |
| Published standalone Java order/window oracle | 621,981 checks; observation digest 6c6021ba0b376feef67c17df305bca282e09aae27d6e366d93f44ac992afe827 |
| Java and missing-native alias controls | Exact published Java, required/forbidden alias and unavailable-library controls passed |
| Native independent qualification | See native-candidates/verification/combined-02/EXECUTION.json for actual C17, checked-JNI, fatal UBSan, export/prototype, runtime and input-recheck records |

The initial union failure was concrete: expected direct-owner observations [isEmpty, first], observed []. Reusing #1328's exact direct null-candidate method fixed it without changing any oracle. The unchanged baseline also reproduces the original interior-null wrong result: expected predecessor -1, actual null.

The first full native Maven launch deliberately retained its failed evidence when the strict availability gate revealed missing rank and signal opt-ins. Those existing owners require m3.collections.rank.native=true and m3.collections.signal.native=true. The corrected launch changes JVM settings only, preserving sources, tests and POM.

The same published allocation probe ran against baseline and candidate classes in separate JVMs. For 100 opposite-order sorts of 32,750 selected units, each of three baseline samples allocated 26,206,400 bytes; each candidate sample allocated 13,104,800 bytes. This is evidence for the one-directory group-reversal resource change on the tested JVM. It is not a general throughput or full-runtime benchmark.

Declared public API comparison includes the existing public compiled classes and their explicit members/descriptors. It does not claim a complete inherited API census. New declarations are the additive selectLong and filteredPathMasks methods and explicit inherited overrides on M3FilteredNavigableSet. The native sibling compares actual generated prototypes and exports.

## Canonical recipe authoring and receiving plan

Reuse the existing CollectionsContractRefinement20261017 crate and M3HashPinnedJavaSnapshotRecipe / M3HashPinnedTextSnapshotRecipe owners at the reviewed com.synexia source lineage. Add a finite convergence dataset with exact pre/post images; do not introduce another transformation engine or change its row limits.

1. Import the frozen Java candidate manifest and sibling native source manifest. Preserve all original Apache-2.0 headers and add this notice to new catalogue material. Keep the existing GPL java.base sort sources under their own notices.
2. Encode the 40 Java targets through typed Java snapshot admission and the two native runtime targets through the owned text lane. Qualifier/test artifacts may be added as explicit further rows. Each target has one writer and an exact preimage or absence contract.
3. Run the actual existing Maven/OpenRewrite SDK admission, fixed-point, missing/drift/duplicate and receiver tests. These candidate runtime tests do not substitute for SDK execution.
4. Replay into an isolated receiving tree rooted at the pinned full baseline tree. Rehash every received file against the tested postimage, retain unchanged owners and the owning POM, then run the actual full Java and required-native module gates on the received result.
5. Record the canonical immutable revision, source recipe and received hashes in the new receiver receipt and name-mapping/catalogue entries. Publish the convergence PR separately from the disjoint java.util sorting PR, with their cross-proof attached as qualification evidence.

The native/sorts parity proof references the qualified java.util Sorts API. Keep it in the qualification source set and execute it with the actual java.base patch; placing it in the ordinary baseline module test source set would add an undeclared build dependency.

## Scope and rights

The accompanying 25-row catalogue covers the selected runtime owners, retained core representation owners, implementation alternatives, public challenge categories, explicit exclusions and recipe admission. It is a finite evaluated union, not a claim that every collection algorithm or every challenge submission has been integrated.

Guava source was reviewed at df9602b6f1dd36414464229e0a441469d06e4575, blob b7bec7e7afd7aa6a737a290c764b1aa5c0fd147a; it carries Apache-2.0 and Copyright (C) 2015 The Guava Authors. No Guava source body is included. Java SE 21 SortedSet documentation supplies an independent contract reference.

LeetCode 27, HackerRank Dynamic Array and GeeksforGeeks Two Pointers are primary category references only. No editorial, solution, submission, example or test body was copied and no source-copy permission was inferred. Their problem policies differ from M3's stable borrowed-array and callback contracts.

The #1321 provider API expansion and unrelated #1303 deque optimization are outside this union. Provider-specific interfaces and JNI integration need their own actual providers and oracles. Existing source owners and prior qualified records remain available for a later finite convergence.

Copyright 2026 Hitesh Soliwal and contributors. This new document and the accompanying collection-convergence-catalogue.tsv are licensed under Apache-2.0. Copied first-party source and oracle files retain their existing notices. The module LICENSE remains unchanged.
