// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Java21/JUnit execution and the original M3 packed collection owner. */
final class M3FilteredDequeStreamTest {
    @Test void adversarialLiveStreamDemand() {
        M3FilteredDequeStreamContract.main(new String[0]);
    }

    @Test void originalPackedDequeRemainsTheSoleElementOwner() {
        M3LinkedList<Integer> owner = new M3LinkedList<>();
        owner.addLast(1);
        owner.addLast(2);
        owner.addLast(3);
        owner.addLast(4);
        var selected = M3FilteredDeques.filtered(owner, x -> (x & 1) == 0);
        assertEquals(List.of(2, 4), selected.stream().toList());
        assertEquals(List.of(4, 2), selected.reversed().stream().toList());
        assertEquals(2, selected.pollFirst());
        assertEquals(List.of(1, 3, 4), new ArrayList<>(owner));
        assertTrue(selected.contains(4));
    }
}
