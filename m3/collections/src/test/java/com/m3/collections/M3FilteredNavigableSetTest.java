// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class M3FilteredNavigableSetTest {
    @Test
    void preservesOrderingRangeSemanticsAndLiveBackingChanges() {
        NavigableSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        NavigableSet<Integer> even = M3FilteredNavigableSet.view(backing, x -> x % 2 == 0);
        assertEquals(List.of(2, 4, 6, 8), new ArrayList<>(even));
        assertEquals(4, even.size());
        assertEquals(2, even.first());
        assertEquals(8, even.last());
        assertEquals(4, even.lower(5));
        assertEquals(4, even.floor(5));
        assertEquals(6, even.ceiling(5));
        assertEquals(6, even.higher(5));
        assertEquals(List.of(6, 4, 2), new ArrayList<>(even.headSet(7, false).descendingSet()));
        assertEquals(List.of(4, 6), new ArrayList<>(even.subSet(3, true, 7, false)));
        backing.add(10);
        assertEquals(10, even.last());
        backing.remove(2);
        assertEquals(4, even.first());
        assertFalse(even.contains(3));
        assertTrue(even.contains(10));
    }

    @Test
    void readOnlyViewsRejectAllMutatorsIncludingIteratorRemove() {
        NavigableSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3));
        NavigableSet<Integer> view = M3FilteredNavigableSet.view(backing, x -> x > 1);
        assertThrows(UnsupportedOperationException.class, () -> view.add(4));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(2));
        assertThrows(UnsupportedOperationException.class, view::pollFirst);
        assertThrows(UnsupportedOperationException.class, view::pollLast);
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertThrows(UnsupportedOperationException.class, () -> view.removeIf(x -> true));
        assertThrows(UnsupportedOperationException.class, () -> view.addAll(List.of(5)));
        assertThrows(UnsupportedOperationException.class, () -> view.removeAll(List.of(2)));
        assertThrows(UnsupportedOperationException.class, () -> view.retainAll(List.of(2)));
        var iterator = view.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals(List.of(1, 2, 3), new ArrayList<>(backing));
    }

    @Test
    void reversedComparatorAndEmptyResultsKeepNavigableContracts() {
        NavigableSet<Integer> reversed = new TreeSet<>(Comparator.reverseOrder());
        reversed.addAll(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> even = M3FilteredNavigableSet.view(reversed, x -> x % 2 == 0);
        assertEquals(List.of(6, 4, 2), new ArrayList<>(even));
        assertEquals(List.of(2, 4, 6), new ArrayList<>(even.descendingSet()));
        assertEquals(6, even.first());
        assertEquals(2, even.last());
        assertEquals(6, even.lower(5));
        assertEquals(4, even.higher(5));
        NavigableSet<Integer> none = M3FilteredNavigableSet.view(reversed, x -> x < 0);
        assertTrue(none.isEmpty());
        assertThrows(java.util.NoSuchElementException.class, none::first);
        assertThrows(java.util.NoSuchElementException.class, none::last);
        assertEquals(null, none.lower(4));
    }

    @Test
    void comparatorEquivalentDifferentObjectsRemainMembers() {
        NavigableSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.add("Alpha");
        NavigableSet<String> view = M3FilteredNavigableSet.view(names, x -> true);
        assertTrue(view.contains("ALPHA"));
    }
}
