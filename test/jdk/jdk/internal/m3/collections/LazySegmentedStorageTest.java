package jdk.internal.m3.collections;

import jdk.internal.m3.collections.bits.LazyBitAddressSpace;
import jdk.m3.collections.LazyPrimitiveAddressSpace;
import jdk.m3.collections.PrimitiveKind;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LazySegmentedStorageTest {

    @Test
    void untouchedAddressSpaceHasLogicalCapacityWithoutPayloadLeaves() {
        LazyPrimitiveAddressSpace storage =
                new LazyPrimitiveAddressSpace(PrimitiveKind.LONG, 8, 8, 12);

        assertEquals(1L << 28, storage.logicalCapacity());
        assertEquals(0, storage.allocatedLeafCount());
        assertEquals(0L, storage.allocatedPayloadBytes());

        assertEquals(0L, storage.getBits(123_456L));
        assertEquals(0, storage.allocatedLeafCount(), "read must not allocate");
    }

    @Test
    void oneWriteAllocatesExactlyItsDirectoryPathAndOnePrimitiveLeaf() {
        LazyPrimitiveAddressSpace storage =
                new LazyPrimitiveAddressSpace(PrimitiveKind.LONG, 8, 8, 12);

        long id = (37L << 20) | (19L << 12) | 123L;
        storage.setBits(id, 99L);

        assertEquals(99L, storage.getBits(id));
        assertEquals(1, storage.allocatedMiddleDirectoryCount());
        assertEquals(1, storage.allocatedLeafCount());
        assertEquals(4096L * Long.BYTES, storage.allocatedPayloadBytes());

        storage.setBits(id + 1L, 100L);
        assertEquals(1, storage.allocatedLeafCount(), "same leaf must reuse allocation");

        storage.setBits((38L << 20) | 5L, 7L);
        assertEquals(2, storage.allocatedMiddleDirectoryCount());
        assertEquals(2, storage.allocatedLeafCount());
    }

    @Test
    void zeroWriteIntoAbsentBranchDoesNotAllocate() {
        LazyPrimitiveAddressSpace storage =
                new LazyPrimitiveAddressSpace(PrimitiveKind.INT, 8, 8, 12);

        storage.setBits((200L << 20) | 42L, 0L);

        assertEquals(0, storage.allocatedLeafCount());
        assertEquals(0, storage.allocatedMiddleDirectoryCount());
    }

    @Test
    void sparsePresenceDistinguishesStoredZeroFromAbsent() {
        LazyPrimitiveSoaTable table =
                new LazyPrimitiveSoaTable(
                        8, 8, 12,
                        PrimitiveKind.LONG,
                        PrimitiveKind.INT,
                        PrimitiveKind.BYTE,
                        PrimitiveKind.BOOLEAN);

        long row = (200L << 20) | (17L << 12) | 123L;
        long zeroRow = row + 1L;

        assertTrue(table.put(row, 42L, 7L, 255L, 1L));
        assertTrue(table.put(zeroRow, 0L, 0L, 0L, 0L));

        assertTrue(table.contains(zeroRow));
        assertEquals(0L, table.getBits(0, zeroRow, 123L));
        assertEquals(123L, table.getBits(0, zeroRow + 1L, 123L));
        assertEquals(2L, table.size());

        List<Long> ids = new ArrayList<>();
        table.forEachRowId(ids::add);
        assertEquals(List.of(row, zeroRow), ids);
    }

    @Test
    void sparseBitAddressSpaceAllocatesPackedLeavesOnlyWhenTouched() {
        LazyBitAddressSpace bits = new LazyBitAddressSpace(8, 8, 12);
        long[] ids = {
                0L,
                4095L,
                4096L,
                12_345L,
                (1L << 20) + 1L,
                (255L << 20) + 3L
        };

        for (long id : ids) assertTrue(bits.set(id));
        assertEquals(ids.length, bits.cardinality());
        for (long id : ids) assertTrue(bits.get(id));

        List<Long> actual = new ArrayList<>();
        bits.forEachSetBit(actual::add);
        assertArrayEquals(ids, actual.stream().mapToLong(Long::longValue).toArray());

        assertTrue(bits.clear(4096L));
        assertFalse(bits.get(4096L));
    }

    @Test
    void releaseLeafDropsPhysicalPayloadWithoutChangingOtherRegions() {
        LazyPrimitiveAddressSpace storage =
                new LazyPrimitiveAddressSpace(PrimitiveKind.LONG, 8, 8, 12);

        long first = (5L << 20) | 7L;
        long second = (6L << 20) | 9L;
        storage.setBits(first, 11L);
        storage.setBits(second, 22L);

        assertTrue(storage.releaseLeaf(first));
        assertEquals(0L, storage.getBits(first));
        assertEquals(22L, storage.getBits(second));
        assertEquals(1, storage.allocatedLeafCount());
    }

    @Test
    void denseCollectionsFactoriesUseSameGeometry() {
        LazyPrimitiveAddressSpace values =
                DenseCollections.lazyPrimitiveAddressSpace(PrimitiveKind.LONG);
        LazyBitAddressSpace bits = DenseCollections.lazyBits();
        LazyPrimitiveSoaTable rows =
                DenseCollections.lazyPrimitiveRows(PrimitiveKind.LONG, PrimitiveKind.INT);

        assertEquals(1L << 28, values.logicalCapacity());
        assertEquals(values.logicalCapacity(), bits.logicalCapacity());
        assertEquals(values.logicalCapacity(), rows.logicalCapacity());
    }
}
