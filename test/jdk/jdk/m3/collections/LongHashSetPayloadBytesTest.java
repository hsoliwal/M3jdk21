/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Contract of {@code LongHashSet.payloadBytes()}: the key lane plus the packed two-bit state lane, by capacity. */
final class LongHashSetPayloadBytesTest {
    private static final int SLOTS_PER_STATE_WORD = 32;
    private static final int KEYS = 10_000;

    /** The model: eight bytes per key slot, and one eight-byte word for every 32 slots of state, rounded up. */
    private static long expected(int capacity) {
        long stateWords = (capacity + SLOTS_PER_STATE_WORD - 1) / SLOTS_PER_STATE_WORD;
        return (long) capacity * Long.BYTES + stateWords * Long.BYTES;
    }

    @Test
    void followsTheCapacityThroughGrowth() {
        LongHashSet set = new LongHashSet();
        assertEquals(expected(set.capacity()), set.payloadBytes(), "empty set");
        long previous = set.payloadBytes();
        int rebuilds = 0;
        for (long key = 0; key < KEYS; key++) {
            set.add(key * 0x9E3779B97F4A7C15L);
            assertEquals(expected(set.capacity()), set.payloadBytes(), "after add " + key);
            assertTrue(set.payloadBytes() >= previous, "payload never shrinks while only adding");
            if (set.payloadBytes() != previous) rebuilds++;
            previous = set.payloadBytes();
        }
        assertTrue(rebuilds > 0, "the table was rebuilt at least once");
    }

    @Test
    void doesNotDependOnTheKeysHeld() {
        LongHashSet set = new LongHashSet(KEYS);
        long empty = set.payloadBytes();
        for (long key = 1; key <= KEYS / 2; key++) set.add(key);
        assertEquals(empty, set.payloadBytes(), "adding within the reserved capacity");
        for (long key = 1; key <= KEYS / 4; key++) set.remove(key);
        assertEquals(empty, set.payloadBytes(), "removing keeps the lanes");
        set.clear();
        assertEquals(expected(set.capacity()), set.payloadBytes(), "after clear");
    }
}
