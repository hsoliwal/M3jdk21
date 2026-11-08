// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PackedHeapGrowthTest {
    private static final long MIB = 1024L * 1024L;

    @Test
    void exactRequiredCapacityWhenHeapIsPressured() {
        assertEquals(9, PackedHeapGrowth.choose(8, 9, 8, 64L * MIB, 60L * MIB));
        assertEquals(20, PackedHeapGrowth.choose(8, 20, 8, 64L * MIB, 60L * MIB));
        assertEquals(12, PackedHeapGrowth.choose(8, 9, 8, 512L * MIB, 32L * MIB));
        assertEquals(12, PackedHeapGrowth.choose(8, 9, 8, Long.MAX_VALUE, 8L * MIB));
        assertEquals(12, PackedHeapGrowth.choose(8, 9, 8, 64L * MIB, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> PackedHeapGrowth.choose(8, 9, 3, 64L * MIB, 60L * MIB));
    }

    @Test
    void allWidthsKeepRequiredCapacityUnderPressure() {
        for (int width : new int[] {1, 2, 4, 8}) {
            for (int current = 0; current < 90; current++) {
                for (int extra = 1; extra < 10; extra++) {
                    int minimum = current + extra;
                    int chosen = PackedHeapGrowth.choose(
                            current, minimum, width, 64L * MIB, 62L * MIB);
                    assertTrue(chosen >= minimum);
                    assertTrue(chosen <= PackedSupport.growArrayCapacity(current, minimum));
                }
            }
        }
    }

    @Test
    void everyPrimitiveGrowthRetainsExistingValues() {
        assertArrayEquals(new boolean[] {true, false},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new boolean[] {true, false}, 3), 2));
        assertArrayEquals(new byte[] {1, 2},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new byte[] {1, 2}, 3), 2));
        assertArrayEquals(new short[] {1, 2},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new short[] {1, 2}, 3), 2));
        assertArrayEquals(new char[] {'a', 'b'},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new char[] {'a', 'b'}, 3), 2));
        assertArrayEquals(new int[] {1, 2},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new int[] {1, 2}, 3), 2));
        assertArrayEquals(new long[] {1L, 2L},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new long[] {1L, 2L}, 3), 2));
        assertArrayEquals(new float[] {1f, 2f},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new float[] {1f, 2f}, 3), 2));
        assertArrayEquals(new double[] {1d, 2d},
                java.util.Arrays.copyOf(PackedFlatArrays.grow(new double[] {1d, 2d}, 3), 2));
    }
}
