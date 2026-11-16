/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Contract of {@code LongArrayDeque.payloadBytes()}: eight bytes for every slot of the element lane. */
final class LongArrayDequePayloadBytesTest {
    private static final int VALUES = 10_000;

    @Test
    void followsTheCapacityThroughGrowthAtBothEnds() {
        LongArrayDeque deque = new LongArrayDeque();
        assertEquals((long) deque.capacity() * Long.BYTES, deque.payloadBytes(), "empty deque");
        long previous = deque.payloadBytes();
        int replacements = 0;
        for (long value = 0; value < VALUES; value++) {
            if ((value & 1) == 0) deque.addLast(value);
            else deque.addFirst(value);
            assertEquals((long) deque.capacity() * Long.BYTES, deque.payloadBytes(), "after add " + value);
            assertTrue(deque.payloadBytes() >= previous, "payload never shrinks while only adding");
            if (deque.payloadBytes() != previous) replacements++;
            previous = deque.payloadBytes();
        }
        assertTrue(replacements > 0, "the lane was replaced at least once");
    }

    @Test
    void doesNotDependOnTheElementsHeld() {
        LongArrayDeque deque = new LongArrayDeque(VALUES);
        long empty = deque.payloadBytes();
        for (long value = 0; value < VALUES / 2; value++) deque.addLast(value);
        assertEquals(empty, deque.payloadBytes(), "adding within the reserved capacity");
        for (int count = 0; count < VALUES / 4; count++) deque.removeFirstLong();
        assertEquals(empty, deque.payloadBytes(), "removing keeps the lane");
        deque.clear();
        assertEquals((long) deque.capacity() * Long.BYTES, deque.payloadBytes(), "after clear");
    }
}
