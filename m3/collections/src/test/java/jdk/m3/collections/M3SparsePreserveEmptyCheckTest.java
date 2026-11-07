// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Original-owner Java 21 contracts for the preserve-only directory scan atom. */
final class M3SparsePreserveEmptyCheckTest {
    private static final int SHIFT = 10;
    private static final long SIZE = 1L << 18;

    private static long raw(M3PrimitiveKind kind, int n) {
        return switch (kind) {
            case BOOLEAN -> n % 4 == 0 ? 0L : 1L;
            case BYTE, SHORT, CHAR, INT, LONG -> n % 11 == 0 ? 0L : 0x4dd7L + 43L * n;
            case FLOAT -> switch (n % 5) {
                case 0 -> 0L;
                case 1 -> 0x80000000L;
                case 2 -> 0x7fc00001L;
                case 3 -> 0x7fc01234L;
                default -> Integer.toUnsignedLong(Float.floatToRawIntBits((float) n));
            };
            case DOUBLE -> switch (n % 5) {
                case 0 -> 0L;
                case 1 -> 0x8000000000000000L;
                case 2 -> 0x7ff8000000000001L;
                case 3 -> 0x7ff8000000001234L;
                default -> Double.doubleToRawLongBits((double) n);
            };
        };
    }

    @Test
    void preservingCompleteRangeKeepsEveryPhysicalLeafAndDirectory() {
        long[] points = {0L, 1035L, 1L << 30, (1L << 42) + 7L, Long.MAX_VALUE - 1L};
        for (M3PrimitiveKind kind : M3PrimitiveKind.values()) {
            M3LazyPrimitiveAddressSpace owner = M3LazyPrimitiveAddressSpace.paged(
                    kind, Long.MAX_VALUE, SHIFT);
            Object[] pages = new Object[points.length];
            for (int i = 0; i < points.length; i++) {
                owner.setBits(points[i], raw(kind, i + 1));
                // BOOLEAN/zero values may be unallocated; force one nonzero byte in
                // every leaf without changing the exact physical kind.
                owner.setBits(points[i], kind.storageBits(1L));
                pages[i] = owner.existingLeaf(points[i]);
                assertNotNull(pages[i]);
            }
            long leaves = owner.allocatedLeafCount64();
            long directories = owner.allocatedMiddleDirectoryCount64();
            long slots = owner.directoryReferenceSlots();
            owner.clearRangePreservingLeaves(0L, Long.MAX_VALUE);
            owner.clearRangePreservingLeaves(0L, Long.MAX_VALUE);
            assertEquals(leaves, owner.allocatedLeafCount64(), kind.toString());
            assertEquals(directories, owner.allocatedMiddleDirectoryCount64(), kind.toString());
            assertEquals(slots, owner.directoryReferenceSlots(), kind.toString());
            for (int i = 0; i < points.length; i++) {
                assertSame(pages[i], owner.existingLeaf(points[i]), kind + " physical alias " + i);
                assertEquals(0L, owner.getBits(points[i]), kind + " zeroed " + i);
            }
            owner.clearRange(0L, Long.MAX_VALUE);
            assertEquals(0L, owner.allocatedLeafCount64());
            assertEquals(0L, owner.allocatedMiddleDirectoryCount64());
            for (long point : points) assertNull(owner.existingLeaf(point));
        }
    }

