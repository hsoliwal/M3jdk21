package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LazyElementFlags;
import jdk.internal.m3.collections.PrimitiveRowProvider;
import jdk.internal.m3.collections.ProviderBackedPrimitiveRows;
import jdk.internal.m3.collections.ProviderLoadException;

class M3ProviderBackedPrimitiveRowsTest {

    @Test
    void firstTouchLoadsOnceAndRepeatedReadsStayPrimitive() {
        int[] loads = {0};
        PrimitiveRowProvider provider = (rowId, target) -> {
            loads[0]++;
            target[0] = rowId * 10L;
            target[1] = rowId == 5L ? 0L : rowId + 1L;
        };

        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(
                        provider,
                        PrimitiveKind.LONG,
                        PrimitiveKind.INT);

        assertFalse(rows.isMaterialized(5L));
        assertEquals(50L, rows.getBits(0, 5L));
        assertEquals(0L, rows.getBits(1, 5L));
        assertEquals(1, loads[0]);
        assertTrue(rows.isMaterialized(5L));
        assertTrue(LazyElementFlags.has(rows.flags(5L), LazyElementFlags.MATERIALIZED));

        assertEquals(50L, rows.getBits(0, 5L));
        assertEquals(1, loads[0], "second touch must not reload");
        assertEquals(1L, rows.materializedSize(), "all-zero lanes still retain row presence");
    }

    @Test
    void invalidationAndRetryAreExplicit() {
        int[] attempts = {0};
        PrimitiveRowProvider provider = (rowId, target) -> {
            attempts[0]++;
            if (attempts[0] == 1) throw new IllegalStateException("first failure");
            target[0] = 77L;
        };

        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(provider, PrimitiveKind.LONG);

        ProviderLoadException failure =
                assertThrows(ProviderLoadException.class, () -> rows.touch(9L));
        assertEquals(9L, failure.logicalId());
        assertTrue(LazyElementFlags.has(rows.flags(9L), LazyElementFlags.FAILED));
        assertFalse(rows.isMaterialized(9L));

        assertThrows(IllegalStateException.class, () -> rows.touch(9L));
        assertTrue(rows.retry(9L));
        assertEquals(2, attempts[0]);
        assertEquals(77L, rows.getBits(0, 9L));

        assertTrue(rows.invalidate(9L));
        assertTrue(LazyElementFlags.has(rows.flags(9L), LazyElementFlags.INVALIDATED));
        assertEquals(77L, rows.getBits(0, 9L));
        assertEquals(3, attempts[0]);
    }

    @Test
    void applicationFlagsAndPinningShareOneShortWord() {
        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(
                        (rowId, target) -> target[0] = rowId,
                        PrimitiveKind.LONG);

        rows.applicationFlags(
                11L,
                (short) (LazyElementFlags.APPLICATION_1 | LazyElementFlags.APPLICATION_3));
        rows.pin(11L, true);
        rows.touch(11L);

        short flags = rows.flags(11L);
        assertTrue(LazyElementFlags.has(flags, LazyElementFlags.PINNED));
        assertEquals(
                (short) (LazyElementFlags.APPLICATION_1 | LazyElementFlags.APPLICATION_3),
                rows.applicationFlags(11L));
        assertFalse(rows.evict(11L), "pinned row must not evict");

        rows.pin(11L, false);
        assertTrue(rows.evict(11L));
        assertFalse(rows.isMaterialized(11L));
    }

    @Test
    void providerFailureCallbackCannotMaskPrimaryFailure() {
        PrimitiveRowProvider provider = new PrimitiveRowProvider() {
            @Override
            public void load(long rowId, long[] target) {
                throw new IllegalArgumentException("load");
            }

            @Override
            public void onLoadFailure(long rowId, Exception failure) {
                throw new IllegalStateException("observer");
            }
        };

        ProviderBackedPrimitiveRows rows =
                new ProviderBackedPrimitiveRows(provider, PrimitiveKind.LONG);
        ProviderLoadException failure =
                assertThrows(ProviderLoadException.class, () -> rows.touch(1L));

        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertEquals(1, failure.getCause().getSuppressed().length);
        assertEquals("observer", failure.getCause().getSuppressed()[0].getMessage());
    }

    @Test
    void flagsUseExactlySixteenBitsOfDeclaredState() {
        int union = 0;
        short[] bits = {
                LazyElementFlags.DISCOVERED,
                LazyElementFlags.LOADING,
                LazyElementFlags.MATERIALIZED,
                LazyElementFlags.FAILED,
                LazyElementFlags.DIRTY,
                LazyElementFlags.PINNED,
                LazyElementFlags.INVALIDATED,
                LazyElementFlags.CHILDREN_KNOWN,
                LazyElementFlags.HAS_CHILDREN,
                LazyElementFlags.CHILDREN_LOADING,
                LazyElementFlags.CHILDREN_PARTIAL,
                LazyElementFlags.CHILDREN_COMPLETE,
                LazyElementFlags.APPLICATION_0,
                LazyElementFlags.APPLICATION_1,
                LazyElementFlags.APPLICATION_2,
                LazyElementFlags.APPLICATION_3
        };
        for (short bit : bits) {
            int unsigned = Short.toUnsignedInt(bit);
            assertEquals(1, Integer.bitCount(unsigned));
            assertEquals(0, union & unsigned, "flag bit reused");
            union |= unsigned;
        }
        assertEquals(0xffff, union);
    }
}
