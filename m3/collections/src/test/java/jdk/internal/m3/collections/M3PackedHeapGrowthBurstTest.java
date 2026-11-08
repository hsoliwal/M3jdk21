// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Source-sealed compatibility evidence for the cold, advisory pressure branch. */
final class M3PackedHeapGrowthBurstTest {
    private static final long MIB = 1024L * 1024L;
    private static final long MAX = 256L * MIB;
    private static final long USED = 192L * MIB;

    @Test
    void moderatePressureKeepsBoundedAmortization() {
        int current = 6_000_000;
        int minimum = current + 1;
        int chosen = M3PackedHeapGrowth.choose(current, minimum, 4, MAX, USED);
        assertEquals(minimum + (1 << 20) / Integer.BYTES, chosen);
        assertTrue(chosen < M3PackedSupport.growArrayCapacity(current, minimum));
        assertTrue((long) chosen * 4L + 16L <= (MAX - USED) - MAX / 8L);
    }

    @Test
    void originalCriticalPressureAndUnknownHeapDecisionsRemainUnchanged() {
        assertEquals(9, M3PackedHeapGrowth.choose(8, 9, 8, 64L * MIB, 60L * MIB));
        assertEquals(20, M3PackedHeapGrowth.choose(8, 20, 8, 64L * MIB, 60L * MIB));
        assertEquals(12, M3PackedHeapGrowth.choose(8, 9, 8, 512L * MIB, 32L * MIB));
        assertEquals(12, M3PackedHeapGrowth.choose(8, 9, 8, Long.MAX_VALUE, 8L * MIB));
        assertEquals(12, M3PackedHeapGrowth.choose(8, 9, 8, 64L * MIB, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> M3PackedHeapGrowth.choose(8, 9, 3, MAX, USED));
    }

    @Test
    void everyWidthPreservesRequiredAndGeometricBounds() {
        for (int width : new int[] {1, 2, 4, 8}) {
            for (int current : new int[] {0, 1, 8, 63, 1024, 8192, 250_000,
                    1_000_000, 6_000_000, 12_000_000}) {
                for (int step : new int[] {1, 2, 17, 4096}) {
                    int minimum = current + step;
                    int chosen = M3PackedHeapGrowth.choose(current, minimum, width, MAX, USED);
                    int geometric = M3PackedSupport.growArrayCapacity(current, minimum);
                    assertTrue(chosen >= minimum, "minimum");
                    assertTrue(chosen <= geometric, "geometric");
                    long availableAfterReserve = MAX - USED - MAX / 8L;
                    if ((long) minimum * width + 16L <= availableAfterReserve) {
                        assertTrue((long) chosen * width + 16L <= availableAfterReserve,
                                "bounded optional headroom");
                    } else {
                        assertEquals(minimum, chosen, "mandatory allocation remains advisory");
                    }
                }
            }
        }
    }

    @Test
    void boundedBurstAvoidsPerAppendCopiesWhenBudgetAllows() {
        int capacity = 6_000_000;
        int copies = 0;
        long copiedElements = 0;
        for (int size = capacity; size < 6_200_000; size++) {
            if (size == capacity) {
                copiedElements += size;
                capacity = M3PackedHeapGrowth.choose(capacity, size + 1, 4, MAX, USED);
                copies++;
            }
        }
        assertEquals(1, copies);
        assertEquals(6_000_000L, copiedElements);
        // The preceding exact-size policy would copy at every individual append.
        assertTrue(copies < 200_000);
    }

    @Test
    void hugeArrayGeometryCannotOverflowIntoNegativeCapacity() {
        int maximum = M3PackedSupport.MAX_ARRAY_SIZE;
        int chosen = M3PackedHeapGrowth.choose(maximum - 1024, maximum - 1,
                8, 64L * MIB, 60L * MIB);
        assertEquals(maximum - 1, chosen);
    }
}
