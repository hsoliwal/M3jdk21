<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered/sorted indexed live-view callback epoch receiver

**Status:** Implemented Java 21 product-source candidate, pending actual
exact-head GitHub Actions, source-sealed Synexia Maven/OpenRewrite replay,
baseline public ABI and original JDK List differential acceptance.

## Product ownership

M3JDK21 is the standalone JDK product; `com.m3.collections.M3IndexedLiveView`
is part of its original indexed dynamic-array family, not a second M3-over-JDK
wrapper layer. The same existing `M3ArrayList` owns the `Object[]` payload.
View state remains a selected `int[]` of logical-to-physical offsets, a
transient `byte[]` filter mask, and stable index sorting via `M3IndexSort`.
An explicit existing `M3IndexMaskCompactor.Backend.NATIVE_REQUIRED` option
changes only mask compaction; Java predicates/comparators remain Java callbacks.
No Entry-per-element structure, extra element array, shared native payload
store, or JNI ABI is introduced.

## Contract-preserving behavioral atom

The old view checked the **parent** structure/value epochs in callbacks but
not the view-local invalidation epoch in the same refresh. Calling
`view.invalidate()` inside a predicate or comparator could publish stale
indices; calling `view.size()` recursively inside that callback could recurse
until stack exhaustion.

The Synexia source-sealed result introduces:
- one small `preparing` object-state flag guarding against recursive index preparation;
- one captured `invalidationEpoch` and checked preparation invariant;
- a `finally` cleanup of `preparing` on normal return and exceptional paths;
- verification after the **last comparator callback** before publishing the
  new selected index.

These are internal behavioral atoms only; the public `List<E>` methods,
ordering, null acceptance, duplicate physical indexes, backed/detached
projections, error types, and native fallback policy remain unchanged except
that previously incorrect callback invalidations now fail closed with
`ConcurrentModificationException`.

## Source custody and actual promotion authority

Only **Synexia PR
[13016](https://github.com/hsoliwal/com.synexia/pull/13016)**
owns the Maven/OpenRewrite recipe
`com.synexia.m3.M3Jdk21IndexedLiveCallbackEpoch20261022`.
Exact preimage/postimage SHA-256 values are recorded in
[`source-handoff.tsv`](source-handoff.tsv). The original M3 product file's
Git blob is `d710423f42f656b6ab5008076b6f3831118b5b12`; new owner
postimage is `fbaa5cd8b1603c1fbc75e2a6dc4e484bd4c43f0f`.
The new contract is also an exact canonical Synexia template postimage.
No executable transformation recipe was added to this public target.

## Differential and acceptance gates

- Standalone Java 21 compile of the product source + test under
  `-Xlint:all,-module -Werror`.
- Exact public Java API comparison via `javap -public` against the pinned
  original M3JDK21 baseline, without any new public owner.
- Existing `M3IndexedLiveViewContract` regression and new
  `M3IndexedLiveViewPreparationContract` including 1,500 seeded
  JDK `ArrayList` differential steps, nullable/duplicate values, stable
  physical offsets, Java 21 reversed/read-only behavior and exception paths.
- Filter invalidating its own view, comparator invalidation **after final
  value access**, recursive filter/comparator callbacks, original comparator
  exception and parent mutation in a predicate must reject partial publication.
  Subsequent safe calls must retry normally.
- Interpreter-mode (`-Xint`) execution of the new independent differential test.
- Synexia's original Java-LST recipe must pass generation, fixed-point,
  SHA-256 manifest and drift refusal gates separately at its exact PR head.

**Do not merge merely because a PR is mergeable.** JNI parity/performance,
complete JDK image building, EMF/EList, JFace/SWT, JDT and other Eclipse
compatibility layers are separate contract gates. This source refinement
does not claim those as complete.

## Algorithm catalogue

See
[`indexed-live-callback-epoch-20261022.tsv`](../../algorithm-catalogue/indexed-live-callback-epoch-20261022.tsv)
for each existing/rejected atom, complexity, licensing and LeetCode/
HackerRank/GeeksforGeeks donor-category comparison.