    @Test
    void partialPreserveThenReleaseOnlyDetachesFullyCoveredPages() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, 1L << 23, SHIFT);
        long[] p = {10L, 1030L, 2050L, 4100L};
        for (int i = 0; i < p.length; i++) space.setBits(p[i], i + 11L);
        Object first = space.existingLeaf(p[0]);
        Object second = space.existingLeaf(p[1]);
        Object third = space.existingLeaf(p[2]);
        long leaves = space.allocatedLeafCount64();
        long dirs = space.allocatedMiddleDirectoryCount64();
        space.clearRangePreservingLeaves(1024L, 3072L);
        assertSame(second, space.existingLeaf(p[1]));
        assertSame(third, space.existingLeaf(p[2]));
        assertEquals(leaves, space.allocatedLeafCount64());
        assertEquals(dirs, space.allocatedMiddleDirectoryCount64());
        assertEquals(0L, space.getBits(p[1]));
        assertEquals(0L, space.getBits(p[2]));
        space.clearRange(1024L, 3072L);
        assertNull(space.existingLeaf(p[1]));
        assertNull(space.existingLeaf(p[2]));
        assertSame(first, space.existingLeaf(p[0]));
        assertEquals(leaves - 2L, space.allocatedLeafCount64());
        space.setBits(p[1], 99L);
        assertNotSame(second, space.existingLeaf(p[1]));
        assertEquals(99L, space.getBits(p[1]));
    }

    @Test
    void mixedRandomizedClearAndMutationPreserveOriginalRawBits() {
        for (M3PrimitiveKind kind : M3PrimitiveKind.values()) {
            M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                    kind, SIZE, SHIFT);
            Map<Long, Long> expected = new HashMap<>();
            Random random = new Random(0x4d33505245534552L + kind.ordinal());
            for (int i = 1; i <= 240; i++) {
                long address = random.nextLong(SIZE);
                long bits = kind.storageBits(raw(kind, i));
                space.setBits(address, bits);
                if (bits == 0L) expected.remove(address);
                else expected.put(address, bits);
            }
            for (int trial = 0; trial < 180; trial++) {
                long from = random.nextLong(SIZE + 1);
                long count = random.nextLong(Math.min(SIZE - from, 2048L) + 1);
                long to = from + count;
                boolean preserve = (trial % 3) != 0;
                if (trial % 43 == 0) { from = 0L; to = SIZE; }
                long leafBefore = space.allocatedLeafCount64();
                long directoryBefore = space.allocatedMiddleDirectoryCount64();
                if (preserve) space.clearRangePreservingLeaves(from, to);
                else space.clearRange(from, to);
                final long left = from, right = to;
                expected.entrySet().removeIf(e -> e.getKey() >= left && e.getKey() < right);
                if (preserve) {
                    assertEquals(leafBefore, space.allocatedLeafCount64(),
                            "preserving leaf count: " + kind + " trial " + trial);
                    assertEquals(directoryBefore, space.allocatedMiddleDirectoryCount64(),
                            "preserving directory count: " + kind + " trial " + trial);
                }
                for (var entry : expected.entrySet()) {
                    assertEquals(entry.getValue().longValue(), space.getBits(entry.getKey()),
                            "value " + kind + " trial " + trial);
                }
                for (int probe = 0; probe < 20; probe++) {
                    long index = random.nextLong(SIZE);
                    assertEquals(expected.getOrDefault(index, 0L).longValue(), space.getBits(index),
                            "hole " + kind + " trial " + trial);
                }
                long address = random.nextLong(SIZE);
                long bits = kind.storageBits(raw(kind, 1500 + trial));
                space.setBits(address, bits);
                if (bits == 0L) expected.remove(address);
                else expected.put(address, bits);
            }
        }
    }

    @Test
    void sparseCopyUsesPreservingClearWithoutDestroysUnrelatedRoots() {
        M3PrimitiveBigArray from = M3PrimitiveBigArray.sparse(M3PrimitiveKind.LONG, 1L << 20, SHIFT);
        M3PrimitiveBigArray target = M3PrimitiveBigArray.sparse(M3PrimitiveKind.LONG, 1L << 20, SHIFT);
        from.setBits(1021, 17L);
        from.setBits(2055, 31L);
        from.setBits(6145, 49L);
        target.setBits(1, 707L);
        target.setBits(15151, 808L);
        target.setBits(4242, 909L);
        final long sourceIndex = 900L, targetIndex = 4100L;
        final int count = 5400;
        long[] snapshot = new long[count];
        for (int i = 0; i < count; i++) snapshot[i] = from.getBits(sourceIndex + i);
        from.copyTo(sourceIndex, target, targetIndex, count);
        for (int i = 0; i < count; i++) {
            assertEquals(snapshot[i], target.getBits(targetIndex + i), "copy offset " + i);
        }
        assertEquals(707L, target.getBits(1L));
        assertEquals(808L, target.getBits(15151L));
        assertEquals(17L, from.getBits(1021L));
        assertEquals(49L, from.getBits(6145L));
    }

    @Test
    void emptyAndInvalidRangeRefuseWithoutPageOrDirectoryMutation() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                M3PrimitiveKind.LONG, 1L << 20, SHIFT);
        space.setBits(42L, 99L);
        Object page = space.existingLeaf(42L);
        long leaves = space.allocatedLeafCount64();
        long dirs = space.allocatedMiddleDirectoryCount64();
        space.clearRangePreservingLeaves(0L, 0L);
        space.clearRangePreservingLeaves(space.logicalCapacity(), space.logicalCapacity());
        space.clearRange(10L, 10L);
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.clearRangePreservingLeaves(-1L, 2L));
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.clearRangePreservingLeaves(100L, 99L));
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.clearRangePreservingLeaves(0L, space.logicalCapacity() + 1L));
        assertEquals(leaves, space.allocatedLeafCount64());
        assertEquals(dirs, space.allocatedMiddleDirectoryCount64());
        assertSame(page, space.existingLeaf(42L));
        assertEquals(99L, space.getBits(42L));
    }
}
