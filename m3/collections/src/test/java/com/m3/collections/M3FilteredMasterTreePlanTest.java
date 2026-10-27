/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Current-master-specific tree range and callback-state contract oracles. */
class M3FilteredMasterTreePlanTest {
    @Test
    void boundedM3TreeMapNavigationPreservesBackingAndEndpointContracts() {
        M3TreeMap<Integer, String> owned = new M3TreeMap<>();
        for (int key = 0; key <= 18; key++) {
            owned.put(key, "v" + key);
        }
        NavigableMap<Integer, String> multiples = M3FilteredViews.filterEntries(
                (NavigableMap<Integer, String>) owned,
                (key, value) -> key % 3 == 0);

        assertEquals(List.of(0, 3, 6, 9, 12, 15, 18),
                new ArrayList<>(multiples.navigableKeySet()));
        assertEquals(6, multiples.ceilingKey(4));
        assertEquals(3, multiples.floorKey(4));
        assertEquals(9, multiples.higherKey(6));
        assertEquals(3, multiples.lowerKey(6));

        NavigableMap<Integer, String> bounded = multiples.subMap(3, true, 15, true);
        assertEquals(List.of(3, 6, 9, 12, 15),
                new ArrayList<>(bounded.navigableKeySet()));
        assertEquals(3, bounded.ceilingKey(-100));
        assertEquals(15, bounded.floorKey(999));
        assertNull(bounded.lowerKey(-100));
        assertNull(bounded.higherKey(999));

        assertThrows(UnsupportedOperationException.class, bounded::pollFirstEntry);
        assertThrows(UnsupportedOperationException.class, () -> multiples.put(21, "illegal"));
        owned.remove(6);
        owned.put(21, "v21");
        assertEquals(List.of(0, 3, 9, 12, 15, 18, 21),
                new ArrayList<>(multiples.navigableKeySet()));
        assertEquals(List.of(3, 9, 12, 15),
                new ArrayList<>(bounded.navigableKeySet()));
    }

    @Test
    void cachedPredicatePlanDoesNotCacheMutablePredicateResults() {
        List<Integer> backing = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        AtomicInteger threshold = new AtomicInteger(3);
        Collection<Integer> view = M3FilteredViews.filter(backing,
                value -> value > 0);
        for (int depth = 0; depth < 128; depth++) {
            view = M3FilteredViews.filter(view, value -> value > 0);
        }
        view = M3FilteredViews.filter(view, value -> value <= threshold.get());
        assertEquals(List.of(1, 2, 3), visitOnce(view));

        threshold.set(5);
        backing.add(6);
        assertEquals(List.of(1, 2, 3, 4, 5), visitOnce(view));
        threshold.set(1);
        assertEquals(List.of(1), visitOnce(view));
    }

    private static <T> List<T> visitOnce(Collection<T> source) {
        List<T> result = new ArrayList<>();
        source.iterator().forEachRemaining(result::add);
        return result;
    }
}
