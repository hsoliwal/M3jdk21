/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrimitiveCollectionsTest {
    @Test
    void longIntMapSupportsAllLongKeysWithoutEntryObjects() {
        LongIntHashMap map = new LongIntHashMap(1);
        long[] special = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE};
        for (int i = 0; i < special.length; i++) assertTrue(map.put(special[i], i + 10));
        for (int i = 0; i < 10_000; i++) map.put(0x9e3779b97f4a7c15L * i, i);

        assertEquals(10_003, map.size());
        assertEquals(11, map.getOrDefault(-1L, -99));
        assertEquals(12, map.getOrDefault(Long.MIN_VALUE, -99));
        assertTrue(map.containsKey(Long.MAX_VALUE));

        for (int i = 1; i < 10_000; i += 2) {
            assertTrue(map.remove(0x9e3779b97f4a7c15L * i));
        }
        for (int i = 1; i < 10_000; i += 2) {
            assertEquals(-1, map.getOrDefault(0x9e3779b97f4a7c15L * i, -1));
        }
        for (int i = 1; i < 10_000; i += 2) {
            map.put(0x9e3779b97f4a7c15L * i, i + 1);
        }

        assertEquals(10_003, map.size());
        assertTrue(map.capacity() > 16);
        assertTrue(map.estimatedPayloadBytes() > 0L);
    }

    @Test
    void longLongMapAndSetPreserveZeroAndNegativeKeys() {
        LongLongHashMap map = new LongLongHashMap();
        assertTrue(map.put(0L, Long.MIN_VALUE));
        assertTrue(map.put(-7L, 99L));
        assertEquals(Long.MIN_VALUE, map.getOrDefault(0L, 1L));
        assertEquals(99L, map.getOrDefault(-7L, 1L));
        assertTrue(map.remove(-7L));
        assertFalse(map.containsKey(-7L));

        LongHashSet set = new LongHashSet();
        assertTrue(set.add(0L));
        assertTrue(set.add(-1L));
        assertFalse(set.add(0L));
        assertTrue(set.contains(-1L));
        assertTrue(set.remove(0L));
        assertFalse(set.contains(0L));
    }

    @Test
    void primitiveVectorsDoNotBoxElements() {
        IntVector ints = new IntVector(0);
        LongVector longs = new LongVector(0);
        ByteVector bytes = new ByteVector(0);
        for (int i = 0; i < 1_000; i++) {
            ints.add(i);
            longs.add(Integer.toUnsignedLong(i));
            bytes.addUnsigned(i & 0xff);
        }
        assertEquals(999, ints.get(999));
        assertEquals(999L, longs.get(999));
        assertEquals(999 & 0xff, bytes.getUnsigned(999));
        assertEquals(1_000, ints.size());
    }
}
