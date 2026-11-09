/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class PrimitiveBigDequeTest {
    @Test
    void allKindsMatchJdkArrayDequeOperationTraces() {
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            PrimitiveArrayDeque.Big actual = PrimitiveArrayDeque.big(kind, 3L, 10);
            ArrayDeque<Long> expected = new ArrayDeque<>();
            Random random = new Random(0x4d335f4445515545L + kind.ordinal());
            for (int step = 0; step < 4_000; step++) {
                int operation = random.nextInt(12);
                long value = random.nextLong();
                switch (operation) {
                    case 0, 1 -> {
                        actual.addFirstBits(value);
                        expected.addFirst(stored(kind, value));
                    }
                    case 2, 3 -> {
                        actual.addLastBits(value);
                        expected.addLast(stored(kind, value));
                    }
                    case 4 -> {
                        if (!expected.isEmpty()) {
                            assertEquals(expected.removeFirst().longValue(), actual.removeFirstBits());
                        }
                    }
                    case 5 -> {
                        if (!expected.isEmpty()) {
                            assertEquals(expected.removeLast().longValue(), actual.removeLastBits());
                        }
                    }
                    case 6 -> {
                        if (!expected.isEmpty()) {
                            int index = random.nextInt(expected.size());
                            List<Long> values = new ArrayList<>(expected);
                            long old = values.set(index, stored(kind, value));
                            assertEquals(old, actual.setBits(index, value));
                            expected.clear();
                            expected.addAll(values);
                        }
                    }
                    case 7 -> actual.ensureCapacity(random.nextInt(160));
                    case 8 -> {
                        int newSize = random.nextInt(96);
                        actual.resize(newSize);
                        while (expected.size() > newSize) expected.removeLast();
                        while (expected.size() < newSize) expected.addLast(0L);
                    }
                    case 9 -> {
                        if ((step & 15) == 0) {
                            actual.clear();
                            expected.clear();
                        }
                    }
                    case 10 -> assertSnapshot(expected, actual);
                    case 11 -> {
                        if (!expected.isEmpty()) {
                            assertEquals(expected.getFirst().longValue(), actual.elementBits());
                            assertEquals(expected.getLast().longValue(), actual.getLastBits());
                        }
                    }
                    default -> throw new AssertionError("operation");
                }
                assertEquals(expected.size(), actual.size64());
                assertEquals(actual.size64(), actual.size());
                assertEquals(expected.isEmpty(), actual.isEmpty());
                assertTrue(actual.capacity64() >= actual.size64());
                if (!expected.isEmpty()) {
                    assertEquals(expected.getFirst().longValue(), actual.peekFirstBits());
                    assertEquals(expected.getLast().longValue(), actual.peekLastBits());
                }
                if (step % 79 == 0) assertSnapshot(expected, actual);
            }
        }
    }

    @Test
    void wrappedPageSpansGrowAndTruncateWithoutLosingRawCarriers() {
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            PrimitiveArrayDeque.Big actual = PrimitiveArrayDeque.big(kind, 2051L, 10);
            ArrayDeque<Long> expected = new ArrayDeque<>();
            for (int i = 0; i < 2051; i++) {
                long value = fixtureBits(kind, i);
                actual.addLastBits(value);
                expected.addLast(stored(kind, value));
            }
            for (int i = 0; i < 1027; i++) {
                assertEquals(expected.removeFirst().longValue(), actual.removeFirstBits());
            }
            for (int i = 0; i < 1027; i++) {
                long value = fixtureBits(kind, 2051 + i);
                actual.addLastBits(value);
                expected.addLast(stored(kind, value));
            }
            assertEquals(2051L, actual.capacity64());
            assertSnapshot(expected, actual);
            PrimitiveIterator.OfLong beforeGrowth = actual.iterator();
            actual.ensureCapacity(2052L);
            assertEquals(expected.getFirst().longValue(), beforeGrowth.nextLong(),
                    "capacity-only migration preserves encounter order and iterator validity");
            assertSnapshot(expected, actual);
            actual.resize(2100L);
            while (expected.size() < 2100) expected.addLast(0L);
            assertSnapshot(expected, actual);
            actual.resize(137L);
            while (expected.size() > 137) expected.removeLast();
            actual.resize(2100L);
            while (expected.size() < 2100) expected.addLast(0L);
            assertSnapshot(expected, actual);
            long capacity = actual.capacity64();
            actual.clear();
            assertEquals(capacity, actual.capacity64());
            assertEquals(0L, actual.size64());
            actual.pushBits(fixtureBits(kind, 91));
            assertEquals(stored(kind, fixtureBits(kind, 91)), actual.popBits());
            assertTrue(actual.isEmpty());
        }
    }

    @Test
    void sparseLongCapacityWrapAndGrowthStayBoundedByResidentPages() {
        long capacity = (1L << 33) + 17L;
        long middle = (long) Integer.MAX_VALUE + 13L;
        PrimitiveArrayDeque.Big actual = PrimitiveArrayDeque.big(PrimitiveKind.LONG, capacity, 10);
        actual.resize(capacity);
        assertEquals(capacity, actual.size64());
        assertEquals(0L, actual.allocatedPayloadBytes());
        actual.setBits(0L, 11L);
        actual.setBits(middle, -23L);
        actual.setBits(capacity - 1L, 31L);
        assertTrue(actual.allocatedPayloadBytes() <= 3L * 1024L * Long.BYTES);
        assertEquals(11L, actual.removeFirstBits());
        actual.addLastBits(47L);
        assertEquals(0L, actual.peekFirstBits());
        assertEquals(-23L, actual.getBits(middle - 1L));
        assertEquals(31L, actual.getBits(capacity - 2L));
        assertEquals(47L, actual.peekLastBits());
        actual.ensureCapacity(capacity + 1L);
        assertEquals(capacity, actual.size64());
        assertEquals(-23L, actual.getBits(middle - 1L));
        assertEquals(31L, actual.getBits(capacity - 2L));
        assertEquals(47L, actual.peekLastBits());
        assertTrue(actual.allocatedPayloadBytes() <= 6L * 1024L * Long.BYTES,
                "sparse migration may split resident pages but must not materialize logical holes");
        actual.resize(4L);
        assertEquals(0L, actual.getBits(3L));
        actual.resize(capacity);
        assertEquals(0L, actual.getBits(middle - 1L));
        assertEquals(0L, actual.peekLastBits());
        actual.clear();
        assertEquals(0L, actual.size64());
        assertEquals(0L, actual.allocatedPayloadBytes());
    }

    @Test
    void maximumLongGeometryDoesNotOverflowRingArithmeticOrPublishFailedInsertion() {
        PrimitiveArrayDeque.Big actual =
                PrimitiveArrayDeque.big(PrimitiveKind.LONG, Long.MAX_VALUE, 10);
        actual.addFirstBits(71L); // head is now Long.MAX_VALUE - 1
        actual.resize(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, actual.size64());
        assertEquals(71L, actual.peekFirstBits());
        assertEquals(0L, actual.getBits(1L));
        assertEquals(0L, actual.peekLastBits());
        actual.setBits(Long.MAX_VALUE - 1L, -31L);
        assertEquals(-31L, actual.removeLastBits());
        actual.addLastBits(Long.MIN_VALUE);
        long payload = actual.allocatedPayloadBytes();
        assertThrows(OutOfMemoryError.class, () -> actual.addFirstBits(99L));
        assertThrows(OutOfMemoryError.class, () -> actual.addLastBits(99L));
        assertEquals(Long.MAX_VALUE, actual.size64());
        assertEquals(payload, actual.allocatedPayloadBytes());
        assertEquals(71L, actual.peekFirstBits());
        assertEquals(Long.MIN_VALUE, actual.peekLastBits());
        assertEquals(71L, actual.removeFirstBits());
        assertEquals(0L, actual.peekFirstBits());
        assertEquals(Long.MIN_VALUE, actual.peekLastBits());
        actual.addFirstBits(89L);
        assertEquals(89L, actual.peekFirstBits());
        assertThrows(IndexOutOfBoundsException.class, () -> actual.getBits(Long.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.setBits(Long.MAX_VALUE, 5L));
        actual.resize(2L);
        assertEquals(89L, actual.getBits(0L));
        assertEquals(0L, actual.getBits(1L));
        actual.resize(Long.MAX_VALUE);
        assertEquals(0L, actual.peekLastBits(), "truncated values cannot reappear");
        actual.clear();
        assertTrue(actual.isEmpty());
        assertEquals(0L, actual.allocatedPayloadBytes());
        assertEquals(Long.MAX_VALUE, actual.capacity64());
    }

    @Test
    void growthIsFailureAtomicAtAllocationAndEachWrappedCopySpan() {
        for (int failAt = 0; failAt <= 2; failAt++) {
            PrimitiveArrayDeque.Big actual = wrappedFour();
            long physicalBytes = actual.allocatedPayloadBytes();
            int injected = failAt;
            int[] copies = {0};
            PrimitiveArrayDeque.Big.GrowthOps operations = new PrimitiveArrayDeque.Big.GrowthOps() {
                @Override
                public BigArray allocate(PrimitiveKind kind, long capacity, int shift) {
                    if (injected == 0) throw new OutOfMemoryError("injected allocation");
                    return BigArray.sparse(kind, capacity, shift);
                }
                @Override
                public void copy(BigArray source, long from,
                                 BigArray target, long offset, long count) {
                    if (++copies[0] == injected) throw new OutOfMemoryError("injected span copy");
                    source.copyTo(from, target, offset, count);
                }
            };
            assertThrows(OutOfMemoryError.class, () -> actual.ensureCapacity(5L, operations));
            assertEquals(4L, actual.capacity64());
            assertEquals(physicalBytes, actual.allocatedPayloadBytes());
            assertSnapshot(new ArrayDeque<>(List.of(3L, 4L, 5L, 6L)), actual);
            actual.ensureCapacity(5L);
            assertTrue(actual.capacity64() >= 5L);
            assertSnapshot(new ArrayDeque<>(List.of(3L, 4L, 5L, 6L)), actual);
        }
    }

    @Test
    void validatesInputsEmptyOperationsAndStructuralCursorChanges() {
        assertThrows(NullPointerException.class, () -> PrimitiveArrayDeque.big(null, 0L, 10));
        assertThrows(IllegalArgumentException.class, () -> PrimitiveArrayDeque.big(PrimitiveKind.LONG, -1L, 10));
        assertThrows(IllegalArgumentException.class, () -> PrimitiveArrayDeque.big(PrimitiveKind.LONG, 0L, 9));
        assertThrows(IllegalArgumentException.class, () -> PrimitiveArrayDeque.big(PrimitiveKind.LONG, 0L, 27));
        PrimitiveArrayDeque.Big actual = PrimitiveArrayDeque.big(PrimitiveKind.LONG, 0L, 10);
        assertEquals(1L, actual.capacity64());
        assertEquals(0L, actual.allocatedPayloadBytes());
        assertThrows(NoSuchElementException.class, actual::peekFirstBits);
        assertThrows(NoSuchElementException.class, actual::peekLastBits);
        assertThrows(NoSuchElementException.class, actual::removeFirstBits);
        assertThrows(NoSuchElementException.class, actual::removeLastBits);
        assertThrows(NoSuchElementException.class, actual::getFirstBits);
        assertThrows(NoSuchElementException.class, actual::getLastBits);
        assertThrows(NoSuchElementException.class, actual::elementBits);
        assertThrows(NoSuchElementException.class, actual::popBits);
        assertFalse(actual.iterator().hasNext());
        assertThrows(NoSuchElementException.class, () -> actual.iterator().nextLong());
        assertThrows(NullPointerException.class, () -> actual.forEachBits(null));
        assertThrows(IllegalArgumentException.class, () -> actual.ensureCapacity(-1L));
        assertThrows(IllegalArgumentException.class, () -> actual.resize(-1L));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.getBits(-1L));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.getBits(0L));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.setBits(0L, 1L));
        actual.addLastBits(3L);
        PrimitiveIterator.OfLong live = actual.iterator();
        assertEquals(3L, actual.setBits(0L, 5L));
        assertEquals(5L, live.nextLong());
        PrimitiveIterator.OfLong stale = actual.iterator();
        actual.addFirstBits(7L);
        assertThrows(ConcurrentModificationException.class, stale::hasNext);
        assertThrows(ConcurrentModificationException.class, stale::nextLong);
        assertThrows(ConcurrentModificationException.class,
                () -> actual.forEachBits(value -> actual.addLastBits(value)));
        RuntimeException marker = new RuntimeException("consumer failure");
        assertSame(marker, assertThrows(RuntimeException.class,
                () -> actual.forEachBits(value -> { throw marker; })));
    }

    private static PrimitiveArrayDeque.Big wrappedFour() {
        PrimitiveArrayDeque.Big actual = PrimitiveArrayDeque.big(PrimitiveKind.LONG, 4L, 10);
        for (long value = 1; value <= 4; value++) actual.addLastBits(value);
        assertEquals(1L, actual.removeFirstBits());
        assertEquals(2L, actual.removeFirstBits());
        actual.addLastBits(5L);
        actual.addLastBits(6L);
        return actual;
    }

    private static void assertSnapshot(ArrayDeque<Long> expected, PrimitiveArrayDeque.Big actual) {
        List<Long> ordered = new ArrayList<>(expected);
        List<Long> visited = new ArrayList<>();
        actual.forEachBits(visited::add);
        assertEquals(ordered, visited);
        PrimitiveIterator.OfLong iterator = actual.iterator();
        for (int i = 0; i < ordered.size(); i++) {
            assertTrue(iterator.hasNext());
            assertEquals(ordered.get(i).longValue(), iterator.nextLong());
            assertEquals(ordered.get(i).longValue(), actual.getBits(i));
        }
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::nextLong);
    }

    private static long fixtureBits(PrimitiveKind kind, int index) {
        return switch (kind) {
            case FLOAT -> switch (index & 7) {
                case 0 -> 0x8000_0000L;
                case 1 -> 0x7fc0_0031L;
                case 2 -> 0xff80_0000L;
                default -> Float.floatToRawIntBits(index + 0.25f);
            };
            case DOUBLE -> switch (index & 7) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> 0x7ff8_0000_0000_0031L;
                case 2 -> 0xfff0_0000_0000_0000L;
                default -> Double.doubleToRawLongBits(index + 0.25d);
            };
            default -> ((long) index << 33) ^ (index * 1_000_003L) ^ Long.MIN_VALUE;
        };
    }

    /** Independent write/read carrier oracle, without the production PrimitiveKind helpers. */
    private static long stored(PrimitiveKind kind, long bits) {
        return switch (kind) {
            case BOOLEAN -> bits == 0L ? 0L : 1L;
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG -> bits;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits(Float.intBitsToFloat((int) bits)));
            case DOUBLE -> Double.doubleToRawLongBits(Double.longBitsToDouble(bits));
        };
    }
}
