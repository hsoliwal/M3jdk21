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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real Java 21 empty-endpoint/reversed/backed filtering contract. */
final class M3FilteredSortedReadOnlyForward20261019Test {
    @Test void rejectsEndpointWritesWithoutInspectingAnEmptyFilteredSource() {
        TreeSet<Integer> parent = new TreeSet<>(List.of(1, 3, 5));
        AtomicInteger invocations = new AtomicInteger();
        java.util.function.Predicate<Integer> even = n -> {
            invocations.incrementAndGet();
            return (n & 1) == 0;
        };
        SortedSet<Integer> sorted = M3FilteredViews.filter((SortedSet<Integer>) parent, even);
        NavigableSet<Integer> navigable = M3FilteredViews.filter(
                (NavigableSet<Integer>) parent, even);

        assertThrows(UnsupportedOperationException.class, sorted::removeFirst);
        assertThrows(UnsupportedOperationException.class, sorted::removeLast);
        assertThrows(UnsupportedOperationException.class, sorted.reversed()::removeFirst);
        assertThrows(UnsupportedOperationException.class, sorted.reversed()::removeLast);
        assertThrows(UnsupportedOperationException.class, navigable::removeFirst);
        assertThrows(UnsupportedOperationException.class, navigable::removeLast);
        assertThrows(UnsupportedOperationException.class, navigable.reversed()::removeFirst);
        assertThrows(UnsupportedOperationException.class, navigable.reversed()::removeLast);
        assertEquals(0, invocations.get(), "rejected writes must not run predicates");
    }

    @Test void sortedReversalAndNavigableRangesRetainOneLiveBackingOwner() {
        TreeSet<Integer> parent = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6, 7));
        SortedSet<Integer> sorted = M3FilteredViews.filter((SortedSet<Integer>) parent,
                n -> (n & 1) == 0);
        SortedSet<Integer> reverse = sorted.reversed();
        assertEquals(List.of(6, 4, 2), new ArrayList<>(reverse));
        parent.add(8);
        assertEquals(List.of(8, 6, 4, 2), new ArrayList<>(reverse));
        assertEquals(Integer.valueOf(8), reverse.first());
        assertEquals(Integer.valueOf(2), reverse.last());
        assertThrows(UnsupportedOperationException.class, reverse::removeFirst);

        NavigableSet<Integer> selected = M3FilteredViews.filter(
                (NavigableSet<Integer>) parent, n -> (n & 1) == 0);
        assertEquals(Integer.valueOf(4), selected.floor(5));
        assertEquals(Integer.valueOf(6), selected.ceiling(5));
        NavigableSet<Integer> range = selected.subSet(2, true, 8, true);
        assertEquals(List.of(8, 6, 4, 2), new ArrayList<>(range.reversed()));
        parent.remove(8);
        assertEquals(List.of(6, 4, 2), new ArrayList<>(range.reversed()));
        assertThrows(UnsupportedOperationException.class, range.reversed()::removeFirst);
    }
}
