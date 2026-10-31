/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Contract assertions for indexed predicate planning and live sorted wrappers. */
class M3FilteredSortedIteratorPlanTest {

    @Test
    void deepFiltersReuseTheSameLogicalPipelineWithoutFreezingElements() {
        List<Integer> backing = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collection<Integer> view = backing;
        for (int depth = 0; depth < 1_200; depth++) {
            view = M3FilteredViews.filter(view, value -> value >= 0);
        }
        view = M3FilteredViews.filter(view, value -> (value & 1) == 0);

        assertEquals(List.of(0, 2, 4), new ArrayList<>(view));
        backing.add(6);
        assertEquals(List.of(0, 2, 4, 6), new ArrayList<>(view));
        backing.remove(Integer.valueOf(2));
        assertEquals(List.of(0, 4, 6), new ArrayList<>(view));
        assertEquals(3, view.size());
    }

    @Test
    void predicateOrderShortCircuitAndExceptionIdentityAreRetained() {
        List<Integer> backing = new ArrayList<>(List.of(1, 2, 3));
        List<String> calls = new ArrayList<>();
        Collection<Integer> inner = M3FilteredViews.filter(backing, value -> {
            calls.add("inner:" + value);
            return value != 2;
        });
        Collection<Integer> outer = M3FilteredViews.filter(inner, value -> {
            calls.add("outer:" + value);
            return true;
        });
        assertEquals(List.of(1, 3), consumeExactlyOnce(outer));
        assertEquals(List.of("inner:1", "outer:1", "inner:2", "inner:3", "outer:3"),
                calls);

        calls.clear();
        assertEquals(List.of(1, 3), consumeExactlyOnce(outer));
        assertEquals(List.of("inner:1", "outer:1", "inner:2", "inner:3", "outer:3"),
                calls);

        IllegalStateException sentinel = new IllegalStateException("predicate sentinel");
        Collection<Integer> failed = M3FilteredViews.filter(outer, value -> {
            throw sentinel;
        });
        assertTrue(sentinel == assertThrows(IllegalStateException.class,
                () -> failed.iterator().hasNext()));
    }

    @Test
    void sortingAndReversingRemainLiveAndReadOnly() {
        TreeSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> even = M3FilteredViews.filter((NavigableSet<Integer>) backing,
                value -> (value & 1) == 0);
        assertEquals(List.of(2, 4, 6), new ArrayList<>(even));
        assertEquals(List.of(6, 4, 2), new ArrayList<>(even.descendingSet()));
        assertEquals(4, even.floor(5));
        assertEquals(6, even.ceiling(5));
        assertEquals(2, even.first());
        assertEquals(6, even.last());
        assertThrows(UnsupportedOperationException.class, even::pollFirst);
        assertThrows(UnsupportedOperationException.class, () -> even.remove(2));

        backing.add(8);
        assertEquals(List.of(2, 4, 6, 8), new ArrayList<>(even));
        assertEquals(8, even.last());
    }

    @Test
    void sortedSetComparatorAndSplitMetadataSurviveFiltering() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        SortedSet<Integer> backing = new TreeSet<>(reverse);
        backing.addAll(List.of(2, 1, 3, 5, 4));
        SortedSet<Integer> filtered = M3FilteredViews.filter(backing, x -> (x & 1) == 1);
        assertEquals(List.of(5, 3, 1), new ArrayList<>(filtered));
        Spliterator<Integer> cursor = filtered.spliterator();
        assertTrue(cursor.hasCharacteristics(Spliterator.SORTED));
        assertEquals(reverse, cursor.getComparator());

        backing.add(7);
        assertEquals(List.of(7, 5, 3, 1), new ArrayList<>(filtered));
        SortedSet<Integer> smaller = filtered.tailSet(3);
        assertEquals(List.of(3, 1), new ArrayList<>(smaller));
        assertThrows(UnsupportedOperationException.class, () -> filtered.add(9));
        assertThrows(UnsupportedOperationException.class, () -> filtered.removeFirst());
    }

    @Test
    void sortedMapEntriesStayLiveAndApiEntriesAreDetached() {
        NavigableMap<Integer, String> backing = new TreeMap<>();
        for (int i = 0; i < 8; i++) backing.put(i, "v" + i);
        NavigableMap<Integer, String> filtered =
                M3FilteredViews.filterEntries(backing, (key, value) -> (key & 1) == 0);
        assertEquals(List.of(0, 2, 4, 6), new ArrayList<>(filtered.navigableKeySet()));
        assertEquals(4, filtered.ceilingKey(3));
        assertEquals(2, filtered.floorKey(3));

        var entry = filtered.firstEntry();
        assertEquals(0, entry.getKey());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue("other"));
        assertThrows(UnsupportedOperationException.class, filtered::pollLastEntry);
        Iterator<?> cursor = filtered.entrySet().iterator();
        assertTrue(cursor.hasNext());
        cursor.next();
        assertThrows(UnsupportedOperationException.class, cursor::remove);

        backing.remove(0);
        backing.put(8, "v8");
        assertFalse(filtered.containsKey(0));
        assertEquals(List.of(2, 4, 6, 8), new ArrayList<>(filtered.navigableKeySet()));
    }

    private static <E> List<E> consumeExactlyOnce(Collection<E> source) {
        List<E> result = new ArrayList<>();
        Iterator<E> iterator = source.iterator();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }
}
