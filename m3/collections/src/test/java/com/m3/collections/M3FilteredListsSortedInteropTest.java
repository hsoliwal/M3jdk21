// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import org.junit.jupiter.api.Test;

/** JDK21 sorted-source interop; filtering never claims a sorted List spliterator. */
class M3FilteredListsSortedInteropTest {
    @Test void sourceDefinedReverseOrderAndLiveVisibility() {
        var sorted = new M3TreeSet<Integer>(Comparator.reverseOrder());
        sorted.addAll(List.of(1, 2, 3, 4, 7, 8, 9));
        List<Integer> backing = new ArrayList<>(sorted);
        List<Integer> view = M3FilteredLists.filtered(backing, v -> (v & 1) == 0);
        assertEquals(List.of(8, 4, 2), view);
        assertEquals(List.of(2, 4, 8), view.reversed());
        assertEquals(0, view.spliterator().characteristics() &
                (Spliterator.SORTED | Spliterator.SIZED | Spliterator.DISTINCT));
        backing.add(10);
        assertEquals(List.of(8, 4, 2, 10), view);
        view.set(1, 6);
        assertEquals(List.of(9, 8, 7, 6, 3, 2, 1, 10), backing);
        assertThrows(IllegalArgumentException.class, () -> view.add(2, 5));
        assertEquals(List.of(8, 6, 2, 10), view);
    }

    @Test void readonlyEmptyAndNestedReversedMutatorsRefuse() {
        List<Integer> backing = new ArrayList<>();
        List<Integer> readonly = M3FilteredLists.readOnlyFiltered(backing, n -> n > 0);
        assertThrows(UnsupportedOperationException.class, readonly::removeFirst);
        assertThrows(UnsupportedOperationException.class, () -> readonly.addAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> readonly.reversed().add(1));
        backing.addAll(List.of(-2, 3, 1));
        assertEquals(List.of(3, 1), readonly);
        assertEquals(List.of(1, 3), readonly.reversed());
        assertThrows(UnsupportedOperationException.class, () -> readonly.subList(0, 1).clear());
    }
}
