<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 integrated collection wrappers: short-circuit filtered isEmpty

**Product:** M3JDK21. The code is modified in its original `M3CollectionViews` Java21 source owner; this is **not M3 layered around stock JDK**. Canonical reusable Maven/OpenRewrite recipe and donor decisions: [Synexia recipe candidate](https://github.com/hsoliwal/com.synexia/tree/m3/filtered-isempty-recipe-20261021/synexia-openrewrite-recipes/recipe-crates/m3jdk21-filtered-isempty-20261021). Reconciles with [M3JDK21 filtered/sorted/JNI PR #1619](https://github.com/hsoliwal/M3jdk21/pull/1619) without replacing its separate frozen 55-file candidate.

## Source and atom

Original `m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java` Git blob `7b777fcabcf7c47ab75ad0df1095663b11dcc31c`, SHA-256 `511800d4d79c7d8838ddb497f031e13827decd44c9c2790093f89ea01cdaa349`.
Candidate Git blob `a99cd0355879a2f941079e8401de00b51cd0093e`, SHA-256 `34c5ebc2cfde388f02dfee4fcaa64d1cb81058210d5dcb261bb76ef01adaf0c7`.

Replace the single `FilteredCollection.isEmpty()` terminal scan. Before: `!iterator().hasNext()` constructs a transient filtering iterator and stops when the predicate finds a visible element. After: scan `backing.iterator()` with the same `hasNext`/`next`/`visible` call order, stopping on the first visible item. This avoids only the temporary selected iterator; no external entry/payload/cache exists. Inherited by `FilteredCollection`, `FilteredSet`, `FilteredSortedSet`, `FilteredNavigableSet` (these have no `iterator()` override at the pinned source epoch).

**Contract locked:** exactly the same source `iterator()`, `hasNext()`, `next()`, predicate-call order and exceptions; null elements, duplicate object identities, live source updates, backed read-only/mutable, comparator equivalence, range checks, streams, iteration mutation and other view operations unchanged. No factory or public/protected signature change. O(k) time on first match, O(1) working locals; **no new heap-owner or entry objects**.

## JNI and native safety

All state/offset/mask primitive batching already belongs to original M3JDK21 JNI owners (e.g. `M3IndexPathMasks` and `M3IndexMaskWindow`). This generic-filter predicate can have arbitrary Java effects, so a per-item JNI conversion would change observable callbacks/exception origin and introduce avoidable crossings. **No JNI symbol/ABI changes are made here.** JNI donor improvements remain separate first-party M3JDK21 work; a Java-only atom does not claim to complete native integrations.

## Problem-category catalogue

See [CATALOGUE.tsv](CATALOGUE.tsv). Categories: `PREDICATE_ANY`, `SHORT_CIRCUIT`, `ITERATOR_EFFECTS`, `BACKED_FILTER`, `JNI_MASK`, `BINARY_SEARCH_REJECTED`. Reuse the existing JDK and Synexia owners. LeetCode/HackerRank/GeeksforGeeks entries supply categorized adversarial problems; challenge submissions/editorials are **not** copied. Review third-party license and attribution before implementing any donor.

## Execute full focused gate

```bash
bash m3/collections/qualification/filtered-isempty-20261021/verify.sh
```

This script verifies the SHA-256 source and two test postimages; uses Git's retained original preimage for strict Java21 original/candidate compilation; checks `javap -public` API identity; runs identical Java21 contract tests on baseline and candidate under normal and interpreted `-Xint` modes. Tests log original owner iterator/predicate calls and exceptions, including zero/first/last match, nulls, duplicates, source and predicate failures, sorted/navigable inherited views and late backing edits. The paired Synexia Maven/OpenRewrite JUnit tests must verify exact source rendering, drift/missing/wrong-kind/duplicate refusal and replay fixed point **before** product promotion.

**Evidence status:** canonical/product Git blob byte identity checked. Product Java21/CI and Maven/OpenRewrite are NOT_RUN in the authoring environment and must be read from actual workflows before claiming a pass. Full original M3JDK21 matched HotSpot/native/JNI/JCK, Android/Windows/macOS and allocation benchmarks are separate release gates. Draft only.
