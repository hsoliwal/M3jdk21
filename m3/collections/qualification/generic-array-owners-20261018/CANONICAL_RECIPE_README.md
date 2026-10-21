<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 native generic object collections: ArrayDeque and PriorityQueue

The request is the entire native collection superset across JDK, Guava,
Eclipse Collections, Spring and Apache Commons **without vendor adapters**.
The JDK21 M3JDK21 core already contains generic List, Map, Set, counted Bag,
table and multivalue owners. This task adds two missing **generic reference
collection owners**, not yet the whole exhaustive superset.

## Concrete product Java
- `M3ArrayDeque<E>` implements JDK `Deque<E>` directly on one
  GC-traced `Object[]` plus `byte[]` aligned state flags, primitive head,
  size and physical-offset calculations. No linked nodes/Entry classes.
  Includes full deque mutation endpoints, occurrence removal, reverse-backed
  JDK21 `reversed()`, iterator remove, clone, serialization and predicate
  preflight / stable compaction.
- `M3PriorityQueue<E>` implements `Queue<E>` directly using an
  `Object[]` binary heap, `byte[]` flags and `long[]` occurrence
  identities. Comparator or natural ordering is Java-authoritative.
  Transactional comparison planning before mutation refuses comparator
  failures/reentrancy without partially corrupting the heap. Stable
  duplicate-occurrence iterator removal, clone and serialized form.
- `selectedPhysicalIndices(required, forbidden, backend)` on both owners
  reuses the **existing** `M3IndexMaskCompactor` C17 JNI for explicit
  `NATIVE_REQUIRED` masks; Java references and predicates never cross JNI.
  Missing native library refuses, never silently falls back. Original
  `M3SlotArrays` is the sole checked growth helper. No new native ABI.

These are opt-in native M3 classes, not JDK java.base replacements,
Guava/Eclipse/Apache/Spring adapters or aliases, and are single-threaded.

## Original source-sealed Maven/OpenRewrite authority

Named recipe `com.synexia.rewrite.M3NativeGenericDequePriority20261018`
uses the **unchanged installed**
`M3HashPinnedJavaSnapshotRecipe`, resource crate
`m3-native-generic-deque-priority-20261018` under
`synexia-openrewrite-recipes/src/main/resources`. Four
`ABSENT`-before paths (two production, one dependency-free differential
oracle and its JUnit wrapper) are generated exactly from the canonical
Apache-2.0 templates. The existing Maven OpenRewrite JUnit SDK test
checks source identity, fixed point, and preexisting-source drift refusal.
Fix source in the Synexia templates, qualify, regenerate the exact M3JDK21
output; never manually edit recipient-generated Java.

## Executed evidence and hold

Actually completed on Java21.0.12.1: strict
`javac --release 21 -proc:none -Xlint:all -Werror`; 51,972
deterministic JDK differential assertions from the comprehensive 16k deque /
20k queue randomized suite; an additional selected 12k-operation oracle
passed 9,760 assertions; the committed smaller direct JDK/JNI-mode proof
passed 7,773 assertions across 10k seeded operations. Corpora overlap.
**These are local Java tests with compile-only API-shaped stand-ins for
M3SlotArrays and M3IndexMaskCompactor, not actual module/native parity**.
The actual M3JDK21 source closure, Maven/JUnit, genuine C17 JNI checked
backend, full integration/HotSpot and installed JDK image need CI receipts;
none are claimed passed solely because workflows are committed.

Donor selection is category-driven: first-party M3SlotArrays,
M3LongPriorityQueue and M3BlockingDeque reusable atoms, plus
LeetCode 215, HackerRank Java BitSet and GeeksforGeeks circular queue
taxonomy. No contest/editorial/vendor source copied or relicensed.
Record serial provenance and refusal/fixed-point evidence in the
paired public receiver PR; preserve STRING → arrays → collections →
AST/compiler product admission.
