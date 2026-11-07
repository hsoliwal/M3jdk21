// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real M3PrimitiveBigArray copyTo differential checks for bounded sparse visitation. */
final class M3SparseCopyRangeTest {
    private static final int SHIFT = 10;
    private static final long LENGTH = 1L << 17;

    private static long bits(M3PrimitiveKind kind, int n) {
        return switch (kind) {
            case BOOLEAN -> n % 3 == 0 ? 0L : 1L;
            case BYTE, SHORT, CHAR, INT, LONG -> n == 0 ? 0L : 0x5dd5L + (long) n * 31;
            case FLOAT -> switch (n % 4) {
                case 0 -> 0L;
                case 1 -> 0x80000000L;
                case 2 -> 0x7fc00001L;
                default -> Integer.toUnsignedLong(Float.floatToRawIntBits((float) n));
            };
            case DOUBLE -> switch (n % 4) {
                case 0 -> 0L;
                case 1 -> 0x8000000000000000L;
                case 2 -> 0x7ff8000000000001L;
                default -> Double.doubleToRawLongBits((double) n);
            };
        };
    }

    @Test
    void sparseToSparseCopiesExactlyTheOriginalSourceWindowInEveryPrimitiveKind() {
        for (M3PrimitiveKind kind : M3PrimitiveKind.values()) {
            M3PrimitiveBigArray source = M3PrimitiveBigArray.sparse(kind, LENGTH, SHIFT);
            M3PrimitiveBigArray target = M3PrimitiveBigArray.sparse(kind, LENGTH, SHIFT);
            Random random = new Random(0x4d33535041525345L + kind.ordinal());
            for (int i = 0; i < 280; i++) {
                source.setBits(random.nextInt((int) LENGTH), bits(kind, i + 1));
                target.setBits(random.nextInt((int) LENGTH), bits(kind, i + 1000));
            }
            long before = source.allocatedPayloadBytes();
            for (int q = 0; q < 480; q++) {
                int count = random.nextInt(195);
                long from = random.nextInt((int) LENGTH - count + 1);
                long to = random.nextInt((int) LENGTH - count + 1);
                long[] expected = new long[count];
                for (int i = 0; i < count; i++) expected[i] = source.getBits(from + i);
                long guardLeft = to > 0 ? target.getBits(to - 1) : Long.MIN_VALUE;
                long guardRight = to + count < LENGTH
                        ? target.getBits(to + count) : Long.MIN_VALUE;
                source.copyTo(from, target, to, count);
                for (int i = 0; i < count; i++) {
                    assertEquals(expected[i], target.getBits(to + i),
                            kind + " query=" + q + " offset=" + i);
                }
                if (to > 0) assertEquals(guardLeft, target.getBits(to - 1));
                if (to + count < LENGTH) assertEquals(guardRight, target.getBits(to + count));
            }
            assertEquals(before, source.allocatedPayloadBytes(),
                    kind + " source residency must not change");
        }
    }

