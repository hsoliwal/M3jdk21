// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3FilteredDequesTest {
    @Test void liveFilteredDequeContracts() {
        M3FilteredDequeContract.main(new String[0]);
    }

    @Test void existingPackedM3LinkedListIsTheOnlyPayloadOwner() {
        var backing = new M3LinkedList<Integer>();
        backing.addLast(1);
        backing.addLast(2);
        backing.addLast(3);
        backing.addLast(4);
        var selected = M3FilteredDeques.filtered(backing, n -> (n & 1) == 0);
        assertEquals(List.of(2, 4), new ArrayList<>(selected));
        Iterator<Integer> cursor = selected.iterator();
        assertEquals(2, cursor.next());
        assertTrue(cursor.hasNext()); // prefetched element must not be removed
        cursor.remove();
        assertEquals(List.of(1, 3, 4), new ArrayList<>(backing));
        assertEquals(4, selected.reversed().getFirst());
        assertFalse(selected.isEmpty());
        selected.clear();
        assertEquals(List.of(1, 3), new ArrayList<>(backing));
    }
}
