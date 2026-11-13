<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Actual M3 generic object collections (2026-10-17)

Canonical owner: `hsoliwal/com.synexia`, reusable Maven source-sealed recipe. Target product: `hsoliwal/M3jdk21`, independently built Apache-2.0 `m3/collections` module. Keep the canonical String → arrays → collections migration order. This is an additive, isolated generic-object lane, not a replacement for JDK `java.util` classes or a claim of provider-equivalent Guava/Eclipse/Spring/Apache APIs.

## Real Java object collection implementations

- `M3ArrayList<E>`: a real mutable JDK `List<E>` / `RandomAccess` backed by a growable, GC-traced `Object[]`, retaining null values, duplicate object references and encounter order. Supports indexed get/set/add/remove, alias-safe batch insertion, backed `subList`, `ListIterator` operations, fail-fast best-effort structural epoch, clone, own serialized form, typed toArray. `removeIf` first evaluates the predicate once per occurrence into a dynamically sized `BitSet`, then commits one stable reference-array compaction on success. It does not assert concrete JDK `ArrayList` class/serialization/subclass identity.
- `M3IndexedListSnapshot<E>`: independent immutable generic `List<E>` backed by a privately owned `Object[]` and an `int[]` permutation. Filter predicates run once and produce selected positions, not object-entry nodes; stable merge sort reorders only indexes, preserving equal-valued distinct occurrences; slicing uses `from/to` offsets and shares immutable snapshot storage. `BitSet` membership is prepared on demand and bounded to actual positions; no `BitSet(Integer.MAX_VALUE)`. All mutation entry points reject even no-op changes.
- `M3GenericCollections`: real generic object factory, not a primitive-long substitute. Existing `M3Collections.arrayList()` continues returning JDK `ArrayList<E>` to avoid changing its established concrete return type. Existing generic maps, sets, trees, linked lists and live filtered wrappers remain independent contract owners.
- `M3GenericArrayListSelfTest` differential tests against the real JDK `ArrayList`; JUnit adapter invokes it in the owning Maven module.

Optional JNI never marshals `Object[]`, Java `Predicate` or `Comparator`: it delegates the detached `int[]` physical row IDs and `byte[]` selection mask to existing `M3IndexMaskCompactor` Java/C17 without a new ABI or native lifetime owner. Native `NATIVE_REQUIRED` refuses before evaluating callbacks when unavailable.

## Mandatory recipe generation

`com.synexia.m3.M3GenericObjectListRecipe` is a Maven-bound, hash-sealed, **add-only** source generator. The five source files above are generated **exactly** from SHA-256 verified templates. All target destinations are preflighted before writing: missing outputs refuse `--check`; occupied/modified/symlinked outputs refuse `--apply`; correct source reaches a fixed point after repeated apply. Fix the recipe templates and rerun, not the emitted target files. Existing-source edits require a separately qualified Maven/OpenRewrite AST transformation; this source-only initial installer never modifies existing production source.

## Executed selected-source evidence and pending gates

Java 21 strict compile `javac --release 21 -proc:none -Xlint:all -Werror` against the **actual existing M3IndexMaskCompactor** passed. Deterministic JDK list differential: **960,955** assertions (Java-only). The same oracle under `-Xcheck:jni` after strict GCC C17/UBSan build of the existing native mask selector passed **960,583** assertions (native true). Counts differ because native-present and native-absent modes exercise different contract branches. Direct-Java source generator test PASSED missing/drift/symlink refusal and fixed point.

Local Maven is unavailable; hosted exact-head Maven recipe execution, full owning-module JUnit, whole JDK/jtreg/JCK, GC/JMM, cross-platform native and comparative CPU/heap measurements remain PENDING. No throughput winner or public JDK replacement claimed. **Draft/HOLD until owning gates pass**.

## Donor selection and licensing

The `CATEGORY_REVIEW.tsv` records category-by-category comparison and runtime choice. Actual source is first-party Apache-2.0 with Copyright 2026 Hitesh Soliwal and contributors. OpenJDK `List` contract is a behavioral oracle; JDK ArrayList and existing M3 metadata operators are donor/reference designs. LeetCode, HackerRank, GeeksforGeeks and Guava/Eclipse provider APIs are problem-category or semantics references only. No challenge submission, OpenJDK source, Guava/Eclipse/Apache/Spring implementation, or incompatible-license code was copied.