    @Test
    void sparseToDenseRetainsZeroHolesAndRawFloatingPointPayloads() {
        for (M3PrimitiveKind kind : M3PrimitiveKind.values()) {
            M3PrimitiveBigArray source = M3PrimitiveBigArray.sparse(kind, 1L << 15, SHIFT);
            M3PrimitiveBigArray target = new M3PrimitiveBigArray(kind, 1L << 15, SHIFT);
            source.setBits(1020, bits(kind, 3));
            source.setBits(2050, bits(kind, 2));
            source.setBits(4109, bits(kind, 7));
            target.fillBits(1000, 5000, bits(kind, 5));
            long[] expected = new long[3200];
            for (int i = 0; i < expected.length; i++) expected[i] = source.getBits(1000 + i);
            source.copyTo(1000, target, 1400, expected.length);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], target.getBits(1400 + i),
                        kind + " dense offset=" + i);
            }
            assertEquals(0L, target.getBits(1500));
        }
    }

    @Test
    void boundedCopyPreservesExtremeLongCoordinatesAndUnrelatedPages() {
        M3PrimitiveBigArray source = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, Long.MAX_VALUE, SHIFT);
        M3PrimitiveBigArray target = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, Long.MAX_VALUE, SHIFT);
        source.setBits(1, 17L);
        source.setBits(1L << 42, 29L);
        source.setBits(Long.MAX_VALUE - 1L, 73L);
        target.setBits(3, 999L);
        target.setBits(Long.MAX_VALUE - 2400L, 81L);
        long[] before = new long[1500];
        long from = Long.MAX_VALUE - before.length;
        long to = Long.MAX_VALUE - 2600L;
        for (int i = 0; i < before.length; i++) before[i] = source.getBits(from + i);
        long residentBytes = source.allocatedPayloadBytes();
        source.copyTo(from, target, to, before.length);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], target.getBits(to + i));
        }
        assertEquals(999L, target.getBits(3));
        assertEquals(17L, source.getBits(1));
        assertEquals(29L, source.getBits(1L << 42));
        assertEquals(residentBytes, source.allocatedPayloadBytes());
    }

    @Test
    void stagingForOverlappingSparseMemmoveMatchesIndependentSnapshot() {
        M3PrimitiveBigArray array = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, 1L << 16, SHIFT);
        for (int i : new int[] {1000, 1023, 1024, 2048, 3072, 4095, 5010, 7999}) {
            array.setBits(i, i + 100);
        }
        int from = 995;
        int to = 1527;
        int count = 4100;
        long[] snapshot = new long[count];
        for (int i = 0; i < count; i++) snapshot[i] = array.getBits(from + i);
        long left = array.getBits(to - 1);
        long right = array.getBits(to + count);
        array.copyTo(from, array, to, count);
        for (int i = 0; i < count; i++) {
            assertEquals(snapshot[i], array.getBits(to + i), "memmove offset=" + i);
        }
        assertEquals(left, array.getBits(to - 1));
        assertEquals(right, array.getBits(to + count));
    }

    @Test
    void boundedTraversalMatchesOriginalResidentLeafIdentityAndOrder() {
        M3LazyPrimitiveAddressSpace owner = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, Long.MAX_VALUE, SHIFT);
        long[] positions = {0L, 1024L, 1L << 31, 1L << 42,
                Long.MAX_VALUE - 2049, Long.MAX_VALUE - 1};
        for (int i = 0; i < positions.length; i++) owner.setBits(positions[i], i + 1);
        List<Long> original = new ArrayList<>();
        List<Object> oldReferences = new ArrayList<>();
        owner.forEachAllocatedLeaf((base, page) -> {
            original.add(base);
            oldReferences.add(page);
        });
        List<Long> actual = new ArrayList<>();
        List<Object> references = new ArrayList<>();
        long from = (1L << 42) - 1L;
        long to = (1L << 42) + 4L;
        owner.forEachAllocatedLeaf(from, to, (base, page) -> {
            actual.add(base);
            references.add(page);
        });
        assertEquals(List.of(1L << 42), actual);
        assertTrue(actual.size() < original.size());
        assertSame(oldReferences.get(original.indexOf(1L << 42)), references.get(0));
    }

    @Test
    void invalidRangesAndKindsRefuseBeforeModifyingTheTarget() {
        M3PrimitiveBigArray source = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, 1L << 15, SHIFT);
        M3PrimitiveBigArray target = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, 1L << 15, SHIFT);
        target.setBits(17L, 99L);
        assertThrows(IndexOutOfBoundsException.class,
                () -> source.copyTo(-1L, target, 10L, 1L));
        assertThrows(IndexOutOfBoundsException.class,
                () -> source.copyTo(0L, target, 0L, -1L));
        assertThrows(ArithmeticException.class,
                () -> source.copyTo(Long.MAX_VALUE, target, 0L, 2L));
        assertThrows(IllegalArgumentException.class,
                () -> source.copyTo(0L, M3PrimitiveBigArray.sparse(
                        M3PrimitiveKind.INT, 1L << 15, SHIFT), 0L, 1L));
        assertEquals(99L, target.getBits(17L));
    }
}
