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

## Continuation: exact sorted and Set Spliterator metadata

Third serial source-file pass receives the **existing first-party donor**
`M3CollectionViews` `FilteredSet.spliterator`,
`FilteredSortedSet.spliterator`, and
`SelectionSpliterator` metadata/constructor atom. The original
`java.util.CollectionViews` remains the only runtime owner.

The new source blob is
`1df992f3258e3b8573003172d9b7ea7083ac5e18`, SHA-256
`e6c6779848e867e0898a5b577a5c6109094bb2b9ed5ba5e625b446735102e3c6`.
Filtered sorted views retain the stored comparator and
`SORTED|ORDERED|DISTINCT` characteristics, even if an underlying
`SortedSet.spliterator` does not advertise them. Split cursors retain those
guarantees, and filtered Set views retain `DISTINCT`. Existing reentrant
selection and stored-representative mutation corrections are preserved.

`test/jdk/java/util/CollectionViews/FilteredSortedSpliteratorMetadataTest.java`
is the new jtreg oracle: a deliberately metadata-sparse SortedSet owner,
natural/reverse comparators, split forwarding, stable filter order and
Set distinctness.

**Executed bounded proof:** an independent Java 21 source-extracted
Spliterator atom passed `javac --release 21 -Xlint:all -Werror`
and **4,006** checks. Maven/OpenRewrite focused tests, the patched
whole-JDK build/jtreg, native JNI and UI rendering suites remain **NOT RUN**.
No benchmark or new JNI symbol is claimed.

## Continuation — original owner CopyOnWrite bulk mutation (pass 4)

The original `java.util.CollectionViews.FilteredCollection.removeMatching` still
used `Iterator.remove` for every backing type. JDK 21 `CopyOnWriteArrayList`
and `CopyOnWriteArraySet` snapshot iterators prohibit removal, even though
the actual owning collections offer `removeIf`. The first-party
`m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java`
already contains the exact owner-sensitive correction. This pass transfers
only that existing private method atom into `java.base`.

- Preimage source Git blob `1df992f3258e3b8573003172d9b7ea7083ac5e18`
- Final source Git blob `8563e257b27a9702514dfbd7978ddcbaa8959e54`
- Final UTF-8 SHA-256 `a19d6a62821d81bc426ae14fb0f7bf5e9fad2efff6d68a232bc4a8b05b80ca8f`
- Exact COW owners use their original `removeIf` with the composed
  `visible(element) && condition.test(element)` predicate. Every other
  collection keeps its original iterator-removal path.
- Existing `clear`, `removeIf`, `removeAll` and `retainAll`
  now share the same backed mutation atom, with no new public signature,
  retained payload arrays, Entry objects, JNI ABI or compatibility layer.

Added product test:
`test/jdk/java/util/CollectionViews/FilteredCopyOnWriteBulkTest.java`.
It covers COW List/Set, hidden equality and physical row order, no-op behavior,
non-COW fallback and 10,000 seeded randomized comparisons.

**Executed on local Java21:** independently source-extracted first-party
`removeMatching` under `javac --release 21 -Xlint:all -Werror`
passed **20,012** checks, including 10,000 randomized differential trials.
The exact product test, patched full JDK build/jtreg, Maven/OpenRewrite
fixture, JNI parity and SWT/Nebula/GEF/RWT integration are **NOT RUN**.

Canonical build-time source-sealed pass and donor catalogue remain on
[Synexia PR #12865](https://github.com/hsoliwal/com.synexia/pull/12865).

## Continuation — direct owner filtered isEmpty atom (pass 5)

The existing `FilteredCollection.isEmpty()` constructed a temporary filtered
iterator only to query `hasNext()`. The pre-existing first-party
`M3CollectionViews` implementation instead scans the backing iterator
directly. Its only observable work is the same ordered predicate evaluation and
short circuit. This file-local performance pass reuses that existing private
method body; no additional object store, result mask, JNI call, public API
change, or new runtime layer.

- Preimage Git blob: `8563e257b27a9702514dfbd7978ddcbaa8959e54`
- Postimage Git blob: `7f010e155d26dd7c49aeb61120ce9b98b7e6d4fc`
- Final source SHA-256: `6b8fc8b8cf685cc3ba3ccc08dc117c963c56112d217fb29c4cdfa3bc1de203df`
- New regression source:
  `test/jdk/java/util/CollectionViews/FilteredIsEmptyDirectTest.java`

**Executed** a local isolated Java 21 comparison, passing **180,000**
assertions over 60,000 seeded traces, including exception and predicate-order
parity. **Not executed**: this branch's full patched OpenJDK build, jtreg,
Maven/OpenRewrite fixture, JNI/native and toolkit UI verification.
The authoring source and exact SHA-256 sealed recipe remain in
Synexia PR #12865. The separate existing standalone collection `isEmpty`
recipe is left unchanged.
