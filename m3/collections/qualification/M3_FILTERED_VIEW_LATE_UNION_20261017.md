<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered/sorted primitive view owner convergence — 2026-10-17

Product runtime [M3JDK21 PR #981](https://github.com/hsoliwal/M3jdk21/pull/981)
was the receiving base, not replaced. Borrow only two proven first-party
behavior atoms and the tests from independent
[M3JDK21 PR #978](https://github.com/hsoliwal/M3jdk21/pull/978).
Canonical source recipe: [Synexia #11926](https://github.com/hsoliwal/com.synexia/pull/11926),
head `3c2ef7253b1a0f679298a8d652a67d7559d6b52c`, Apache-2.0 attribution retained.

## Real implementation / allocation
One existing `M3LongFilteredView` owns the immutable descriptor
(source reference and predicate reference), implements M3LongCollection,
and delegates to the original owner. Two source-level refinements:

- `longSpliterator()` creates its primitive iterator **only when
  consumed**; stream creation never scans/boxes/copies the source.
  Spliterator is unsplittable and reports truthful unknown size and
  NONNULL, without invented SORTED/DISTINCT flags for arbitrary backing.
- `toArray()` no longer asks `source.size()` before filtering. Growth
  buffers are primitive `long[]`, allocated only when a matching element
  appears, and the final result is detached. No `Long`, `Map.Entry`,
  per-element node or second payload storage system.

Existing nested short-circuit filters, `clear()` refusal, sorted
snapshots, order/range semantics and the target C17 JNI kernel remain
unchanged. An explicit JDK-facing iterator or stream descriptor still
requires one control object per traversal—this is **not** a promise of
zero objects per public API invocation.

## Source custody and tests
Three runtime/test targets equal reviewed Synexia source templates:
one MODIFY preimage and two ABSENT-before Java tests. This branch's
manifest and `verify_filtered_view_late_union.py` refuse missing,
symlinked or drifted outputs; original files and source SHA-256 are
retained by the recipe. New adapted standalone test exercises the donor's
500-seed differential, late binding, lazy result allocation, predicate
order, exception/refusal contract, plus direct sortedSnapshot,
filteredSortedSnapshot and maskedSortedSnapshot compatibility. Existing
primitive mask List/Deque/Set/Map/PriorityQueue tests and JNI proof are
rerun in the new focused Java21 workflow.

Pending: exact hosted CI conclusion, Maven/OpenRewrite SDK/fixed point,
full JUnit/reactor, installed-JDK java.base/java.util adoption,
provider-complete JDK/Guava/Eclipse/Commons/Spring compatibility,
memory/throughput benchmarks, multi-architecture and independent review.
String → arrays → collections → AST/compiler receiving policy still holds.
