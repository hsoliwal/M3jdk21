<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Filtered Lists — history-led additive repair, 17 October 2026

## Actual lost-code observation

GitHub identifies merged M3JDK21 PRs [#1045](https://github.com/hsoliwal/M3jdk21/pull/1045) and
[#1069](https://github.com/hsoliwal/M3jdk21/pull/1069). Their final owner
`M3FilteredLists.java` is Git blob
`e094fdde4a153eab2d1a125785bc55c68187bbd1` and SHA-256
`b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d`.
The currently selected M3JDK21 `master` instead has Git blob
`e381d23e93702cea3ccd7d9c3f7ff5e0d56baaa6` and SHA-256
`7a9d818d38beae4df178317f7913050b47d6442e85f1ef5d5873a0df89262101`,
still with the recursive predicate lambda and eager spliterator iterator binding.
The two merged historical PRs' **four self-test classes are also absent**.
This is missing production behavior, not an invitation to overwrite unrelated code.

## Recipe-first source authority

Synexia is the canonical donor/algorithm/recipe convergence workspace;
M3JDK21 is the Java product owner. Use the existing
`M3HashPinnedJavaSnapshotRecipe` under the existing Maven/OpenRewrite
module with named descriptor `com.synexia.rewrite.M3FilteredListHistoryRestore20261017`.
Exactly five Java paths are sealed; one existing source path must match
the listed preimage and the four test paths must be ABSENT.
The generated product is **exact original historical Git source**, not a
hand-reimplementation. Recipe correctness and tests must be fixed in Synexia
then the receiver re-emitted, not patched manually.

Pipeline: inventory -> chronology-checked additive convergence -> problem
catalogue -> Inline -> Atomize -> Patternize -> Comment -> compile/test
and source-contract gates -> reviewable PR. This is one FILE-local behavior
repair and does not advance the product's STRING -> arrays -> collections ->
AST/compiler receiving phase.

## Recovered contract

- Public `filtered(List,Predicate)` and `readOnlyFiltered(List,Predicate)`
  descriptors unchanged.
- Immutable FilterTerm prefix sharing and lazily prepared ordered
  `Predicate[]` evaluation avoid recursive Java stack frames.
- Deep `readOnlyFiltered` nesting fuses only this class's private
  StrictReadOnlyList/View; public writable filtering never bypasses the
  original protected source.
- Spliterator iterator is late-bound; no element payloads are copied or
  allocated per source occurrence. Existing physical position mapping,
  subList/reversed/duplicate/null behavior and mutation rules are retained.
- All arbitrary Java Predicate/Comparator calls stay in Java. The existing
  `M3IndexMaskCompactor` JNI continues as a separate opt-in primitive
  index/mask owner; no second C kernel or Java heap pointer protocol is added.

## Test and verification boundary

Canonical Maven SDK:
`mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml
-Dtest=M3FilteredListHistoryRestore20261017RecipeTest test`

JDK21 receiver selected-source strict compile and previous four original
self-test/JUnit classes must run; deep tests should be run with `-Xss256k`
in normal and interpreted modes to detect accidental recursive evaluation.
Owner `M3FilteredListsContract`, `M3FilteredListsEdges`,
`M3FilteredListsWorkProof` and selected `M3IndexMaskCompactor`
Java/JNI checks remain mandatory for wider promotion.

**Current evidence:** Historical exact source Git blobs and source
comparison have been checked; source output/template identities are pinned.
Do not label the actual new recipe's Maven SDK, new-head Actions, full
M3 reactor, JDK/Jtreg/HotSpot and JNI/GC/performance gates as passed until
executed. The previous PRs' historical test claims are not new-run proof.

LeetCode, HackerRank and GeeksforGeeks are problem-catalogue references
only, not copied implementations. Reused first-party source is Apache-2.0;
per-file original copyright and NOTICE remain intact.

## Additional original Java/JNI consumer proof

The canonical recipe now also generates `M3FilteredListsMaskBridgeProof`,
a first-party Java21 adapter test using the existing unchanged
`M3IndexMaskCompactor` Java and C17 JNI. It compares the live List's
admitted elements against detached selected physical row IDs, including
nulls, duplicate values, duplicate row IDs, high-bit state flags and 512
seeded examples. The `--native` mode requires the real JNI library and
refuses silent fallback; no Java object/predicate enters C. This extra
candidate is absent-before and hash-sealed in the same recipe.
The source hash is `02d4463f758ed57c9d5ca27c8892f5fe5776e034a0f03bae8767fde8d61a6363`.
