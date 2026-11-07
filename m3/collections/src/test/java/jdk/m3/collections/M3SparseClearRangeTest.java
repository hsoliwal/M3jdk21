// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Exact current-owner Java21 differential and physical leaf-ownership contracts. */
final class M3SparseClearRangeTest {
    private static final int SHIFT = 10;
    private static final int PAGE_SIZE = 1 << SHIFT;

    private static long sample(M3PrimitiveKind kind, int n) {
        return switch (kind) {
            case BOOLEAN -> n % 3 == 0 ? 0L : 1L;
            case BYTE, SHORT, CHAR, INT, LONG -> n % 7 == 0 ? 0L
                    : (0x5dda09aL + (long) n * 37L);
            case FLOAT -> switch (n % 5) {
                case 0 -> 0L;
                case 1 -> 0x80000000L;
                case 2 -> 0x7fc00001L;
                case 3 -> 0x7fc00123L;
                default -> Integer.toUnsignedLong(Float.floatToRawIntBits((float) n));
            };
            case DOUBLE -> switch (n % 5) {
                case 0 -> 0L;
                case 1 -> 0x8000000000000000L;
                case 2 -> 0x7ff8000000000001L;
                case 3 -> 0x7ff8000000000123L;
                default -> Double.doubleToRawLongBits((double) n);
            };
        };
    }

    @Test
    void repeatedClearAndPreservingClearMatchIndependentRawBitOracleForEveryKind() {
        final int capacity = 1 << 17;
        for (M3PrimitiveKind kind : M3PrimitiveKind.values()) {
            M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(kind, capacity, SHIFT);
            Random random = new Random(0x4d33534c4f54534cL + kind.ordinal());
            Map<Long, Long> wanted = new HashMap<>();
            for (int initial = 1; initial <= 260; initial++) {
                long at = random.nextInt(capacity);
                long bits = kind.storageBits(sample(kind, initial));
                space.setBits(at, bits);
                if (bits == 0L) wanted.remove(at);
                else wanted.put(at, bits);
            }
            for (int op = 0; op < 440; op++) {
                long from = random.nextInt(capacity + 1);
                long to = Math.min(capacity, from + random.nextInt(2000));
                if (op % 53 == 0) {
                    from = 0L;
                    to = capacity;
                }
                boolean retainLeaves = (op & 1) != 0;
                long leavesBefore = space.allocatedLeafCount64();
                if (retainLeaves) space.clearRangePreservingLeaves(from, to);
                else space.clearRange(from, to);
                final long first = from, end = to;
                wanted.entrySet().removeIf(e -> e.getKey() >= first && e.getKey() < end);
                if (retainLeaves) assertEquals(leavesBefore, space.allocatedLeafCount64(),
                        kind + " preserving clear cannot release physical pages");
                for (int probe = 0; probe < 24; probe++) {
                    long at = random.nextInt(capacity);
                    assertEquals(wanted.getOrDefault(at, 0L).longValue(), space.getBits(at),
                            kind + " op=" + op + " probe=" + probe);
                }
                for (var entry : wanted.entrySet()) {
                    assertEquals(entry.getValue().longValue(), space.getBits(entry.getKey()),
                            kind + " retained value at " + entry.getKey());
                }
                long insertAt = random.nextInt(capacity);
                long bits = kind.storageBits(sample(kind, 1000 + op));
                space.setBits(insertAt, bits);
                if (bits == 0L) wanted.remove(insertAt);
                else wanted.put(insertAt, bits);
            }
        }
    }

