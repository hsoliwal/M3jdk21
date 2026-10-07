// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.ELEMENT;
import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.KEY;
import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.RELATION;
import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class MIndexPrecomputeTest {
    private final MIndexSpace<Integer> space = new NumericSpace();
    private final MIndexCompositeIndex store = new MIndexCompositeIndex();
    private final MIndexCollectionPrecompute cache =
            new MIndexCollectionFactory().precompute(store, 8, 1_000_000);

    @Test
    void allNineFamiliesExposeOnlyTheirSemanticIds() {
        int[] ids = {4, 1, 4, 2};
        MIndexCanonicalCollection[] sequences = {
            MIndexFrozenList.ofIds(space, store, ids),
            MIndexFrozenDeque.ofIds(space, store, ids),
            MIndexFrozenTuple.ofIds(space, store, ids),
            MIndexFrozenSet.ofIds(space, store, ids),
            MIndexFrozenTable.ofIds(space, store, 2, ids),
            MIndexFrozenPriorityQueue.ofOrderedLane(space, store,
                    new int[] {Integer.MIN_VALUE, 0, 4, -1, -1, 1, 0, 0, 4, Integer.MAX_VALUE, -1, 2})
        };
        for (MIndexCanonicalCollection sequence : sequences) {
            MIndexCollectionMetadata metadata = sequence.precompute(cache);
            MIndexIdIndex lane = metadata.column(ELEMENT);
            assertEquals(sequence.size(), lane.size());
            assertEquals(3, lane.distinctSize());
            assertEquals(sequence instanceof MIndexFrozenSet<?> ? 1 : 2, lane.count(4));
            assertEquals(sequence.structuralHash64(), metadata.structuralHash64());
            assertEquals(sequence.signal64(), metadata.signal64());
            assertEquals(sequence.kind(), metadata.kind());
            assertFalse(metadata.hasRole(KEY));
            assertThrows(IllegalArgumentException.class, () -> metadata.column(KEY));
        }
        MIndexFrozenMap<Integer, Integer> map = MIndexFrozenMap.ofIds(space, space, store,
                new int[] {9, 4, 2, 4});
        assertEquals(2, map.precompute(cache).column(VALUE).count(4));
        MIndexFrozenMultiMap<Integer, Integer> multi = MIndexFrozenMultiMap.ofIds(space, space, store,
                new int[] {9, 4, 9, 2, 9, 4});
        assertEquals(2, multi.precompute(cache).column(KEY).count(9));
        MIndexFrozenGraph<Integer, Integer> graph = MIndexFrozenGraph.ofIds(space, space, store,
                new int[] {9, 42, 4, 9, 43, 4, 2, 42, 9});
        MIndexCollectionMetadata graphMeta = graph.precompute(cache);
        assertEquals(2, graphMeta.column(RELATION).count(42));
        assertEquals(2, graphMeta.relations().edgeCount(9, 4));
        assertEquals(2, graphMeta.relations().inDegree(4));
    }

    @Test
    void emptyShapesAndEmptySlicesHaveNoOccurrences() {
        MIndexCanonicalCollection[] empty = {
            MIndexFrozenList.ofIds(space, store, new int[0]),
            MIndexFrozenDeque.ofIds(space, store, new int[0]),
            MIndexFrozenTuple.ofIds(space, store, new int[0]),
            MIndexFrozenSet.ofIds(space, store, new int[0]),
            MIndexFrozenTable.ofIds(space, store, 3, new int[0]),
            MIndexFrozenPriorityQueue.ofOrderedLane(space, store, new int[0]),
            MIndexFrozenMap.ofIds(space, space, store, new int[0]),
            MIndexFrozenMultiMap.ofIds(space, space, store, new int[0]),
            MIndexFrozenGraph.ofIds(space, space, store, new int[0])
        };
        for (MIndexCanonicalCollection value : empty) {
            MIndexCollectionMetadata metadata = value.precompute(cache);
            for (MIndexCollectionMetadata.Role role : MIndexCollectionMetadata.Role.values()) {
                if (!metadata.hasRole(role)) { continue; }
                MIndexIdIndex lane = metadata.column(role);
                assertEquals(0, lane.size());
                assertEquals(0, lane.rank(8));
                assertEquals(1.0, lane.jaccard(lane));
                assertEquals(0, lane.slice(0, 0).size());
                assertEquals(0, MIndexRelations.successors(lane).size());
                assertThrows(IndexOutOfBoundsException.class, () -> lane.positionOf(0, 0));
            }
        }
        MIndexCollectionMetadata table = empty[4].precompute(cache);
        assertEquals(0, table.tableColumn(2).size());
    }

    @Test
    void cachedMetadataSharesExactIdentityAndSurvivesMutationAndEviction() {
        MIndexList<Integer> list = new MIndexList<>(space);
        list.addId(3);
        MIndexFrozenList<Integer> first = list.freeze(store);
        MIndexCollectionMetadata metadata = first.precompute(cache);
        assertSame(metadata, list.freeze(store).precompute(cache));
        list.addId(4);
        assertNotSame(metadata, list.freeze(store).precompute(cache));
        cache.clear();
        assertEquals(0, cache.cachedSize());
        assertEquals(0, cache.primitiveBytes());
        assertEquals(1, metadata.size());
        assertEquals(3, metadata.column(ELEMENT).idAt(0));
        assertNotSame(metadata, first.precompute(cache));
        assertEquals(1, cache.hitCount());
    }

    @Test
    void equalIntegersFromDifferentDomainsOrOwnersNeverAlias() {
        MIndexFrozenList<Integer> first = MIndexFrozenList.ofIds(space, store, new int[] {0});
        MIndexFrozenList<Integer> other = MIndexFrozenList.ofIds(new NumericSpace(), store, new int[] {0});
        MIndexIdIndex a = first.precompute(cache).column(ELEMENT);
        MIndexIdIndex b = other.precompute(cache).column(ELEMENT);
        assertNotEquals(first.canonicalId(), other.canonicalId());
        assertThrows(IllegalArgumentException.class, () -> a.intersectionSize(b));
        assertThrows(IllegalArgumentException.class, () -> a.jaccard(b));
        MIndexFrozenList<Integer> foreign = MIndexFrozenList.ofIds(space,
                new MIndexCompositeIndex(), new int[] {0});
        assertThrows(IllegalArgumentException.class, () -> foreign.precompute(cache));
    }

    @Test
    void shapedTableIndexesCellsAndExplicitColumnsWithoutHeaderPollution() {
        int nameA = MIndexSpaces.stringId("precompute-left");
        int nameB = MIndexSpaces.stringId("precompute-right");
        MIndexFrozenTable<Integer> table = MIndexFrozenTable.ofIds(space, store, 2,
                new int[] {nameA, -1, nameB, -1}, new int[] {100, 101, 100, 102, 103, 101});
        MIndexCollectionMetadata metadata = table.precompute(cache);
        assertEquals(6, metadata.column(ELEMENT).size());
        assertEquals(4, metadata.column(ELEMENT).distinctSize());
        assertEquals(2, metadata.tableColumn(0).count(100));
        assertEquals(1, metadata.tableColumn(1).positionOf(102, 0));
        MIndexRelations relation = metadata.tableRelations(0, 1);
        assertEquals(2, relation.outDegree(100));
        assertEquals(2, relation.inDegree(101));
        assertEquals(103, relation.incomingSourceAt(101, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> metadata.tableColumn(2));
        assertThrows(IllegalStateException.class, metadata::relations);
    }

    @Test
    void sparseMaximumIdsUseOccurrenceStorageInsteadOfUniverseSizedArrays() {
        MIndexIdIndex lane = lane(new int[] {Integer.MAX_VALUE, 0, Integer.MAX_VALUE});
        assertEquals(2, lane.count(Integer.MAX_VALUE));
        assertEquals(2, lane.positionOf(Integer.MAX_VALUE, 1));
        assertEquals(0, lane.sortedIdAt(0));
        assertEquals(32, lane.primitiveBytes());
        assertEquals(0, lane.count(-1));
        assertEquals(2, lane.rank(Integer.MAX_VALUE - 1) + 1);
    }

    @Test
    void recursiveCollectionIdsRemainDirectChildren() {
        MIndexFrozenList<Integer> child = MIndexFrozenList.ofIds(space, store, new int[] {9, 10});
        MIndexFrozenList<MIndexCompositeRef> parent = MIndexFrozenList.ofIds(store.space(), store,
                new int[] {child.canonicalId(), child.canonicalId()});
        MIndexIdIndex index = parent.precompute(cache).column(ELEMENT);
        assertSame(store.space(), index.space());
        assertEquals(2, index.count(child.canonicalId()));
        assertEquals(1, index.distinctSize());
    }

    @Test
    void admissionBudgetsAndZeroCapacityAreExplicit() {
        MIndexFrozenList<Integer> value = MIndexFrozenList.ofIds(space, store, new int[] {1, 2, 1});
        MIndexCollectionPrecompute tiny = new MIndexCollectionPrecompute(store, 2, 39);
        assertThrows(IllegalArgumentException.class, () -> tiny.prepare(value));
        assertEquals(0, tiny.cachedSize());
        MIndexCollectionPrecompute disabled = new MIndexCollectionPrecompute(store, 0, 40);
        assertNotSame(disabled.prepare(value), disabled.prepare(value));
        assertEquals(0, disabled.cachedSize());
        assertEquals(0, disabled.primitiveBytes());
        assertThrows(IllegalArgumentException.class, () -> new MIndexCollectionPrecompute(store, -1, 40));
        assertThrows(IllegalArgumentException.class, () -> new MIndexCollectionPrecompute(store, 1, -1));
    }

    @Test
    void sourceAndPrimitiveByteBudgetsEvictLeastRecentlyUsed() {
        MIndexCollectionPrecompute bounded = new MIndexCollectionPrecompute(store, 4, 40);
        MIndexFrozenList<Integer> a = MIndexFrozenList.ofIds(space, store, new int[] {1});
        MIndexFrozenList<Integer> b = MIndexFrozenList.ofIds(space, store, new int[] {2});
        MIndexFrozenList<Integer> c = MIndexFrozenList.ofIds(space, store, new int[] {3});
        MIndexCollectionMetadata ma = bounded.prepare(a);
        MIndexCollectionMetadata mb = bounded.prepare(b);
        assertSame(ma, bounded.prepare(a));
        bounded.prepare(c);
        assertEquals(2, bounded.cachedSize());
        assertSame(ma, bounded.prepare(a));
        assertNotSame(mb, bounded.prepare(b));
        assertTrue(bounded.primitiveBytes() <= 40);
    }

    @Test
    void randomizedLruMatchesReferenceIncludingProbeClusterDeletion() {
        int capacity = 17;
        MIndexCollectionPrecompute bounded = new MIndexCollectionPrecompute(store, capacity, 10_000);
        Map<Integer, MIndexCollectionMetadata> reference = new LinkedHashMap<>(32, 0.75f, true);
        MIndexFrozenList<?>[] values = new MIndexFrozenList<?>[129];
        for (int i = 0; i < values.length; i++) {
            values[i] = MIndexFrozenList.ofIds(space, store, new int[] {i});
        }
        Random random = new Random(0x4d494e44584c5255L);
        long hits = 0;
        for (int operation = 0; operation < 30_000; operation++) {
            int key = random.nextInt(values.length);
            MIndexCollectionMetadata expected = reference.get(key);
            MIndexCollectionMetadata actual = bounded.prepare(values[key]);
            if (expected != null) { assertSame(expected, actual); hits++; }
            else {
                reference.put(key, actual);
                if (reference.size() > capacity) { reference.remove(reference.keySet().iterator().next()); }
            }
            assertEquals(reference.size(), bounded.cachedSize());
            assertEquals(reference.size() * 16L, bounded.primitiveBytes());
        }
        assertEquals(hits, bounded.hitCount());
    }

    @Test
    void randomizedOccurrenceQueriesMatchIndependentArrayModel() {
        Random random = new Random(0x4d494e4458504f53L);
        for (int trial = 0; trial < 200; trial++) {
            int[] input = random.ints(random.nextInt(150), 0, 31).toArray();
            MIndexIdIndex index = lane(input);
            int[] sorted = input.clone();
            Arrays.sort(sorted);
            for (int ordinal = 0; ordinal < sorted.length; ordinal++) {
                assertEquals(sorted[ordinal], index.sortedIdAt(ordinal));
            }
            for (int id = -1; id <= 32; id++) {
                int count = 0;
                int from = random.nextInt(input.length + 1);
                int to = from + random.nextInt(input.length - from + 1);
                int inRange = 0;
                for (int position = 0; position < input.length; position++) {
                    if (input[position] == id) {
                        assertEquals(position, index.positionOf(id, count++));
                        if (position >= from && position < to) { inRange++; }
                    }
                }
                assertEquals(count, index.count(id));
                assertEquals(count != 0, index.containsId(id));
                assertEquals(inRange, index.countInRange(id, from, to));
            }
        }
    }

    @Test
    void setRelationsAndBoundsAreExact() {
        MIndexIdIndex a = lane(new int[] {2, 1, 2, 4});
        MIndexIdIndex b = lane(new int[] {4, 2, 8});
        assertEquals(2, a.intersectionSize(b));
        assertEquals(0.5, a.jaccard(b));
        assertFalse(a.isSubsetOf(b));
        assertTrue(a.slice(0, 1).isSubsetOf(b));
        assertEquals(1, a.countInRange(2, 1, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> a.countInRange(2, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> a.sortedPositionAt(4));
        assertThrows(IndexOutOfBoundsException.class, () -> a.idAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.positionOf(2, 2));
    }

    @Test
    void occurrenceAndWeightStorageHasOnlyPrimitiveArrayComponents() {
        for (Class<?> type : new Class<?>[] {MIndexIdIndex.class, MIndexWeights.class,
                MIndexWeightTree.class, MIndexRelations.class}) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !field.getType().isArray()) { continue; }
                assertTrue(field.getType().componentType().isPrimitive(), field.toString());
            }
        }
    }

    @Test
    void unknownImplementationsCannotImpersonateCachedBuiltInCollections() {
        MIndexFrozenList<Integer> builtIn = MIndexFrozenList.ofIds(space, store, new int[] {1});
        builtIn.precompute(cache);
        MIndexCanonicalCollection unknown = new MIndexCanonicalCollection() {
            @Override public MIndexCompositeIndex compositeIndex() { return store; }
            @Override public int canonicalId() { return builtIn.canonicalId(); }
            @Override public int kind() { return builtIn.kind(); }
            @Override public int size() { return builtIn.size(); }
            @Override public long structuralHash64() { return builtIn.structuralHash64(); }
            @Override public long signal64() { return builtIn.signal64(); }
        };
        assertThrows(IllegalArgumentException.class, () -> unknown.precompute(cache));
    }

    private MIndexIdIndex lane(int[] ids) {
        return MIndexFrozenList.ofIds(space, store, ids).precompute(cache).column(ELEMENT);
    }

    static final class NumericSpace implements MIndexSpace<Integer> {
        @Override public int id(Integer value) { requireId(value); return value; }
        @Override public int findId(Integer value) { return value == null || value < 0 ? -1 : value; }
        @Override public Integer value(int id) { requireId(id); return id; }
        @Override public int size() { return Integer.MAX_VALUE; }
        @Override public boolean containsId(int id) { return id >= 0; }
    }
}
