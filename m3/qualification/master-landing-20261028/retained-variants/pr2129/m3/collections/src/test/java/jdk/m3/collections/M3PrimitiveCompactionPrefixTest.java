/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Contract oracles for primitive prefix-skipping compaction. Expected sequences are built
 * separately from the mutated owner. These finite tests preserve the actual primitive
 * owners' contracts; they do not establish java.util.ArrayList or installed-JDK admission.
 */
final class M3PrimitiveCompactionPrefixTest {
    private static final Field INT_DATA = field(M3IntArrayList.class, "data");
    private static final Field LONG_DATA = field(M3LongArrayList.class, "elements");
    private static final int INT_SLACK = 0x6d72a951;
    private static final long LONG_SLACK = 0x6d72a951c82430ebL;
    private static final int[] LENGTHS = {0, 1, 2, 3, 31, 32, 33, 63, 64, 65,
            127, 128, 129, 255, 256, 257, 1024};

    @Test
    void sortedDeduplicationExhaustsSmallSignedInputs() throws ReflectiveOperationException {
        int combinations = 1;
        for (int length = 0; length <= 7; length++) {
            for (int code = 0; code < combinations; code++) {
                int value = code;
                int[] ints = new int[length];
                long[] longs = new long[length];
                for (int index = 0; index < length; index++) {
                    int digit = value % 3;
                    value /= 3;
                    ints[index] = digit - 1;
                    longs[index] = switch (digit) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> 0L;
                        default -> Long.MAX_VALUE;
                    };
                }
                Arrays.sort(ints);
                Arrays.sort(longs);
                checkDeduplicate(ints, Arrays.stream(ints).distinct().toArray());
                checkDeduplicate(longs, Arrays.stream(longs).distinct().toArray());
            }
            combinations *= 3;
        }
    }

    @Test
    void deduplicationCoversEveryPrefixAndBoundaryLength() throws ReflectiveOperationException {
        for (int length : LENGTHS) {
            int[] distinctInts = IntStream.range(0, length).map(i -> i - 512).toArray();
            long[] distinctLongs = IntStream.range(0, length)
                    .mapToLong(i -> Long.MIN_VALUE + 3L * i).toArray();
            checkDeduplicate(distinctInts, distinctInts);
            checkDeduplicate(distinctLongs, distinctLongs);
            int[] sameInts = new int[length];
            long[] sameLongs = new long[length];
            Arrays.fill(sameInts, Integer.MIN_VALUE);
            Arrays.fill(sameLongs, Long.MAX_VALUE);
            checkDeduplicate(sameInts, length == 0 ? new int[0] : new int[] {Integer.MIN_VALUE});
            checkDeduplicate(sameLongs, length == 0 ? new long[0] : new long[] {Long.MAX_VALUE});
            for (int duplicate = 1; duplicate < length; duplicate++) {
                int[] ints = distinctInts.clone();
                long[] longs = distinctLongs.clone();
                ints[duplicate] = ints[duplicate - 1];
                longs[duplicate] = longs[duplicate - 1];
                checkDeduplicate(ints, Arrays.stream(ints).distinct().toArray());
                checkDeduplicate(longs, Arrays.stream(longs).distinct().toArray());
            }
        }
    }

    @Test
    void sortedDeduplicationCoversFullWidthValuesAndRandomRuns() throws ReflectiveOperationException {
        SplittableRandom random = new SplittableRandom(0x4d33434f4d504143L);
        for (int trial = 0; trial < 1024; trial++) {
            int length = random.nextInt(258);
            int[] ints = new int[length];
            long[] longs = new long[length];
            for (int i = 0; i < length; i++) {
                ints[i] = switch (random.nextInt(7)) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> 0;
                    case 3 -> -1;
                    case 4 -> 1;
                    default -> random.nextInt();
                };
                longs[i] = switch (random.nextInt(7)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    case 3 -> -1L;
                    case 4 -> 1L;
                    default -> random.nextLong();
                };
            }
            Arrays.sort(ints);
            Arrays.sort(longs);
            checkDeduplicate(ints, Arrays.stream(ints).distinct().toArray());
            checkDeduplicate(longs, Arrays.stream(longs).distinct().toArray());
        }
    }

    @Test
    void adjacentRunBehaviorRemainsUnchangedOutsideSortedPrecondition()
            throws ReflectiveOperationException {
        // These observations deliberately freeze the incumbent, without expanding its sorted-input API promise.
        checkDeduplicate(new int[] {3, 3, 1, 3, 3, 2, 2, 1}, new int[] {3, 1, 3, 2, 1});
        checkDeduplicate(new long[] {Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE,
                Long.MAX_VALUE, 0L, 0L, Long.MIN_VALUE},
                new long[] {Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, 0L, Long.MIN_VALUE});
        checkDeduplicate(new int[] {2, 1, 2, 1}, new int[] {2, 1, 2, 1});
        checkDeduplicate(new long[] {2L, 1L, 2L, 1L}, new long[] {2L, 1L, 2L, 1L});
    }

    @Test
    void everyUnsignedStateAndRequiredMaskUsesOriginalCoordinates()
            throws ReflectiveOperationException {
        int[] ints = {Integer.MIN_VALUE, 21, 21, 0, -9, Integer.MAX_VALUE};
        long[] longs = {Long.MIN_VALUE, 21L, 21L, 0L, -9L, Long.MAX_VALUE};
        for (int required = 0; required <= 0xff; required++) {
            for (int state = 0; state <= 0xff; state++) {
                byte[] states = {(byte) 0xff, (byte) 0xff, (byte) state,
                        (byte) ~state, 0, (byte) 0xff};
                checkMask(ints, states, required, selected(ints, states, required));
                checkMask(longs, states, required, selected(longs, states, required));
            }
        }
    }

    @Test
    void maskCompactionCoversEveryFirstRejectionAndBoundaryLength()
            throws ReflectiveOperationException {
        for (int length : LENGTHS) {
            int[] ints = IntStream.range(0, length).map(i -> i * 17 - 1024).toArray();
            long[] longs = IntStream.range(0, length)
                    .mapToLong(i -> Long.MAX_VALUE - 17L * i).toArray();
            byte[] states = new byte[length];
            Arrays.fill(states, (byte) 0xff);
            checkMask(ints, states, 0x80, ints);
            checkMask(longs, states, 0x80, longs);
            for (int rejected = 0; rejected < length; rejected++) {
                states[rejected] = 0x7f;
                checkMask(ints, states, 0x80, selected(ints, states, 0x80));
                checkMask(longs, states, 0x80, selected(longs, states, 0x80));
                states[rejected] = (byte) 0xff;
            }
            Arrays.fill(states, (byte) 0x7f);
            checkMask(ints, states, 0x80, new int[0]);
            checkMask(longs, states, 0x80, new long[0]);
            checkMask(ints, states, 0, ints);
            checkMask(longs, states, 0, longs);
        }
    }

    @Test
    void masksCoverRandomUnsignedBitsAndRepeatedPayloads() throws ReflectiveOperationException {
        SplittableRandom random = new SplittableRandom(0x4d335354415445L);
        for (int trial = 0; trial < 1024; trial++) {
            int length = random.nextInt(258);
            int[] ints = new int[length];
            long[] longs = new long[length];
            byte[] states = new byte[length];
            for (int i = 0; i < length; i++) {
                ints[i] = random.nextInt(-4, 5);
                longs[i] = (i & 1) == 0 ? Long.MIN_VALUE + ints[i] : Long.MAX_VALUE - ints[i];
                states[i] = (byte) random.nextInt(256);
            }
            int required = random.nextInt(256);
            checkMask(ints, states, required, selected(ints, states, required));
            checkMask(longs, states, required, selected(longs, states, required));
        }
    }

    @Test
    void invalidMaskInputsPreserveValidationPrecedenceAndOwnerState()
            throws ReflectiveOperationException {
        int[] invalidMasks = {-1, -256, 256, 0x10080, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int mask : invalidMasks) {
            assertIntRefusal(null, mask, NullPointerException.class, "alignedStates");
            assertLongRefusal(null, mask, NullPointerException.class, "rowStates");
            assertIntRefusal(new byte[1], mask, IllegalArgumentException.class, "unaligned state length");
            assertLongRefusal(new byte[1], mask, IllegalArgumentException.class,
                    "index and aligned state lengths differ");
            assertIntRefusal(new byte[3], mask, IllegalArgumentException.class, "invalid state mask");
            assertLongRefusal(new byte[3], mask, IllegalArgumentException.class,
                    "required mask must fit an unsigned byte");
        }
        for (int mask : new int[] {0, 0x80, 0xff}) {
            assertIntRefusal(null, mask, NullPointerException.class, "alignedStates");
            assertLongRefusal(null, mask, NullPointerException.class, "rowStates");
            assertIntRefusal(new byte[2], mask, IllegalArgumentException.class, "unaligned state length");
            assertLongRefusal(new byte[4], mask, IllegalArgumentException.class,
                    "index and aligned state lengths differ");
        }
    }

    @Test
    void repeatedCompactionAndSubsequentMutationsKeepExistingBehavior() {
        M3IntArrayList ints = M3IntArrayList.copyOf(new int[] {1, 1, 2, 3, 3, 4});
        M3LongArrayList longs = M3LongArrayList.copyOf(new long[] {1, 1, 2, 3, 3, 4});
        assertEquals(2, ints.deduplicateSorted());
        assertEquals(2, longs.deduplicateSorted());
        M3IntIterator intIterator = ints.iterator();
        M3LongIterator longIterator = longs.iterator();
        assertEquals(0, ints.deduplicateSorted());
        assertEquals(0, longs.deduplicateSorted());
        assertEquals(0, ints.retainByStateMask(new byte[] {-1, -1, -1, -1}, 0xff));
        assertEquals(0, longs.retainByStateMask(new byte[] {-1, -1, -1, -1}, 0xff));
        assertEquals(1, intIterator.nextInt());
        assertEquals(1L, longIterator.nextLong());
        assertEquals(2, ints.retainByStateMask(new byte[] {0, -1, 0, -1}, 0x80));
        assertEquals(2, longs.retainByStateMask(new byte[] {0, -1, 0, -1}, 0x80));
        ints.insert(1, 3);
        longs.add(1, 3L);
        ints.add(5);
        longs.add(5L);
        assertArrayEquals(new int[] {2, 3, 4, 5}, ints.toArray());
        assertArrayEquals(new long[] {2, 3, 4, 5}, longs.toArray());
        assertEquals(0, ints.binarySearch(2));
        assertEquals(0, longs.binarySearch(2L));
        assertEquals(5, ints.removeAt(3));
        assertEquals(5L, longs.pop());
        assertArrayEquals(new int[] {2, 3, 4}, ints.toArray());
        assertArrayEquals(new long[] {2, 3, 4}, longs.toArray());
        assertThrows(ConcurrentModificationException.class, intIterator::hasNext);
        assertThrows(ConcurrentModificationException.class, longIterator::hasNext);
    }

    private static void checkDeduplicate(int[] values, int[] expected)
            throws ReflectiveOperationException {
        M3IntArrayList list = intOwner(values);
        int[] backing = intBacking(list);
        int[] before = backing.clone();
        M3IntIterator iterator = list.iterator();
        int cursor = consumeOne(iterator, values);
        int removed = list.deduplicateSorted();
        assertEquals(values.length - expected.length, removed);
        checkIntState(list, backing, before, values.length, expected);
        checkIterator(iterator, values, cursor, removed != 0);
    }

    private static void checkDeduplicate(long[] values, long[] expected)
            throws ReflectiveOperationException {
        M3LongArrayList list = longOwner(values);
        long[] backing = longBacking(list);
        long[] before = backing.clone();
        M3LongIterator iterator = list.iterator();
        int cursor = consumeOne(iterator, values);
        int removed = list.deduplicateSorted();
        assertEquals(values.length - expected.length, removed);
        checkLongState(list, backing, before, values.length, expected);
        checkIterator(iterator, values, cursor, removed != 0);
    }

    private static void checkMask(int[] values, byte[] states, int required, int[] expected)
            throws ReflectiveOperationException {
        M3IntArrayList list = intOwner(values);
        int[] backing = intBacking(list);
        int[] before = backing.clone();
        byte[] stateBefore = states.clone();
        M3IntIterator iterator = list.iterator();
        int cursor = consumeOne(iterator, values);
        int removed = list.retainByStateMask(states, required);
        assertEquals(values.length - expected.length, removed);
        assertArrayEquals(stateBefore, states, "caller-owned state bytes must remain unchanged");
        checkIntState(list, backing, before, values.length, expected);
        checkIterator(iterator, values, cursor, removed != 0);
    }

    private static void checkMask(long[] values, byte[] states, int required, long[] expected)
            throws ReflectiveOperationException {
        M3LongArrayList list = longOwner(values);
        long[] backing = longBacking(list);
        long[] before = backing.clone();
        byte[] stateBefore = states.clone();
        M3LongIterator iterator = list.iterator();
        int cursor = consumeOne(iterator, values);
        int removed = list.retainByStateMask(states, required);
        assertEquals(values.length - expected.length, removed);
        assertArrayEquals(stateBefore, states, "caller-owned state bytes must remain unchanged");
        checkLongState(list, backing, before, values.length, expected);
        checkIterator(iterator, values, cursor, removed != 0);
    }

    private static void assertIntRefusal(byte[] states, int required,
            Class<? extends RuntimeException> type, String message) throws ReflectiveOperationException {
        M3IntArrayList list = intOwner(new int[] {7, 7, -9});
        int[] backing = intBacking(list);
        int[] before = backing.clone();
        byte[] stateBefore = states == null ? null : states.clone();
        M3IntIterator iterator = list.iterator();
        RuntimeException thrown = assertThrows(type, () -> list.retainByStateMask(states, required));
        assertEquals(type, thrown.getClass());
        assertEquals(message, thrown.getMessage());
        checkIntState(list, backing, before, 3, new int[] {7, 7, -9});
        checkIterator(iterator, new int[] {7, 7, -9}, 0, false);
        if (states != null) assertArrayEquals(stateBefore, states);
    }

    private static void assertLongRefusal(byte[] states, int required,
            Class<? extends RuntimeException> type, String message) throws ReflectiveOperationException {
        M3LongArrayList list = longOwner(new long[] {7, 7, -9});
        long[] backing = longBacking(list);
        long[] before = backing.clone();
        byte[] stateBefore = states == null ? null : states.clone();
        M3LongIterator iterator = list.iterator();
        RuntimeException thrown = assertThrows(type, () -> list.retainByStateMask(states, required));
        assertEquals(type, thrown.getClass());
        assertEquals(message, thrown.getMessage());
        checkLongState(list, backing, before, 3, new long[] {7, 7, -9});
        checkIterator(iterator, new long[] {7, 7, -9}, 0, false);
        if (states != null) assertArrayEquals(stateBefore, states);
    }

    private static void checkIntState(M3IntArrayList list, int[] backing, int[] before,
            int oldSize, int[] expected) throws ReflectiveOperationException {
        assertSame(backing, intBacking(list), "compaction must retain the existing storage owner");
        assertEquals(before.length, list.capacity());
        assertEquals(expected.length, list.size());
        assertArrayEquals(expected, list.toArray());
        int[] expectedBacking = before.clone();
        System.arraycopy(expected, 0, expectedBacking, 0, expected.length);
        Arrays.fill(expectedBacking, expected.length, oldSize, 0);
        assertArrayEquals(expectedBacking, backing, "zero retired slots; preserve nonlogical spare capacity");
    }

    private static void checkLongState(M3LongArrayList list, long[] backing, long[] before,
            int oldSize, long[] expected) throws ReflectiveOperationException {
        assertSame(backing, longBacking(list), "compaction must retain the existing storage owner");
        assertEquals(before.length, list.capacity());
        assertEquals(expected.length, list.size());
        assertArrayEquals(expected, list.toArray());
        long[] expectedBacking = before.clone();
        System.arraycopy(expected, 0, expectedBacking, 0, expected.length);
        Arrays.fill(expectedBacking, expected.length, oldSize, 0L);
        assertArrayEquals(expectedBacking, backing, "zero retired slots; preserve nonlogical spare capacity");
    }

    private static M3IntArrayList intOwner(int[] values) throws ReflectiveOperationException {
        M3IntArrayList list = new M3IntArrayList(values.length + 5);
        list.addAll(values);
        // Test-only sentinels outside the logical range make accidental over-clearing observable.
        Arrays.fill(intBacking(list), values.length, list.capacity(), INT_SLACK);
        return list;
    }

    private static M3LongArrayList longOwner(long[] values) throws ReflectiveOperationException {
        M3LongArrayList list = new M3LongArrayList(values.length + 5);
        list.addAll(values);
        Arrays.fill(longBacking(list), values.length, list.capacity(), LONG_SLACK);
        return list;
    }

    private static int consumeOne(M3IntIterator iterator, int[] values) {
        if (values.length == 0) return 0;
        assertTrue(iterator.hasNext());
        assertEquals(values[0], iterator.nextInt());
        return 1;
    }

    private static int consumeOne(M3LongIterator iterator, long[] values) {
        if (values.length == 0) return 0;
        assertTrue(iterator.hasNext());
        assertEquals(values[0], iterator.nextLong());
        return 1;
    }

    private static void checkIterator(M3IntIterator iterator, int[] original, int cursor, boolean changed) {
        if (changed) {
            assertThrows(ConcurrentModificationException.class, iterator::hasNext);
            assertThrows(ConcurrentModificationException.class, iterator::nextInt);
        } else {
            for (int i = cursor; i < original.length; i++) {
                assertTrue(iterator.hasNext());
                assertEquals(original[i], iterator.nextInt());
            }
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::nextInt);
        }
    }

    private static void checkIterator(M3LongIterator iterator, long[] original, int cursor, boolean changed) {
        if (changed) {
            assertThrows(ConcurrentModificationException.class, iterator::hasNext);
            assertThrows(ConcurrentModificationException.class, iterator::nextLong);
        } else {
            for (int i = cursor; i < original.length; i++) {
                assertTrue(iterator.hasNext());
                assertEquals(original[i], iterator.nextLong());
            }
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::nextLong);
        }
    }

    private static int[] selected(int[] values, byte[] states, int required) {
        return IntStream.range(0, values.length).filter(i -> admitted(states[i], required))
                .map(i -> values[i]).toArray();
    }

    private static long[] selected(long[] values, byte[] states, int required) {
        return IntStream.range(0, values.length).filter(i -> admitted(states[i], required))
                .mapToLong(i -> values[i]).toArray();
    }

    private static boolean admitted(byte state, int required) {
        // Independent per-bit requirement oracle; no owner helper or compaction state is consulted.
        for (int bit = 0; bit < 8; bit++) {
            if (((required >>> bit) & 1) != 0 && ((state >>> bit) & 1) == 0) return false;
        }
        return true;
    }

    private static int[] intBacking(M3IntArrayList owner) throws IllegalAccessException {
        return (int[]) INT_DATA.get(owner);
    }

    private static long[] longBacking(M3LongArrayList owner) throws IllegalAccessException {
        return (long[]) LONG_DATA.get(owner);
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
}
