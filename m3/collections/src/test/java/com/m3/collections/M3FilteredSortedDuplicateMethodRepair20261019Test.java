// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Java 21 read-only sorted/navigable wrapper regression for the canonical
 * single-declaration replay. Backing storage remains the original JDK owner.
 */
final class M3FilteredSortedDuplicateMethodRepair20261019Test {
    @Test
    void sortedReversedKeepsSingleBackingSourceAndStrictMutationRefusal() {
        TreeSet<Integer> parent = new TreeSet<>(
                List.of(1, 2, 3, 4, 5, 6, 7));
        SortedSet<Integer> filtered = M3FilteredViews.filter(
                (SortedSet<Integer>) parent, number -> (number & 1) == 0);
        SortedSet<Integer> reversed = filtered.reversed();
        assertEquals(List.of(6, 4, 2), new ArrayList<>(reversed));
        assertEquals(Integer.valueOf(2), filtered.first());
        assertEquals(Integer.valueOf(6), reversed.first());

        parent.add(8);
        assertEquals(List.of(8, 6, 4, 2), new ArrayList<>(reversed));
        assertEquals(Integer.valueOf(8), reversed.first());
        assertThrows(UnsupportedOperationException.class, filtered::removeFirst);
        assertThrows(UnsupportedOperationException.class, filtered::removeLast);
        assertThrows(UnsupportedOperationException.class, reversed::removeFirst);
        assertThrows(UnsupportedOperationException.class, reversed::removeLast);
        assertThrows(UnsupportedOperationException.class, () -> reversed.addFirst(10));
    }

    @Test
    void navigableReversedPreservesRangeMembershipNavigationAndBackingChanges() {
        TreeSet<Integer> parent = new TreeSet<>(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        NavigableSet<Integer> filtered = M3FilteredViews.filter(
                (NavigableSet<Integer>) parent, number -> (number & 1) == 0);
        assertEquals(Integer.valueOf(4), filtered.floor(5));
        assertEquals(Integer.valueOf(4), filtered.lower(6));
        assertEquals(Integer.valueOf(6), filtered.ceiling(5));
        assertEquals(Integer.valueOf(8), filtered.higher(6));

        NavigableSet<Integer> limited = filtered.subSet(2, true, 8, true);
        NavigableSet<Integer> descending = limited.reversed();
        assertEquals(List.of(8, 6, 4, 2), new ArrayList<>(descending));
        assertEquals(Integer.valueOf(8), descending.first());
        assertThrows(UnsupportedOperationException.class, limited::pollFirst);
        assertThrows(UnsupportedOperationException.class, descending::pollLast);
    }
}
