// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MIndexCanonicalCollectionsV2Test {
    @Test
    void tableShapeIsPartOfCanonicalIdentity() {
        TestSpace words = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        int a = words.id("a");
        int b = words.id("b");
        int c = words.id("c");
        int d = words.id("d");

        MIndexTable<String> twoByTwo = new MIndexTable<>(words, 2);
        twoByTwo.addRowIds(new int[] {a, b});
        twoByTwo.addRowIds(new int[] {c, d});

        MIndexTable<String> oneByFour = new MIndexTable<>(words, 4);
        oneByFour.addRowIds(new int[] {a, b, c, d});

        MIndexFrozenTable<String> first = twoByTwo.freeze(composites);
        MIndexFrozenTable<String> second = oneByFour.freeze(composites);

        assertNotEquals(first.canonicalId(), second.canonicalId());
        assertEquals(2, first.rows());
        assertEquals(2, first.columns());
        assertEquals("c", first.get(1, 0));

        MIndexFrozenTable<String> changed = first.withSet(1, 1, "z");
        assertEquals("d", first.get(1, 1));
        assertEquals("z", changed.get(1, 1));
    }

    @Test
    void dequeOrderIsCanonicalButSemanticallyDistinctFromList() {
        TestSpace words = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexDeque<String> deque = new MIndexDeque<>(words);
        deque.addLast("b");
        deque.addFirst("a");
        deque.addLast("c");

        MIndexFrozenDeque<String> frozen = deque.freeze(composites);
        assertEquals("a", frozen.first());
        assertEquals("c", frozen.last());

        MIndexList<String> list = new MIndexList<>(words);
        list.add("a");
        list.add("b");
        list.add("c");

        assertNotEquals(frozen.canonicalId(), list.freeze(composites).canonicalId());
        assertEquals(frozen, frozen.withFirst("x").withoutFirst());
        assertEquals(frozen, frozen.withLast("x").withoutLast());
    }

    @Test
    void multimapCanonicalizesAsSortedRelationSet() {
        TestSpace keys = new TestSpace();
        TestSpace values = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexMultiMap<String, String> first = new MIndexMultiMap<>(keys, values);
        first.add("topic", "two");
        first.add("topic", "one");
        first.add("other", "three");

        MIndexMultiMap<String, String> second = new MIndexMultiMap<>(keys, values);
        second.add("other", "three");
        second.add("topic", "one");
        second.add("topic", "two");

        MIndexFrozenMultiMap<String, String> a = first.freeze(composites);
        MIndexFrozenMultiMap<String, String> b = second.freeze(composites);

        assertEquals(a, b);
        assertEquals(2, a.valueCount("topic"));
        assertTrue(a.containsEntry("topic", "one"));

        MIndexFrozenMultiMap<String, String> extended = a.with("topic", "four");
        assertEquals(3, extended.valueCount("topic"));
        assertEquals(a, extended.without("topic", "four"));
        assertEquals(1, a.withoutKey("topic").size());
    }

    @Test
    void priorityQueueSnapshotPreservesStablePollOrder() {
        TestSpace words = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexPriorityQueue<String> queue = new MIndexPriorityQueue<>(words);
        queue.add("late", 20);
        queue.add("first-a", 5);
        queue.add("first-b", 5);
        queue.add("middle", 10);

        MIndexFrozenPriorityQueue<String> frozen = queue.freeze(composites);

        assertEquals(4, frozen.size());
        assertEquals("first-a", frozen.get(0));
        assertEquals("first-b", frozen.get(1));
        assertEquals("middle", frozen.get(2));
        assertEquals("late", frozen.get(3));
        assertEquals(5L, frozen.firstPriority());

        MIndexFrozenPriorityQueue<String> extended = frozen.with("first-c", 5);
        assertEquals("first-a", extended.get(0));
        assertEquals("first-b", extended.get(1));
        assertEquals("first-c", extended.get(2));
        assertEquals("middle", extended.get(3));

        assertEquals("first-b", extended.withoutFirst().first());
        assertEquals("first-a", frozen.first());
    }

    private static final class TestSpace implements MIndexSpace<String> {
        private final List<String> values = new ArrayList<>();

        @Override
        public int id(String value) {
            int existing = values.indexOf(value);
            if (existing >= 0) {
                return existing;
            }
            values.add(value);
            return values.size() - 1;
        }

        @Override
        public int findId(String value) {
            return values.indexOf(value);
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
