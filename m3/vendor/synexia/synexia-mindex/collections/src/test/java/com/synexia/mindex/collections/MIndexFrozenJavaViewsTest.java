// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

final class MIndexFrozenJavaViewsTest {
    @Test
    void frozenSetAndOrderedSetReadTheCanonicalOwner() {
        TestSpace space = new TestSpace();
        int beta = space.id("beta");
        int alpha = space.id("alpha");
        int gamma = space.id("gamma");
        MIndexCompositeIndex owner = new MIndexCompositeIndex();

        MIndexFrozenSet<String> sorted = MIndexFrozenSet.ofIds(
                space, owner, new int[] {gamma, beta, alpha, gamma});
        Iterator<String> setIterator = MIndexJavaViews.readOnlySet(sorted).iterator();
        assertNoArrayFields(setIterator);
        assertEquals(List.of("beta", "alpha", "gamma"), drain(setIterator));
        assertThrows(NoSuchElementException.class, setIterator::next);
        assertThrows(UnsupportedOperationException.class, setIterator::remove);

        MIndexFrozenOrderedSet<String> ordered =
                MIndexFrozenOrderedSet.ofIds(
                        space, owner, new int[] {gamma, beta, gamma, alpha});
        Iterator<String> orderedIterator =
                MIndexJavaViews.readOnlySet(ordered).iterator();
        assertNoArrayFields(orderedIterator);
        assertEquals(List.of("gamma", "beta", "alpha"), drain(orderedIterator));
        assertThrows(NoSuchElementException.class, orderedIterator::next);

        MIndexSet<String> builder = new MIndexSet<>(space);
        builder.add("alpha");
        builder.add("beta");
        Iterator<String> detached =
                MIndexJavaViews.readOnlySet(builder.freeze(owner)).iterator();
        builder.clear();
        builder.add("gamma");
        assertEquals(List.of("beta", "alpha"), drain(detached));
    }

    @Test
    void bagAndMapProjectOnlyRequestedEntries() {
        TestSpace space = new TestSpace();
        int beta = space.id("beta");
        int alpha = space.id("alpha");
        int gamma = space.id("gamma");
        MIndexCompositeIndex owner = new MIndexCompositeIndex();

        MIndexFrozenBag<String> bag = MIndexFrozenBag.ofIdentityCounts(
                space, owner, new int[] {gamma, 3, beta, 2, alpha, 1});
        Map<String, Integer> counts = MIndexJavaViews.readOnlyBagCounts(bag);
        Iterator<Map.Entry<String, Integer>> bagIterator =
                counts.entrySet().iterator();
        assertNoArrayFields(bagIterator);
        assertEquals(Map.of("beta", 2, "alpha", 1, "gamma", 3), counts);
        assertEquals(List.of("beta", "alpha", "gamma"),
                drainKeys(bagIterator));
        assertThrows(NoSuchElementException.class, bagIterator::next);

        MIndexFrozenMap<String, String> map = MIndexFrozenMap.ofIds(
                space, space, owner,
                new int[] {gamma, alpha, beta, gamma, alpha, beta});
        Map<String, String> projected = MIndexJavaViews.readOnlyMap(map);
        Iterator<Map.Entry<String, String>> mapIterator =
                projected.entrySet().iterator();
        assertNoArrayFields(mapIterator);
        assertEquals(Map.of(
                "beta", "gamma", "alpha", "beta", "gamma", "alpha"),
                projected);
        assertEquals(List.of("beta", "alpha", "gamma"),
                drainKeys(mapIterator));
        assertThrows(NoSuchElementException.class, mapIterator::next);
        Map.Entry<String, String> entry =
                projected.entrySet().iterator().next();
        assertThrows(UnsupportedOperationException.class,
                () -> entry.setValue("changed"));

        MIndexFrozenMap<String, String> empty =
                MIndexFrozenMap.ofIds(space, space, owner, new int[0]);
        Iterator<Map.Entry<String, String>> emptyIterator =
                MIndexJavaViews.readOnlyMap(empty).entrySet().iterator();
        assertFalse(emptyIterator.hasNext());
        assertThrows(NoSuchElementException.class, emptyIterator::next);
    }

    private static <T> List<T> drain(Iterator<T> iterator) {
        List<T> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    private static <K, V> List<K> drainKeys(
            Iterator<Map.Entry<K, V>> iterator) {
        List<K> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next().getKey());
        }
        return result;
    }

    private static void assertNoArrayFields(Iterator<?> iterator) {
        for (Field field : iterator.getClass().getDeclaredFields()) {
            assertFalse(field.getType().isArray(),
                    "iterator retains a copied lane: " + field.getName());
        }
    }

    private static final class TestSpace implements MIndexSpace<String> {
        private final List<String> values = new ArrayList<>();
        private final Map<String, Integer> ids = new HashMap<>();

        @Override
        public int id(String value) {
            Integer present = ids.get(value);
            if (present != null) {
                return present;
            }
            int next = values.size();
            values.add(value);
            ids.put(value, next);
            return next;
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

        @Override
        public boolean javaEqualityCompatible() {
            return true;
        }
    }
}
