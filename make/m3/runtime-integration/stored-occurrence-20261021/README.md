<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original `java.util.CollectionViews` additive convergence

This product candidate modifies the existing `java.base/java.util.CollectionViews`
owner, not an M3-on-JDK adapter. It reconciles **both** existing first-party
corrections without introducing a new collection owner or JNI symbol:

- Stored-occurrence membership/removal from [M3JDK21 #1773](https://github.com/hsoliwal/M3JDK21/pull/1773), canonically source-sealed by [Synexia #12866](https://github.com/hsoliwal/com.synexia/pull/12866). Its original first-party donor lineage is M3JDK21 #1116 / Synexia #12099.
- Reentrant filtered Spliterator state restoration from [M3JDK21 #1770](https://github.com/hsoliwal/M3JDK21/pull/1770), associated with [Synexia #12865](https://github.com/hsoliwal/com.synexia/pull/12865).

The legacy `RECEIPT.json` is the historical **stored-occurrence-only**
source seal. Its SHA-256 does not describe the combined postimage.
`RECEIPT-REENTRY-SUPERSET.json` identifies the combined source and
both donor layers. The original source remains retrievable by its pinned
Git object instead of duplicating a 27 KB Java source snapshot.

## Behavioral atoms

`FilteredCollection.contains` evaluates stored occurrences, not the
caller-supplied equal probe. `remove` removes the selected physical
List occurrence, including CopyOnWriteArrayList, while respecting backing
Set identity and conservative generic iterator behavior. In the existing
`SelectionSpliterator.tryAdvance`, an inner reentrant traversal restores
the outer callback and delivery state. Public methods, sorted/navigable
range owners, predicate chain, owner arrays, and native/JNI APIs are not
renamed or replaced.

The stored-occurrence correction may require O(n) lookup for exact
visible representative semantics; no throughput advantage is claimed.

## Focused Java 21 verification

With a Java 21 compiler and a complete Git object history available:

```sh
bash m3/runtime-integration/stored-occurrence-20261021/verify.sh \
  /absolute/new/evidence-directory
```

The verifier compares pinned preimage and combined postimage hashes, compiles
the actual `java.util.CollectionViews` source into before/after
`--patch-module java.base` directories, diffs public `javap` descriptors,
requires the old source to fail the stored-occurrence oracle, and runs the
combined source through the stored-occurrence oracle in default/-Xint/C1/C2
modes. It additionally runs `FilteredSpliteratorReentrantTest`,
`BootstrapViewsTest` and `CollectionViewsContract`.

The script and CI gate are included; execution must be evidenced by actual
logs. A patch-module pass does **not** establish a built M3JDK21 runtime,
complete jtreg/JCK suite, JNI/GC integration, native-memory/performance
regression, or licensing admission. Those remain separate required gates.
The canonical recipes also require a source-sealed serial composition/
fixed-point receipt against the combined postimage before promotion.
