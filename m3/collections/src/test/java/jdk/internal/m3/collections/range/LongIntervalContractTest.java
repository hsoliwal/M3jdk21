/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.range;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Independent enumeration/overflow oracles for the M3-only scalar interval. */
final class LongIntervalContractTest {
    private static List<Long> independent(long from, long to, long step) {
        ArrayList<Long> expected = new ArrayList<>();
        long value = from;
        for (int at = 0; at < 1_000; at++) {
            if (step > 0 ? value > to : value < to) break;
            expected.add(value);
            try {
                value = Math.addExact(value, step);
            } catch (ArithmeticException overflow) {
                break;
            }
        }
        return expected;
    }

    private static void agrees(long from, long to, long step) {
        LongInterval actual = LongInterval.fromToBy(from, to, step);
        List<Long> expected = independent(from, to, step);
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.getFirst().longValue(), actual.getFirst());
        assertEquals(expected.getLast().longValue(), actual.getLast());
        for (int i = 0; i < expected.size(); i++) {
            long value = expected.get(i);
            assertEquals(value, actual.get(i));
            assertEquals(i, actual.indexOf(value));
            assertTrue(actual.contains(value));
            assertEquals(value, actual.reverseThis().get(expected.size() - 1 - i));
        }
        long[] values = expected.stream().mapToLong(Long::longValue).toArray();
        assertArrayEquals(values, actual.toArray());
        long[] reversed = actual.reverseThis().toArray();
        for (int i = 0; i < values.length; i++) assertEquals(values[i], reversed[values.length - i - 1]);
        for (long probe : new long[] { Long.MIN_VALUE, Long.MAX_VALUE, -101, -1, 0, 1, 2, 101 }) {
            assertEquals(expected.indexOf(probe), actual.indexOf(probe));
            assertEquals(expected.contains(probe), actual.contains(probe));
        }
    }

    @Test void randomInclusiveLongIntervalsMatchIndependentListOracle() {
        Random random = new Random(0x4d33534cL);
        for (int i = 0; i < 30_000; i++) {
            long from = random.nextInt(501) - 250L;
            long to = random.nextInt(501) - 250L;
            long step = (from <= to ? 1 : -1) * (1 + random.nextInt(50));
            agrees(from, to, step);
        }
        assertArrayEquals(new long[]{5, 4, 3, 2, 1}, LongInterval.fromTo(5, 1).toArray());
        assertArrayEquals(new long[]{0, -1, -2}, LongInterval.zeroTo(-2).toArray());
    }

    @Test void unsignedDistanceAndReversalHandleSignedExtrema() {
        agrees(Long.MIN_VALUE, Long.MIN_VALUE + 16, 3);
        agrees(Long.MAX_VALUE, Long.MAX_VALUE - 16, -3);
        agrees(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        agrees(Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
        agrees(-1, Long.MAX_VALUE, Long.MAX_VALUE);
        agrees(Long.MAX_VALUE, -1, Long.MIN_VALUE);
        agrees(Long.MIN_VALUE, -1, Long.MAX_VALUE);
        agrees(1, Long.MIN_VALUE, Long.MIN_VALUE);
        LongInterval huge = LongInterval.fromTo(0L, Integer.MAX_VALUE - 1L);
        assertEquals(Integer.MAX_VALUE, huge.size());
        assertEquals((long) Integer.MAX_VALUE - 1L, huge.getLast());
        assertEquals((long) Integer.MAX_VALUE - 1L, huge.get(Integer.MAX_VALUE - 1));
        assertThrows(IndexOutOfBoundsException.class, () -> huge.get(Integer.MAX_VALUE));
    }

    @Test void capacityErrorsAndIllegalStepsNeverProduceWrappedSequences() {
        assertThrows(IllegalArgumentException.class, () -> LongInterval.fromToBy(0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> LongInterval.fromToBy(10, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> LongInterval.fromToBy(0, 10, -1));
        assertThrows(IllegalArgumentException.class,
                () -> LongInterval.fromTo(Long.MIN_VALUE, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> LongInterval.fromTo(0, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> LongInterval.fromTo(Long.MAX_VALUE, Long.MIN_VALUE));
        assertEquals(1, LongInterval.fromToBy(9, 9, Long.MIN_VALUE).size());
        assertEquals(0, new LongInterval().size());
    }

    @Test void emptyAndCallbackSemanticsPreserveJdkStyleFailures() {
        LongInterval empty = new LongInterval();
        assertThrows(NoSuchElementException.class, empty::getFirst);
        assertThrows(NoSuchElementException.class, empty::getLast);
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertEquals(-1, empty.indexOf(0));
        assertArrayEquals(new long[0], empty.toArray());
        assertSame(empty, empty.reverseThis());
        assertThrows(NullPointerException.class, () -> empty.forEach(null));
        LongInterval values = LongInterval.fromToBy(1, 200, 7);
        AtomicInteger cursor = new AtomicInteger();
        values.forEach(value -> assertEquals(values.get(cursor.getAndIncrement()), value));
        assertEquals(values.size(), cursor.get());
        RuntimeException marker = new RuntimeException("caller error must propagate unchanged");
        RuntimeException seen = assertThrows(RuntimeException.class, () -> values.forEach(v -> {
            throw marker;
        }));
        assertSame(marker, seen);
    }

    @Test void noPerElementStorageAndPublicScaffoldDescriptorsArePreserved() {
        assertEquals("M3LongInterval|LONG_INTERVAL|LONG|FROZEN|long from;long to;long step",
                LongInterval.scaffoldDescriptor());
        for (Field field : LongInterval.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            assertTrue(field.getType().isPrimitive(), "retained object/array: " + field.getName());
            assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
        }
        assertEquals(0, LongInterval.class.getInterfaces().length);
    }
}
