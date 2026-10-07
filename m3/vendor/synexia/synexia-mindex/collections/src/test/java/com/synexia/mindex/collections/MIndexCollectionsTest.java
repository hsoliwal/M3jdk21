// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MIndexCollectionsTest {
    private record ValueRecord(MIndexString name, int count) { }

    private static final class MutableValue {
        private int value;

        MutableValue(int value) {
            this.value = value;
        }

        int value() {
            return value;
        }

        void value(int next) {
            value = next;
        }
    }

    @Test
    void stringCollectionsUsePrimitiveIdsAndCanonicalCompositeIdentity() {
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexString alpha = MIndexString.literal("alpha");
        MIndexString beta = MIndexString.literal("beta");
        MIndexString gamma = MIndexString.literal("gamma");

        MIndexList<MIndexString> list = new MIndexList<>(strings, 2);
        list.add(alpha);
        list.add(beta);
        list.add(alpha);

        assertEquals(alpha.contentIndex(), list.idAt(0));
        assertSame(alpha.intern(), list.get(0).intern());
        assertEquals(3, list.size());

        MIndexSet<MIndexString> first = new MIndexSet<>(strings, 2);
        first.add(alpha);
        first.add(beta);
        first.add(gamma);

        MIndexSet<MIndexString> second = new MIndexSet<>(strings, 2);
        second.add(gamma);
        second.add(alpha);
        second.add(beta);

        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        assertEquals(first.canonicalId(composites), second.canonicalId(composites));

        MIndexMap<MIndexString, MIndexString> left = new MIndexMap<>(strings, strings, 2);
        left.put(alpha, beta);
        left.put(gamma, alpha);

        MIndexMap<MIndexString, MIndexString> right = new MIndexMap<>(strings, strings, 2);
        right.put(gamma, alpha);
        right.put(alpha, beta);

        assertEquals(left.canonicalId(composites), right.canonicalId(composites));
        assertNotEquals(list.canonicalId(composites), first.canonicalId(composites));
    }

    @Test
    void valueObjectSpaceUsesExactMIndexObjectCanonicalization() {
        MIndexObjectSpace<ValueRecord> space = MIndexSpaces.values();

        ValueRecord first = new ValueRecord(MIndexString.literal("same"), 7);
        ValueRecord second = new ValueRecord(MIndexString.literal("same"), 7);
        ValueRecord third = new ValueRecord(MIndexString.literal("different"), 7);

        int firstId = space.id(first);
        assertEquals(firstId, space.id(second));
        assertNotEquals(firstId, space.id(third));
        assertEquals(first, space.value(firstId));
        assertEquals(firstId, space.findId(second));
    }

    @Test
    void mutableObjectsFailClosedForValueSpaceButWorkInIdentitySpace() {
        MIndexObjectSpace<MutableValue> values = MIndexSpaces.values();
        MutableValue object = new MutableValue(1);

        assertThrows(IllegalArgumentException.class, () -> values.id(object));

        MIndexIdentitySpace<MutableValue> identities = MIndexSpaces.identities();
        int first = identities.id(object);
        object.value(2);
        assertEquals(first, identities.id(object));
        assertSame(object, identities.value(first));
        assertEquals(2, identities.value(first).value());
    }

    @Test
    void adaptiveSetPromotesWithoutChangingSemantics() {
        MIndexIdentitySpace<Object> space = new MIndexIdentitySpace<>();
        MIndexSet<Object> set = MIndexSet.dense(space, 8);
        Object[] values = new Object[12];

        for (int index = 0; index < values.length; index++) {
            values[index] = new Object();
            assertTrue(set.add(values[index]));
        }

        assertFalse(set.isDense());
        assertEquals(values.length, set.size());
        for (Object value : values) {
            assertTrue(set.contains(value));
        }
    }

    @Test
    void randomizedSparseSetAndMapMatchReferenceSemantics() {
        TestSpace keys = new TestSpace();
        TestSpace values = new TestSpace();
        MIndexSet<String> indexedSet = new MIndexSet<>(keys, 2);
        Set<String> referenceSet = new HashSet<>();
        MIndexMap<String, String> indexedMap = new MIndexMap<>(keys, values, 2);
        Map<String, String> referenceMap = new HashMap<>();
        Random random = new Random(0x5e71L);

        for (int step = 0; step < 20_000; step++) {
            String key = "k" + random.nextInt(400);
            String value = "v" + random.nextInt(300);
            switch (random.nextInt(6)) {
                case 0 -> assertEquals(referenceSet.add(key), indexedSet.add(key));
                case 1 -> assertEquals(referenceSet.remove(key), indexedSet.remove(key));
                case 2 -> assertEquals(referenceSet.contains(key), indexedSet.contains(key));
                case 3 -> {
                    boolean absent = !referenceMap.containsKey(key);
                    referenceMap.put(key, value);
                    assertEquals(absent, indexedMap.put(key, value));
                }
                case 4 -> {
                    boolean present = referenceMap.containsKey(key);
                    referenceMap.remove(key);
                    assertEquals(present, indexedMap.remove(key));
                }
                default -> assertEquals(referenceMap.get(key), indexedMap.get(key));
            }
            assertEquals(referenceSet.size(), indexedSet.size());
            assertEquals(referenceMap.size(), indexedMap.size());
        }
    }

    @Test
    void dequePriorityMultiMapTableAndGraphStayOnPrimitiveLanes() {
        TestSpace words = new TestSpace();

        MIndexDeque<String> deque = new MIndexDeque<>(words, 1);
        deque.addLast("b");
        deque.addFirst("a");
        deque.addLast("c");
        assertEquals("a", deque.removeFirst());
        assertEquals("c", deque.removeLast());
        assertEquals("b", deque.first());

        MIndexPriorityQueue<String> queue = new MIndexPriorityQueue<>(words, 1);
        queue.add("late", 20);
        queue.add("first-a", 5);
        queue.add("first-b", 5);
        assertEquals("first-a", queue.removeFirst());
        assertEquals("first-b", queue.removeFirst());
        assertEquals("late", queue.removeFirst());

        MIndexMultiMap<String, String> multi = new MIndexMultiMap<>(words, words, 1);
        assertTrue(multi.add("topic", "one"));
        assertTrue(multi.add("topic", "two"));
        assertFalse(multi.add("topic", "one"));
        assertEquals(2, multi.valueCount("topic"));
        assertTrue(multi.remove("topic", "one"));
        assertEquals(1, multi.removeAll("topic"));

        MIndexTable<String> table = new MIndexTable<>(words, 3, 1);
        table.addRowIds(new int[] {words.id("r0c0"), words.id("r0c1"), words.id("r0c2")});
        assertEquals("r0c1", table.get(0, 1));

        MIndexGraph<String, String> graph = new MIndexGraph<>(words, words, 2);
        graph.addEdge("a", "r", "b");
        graph.addEdge("a", "r", "b");
        graph.addEdge("a", "s", "c");
        MIndexGraph.Frozen<String, String> frozen = graph.freeze();

        assertEquals(2, frozen.edgeCount());
        assertEquals(2, frozen.outDegree("a"));

        assertPrimitiveArrayFields(MIndexList.class);
        assertPrimitiveArrayFields(MIndexSet.class);
        assertPrimitiveArrayFields(MIndexMap.class);
        assertPrimitiveArrayFields(MIndexDeque.class);
        assertPrimitiveArrayFields(MIndexPriorityQueue.class);
        assertPrimitiveArrayFields(MIndexTable.class);
        assertPrimitiveArrayFields(MIndexMultiMap.class);
        assertPrimitiveArrayFields(MIndexGraph.class);
        assertPrimitiveArrayFields(MIndexGraph.Frozen.class);
        assertPrimitiveArrayFields(MIndexCompositeIndex.class);
        assertPrimitiveArrayFields(MIndexTransferPlan.class);
        assertPrimitiveArrayFields(MIndexCompositeTransferPlan.class);
    }

    @Test
    void factoryUsesDeterministicPhysicalPolicyOnly() {
        MIndexCollectionFactory factory = new MIndexCollectionFactory();
        MIndexCollectionFactory.Hints dense =
                MIndexCollectionFactory.Hints.membership(256, 1024);
        MIndexCollectionFactory.Hints sparse =
                MIndexCollectionFactory.Hints.membership(8, 65_536);

        assertEquals("dense-bitset", factory.setDecision(dense).shape());
        assertEquals("sparse-open-addressed", factory.setDecision(sparse).shape());

        TestSpace words = new TestSpace();
        assertTrue(factory.set(words, dense).isDense());
        assertFalse(factory.set(words, sparse).isDense());
    }

    @Test
    void tupleAndCompositeIndexAreExactAndKindSeparated() {
        TestSpace words = new TestSpace();
        MIndexList<String> list = new MIndexList<>(words);
        list.add("a");
        list.add("b");
        MIndexTuple<String> tuple = MIndexTuple.copyOf(list);

        MIndexCompositeIndex index = new MIndexCompositeIndex();
        int tupleId = tuple.canonicalId(index);
        int tupleAgain = new MIndexTuple<>(words, tuple.copyIds()).canonicalId(index);
        int listId = list.canonicalId(index);

        assertEquals(tupleId, tupleAgain);
        assertNotEquals(tupleId, listId);
        assertEquals(2, index.length(tupleId));
        assertEquals(words.id("a"), index.valueAt(tupleId, 0));
    }

    private static void assertPrimitiveArrayFields(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (fieldType.isArray()) {
                assertTrue(
                        fieldType.getComponentType().isPrimitive(),
                        () -> type.getName() + "." + field.getName()
                                + " retains non-primitive array " + fieldType.getTypeName());
            }
        }
    }

    private static final class TestSpace implements MIndexSpace<String> {
        private final List<String> values = new ArrayList<>();
        private final Map<String, Integer> ids = new HashMap<>();

        @Override
        public int id(String value) {
            Integer existing = ids.get(value);
            if (existing != null) {
                return existing;
            }
            int id = values.size();
            values.add(value);
            ids.put(value, id);
            return id;
        }

        @Override
        public int findId(String value) {
            return ids.getOrDefault(value, -1);
        }

        @Override
        public String value(int id) {
            return values.get(id);
        }

        @Override
        public int size() {
            return values.size();
        }
    }
}
