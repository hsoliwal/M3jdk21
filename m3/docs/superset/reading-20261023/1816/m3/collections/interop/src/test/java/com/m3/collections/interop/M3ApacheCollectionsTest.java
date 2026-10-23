/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.Predicate;
import org.junit.jupiter.api.Test;

/** Contract differential for Apache admission semantics, not a Guava live-filter oracle. */
@SuppressWarnings("deprecation")
final class M3ApacheCollectionsTest {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    // Apache's published Predicate contract is deprecated in 4.5 but remains the tested API.
=======
>>>>>>> e4ca2243770cb0267bbd84631d2bb6602f39871c
    @SuppressWarnings("deprecation")
    private static final Predicate<Integer> NONNEGATIVE = value -> value != null && value >= 0;

    @Test
    void listPreservesOrderAndValidatesMutationAtTheApacheBoundary() {
        var list = M3ApacheCollections.predicatedList(NONNEGATIVE);
        list.add(3);
        list.add(1);
        list.add(3);
        assertEquals(List.of(3, 1, 3), list);
        assertThrows(IllegalArgumentException.class, () -> list.add(-1));
        assertEquals(List.of(3, 1, 3), list);
        assertThrows(IllegalArgumentException.class, () -> list.listIterator().add(-2));
        assertEquals(List.of(3, 1, 3), list);
        var iterator = list.listIterator();
        assertEquals(3, iterator.next());
        assertThrows(IllegalArgumentException.class, () -> iterator.set(-7));
        assertEquals(List.of(3, 1, 3), list);
    }

    @Test
    void setValidatesAdmissionWithoutHidingStoredMembers() {
        var set = M3ApacheCollections.predicatedSet(NONNEGATIVE);
        assertTrue(set.add(0));
        assertFalse(set.add(0));
        assertTrue(set.add(9));
        assertThrows(IllegalArgumentException.class, () -> set.add(-2));
        assertEquals(2, set.size());
        assertTrue(set.contains(0));
        assertEquals(2, set.size());
        assertTrue(set.remove(0));
        assertFalse(set.contains(0));
    }

    @Test
    void sortedSetPreservesComparatorAndLiveBoundedView() {
        var sorted = M3ApacheCollections.predicatedSortedSet(Comparator.<Integer>reverseOrder(),
                NONNEGATIVE);
        sorted.add(1); sorted.add(5); sorted.add(3);
        assertEquals(5, sorted.first());
        assertEquals(1, sorted.last());
        assertEquals(List.of(5, 3, 1), List.copyOf(sorted));
        assertThrows(IllegalArgumentException.class, () -> sorted.add(-1));
        assertEquals(List.of(5, 3), List.copyOf(sorted.headSet(1)));
        assertThrows(IllegalArgumentException.class, () -> sorted.headSet(1).add(-3));
    }

    @Test
    void mapPreservesNullPredicateAsNoCheckAndRejectsInvalidWrites() {
        var map = M3ApacheCollections.<String, Integer>predicatedMap(Objects::nonNull,
                value -> value != null && value >= 0);
        assertEquals(null, map.put("a", 1));
        assertEquals(1, map.put("a", 2));
        assertThrows(IllegalArgumentException.class, () -> map.put(null, 7));
        assertThrows(IllegalArgumentException.class, () -> map.put("b", -7));
        assertThrows(IllegalArgumentException.class, () -> map.entrySet().iterator().next().setValue(-4));
        assertEquals(2, map.get("a"));
        var noPredicate = M3ApacheCollections.<String, Integer>predicatedMap(null, null);
        noPredicate.put(null, null);
        assertTrue(noPredicate.containsKey(null));
    }

    @Test
    void sortedMapPreservesComparatorRangesAndValueChecks() {
        var map = M3ApacheCollections.<Integer, String>predicatedSortedMap(
                Comparator.naturalOrder(), NONNEGATIVE, Objects::nonNull);
        map.put(4, "four");
        map.put(2, "two");
        map.put(6, "six");
        assertEquals(2, map.firstKey());
        assertEquals(6, map.lastKey());
        assertEquals(List.of(2, 4), List.copyOf(map.headMap(6).keySet()));
        assertThrows(IllegalArgumentException.class, () -> map.put(-1, "x"));
        assertThrows(IllegalArgumentException.class, () -> map.put(5, null));
        assertEquals(3, map.size());
    }

    @Test
    void eachFacadeOwnsIndependentBackingAndValidatesNullArguments() {
        var left = M3ApacheCollections.predicatedSet(NONNEGATIVE);
        var right = M3ApacheCollections.predicatedSet(NONNEGATIVE);
        assertNotSame(left, right);
        left.add(7);
        assertFalse(right.contains(7));
        assertThrows(NullPointerException.class, () -> M3ApacheCollections.predicatedSet(null));
        assertThrows(NullPointerException.class, () -> M3ApacheCollections.predicatedList(null));
        assertThrows(NullPointerException.class, () -> M3ApacheCollections.predicatedSortedSet(null, NONNEGATIVE));
        assertThrows(NullPointerException.class, () -> M3ApacheCollections.predicatedSortedMap(null, null, null));
    }
}
