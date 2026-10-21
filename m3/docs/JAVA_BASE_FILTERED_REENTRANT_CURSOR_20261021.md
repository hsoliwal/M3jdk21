<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# JDK-integrated filtered/sorted collections: reentrant cursor atom

**Product**: M3JDK21 itself is Java21/java.base/HotSpot/JNI; this is an in-place
refinement of its original `java.util.CollectionViews` class, not an M3 +
JDK adapter or a competing collection. This candidate changes precisely one
private `SelectionSpliterator.tryAdvance` behavioral atom.

## Donor inventory and category decision

- Existing M3JDK21 convergence donor:
  `m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java`
  already preserves enclosing consumer and delivered state.
- Existing integrated receiver:
  `src/java.base/share/classes/java/util/CollectionViews.java`
  previously cleared the callback to null after nested `tryAdvance`,
  overwriting the delivery flag belonging to the outer traversal.
- **Category**: reentrant callback state isolation in lazy filtered stream
  cursors (ordered selection over parent-owned state).
- **Decision**: transfer the proven first-party donor's local-state restoration
  to the owning JDK file. Do not copy full wrappers, allocate cursor frames, add
  list stores, use per-element Entry objects or delegate predicates to JNI.
- Challenge platforms are **taxonomy only**, not code donors. For array/mask
  selection keep using the existing native `jdk.internal.m3.M3IndexMaskWindow`
  owner. The public filtered/sorted/navigable capabilities, predicates,
  comparators, encounter order, spliterator flags and all signatures remain
  unchanged.

## Exact source identity

Before Git blob `2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66`.
After Git blob `fb3f4bb7ccee0ba0d7c71e2c3eed6211767ba151`.
Expected SHA-256 and a strict fixed-point OpenRewrite receiving recipe are
published in Synexia branch
`m3/javabase-filtered-reentrant-source-seal-20261021-gpt6`
(`com.synexia.rewrite.M3Jdk21FilteredReentrant20261021`).

## Exact behavior and proof scope

The existing selection cursor has an `action` reference and `delivered` bit.
With a nested consumer, the inner invocation must restore both values on
return (including exceptional paths), so the outer `tryAdvance` returns true
when it delivered an element. The implementation saves two scalars/references
on the Java stack; no per-callback object is retained.

A standalone Java21 structural reproduction of the donor callback-state atom
compiled with `-Xlint:all -Werror` and passed. The new jtreg test exercises
nested exhaustion, a nested rejected-only suffix and successful nested order.
The **full patched JDK test, original java.base suite, Maven/OpenRewrite
test fixture, JNI and toolkit matrix were not executed** in this session.
Review should retain this PR as a draft until those gates are qualified.

## Continuation: original-owner stored representative atom

The second independent source-file pass receives **four existing first-party
M3CollectionViews methods** into java.base's original FilteredCollection and
FilteredSet: stored-representative membership, physical-index removal, and
identity-sensitive Set contains/remove. The donor is Git blob
`5719775a20aa06f9873e07fff0d0d6832cfdb662` and the final receiver image is
`995a35df6926b40c804270622cfc6310e0585b20`. This preserves the
already-qualified reentrant cursor pass. One contiguous JDK source owner remains
authoritative; backing ArrayList/Set state is not copied into extra stores.

A new `FilteredRepresentativeContractTest.java` covers hidden/visible equal
objects, normal and identity Sets, `CopyOnWriteArrayList`,
`CopyOnWriteArraySet`, comparator-backed TreeSet and 20,000 seeded array/list
mutations. The **isolated** Java 21 donor atom harness completed **341,997**
checks under `javac --release 21 -Xlint:all -Werror`.
This is not a completed patched whole-JDK test result.

The canonical Synexia recipe remains
`hsoliwal/com.synexia/pull/12865`, now containing individually source-sealed
pass 1 and pass 2, plus an original-to-final fixed-point receiver. Existing
native mask selectors are reused unchanged; no new JNI ABI was submitted in
this pass.
