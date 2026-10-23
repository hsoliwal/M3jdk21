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


## Successor Java/JNI source-integrity gate (2026-10-23)

The predecessor verifier at
`qualification/filtered-navigable-consolidation-20261017/verify.sh`
remains unchanged and still rejects files diverging from its historical
pinned source. That exact-blob check was observed to fail when invoked
against this newer revision; it is **not** a failure of the new behavior
oracle. Rather than broaden historical hashes to accept arbitrary revisions,
the filtered navigable workflow in this branch now runs:

```sh
bash m3/collections/qualification/filtered-plan-20261023/verify.sh
```

This pins the current Java source owners by Git blob, compiles selected
`M3FilteredViews`, `M3FilteredNavigableSet` and
`M3SlotSpliterators` with `javac --release 21 -Xlint:all -Werror`,
runs the older two dependency-free filtered-view contracts plus a new
deep-filter/sorted/map oracle, then checks the unchanged JNI C owner and
its native-region harness using `cc -Werror -fsanitize=undefined`.

The exact inherited JNI source blob
`521eb6170fd89f3bc62eb810aa6202236a2708e2` and native test blob
`f54a9d1cecffe1049815f80be664937a7f03067f` were independently
compiled and executed locally with UBSan. Result:

```text
PASS viewport JNI regions: bounded=1024 disjoint_allocations=0 overlap_snapshot=4097 late_invalid_writes=0
```

This local JNI result is a predecessor-kernel check, not a newly changed
native implementation. The successor Java filtered-view build and all
committed JUnit tests still require passing remote or local execution;
their GitHub Actions jobs were queued when this receipt was written.
Neither this C harness nor the source-gated Java tests constitute
a full Java/JNI matched-image differential run.


## GitHub Actions successor result

Selected [GitHub Actions run #39960123479](https://github.com/hsoliwal/M3jdk21/actions/runs/39960123479)
completed successfully against product head
`f3896dec812b3999097655387ff7ea30f9b15ee5`.
The workflow's exact source receipt and Java 21 strict compilation passed.
Observed test outputs:

```text
PASS M3_FILTERED_VIEWS checks=123500 randomized=750
PASS M3FilteredNavigableFacadeSelfTest checks=117773 randomized=750
M3FilteredSortedIteratorPlanSelfTest checks=29
PASS viewport JNI regions: bounded=1024 disjoint_allocations=0 overlap_snapshot=4097 late_invalid_writes=0
PASS sealed successor Java21 and inherited JNI region contracts; no full-image or phase promotion
```

This receipt resolves the previous historical-blob CI failure for the new
branch without modifying the older 2026-10-17 source receipt. Maven JUnit,
full M3JDK21 image, and product-VM JNI differential testing remain separate
gates. No benchmark performance claim is made.
