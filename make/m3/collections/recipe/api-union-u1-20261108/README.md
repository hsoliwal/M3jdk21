# api-union-u1-longlongsortedmap-20261108

API_UNION U1, user decision 2026-11-08: admit the LongLongSortedMap union, which unlocks Synexia classes held behind it.

The Synexia `LongLongSortedMap` contract declares `int filterInPlace(LongLongPredicate)`: it retains the entries the predicate
accepts and returns the number removed. The one owner, `jdk.m3.collections.LongLongSortedMap`, lacked it. U1 grows that
owner and adds no second class:

1. `jdk.internal.m3.collections.LongLongPredicate` becomes public `jdk.m3.collections.LongLongPredicate`, beside its twin
   `LongLongConsumer`. `curate_rename_e.py` does the move with `U1_MAP.tsv` (`CURATE_CORE=jdk.internal.m3.collections`), and 16 references follow.
2. `LongLongSortedMap` gains `filterInPlace` as a default method over its own contract (snapshot, then `remove`). This is the
   JDK way of growing a public interface, as with `Collection.removeIf`: no implementer breaks. `union_u1.py` makes this
   exact, idempotent edit.
3. New test: `LongLongSortedMapFilterInPlaceTest` (3 tests) covers retention and the removed count, the
   accept-all / reject-all cases, and a null predicate.

The held Synexia classes it unlocks: 7 direct, all of which land in the next batch (packed sorted-map implementations,
PackedCollectionShapes, PackedLongLongMultiMap).
