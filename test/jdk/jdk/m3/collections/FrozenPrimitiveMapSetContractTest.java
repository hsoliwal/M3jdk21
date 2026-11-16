/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Actual JDK collection oracles for newly received primitive frozen operations. */
final class FrozenPrimitiveMapSetContractTest {
    @Test
    void intSetCopiesCollapsesDuplicatesAndOrdersSignedExtremes() {
        Random random = new Random(0x4d334652534554L);
        for (int trial = 0; trial < 257; trial++) {
            int[] input = new int[trial];
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < input.length; i++) {
                input[i] = switch (i % 9) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    default -> random.nextInt(97) - 48;
                };
                values.add(input[i]);
            }
            Set<Integer> membership = Set.copyOf(values);
            int[] sorted = new TreeSet<>(values).stream().mapToInt(Integer::intValue).toArray();
            int[] original = input.clone();
            FrozenIntSet actual = FrozenIntSet.copyOf(input);
            assertArrayEquals(original, input);
            Arrays.fill(input, 799);
            assertEquals(membership.size(), actual.size());
            assertArrayEquals(sorted, actual.toArray());
            var cursor = actual.iterator();
            for (int i = 0; i < sorted.length; i++) {
                assertTrue(cursor.hasNext());
                assertTrue(cursor.hasNext());
                assertEquals(sorted[i], cursor.nextInt());
                assertEquals(sorted[i], actual.get(i));
                assertTrue(actual.contains(sorted[i]));
            }
            assertFalse(cursor.hasNext());
            assertThrows(NoSuchElementException.class, cursor::nextInt);
            assertThrows(UnsupportedOperationException.class, cursor::remove);
            for (int key : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, -49, -1, 0, 1, 49}) {
                assertEquals(membership.contains(key), actual.contains(key));
            }
            int[] exported = actual.toArray();
            Arrays.fill(exported, 892);
            assertArrayEquals(sorted, actual.toArray());
            List<Integer> seen = new ArrayList<>();
            actual.forEach(seen::add);
            assertEquals(Arrays.stream(sorted).boxed().toList(), seen);
        }
    }

    @Test
    void intMapsKeepPairedValuesDuringSortingAndDetachBothInputs() {
        Random random = new Random(0x4d334652494d4150L);
        for (int size = 0; size < 257; size++) {
            TreeMap<Integer, Integer> sorted = new TreeMap<>();
            while (sorted.size() < size) sorted.put(random.nextInt(), random.nextInt());
            if (size > 2) {
                sorted.pollFirstEntry(); sorted.put(Integer.MIN_VALUE, Integer.MAX_VALUE);
                sorted.pollLastEntry(); sorted.put(Integer.MAX_VALUE, Integer.MIN_VALUE);
            }
            int[] keys = new int[size], values = new int[size];
            int i = size;
            for (var entry : sorted.entrySet()) {
                keys[--i] = entry.getKey(); values[i] = entry.getValue();
            }
            Map<Integer, Integer> membership = Map.copyOf(sorted);
            FrozenIntMap actual = FrozenIntMap.copyOf(keys, values);
            Arrays.fill(keys, 85); Arrays.fill(values, 86);
            assertEquals(size, actual.size());
            i = 0;
            for (var entry : sorted.entrySet()) {
                assertEquals(entry.getKey().intValue(), actual.keyAt(i));
                assertEquals(entry.getValue().intValue(), actual.valueAt(i++));
                assertTrue(actual.containsKey(entry.getKey()));
                assertEquals(entry.getValue().intValue(), actual.getOrDefault(entry.getKey(), 771));
            }
            List<Integer> seenKeys = new ArrayList<>(), seenValues = new ArrayList<>();
            actual.forEach((key, value) -> { seenKeys.add(key); seenValues.add(value); });
            assertEquals(new ArrayList<>(sorted.keySet()), seenKeys);
            assertEquals(new ArrayList<>(sorted.values()), seenValues);
            for (int query = -3; query < 4; query++) {
                assertEquals(membership.containsKey(query), actual.containsKey(query));
                assertEquals(membership.getOrDefault(query, 771).intValue(), actual.getOrDefault(query, 771));
            }
        }
    }

    @Test
    void longMapsPreserveFullWidthPairingOrderMembershipAndExports() {
        Random random = new Random(0x4d3346524c4d4150L);
        for (int size = 0; size < 257; size++) {
            TreeMap<Long, Long> sorted = new TreeMap<>();
            while (sorted.size() < size) sorted.put(random.nextLong(), random.nextLong());
            if (size > 2) {
                sorted.pollFirstEntry(); sorted.put(Long.MIN_VALUE, Long.MAX_VALUE);
                sorted.pollLastEntry(); sorted.put(Long.MAX_VALUE, Long.MIN_VALUE);
            }
            long[] keys = new long[size], values = new long[size];
            int i = size;
            for (var entry : sorted.entrySet()) {
                keys[--i] = entry.getKey(); values[i] = entry.getValue();
            }
            Map<Long, Long> membership = Map.copyOf(sorted);
            FrozenLongMap actual = FrozenLongMap.copyOf(keys, values);
            Arrays.fill(keys, 85); Arrays.fill(values, 86);
            assertEquals(size, actual.size());
            assertEquals(size == 0, actual.isEmpty());
            i = 0;
            for (var entry : sorted.entrySet()) {
                assertEquals(entry.getKey().longValue(), actual.keyAt(i));
                assertEquals(entry.getValue().longValue(), actual.valueAt(i++));
                assertTrue(actual.containsKey(entry.getKey()));
                assertTrue(actual.containsValue(entry.getValue()));
                assertEquals(entry.getValue().longValue(), actual.getOrDefault(entry.getKey(), 771));
            }
            List<Long> seenKeys = new ArrayList<>(), seenValues = new ArrayList<>();
            actual.forEach((key, value) -> { seenKeys.add(key); seenValues.add(value); });
            assertEquals(new ArrayList<>(sorted.keySet()), seenKeys);
            assertEquals(new ArrayList<>(sorted.values()), seenValues);
            long[] expectedKeys = sorted.keySet().stream().mapToLong(Long::longValue).toArray();
            long[] expectedValues = sorted.values().stream().mapToLong(Long::longValue).toArray();
            assertArrayEquals(expectedKeys, actual.keysToArray());
            assertArrayEquals(expectedValues, actual.valuesToArray());
            long[] outputKeys = actual.keysToArray(), outputValues = actual.valuesToArray();
            Arrays.fill(outputKeys, 98); Arrays.fill(outputValues, 99);
            assertArrayEquals(expectedKeys, actual.keysToArray());
            assertArrayEquals(expectedValues, actual.valuesToArray());
            for (long query = -3; query < 4; query++) {
                assertEquals(membership.containsKey(query), actual.containsKey(query));
                assertEquals(membership.containsValue(query), actual.containsValue(query));
                assertEquals(membership.getOrDefault(query, 771L).longValue(), actual.getOrDefault(query, 771));
            }
        }
    }

    @Test
    void mapInputAdmissionRejectsNullLengthsAndDuplicateKeysWithoutSourceMutation() {
        assertThrows(NullPointerException.class, () -> FrozenIntSet.copyOf(null));
        assertThrows(NullPointerException.class, () -> FrozenIntMap.copyOf(null, new int[0]));
        assertThrows(NullPointerException.class, () -> FrozenIntMap.copyOf(new int[0], null));
        assertThrows(NullPointerException.class, () -> FrozenLongMap.copyOf(null, new long[0]));
        assertThrows(NullPointerException.class, () -> FrozenLongMap.copyOf(new long[0], null));
        assertThrows(IllegalArgumentException.class, () -> FrozenIntMap.copyOf(new int[1], new int[0]));
        assertThrows(IllegalArgumentException.class, () -> FrozenLongMap.copyOf(new long[0], new long[1]));
        for (int[] keys : new int[][]{{1, 1}, {2, -1, 2}, {Integer.MIN_VALUE, 0, Integer.MIN_VALUE}}) {
            int[] before = keys.clone(), values = new int[keys.length];
            assertThrows(IllegalArgumentException.class, () -> FrozenIntMap.copyOf(keys, values));
            assertArrayEquals(before, keys);
            assertArrayEquals(new int[keys.length], values);
        }
        for (long[] keys : new long[][]{{1, 1}, {2, -1, 2}, {Long.MIN_VALUE, 0, Long.MIN_VALUE}}) {
            long[] before = keys.clone(), values = new long[keys.length];
            assertThrows(IllegalArgumentException.class, () -> FrozenLongMap.copyOf(keys, values));
            assertArrayEquals(before, keys);
            assertArrayEquals(new long[keys.length], values);
        }
        assertThrows(IllegalArgumentException.class,
                () -> Map.ofEntries(Map.entry(1, 1), Map.entry(1, 1)));
        assertEquals(1, Set.copyOf(List.of(1, 1)).size());
    }

    @Test
    void emptyAndPopulatedOwnersRejectMutationAndInvalidOrdinals() {
        for (FrozenIntSet set : new FrozenIntSet[]{new FrozenIntSet(), FrozenIntSet.copyOf(new int[]{2})}) {
            assertThrows(UnsupportedOperationException.class, () -> set.add(2));
            assertThrows(IndexOutOfBoundsException.class, () -> set.get(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> set.get(set.size()));
            assertThrows(NullPointerException.class, () -> set.forEach(null));
        }
        for (FrozenIntMap map : new FrozenIntMap[]{new FrozenIntMap(), FrozenIntMap.copyOf(new int[]{2}, new int[]{4})}) {
            assertThrows(UnsupportedOperationException.class, () -> map.put(2, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(map.size()));
            assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(map.size()));
            assertThrows(NullPointerException.class, () -> map.forEach(null));
        }
        for (FrozenLongMap map : new FrozenLongMap[]{new FrozenLongMap(), FrozenLongMap.copyOf(new long[]{2}, new long[]{4})}) {
            assertThrows(UnsupportedOperationException.class, () -> map.put(2, 4));
            assertThrows(UnsupportedOperationException.class, () -> map.remove(2));
            assertThrows(UnsupportedOperationException.class, () -> map.remove(17));
            assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(map.size()));
            assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(map.size()));
            assertThrows(NullPointerException.class, () -> map.forEach(null));
        }
        assertEquals(0, new FrozenIntSet().size());
        assertEquals(0, new FrozenIntMap().size());
        assertEquals(0, new FrozenLongMap().size());
        assertFalse(new FrozenIntSet().contains(0));
        assertFalse(new FrozenIntMap().containsKey(0));
        assertFalse(new FrozenLongMap().containsKey(0));
        assertFalse(new FrozenLongMap().containsValue(0));
        assertEquals(71, new FrozenIntMap().getOrDefault(0, 71));
        assertEquals(71L, new FrozenLongMap().getOrDefault(0, 71));
    }

    @Test
    void callbackFailuresStopAtTheSameSortedOrdinalAndPreserveOwner() {
        RuntimeException marker = new RuntimeException("callback marker");
        FrozenIntSet set = FrozenIntSet.copyOf(new int[]{9, 3, 7});
        FrozenIntMap ints = FrozenIntMap.copyOf(new int[]{9, 3, 7}, new int[]{90, 30, 70});
        FrozenLongMap longs = FrozenLongMap.copyOf(new long[]{9, 3, 7}, new long[]{90, 30, 70});
        List<Long> seen = new ArrayList<>();
        assertSame(marker, assertThrows(RuntimeException.class, () -> set.forEach(value -> {
            seen.add((long) value); if (value == 7) throw marker;
        })));
        assertEquals(List.of(3L, 7L), seen);
        seen.clear();
        assertSame(marker, assertThrows(RuntimeException.class, () -> ints.forEach((key, value) -> {
            seen.add((long) key); if (key == 7) throw marker;
        })));
        assertEquals(List.of(3L, 7L), seen);
        seen.clear();
        assertSame(marker, assertThrows(RuntimeException.class, () -> longs.forEach((key, value) -> {
            seen.add(key); if (key == 7) throw marker;
        })));
        assertEquals(List.of(3L, 7L), seen);
        assertArrayEquals(new int[]{3, 7, 9}, set.toArray());
        assertEquals(70, ints.getOrDefault(7, 0));
        assertArrayEquals(new long[]{30, 70, 90}, longs.valuesToArray());
    }

    @Test
    void retainedStateUsesExactWidthDenseArraysAndOneSharedOwnerWord() throws Exception {
        Object[] small = {FrozenIntSet.copyOf(new int[]{4}),
                FrozenIntMap.copyOf(new int[]{4}, new int[]{40}),
                FrozenLongMap.copyOf(new long[]{4}, new long[]{40})};
        Object[] larger = {FrozenIntSet.copyOf(new int[]{4, 7, 9, 4}),
                FrozenIntMap.copyOf(new int[]{4, 7, 9}, new int[]{40, 70, 90}),
                FrozenLongMap.copyOf(new long[]{4, 7, 9}, new long[]{40, 70, 90})};
        for (int i = 0; i < small.length; i++) {
            Class<?> type = small[i].getClass();
            int payloadLanes = 0;
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertTrue(Modifier.isFinal(field.getModifiers()), field.getName());
                assertTrue(field.getType().isPrimitive() || field.getType() == int[].class
                        || field.getType() == long[].class, field.getName());
                if (field.getType().isArray()) {
                    field.setAccessible(true);
                    if (field.getName().equals("stateMaskWords")) {
                        assertSame(field.get(small[i]), field.get(larger[i]));
                        assertArrayEquals(new long[]{1L}, (long[]) field.get(small[i]));
                    } else {
                        payloadLanes++;
                        assertEquals(1, java.lang.reflect.Array.getLength(field.get(small[i])));
                        assertEquals(3, java.lang.reflect.Array.getLength(field.get(larger[i])));
                        assertEquals(i == 2 ? long[].class : int[].class, field.getType());
                    }
                }
            }
            assertEquals(i == 0 ? 1 : 2, payloadLanes);
        }
    }

    @Test
    void sharedHeapKernelPreservesExistingEightByEightPrimitiveSnapshots() {
        for (PrimitiveKind keyKind : PrimitiveKind.values()) {
            for (PrimitiveKind valueKind : PrimitiveKind.values()) {
                PrimitivePrimitiveHashMap source = new PrimitivePrimitiveHashMap(keyKind, valueKind);
                for (int value = 91; value >= -91; value--) {
                    source.putBits(carrier(keyKind, value), carrier(valueKind, value * 3), 0);
                }
                PrimitiveFrozenMap actual = PrimitiveFrozenMap.copyOf(source);
                assertEquals(source.size(), actual.size());
                long[] previous = {0};
                int[] count = {0};
                actual.forEach((key, value) -> {
                    assertTrue(source.containsKeyBits(key));
                    assertEquals(source.getOrDefaultBits(key, 765), value);
                    if (count[0]++ > 0) assertTrue(compare(keyKind, previous[0], key) < 0);
                    previous[0] = key;
                });
                assertEquals(source.size(), count[0]);
            }
        }
    }

    private static long carrier(PrimitiveKind kind, int value) {
        return switch (kind) {
            case BOOLEAN -> value == 0 ? 0L : 1L;
            case BYTE -> (byte) value;
            case SHORT -> (short) value;
            case CHAR -> (char) value;
            case INT -> value;
            case LONG -> value;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits(value));
            case DOUBLE -> Double.doubleToRawLongBits(value);
        };
    }

    private static int compare(PrimitiveKind kind, long left, long right) {
        return switch (kind) {
            case FLOAT -> Float.compare(Float.intBitsToFloat((int) left), Float.intBitsToFloat((int) right));
            case DOUBLE -> Double.compare(Double.longBitsToDouble(left), Double.longBitsToDouble(right));
            default -> Long.compare(left, right);
        };
    }
}
