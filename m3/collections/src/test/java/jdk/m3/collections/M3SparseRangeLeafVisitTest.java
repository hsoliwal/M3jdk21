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

/** Exact existing-trie evidence for branch-pruned sparse range search. */
final class M3SparseRangeLeafVisitTest {
    private record Leaf(long base, Object page) { }
    private static final int SHIFT = 10;
    private static final long PAGE_SIZE = 1L << SHIFT;

    @Test
    void oldWholeTreeTraversalAndNewRangeTraversalSelectIdenticalLeafReferences() {
        M3LazyPrimitiveAddressSpace space = M3LazyPrimitiveAddressSpace.paged(
                PrimitiveKind.LONG, Long.MAX_VALUE, SHIFT);
        long[] indexes = {0L, 1023L, 1024L, 65535L, (1L << 31),
                3_000_000_000L, (1L << 42) + 7L, Long.MAX_VALUE - 2048L,
                Long.MAX_VALUE - 1L};
        for (int i = 0; i < indexes.length; i++) space.setBits(indexes[i], i + 1);
        List<Leaf> original = new ArrayList<>();
        space.forEachAllocatedLeaf((base, page) -> original.add(new Leaf(base, page)));
        long beforeLeaves = space.allocatedLeafCount64();
        long beforeSlots = space.directoryReferenceSlots();
        long[] starts = {0, 1, 1023, 1024, 65535, (1L << 31) - 1,
                (1L << 31), 3_000_000_000L, (1L << 42) - 1,
                (1L << 42), Long.MAX_VALUE - 4096, Long.MAX_VALUE - 1,
                Long.MAX_VALUE};
        long[] lengths = {0, 1, 2, 17, 1024, 8192, 131072};
        long visited = 0;
        long fullVisits = 0;
        for (long from : starts) {
            for (long delta : lengths) {
                long to = from + Math.min(Long.MAX_VALUE - from, delta);
                List<Leaf> wanted = new ArrayList<>();
                if (to > from) {
                    original.stream().filter(leaf -> leaf.base < to
                            && from < (leaf.base > Long.MAX_VALUE - PAGE_SIZE
                                    ? Long.MAX_VALUE : leaf.base + PAGE_SIZE))
                            .forEach(wanted::add);
                }
                List<Leaf> actual = new ArrayList<>();
                space.forEachAllocatedLeaf(from, to,
                        (base, page) -> actual.add(new Leaf(base, page)));
                assertEquals(wanted, actual, "bounded visit [" + from + ',' + to + ')');
                for (int i = 0; i < actual.size(); i++) {
                    assertSame(wanted.get(i).page(), actual.get(i).page());
                }
                visited += actual.size();
                fullVisits += original.size();
            }
        }
        assertTrue(visited < fullVisits, "unrelated resident leaves must not be visited");
        assertEquals(beforeLeaves, space.allocatedLeafCount64());
        assertEquals(beforeSlots, space.directoryReferenceSlots());
    }

    @Test
    void originalThreeLevelGeometryAndRangeRefusalsRemainUnchanged() {
        M3LazyPrimitiveAddressSpace space = new M3LazyPrimitiveAddressSpace(PrimitiveKind.LONG, 2, 3, 6);
        for (long address : new long[] {4L, 70L, 1300L}) space.setBits(address, address);
        List<Long> visited = new ArrayList<>();
        space.forEachAllocatedLeaf(64, 65, (base, page) -> visited.add(base));
        assertEquals(List.of(64L), visited);
        visited.clear();
        space.forEachAllocatedLeaf(0, 2048, (base, page) -> visited.add(base));
        assertEquals(List.of(0L, 64L, 1280L), visited);
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.forEachAllocatedLeaf(-1, 1, (base, page) -> { }));
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.forEachAllocatedLeaf(1, 0, (base, page) -> { }));
        assertThrows(IndexOutOfBoundsException.class,
                () -> space.forEachAllocatedLeaf(0, 2049, (base, page) -> { }));
        assertThrows(NullPointerException.class,
                () -> space.forEachAllocatedLeaf(0, 1, null));
    }

    @Test
    void ownerFirstAndLastSearchPreserveSparseZerosAndExtremeCoordinates() {
        BigArray array = BigArray.sparse(PrimitiveKind.LONG, Long.MAX_VALUE, SHIFT);
        array.setBits(7L, 11L);
        array.setBits(1L << 42, 37L);
        array.setBits(Long.MAX_VALUE - 1, 73L);
        long before = array.allocatedPayloadBytes();
        assertEquals(1L << 42, array.indexOfBits((1L << 42) - 3,
                (1L << 42) + 4, 37L));
        assertEquals(1L << 42, array.lastIndexOfBits((1L << 42) - 3,
                (1L << 42) + 4, 37L));
        assertEquals((1L << 42) - 3, array.indexOfBits((1L << 42) - 3,
                (1L << 42) + 4, 0L));
        assertEquals((1L << 42) + 3, array.lastIndexOfBits((1L << 42) - 3,
                (1L << 42) + 4, 0L));
        assertEquals(Long.MAX_VALUE - 1,
                array.indexOfBits(Long.MAX_VALUE - 16, Long.MAX_VALUE, 73L));
        assertEquals(Long.MAX_VALUE - 1,
                array.lastIndexOfBits(Long.MAX_VALUE - 16, Long.MAX_VALUE, 73L));
        assertEquals(-1L, array.indexOfBits(1024, 2048, 37L));
        assertEquals(before, array.allocatedPayloadBytes());
    }

    @Test
    void boundedSearchMatchesIndependentLinearOracleAcrossManySmallRanges() {
        BigArray array = BigArray.sparse(PrimitiveKind.LONG, 1L << 22, SHIFT);
        Random random = new Random(0x4d33535041525345L);
        for (int i = 0; i < 70; i++) array.setBits(random.nextInt(1 << 22), i + 1);
        long before = array.allocatedPayloadBytes();
        for (int q = 0; q < 1600; q++) {
            long from = random.nextInt(1 << 22);
            long to = Math.min(array.length(), from + random.nextInt(130));
            long wanted = q % 5 == 0 ? 0L : q % 17;
            long first = -1L, last = -1L;
            for (long i = from; i < to; i++) {
                if (array.getBits(i) == wanted) {
                    if (first < 0) first = i;
                    last = i;
                }
            }
            assertEquals(first, array.indexOfBits(from, to, wanted), "first query " + q);
            assertEquals(last, array.lastIndexOfBits(from, to, wanted), "last query " + q);
        }
        assertEquals(before, array.allocatedPayloadBytes());
    }
}
