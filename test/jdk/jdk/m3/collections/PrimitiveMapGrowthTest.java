/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Product regressions for the existing primitive array map owners and growth boundaries. */
final class PrimitiveMapGrowthTest {
    @Test
    void longByteGrowthRemovalAndUnsignedValuesMatchJdkMap() {
        LongByteHashMap map = new LongByteHashMap(0, 0.65f);
        Map<Long, Byte> oracle = new HashMap<>();
        Random random = new Random(0x13b17eL);
        for (int step = 0; step < 12_000; step++) {
            long key = key(random, step);
            byte value = (byte) random.nextInt();
            if (step % 5 == 0) {
                assertEquals(orDefault(oracle.remove(key)), map.remove(key));
            } else {
                byte expected = orDefault(oracle.put(key, value));
                byte actual = step % 2 == 0
                        ? map.putUnsigned(key, Byte.toUnsignedInt(value)) : map.put(key, value);
                assertEquals(expected, actual);
            }
            assertEquals(oracle.size(), map.size());
            assertEquals(oracle.containsKey(key), map.containsKey(key));
            assertEquals(oracle.getOrDefault(key, (byte) 71).byteValue(), map.getOrDefault(key, (byte) 71));
            assertEquals(Byte.toUnsignedInt(orDefault(oracle.get(key))), map.getUnsigned(key));
            if (step % 127 == 0) assertByteContents(oracle, map);
        }
        assertByteContents(oracle, map);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.put(0, (byte) 99));
        assertEquals(99, map.remove(0));
        assertFalse(map.containsKey(0));
    }

    @Test
    void longIntSingleProbeUpdatesAndWraparoundMatchJdkMap() {
        LongIntHashMap map = new LongIntHashMap(0, 0.65f);
        Map<Long, Integer> oracle = new HashMap<>();
        Random random = new Random(0x171317L);
        for (int step = 0; step < 12_000; step++) {
            long key = key(random, step);
            int value = random.nextInt();
            switch (step % 4) {
                case 0 -> assertEquals(orDefault(oracle.remove(key)), map.remove(key));
                case 1 -> {
                    int next = orDefault(oracle.get(key)) + value;
                    oracle.put(key, next);
                    assertEquals(next, map.addTo(key, value));
                }
                default -> assertEquals(orDefault(oracle.put(key, value)), map.put(key, value));
            }
            assertEquals(oracle.size(), map.size());
            assertEquals(oracle.containsKey(key), map.containsKey(key));
            assertEquals(oracle.getOrDefault(key, 71).intValue(), map.getOrDefault(key, 71));
            if (step % 127 == 0) assertIntContents(oracle, map);
        }
        assertIntContents(oracle, map);
        for (long key : new long[]{0, Long.MIN_VALUE, Long.MAX_VALUE, -1}) {
            map.put(key, Integer.MAX_VALUE);
            assertEquals(Integer.MIN_VALUE, map.addTo(key, 1));
            assertEquals(0, map.addTo(key, Integer.MIN_VALUE));
            assertTrue(map.containsKey(key));
        }
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    void exactWidthObjectMapsPreserveAllEightKeyContractsAndNullValues() {
        Object absent = new Object();
        Random random = new Random(0x8a13L);
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            PrimitiveObjectHashMap<Object> map = new PrimitiveObjectHashMap<>(kind, 0, 0.65f);
            Map<Object, Object> oracle = new HashMap<>();
            for (int step = 0; step < 3_000; step++) {
                long bits = key(random, step);
                Object key = boxedKey(kind, bits);
                Object value = step % 11 == 0 ? null : new Object();
                boolean present = oracle.containsKey(key);
                Object expected = present ? oracle.get(key) : absent;
                if (step % 5 == 0) {
                    oracle.remove(key);
                    assertSame(expected, map.removeOrDefault(bits, absent), kind.toString());
                } else {
                    oracle.put(key, value);
                    assertSame(expected, map.put(bits, value, absent), kind.toString());
                }
                assertEquals(oracle.size(), map.size(), kind.toString());
                assertEquals(oracle.containsKey(key), map.containsKeyBits(bits), kind.toString());
                assertSame(oracle.getOrDefault(key, absent), map.getOrDefault(bits, absent), kind.toString());
                if (step % 127 == 0) assertObjectContents(oracle, map);
            }
            assertObjectContents(oracle, map);
            assertEquals(kind.widthBytes() * (long) map.capacity(), map.primitivePayloadBytes());
            assertEquals(map.capacity(), map.referenceSlots());
            map.clear();
            assertTrue(map.isEmpty());
            assertSame(absent, map.getOrDefault(0, absent));
        }
    }

    @Test
    void floatingNaNsAndSignedZerosKeepTheirKeyAndValueIdentitiesDuringGrowth() {
        verifyFloating(PrimitiveKind.FLOAT, 0x80000000L, 0x7fc01234L, 0x7fffffffL);
        verifyFloating(PrimitiveKind.DOUBLE, Long.MIN_VALUE, 0x7ff8000000001234L, 0x7fffffffffffffffL);
    }

    @Test
    void tinyLoadFactorsKeepExistingZeroUpdateGrowthAndPayloadAccounting() {
        LongByteHashMap bytes = new LongByteHashMap(0, 0.01f);
        LongIntHashMap ints = new LongIntHashMap(0, 0.01f);
        assertEquals(8, bytes.capacity());
        assertEquals(8, ints.capacity());
        bytes.put(17, (byte) 1);
        ints.put(17, 1);
        bytes.put(0, (byte) 2);
        ints.put(0, 2);
        assertEquals(16, bytes.capacity());
        assertEquals(16, ints.capacity());
        for (int capacity : new int[]{32, 64, 128, 256}) {
            assertEquals(2, bytes.put(0, (byte) 2));
            assertEquals(2, ints.put(0, 2));
            assertEquals(capacity, bytes.capacity());
            assertEquals(capacity, ints.capacity());
            assertEquals(2, bytes.size());
            assertEquals(2, ints.size());
            assertEquals(capacity * 9L, bytes.payloadBytes());
            assertEquals(capacity * 12L, ints.payloadBytes());
        }
        bytes.put(0, (byte) 3);
        ints.put(0, 3);
        assertEquals(256, bytes.capacity());
        assertEquals(256, ints.capacity());

        PrimitiveObjectHashMap<String> objects = new PrimitiveObjectHashMap<>(PrimitiveKind.LONG, 0, 0.01f);
        objects.put(17, "first", null);
        objects.put(0, "zero", null);
        assertEquals(16, objects.capacity());
        assertEquals("zero", objects.put(0, "replacement", null));
        assertEquals(16, objects.capacity(), "this owner grows only for an absent zero key");
    }

    @Test
    void byteBulkAndRejectedInputsPreserveExistingMappings() {
        LongByteHashMap map = new LongByteHashMap(0);
        map.put(0, (byte) 3);
        map.putAll(new long[]{1, 1, 0, Long.MIN_VALUE}, new byte[]{4, 5, 6, -1});
        assertEquals(3, map.size());
        assertEquals(5, map.get(1));
        assertEquals(6, map.get(0));
        assertEquals(255, map.getUnsigned(Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> map.putUnsigned(1, -1));
        assertThrows(IllegalArgumentException.class, () -> map.putUnsigned(1, 256));
        assertThrows(IllegalArgumentException.class, () -> map.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> map.putAll(new long[]{2}, new byte[0]));
        assertEquals(5, map.get(1));
        assertEquals(3, map.size());
        int capacity = map.capacity();
        map.ensureCapacity(1_000);
        assertTrue(map.capacity() > capacity);
        assertEquals(5, map.get(1));
        assertEquals(6, map.get(0));
        assertEquals(255, map.getUnsigned(Long.MIN_VALUE));
    }

    @Test
    void callbackNullAndEmptyMapBehaviorRemainsOwnerSpecific() {
        assertThrows(NullPointerException.class, () -> new LongByteHashMap().forEach(null));
        new LongIntHashMap().forEach(null);
        assertThrows(NullPointerException.class,
                () -> new PrimitiveObjectHashMap<>(PrimitiveKind.LONG).forEach(null));
    }

    private static void verifyFloating(PrimitiveKind kind, long negativeZero, long firstNaN, long otherNaN) {
        PrimitiveObjectHashMap<Object> map = new PrimitiveObjectHashMap<>(kind, 0, 0.65f);
        Object absent = new Object();
        Object positive = new Object();
        Object negative = new Object();
        Object nan = new Object();
        map.put(0, positive, absent);
        map.put(negativeZero, negative, absent);
        map.put(firstNaN, nan, absent);
        assertSame(nan, map.put(otherNaN, nan, absent));
        for (long bits = 1; bits < 400; bits++) map.put(bits, positive, absent);
        assertSame(positive, map.getOrDefault(0, absent));
        assertSame(negative, map.getOrDefault(negativeZero, absent));
        assertSame(nan, map.getOrDefault(firstNaN, absent));
        assertSame(nan, map.getOrDefault(otherNaN, absent));
        long[] retainedNaN = {-1};
        map.forEach((bits, value) -> { if (value == nan) retainedNaN[0] = bits; });
        assertEquals(firstNaN, retainedNaN[0], "rehash preserves the original physical NaN payload");
    }

    private static long key(Random random, int step) {
        return switch (step % 17) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1L;
            default -> random.nextInt(1_024) - 512L;
        };
    }

    private static Object boxedKey(PrimitiveKind kind, long bits) {
        return switch (kind) {
            case BOOLEAN -> bits != 0L;
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG -> bits;
            case FLOAT -> Float.intBitsToFloat((int) bits);
            case DOUBLE -> Double.longBitsToDouble(bits);
        };
    }

    private static byte orDefault(Byte value) { return value == null ? 0 : value; }
    private static int orDefault(Integer value) { return value == null ? 0 : value; }

    private static void assertByteContents(Map<Long, Byte> expected, LongByteHashMap map) {
        Map<Long, Byte> visited = new HashMap<>();
        map.forEach((key, value) -> {
            assertFalse(visited.containsKey(key), "one visit per stored key");
            visited.put(key, value);
        });
        assertEquals(expected, visited);
    }

    private static void assertIntContents(Map<Long, Integer> expected, LongIntHashMap map) {
        Map<Long, Integer> visited = new HashMap<>();
        map.forEach((key, value) -> {
            assertFalse(visited.containsKey(key), "one visit per stored key");
            visited.put(key, value);
        });
        assertEquals(expected, visited);
    }

    private static void assertObjectContents(Map<Object, Object> expected, PrimitiveObjectHashMap<Object> map) {
        Map<Object, Object> visited = new HashMap<>();
        map.forEach((bits, value) -> {
            Object key = boxedKey(map.keyKind(), bits);
            assertFalse(visited.containsKey(key), "one visit per normalized key");
            visited.put(key, value);
        });
        assertEquals(expected, visited);
    }
}
