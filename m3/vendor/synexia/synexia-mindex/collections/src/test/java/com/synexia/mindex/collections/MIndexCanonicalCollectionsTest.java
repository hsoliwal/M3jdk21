// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.mindex.MIndexString;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MIndexCanonicalCollectionsTest {
    private record Point(int x, int y) { }

    @Test
    void compositeIdentityIncludesOwningSpaces() {
        MIndexObjectSpace<Point> leftSpace = MIndexSpaces.values();
        MIndexObjectSpace<Point> rightSpace = MIndexSpaces.values();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexList<Point> left = new MIndexList<>(leftSpace);
        MIndexList<Point> right = new MIndexList<>(rightSpace);
        left.add(new Point(1, 2));
        right.add(new Point(1, 2));

        assertEquals(0, left.idAt(0));
        assertEquals(0, right.idAt(0));
        assertNotEquals(left.canonicalId(composites), right.canonicalId(composites));
    }

    @Test
    void frozenListIsAFirstClassCanonicalValue() {
        MIndexSpace<MIndexString> strings = MIndexSpaces.strings();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        MIndexString alpha = MIndexString.literal("alpha");
        MIndexString beta = MIndexString.literal("beta");
        MIndexString gamma = MIndexString.literal("gamma");

        MIndexList<MIndexString> first = new MIndexList<>(strings);
        first.add(alpha);
        first.add(beta);
        MIndexList<MIndexString> second = new MIndexList<>(strings);
        second.add(alpha);
        second.add(beta);

        MIndexFrozenList<MIndexString> a = first.freeze(composites);
        MIndexFrozenList<MIndexString> b = second.freeze(composites);

        assertEquals(a, b);
        assertEquals(a.canonicalId(), b.canonicalId());
        assertSame(alpha.intern(), a.get(0).intern());

        MIndexFrozenList<MIndexString> extended = a.withAdded(gamma);
        assertEquals(2, a.size());
        assertEquals(3, extended.size());
        assertEquals(a, extended.withoutIndex(2));
        assertEquals(a.signal64(), composites.signal64(a.canonicalId()));
    }

    @Test
    void frozenSetAlgebraUsesSortedPrimitiveLanes() {
        TestSpace words = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexSet<String> first = new MIndexSet<>(words);
        first.add("c");
        first.add("a");
        first.add("b");

        MIndexSet<String> second = new MIndexSet<>(words);
        second.add("b");
        second.add("c");
        second.add("a");

        MIndexFrozenSet<String> a = first.freeze(composites);
        MIndexFrozenSet<String> b = second.freeze(composites);
        assertEquals(a, b);

        MIndexFrozenSet<String> d = MIndexFrozenSet.ofIds(
                words,
                composites,
                new int[] {words.id("b"), words.id("d")});

        MIndexFrozenSet<String> union = a.union(d);
        MIndexFrozenSet<String> intersection = a.intersection(d);
        MIndexFrozenSet<String> difference = a.difference(d);

        assertEquals(4, union.size());
        assertTrue(union.contains("d"));
        assertEquals(1, intersection.size());
        assertTrue(intersection.contains("b"));
        assertEquals(2, difference.size());
        assertFalse(difference.contains("b"));
    }

    @Test
    void frozenMapIsInsertionOrderIndependentAndPersistent() {
        TestSpace keys = new TestSpace();
        TestSpace values = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();

        MIndexMap<String, String> first = new MIndexMap<>(keys, values);
        first.put("a", "1");
        first.put("b", "2");

        MIndexMap<String, String> second = new MIndexMap<>(keys, values);
        second.put("b", "2");
        second.put("a", "1");

        MIndexFrozenMap<String, String> a = first.freeze(composites);
        MIndexFrozenMap<String, String> b = second.freeze(composites);

        assertEquals(a, b);
        assertEquals("1", a.get("a"));

        MIndexFrozenMap<String, String> changed = a.with("a", "3");
        assertEquals("1", a.get("a"));
        assertEquals("3", changed.get("a"));
        assertEquals(a, changed.with("a", "1"));
        assertEquals(1, changed.without("b").size());
    }

    @Test
    void tupleAndListRemainSemanticallySeparated() {
        TestSpace words = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        MIndexList<String> list = new MIndexList<>(words);
        list.add("x");
        list.add("y");

        MIndexTuple<String> tuple = MIndexTuple.copyOf(list);
        MIndexFrozenList<String> frozenList = list.freeze(composites);
        MIndexFrozenTuple<String> frozenTuple = tuple.freeze(composites);

        assertNotEquals(frozenList.canonicalId(), frozenTuple.canonicalId());
        assertNotEquals(frozenList.kind(), frozenTuple.kind());
        assertEquals(words.id("x"), frozenTuple.idAt(0));
    }

    @Test
    void compositeLookupIsNonAdmittingAndExact() {
        TestSpace words = new TestSpace();
        MIndexCompositeIndex composites = new MIndexCompositeIndex();
        int[] lane = {words.id("a"), words.id("b")};

        int id = composites.intern(
                MIndexCompositeIndex.KIND_LIST,
                words,
                null,
                lane);
        int size = composites.size();

        assertEquals(
                id,
                composites.findId(
                        MIndexCompositeIndex.KIND_LIST,
                        words,
                        null,
                        lane));
        assertEquals(
                -1,
                composites.findId(
                        MIndexCompositeIndex.KIND_LIST,
                        words,
                        null,
                        new int[] {lane[0], words.id("c")}));
        assertEquals(size, composites.size());
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
