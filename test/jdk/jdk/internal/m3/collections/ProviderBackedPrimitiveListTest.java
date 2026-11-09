package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveKind;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderBackedPrimitiveListTest {

    @Test
    void logicalListLoadsOnlyTouchedIndexes() {
        int[] loads = {0};
        ProviderBackedPrimitiveList list =
                DenseCollections.providerList(
                        1_000_000L,
                        (rowId, target) -> {
                            loads[0]++;
                            target[0] = rowId * 10L;
                            target[1] = rowId + 1L;
                        },
                        PrimitiveKind.LONG,
                        PrimitiveKind.INT);

        assertEquals(1_000_000L, list.size());
        assertEquals(0L, list.materializedSize());

        assertEquals(700L, list.getBits(0, 70L));
        assertEquals(71L, list.getBits(1, 70L));
        assertEquals(1, loads[0]);
        assertEquals(1L, list.materializedSize());

        assertEquals(700L, list.getBits(0, 70L));
        assertEquals(1, loads[0], "resident list entry must not reload");
    }

    @Test
    void listTouchObservationUsesOneCollectionObserver() {
        final class Provider implements PrimitiveRowProvider, PrimitiveTouchObserver {
            final List<String> events = new ArrayList<>();
            int loads;

            @Override
            public void beforeTouch(long logicalId, short currentFlags) {
                events.add("before:" + logicalId + ":" + Short.toUnsignedInt(currentFlags));
            }

            @Override
            public void load(long rowId, long[] target) {
                loads++;
                target[0] = rowId;
            }

            @Override
            public void afterTouch(
                    long logicalId,
                    short resultingFlags,
                    boolean materializedNow) {
                events.add("after:" + logicalId + ":" + materializedNow);
            }
        }

        Provider provider = new Provider();
        ProviderBackedPrimitiveList list =
                new ProviderBackedPrimitiveList(100L, provider, PrimitiveKind.LONG);

        assertTrue(list.touch(9L));
        assertFalse(list.touch(9L));
        assertEquals(1, provider.loads);
        assertEquals(
                List.of(
                        "before:9:0",
                        "after:9:true",
                        "before:9:" + Short.toUnsignedInt(list.flags(9L)),
                        "after:9:false"),
                provider.events);
    }

    @Test
    void rangePrefetchRetainsExistingTouchSemanticsAndBounds() {
        int[] loads = {0};
        ProviderBackedPrimitiveList list =
                new ProviderBackedPrimitiveList(
                        16L,
                        (rowId, target) -> {
                            loads[0]++;
                            target[0] = rowId;
                        },
                        PrimitiveKind.LONG);

        list.prefetch(4L, 8L);
        assertEquals(4, loads[0]);
        assertEquals(4L, list.materializedSize());
        for (long index = 4; index < 8; index++) {
            assertTrue(list.isMaterialized(index));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> list.getBits(0, 16L));
        assertThrows(IndexOutOfBoundsException.class, () -> list.prefetch(7L, 17L));
    }

    @Test
    void listPreservesShortApplicationFlagsAndPinning() {
        ProviderBackedPrimitiveList list =
                new ProviderBackedPrimitiveList(
                        10L,
                        (rowId, target) -> target[0] = rowId,
                        PrimitiveKind.LONG);

        short app = (short) (LazyElementFlags.APPLICATION_0 | LazyElementFlags.APPLICATION_3);
        list.applicationFlags(3L, app);
        list.pin(3L, true);
        list.touch(3L);

        assertEquals(app, list.applicationFlags(3L));
        assertFalse(list.evict(3L));
        list.pin(3L, false);
        assertTrue(list.evict(3L));
        assertFalse(list.isMaterialized(3L));
    }
}
