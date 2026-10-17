# M3JDK21 filtered List view — catalogue and receiving note

This increment fills the missing generic `List` capability in the existing M3 filtered-wrapper family.

## Selected behavioral atoms

- reuse the existing live `FilteredCollection` backing/predicate owner;
- flatten nested filters onto the original owner and conjunct predicates;
- map visible List coordinates to backing coordinates on demand;
- retain no visible-index array or copied element store;
- evaluate equality-sensitive removal against the stored visible representative;
- preserve `RandomAccess` only for RandomAccess owners;
- keep hidden backing elements in place during indexed writes;
- provide backed `subList` and visible-coordinate `ListIterator` mutation;
- retain the existing unsized filtered spliterator.

The implementation is original first-party Apache-2.0 M3 code. External libraries are contract/category comparisons only; no third-party implementation or challenge solution is copied.

## Complexity

With no retained visible-index cache, `get/set/add/remove(index)` are O(n) scans in the worst case. This is deliberate: a live view over an externally mutable owner has no validity proof for a persistent index. Iteration remains O(n), O(1) traversal scratch. `RandomAccess` describes the backing capability only; it is not a claim that filtered visible-index lookup is O(1).

A future indexed/cached wrapper would require an explicit owner-version/modification contract and separate proof; it must not silently replace this live owner.

## Recipe authority

Named recipe: `com.synexia.m3jdk.FilteredListView20261017`.

Reusable engine: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.

The recipe/test/resources were committed before the three product/test postimages, and installed targets are byte-identical to their sealed templates.

## Additive branch convergence

The owner postimage also absorbs the already-qualified first-party M3JDK21 #990 `filteredSnapshot` and `filteredSortedSnapshot` APIs plus their shared one-pass `selectedCopy` atom. This avoids losing those methods when the generic filtered-wrapper and snapshot-fusion branches converge.
