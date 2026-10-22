<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Observable indexed filtered and sorted collection views — M3JDK21

**State:** Java 21 runtime implementation + regression candidate, pending exact-head CI. Not an EMF EList implementation, SWT model replacement, new java.util API, or completed JDK21 bootstrap.

## Single-owner representation

Existing `M3ObservableList<E>` owns one `M3ArrayList<E>` dynamic `Object[]` storage. The four new additive factories (`filteredView`, `sortedView`, `filteredSortedView` and explicit-backend `filteredSortedView`) hand that **same** owned array to the already implemented `M3IndexedLiveView<E>`.

`M3IndexedLiveView` evaluates predicates into a byte mask, compacts selected physical positions via `M3IndexMaskCompactor`, then optionally stable-sorts *int* positions via `M3IndexSort`. It never copies or sorts element payloads and retains no Entry graph. Generation epochs on the **existing** underlying `M3ArrayList` automatically invalidate it after structural mutations and `set`. Mutation of the state of an already-referenced mutable object requires an explicit `invalidate()`. External concurrency requires synchronization; the view is read-only, live and fail-fast on detected modification, not an atomic snapshot.

The existing `M3ObservableList` notification listeners and generation remain authoritative. Index preparation is a read and emits no notifications. Listener events, reentrant mutation refusal and observer lifetime are untouched. The explicit JNI option sends only primitive masks and positions to the existing native ABI; predicates, comparators and listener callbacks remain on the Java side. `NATIVE_REQUIRED` refuses before evaluating Java callbacks when native support is absent.

## Complexity and alternative analysis

- Initial filtering: O(n) time, one transient byte-state mask and O(k) retained selected int positions, where k is visible elements.
- Stable indirect sort: O(k log k) comparisons and O(k) transient scratch positions; the parent `Object[]` and order remain unchanged.
- Subsequent reads without a mutation: O(1) indexed lookup, under the caller's synchronization and stable predicate assumptions.
- Build on existing algorithms; do not replace general Comparator sorting with bounded-integer counting sort or an unstable permutation algorithm.
- Catalogue: [observable-indexed-views-20261021.tsv](../../algorithm-catalogue/observable-indexed-views-20261021.tsv). LeetCode, HackerRank and GeeksforGeeks furnish *problem categories*, not copied or relicensed implementation code.

## Recipe admission and tests

- Canonical reusable OpenRewrite machinery: `M3HashPinnedJavaSnapshotRecipe` already provided by M3/Synexia. The new dataset `m3-observable-indexed-views-20261021` retains the SHA-256 old-source preimage, both exact Java output templates, and two-target sorted manifest.
- Run `mvn -B -ntp -f m3/collections/recipe/observable-indexed-views-20261021/pom.xml test`: Java LST generation from original preimage, fixed-point rerun, and drift refusal.
- Compile Java 21 collection owners and run `M3ObservableListContract` and `M3ObservableIndexedViewsContract`. New contract covers nulls, duplicates, stable comparator order, physical positions, sorted-only/filtered-only views, structural/nonstructural changes, notification order, fail-fast iterators, read-only mutation refusal, mutable referent invalidation, optional JNI refusal, and 1,200 seeded randomized differential steps against standard Java collection operations.
- GitHub Actions workflow `.github/workflows/m3-observable-indexed-views-20261021.yml` should prove these on the exact PR head before qualified promotion.
- The complete Eclipse/EMF inverse containment, proxy resolution, notification chains, transaction policies and observer scheduling still need a separate exact upstream differential gate. This Java List view does not claim their contracts.

All new authored source and tests are first-party Apache-2.0 contributions. Existing M3 donors remain in their original provenance and license records. No third-party challenge or Eclipse implementation was copied into this change.