    @Test
    void completeLeafClearDetachesOnlyCoveredOwnerRoots() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, 1L << 23, SHIFT);
        space.setBits(11, 111);
        space.setBits(1033, 222);
        space.setBits(2070, 333);
        Object first = space.existingLeaf(11);
        Object middle = space.existingLeaf(1033);
        Object last = space.existingLeaf(2070);
        assertEquals(3L, space.allocatedLeafCount64());
        space.clearRange(PAGE_SIZE, 2L * PAGE_SIZE);
        assertEquals(2L, space.allocatedLeafCount64());
        assertSame(first, space.existingLeaf(11));
        assertNull(space.existingLeaf(1033));
        assertSame(last, space.existingLeaf(2070));
        assertEquals(222L, ((long[]) middle)[1033 - PAGE_SIZE],
                "a detached external array alias remains its original object");
        space.setBits(1033, 444);
        assertNotSame(middle, space.existingLeaf(1033));
        assertEquals(444L, space.getBits(1033));
        assertEquals(111L, space.getBits(11));
        assertEquals(333L, space.getBits(2070));
    }

    @Test
    void preflightZeroingRetainsTheExactOldBackingLeaf() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, 1L << 20, SHIFT);
        space.setBits(1040, 911L);
        space.setBits(1072, 827L);
        Object root = space.existingLeaf(1040);
        long count = space.allocatedLeafCount64();
        space.clearRangePreservingLeaves(1024L, 2048L);
        assertSame(root, space.existingLeaf(1040));
        assertEquals(count, space.allocatedLeafCount64());
        assertEquals(0L, space.getBits(1040));
        assertEquals(0L, space.getBits(1072));
        space.clearRange(1024L, 2048L);
        assertNull(space.existingLeaf(1040));
        assertEquals(count - 1L, space.allocatedLeafCount64());
    }

    @Test
    void extremeSignedLongExclusiveCapacityAndTruncatedTailStayCorrect() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, Long.MAX_VALUE, SHIFT);
        space.setBits(1L, 17L);
        space.setBits(1L << 42, 29L);
        space.setBits(Long.MAX_VALUE - 1, 73L);
        space.setBits(Long.MAX_VALUE - 1025L, 31L);
        long leavesBefore = space.allocatedLeafCount64();
        Object extremeLeaf = space.existingLeaf(Long.MAX_VALUE - 1L);
        assertNotNull(extremeLeaf);
        space.clearRange(Long.MAX_VALUE - 1L, Long.MAX_VALUE);
        assertSame(extremeLeaf, space.existingLeaf(Long.MAX_VALUE - 1L),
                "partial truncated tail may not detach full physical page");
        assertEquals(0L, space.getBits(Long.MAX_VALUE - 1L));
        assertEquals(31L, space.getBits(Long.MAX_VALUE - 1025L));
        space.clearRange(Long.MAX_VALUE - 2048L, Long.MAX_VALUE);
        assertEquals(0L, space.getBits(Long.MAX_VALUE - 1025L));
        assertNull(space.existingLeaf(Long.MAX_VALUE - 1L));
        assertEquals(leavesBefore - 2L, space.allocatedLeafCount64());
        assertEquals(17L, space.getBits(1L));
        assertEquals(29L, space.getBits(1L << 42));
        assertEquals(2L, space.allocatedLeafCount64());
    }

    @Test
    void emptyAndIllegalBoundsCannotAllocateOrChangeStorage() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, 1L << 17, SHIFT);
        space.setBits(42L, 99L);
        long before = space.allocatedLeafCount64();
        Object root = space.existingLeaf(42L);
        space.clearRange(0L, 0L);
        space.clearRange(space.logicalCapacity(), space.logicalCapacity());
        space.clearRangePreservingLeaves(200L, 200L);
        assertThrows(IndexOutOfBoundsException.class, () -> space.clearRange(-1L, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> space.clearRange(18L, 17L));
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.clearRange(0L, space.logicalCapacity() + 1L));
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.clearRangePreservingLeaves(100L, 1L));
        assertEquals(before, space.allocatedLeafCount64());
        assertEquals(99L, space.getBits(42L));
        assertSame(root, space.existingLeaf(42L));
    }

    @Test
    void sparseCopyStillPreservesZeroHolesAndUnrelatedDestinationAfterClear() {
        M3PrimitiveBigArray source = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, 1L << 20, SHIFT);
        M3PrimitiveBigArray target = M3PrimitiveBigArray.sparse(
                M3PrimitiveKind.LONG, 1L << 20, SHIFT);
        source.setBits(1017L, 111L);
        source.setBits(2053L, 222L);
        source.setBits(6131L, 333L);
        target.setBits(2L, 717L);
        target.setBits(10000L, 818L);
        target.setBits(5000L, 919L);
        final long from = 900L, to = 4150L;
        final int length = 5400;
        long[] sourceSnapshot = new long[length];
        for (int i = 0; i < length; i++) sourceSnapshot[i] = source.getBits(from + i);
        source.copyTo(from, target, to, length);
        for (int i = 0; i < length; i++) {
            assertEquals(sourceSnapshot[i], target.getBits(to + i), "copy offset=" + i);
        }
        assertEquals(717L, target.getBits(2L));
        assertEquals(818L, target.getBits(10000L));
        assertEquals(111L, source.getBits(1017L));
        assertTrue(target.allocatedPayloadBytes() > 0);
    }
}
