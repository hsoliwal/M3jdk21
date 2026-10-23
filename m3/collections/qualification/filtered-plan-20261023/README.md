<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered/sorted live-view predicate plan

Product owner: `m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java`.

This change is stacked on the qualified wrapper/JNI reconciliation branch for
M3JDK21 PR #1497. It is **part of the M3JDK21 source tree**, not a library
injected into OpenJDK. The target remains the existing `com.m3.collections`
M3 collections module; it does not claim to replace the `java.util`
collection-backend implementations.

## Contract and memory invariants

The selected atom is `CollectionView.iterator`. Deep nested read-only
filters previously enumerated wrapper predicates and allocated a new temporary
ArrayList on each iterator creation. It now constructs a shared, indexed
`Predicate[]` plan at first use, retaining immutable wrapper links and
evaluating predicates from the innermost to the outermost, as before.

The plan is **not** an element snapshot, entry cache or materialized mask.
Each traversal obtains a fresh backing iterator and re-runs the existing
predicate callbacks. The backing collection's encounter order, mutation
visibility, fail-fast policy, predicate short-circuiting, and throwable
identity remain authoritative. `NavigableSet`, `SortedSet`, sorted and
navigable maps, comparators, spliterators, and their read-only refusal
semantics retain their existing owners. API `Map.Entry` facades remain
API-facing output, never storage entries.

The plan costs O(depth) initial preparation and O(depth) retained references
per filtered view; subsequent iterator creation reuses the plan. Per-element
predicate complexity and allocation of a live backing iterator remain.
No comparative throughput/retained-heap measurement has been made.

## Source custody

Canonical one-file Maven recipe and donor catalogue:
`hsoliwal/com.synexia` branch
`m3/filtered-sorted-plan-recipe-20261023` (stacked on
`m3/jdk21-array-mask-recipe-20261023`).

Sealed baseline source SHA-256:
`687b0399d959cbeee2f9e29be3b9b2912f38dfb1b4c4e9bfb683b1adb71e3ce1`.

Sealed candidate SHA-256:
`2695ac5fe04eafd079886da5a7bb6c0adf4eb3d16d79c73ad17b4f2e26cd5617`.

The full pre/post source snapshots in the canonical recipe reproduce these
Git blobs exactly. The source change is per-file with unchanged public
signatures. Unknown/mixed revisions must be refused by the recipe.

## Contract evidence and open gates

Added JUnit:
`m3/collections/src/test/java/com/m3/collections/M3FilteredSortedIteratorPlanTest.java`.

It checks nested filter depth, live backing changes, predicate call order and
exception identity, sorted-set comparator metadata, reverse order, navigable
set/range operations, read-only mutation refusal, and detached map entries.

Run with Java 21 and Maven:

```sh
mvn -f m3/collections/pom.xml test
```

The canonical Synexia JUnit test additionally compiles the source and
candidate in isolated classloader environments and compares behaviors.
These JUnit tests have been committed but **not executed in this task**.
The existing earlier JNI source/test qualification belongs to PR #1497;
no new JNI native library or JDK image has been built for this change.
Full M3JDK21 Java/JNI/jtreg/build/performance gates remain open.

## Donor and licensing decisions

The existing first-party `M3FilteredLists.FilterChain.prepare` mechanism
is the immediate reusable donor for the lazy plan. Guava live filtered views,
Apache Commons predicated collections and algorithmic in-place removal
references were evaluated as conceptual/category comparators, but do not
share this API's strict read-only mutation semantics and were not copied.
The per-category decision table and attribution notes are in Synexia.
