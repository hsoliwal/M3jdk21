// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PackedU8Test {
    @Test
    void normalizedArithmeticStaysInteger() {
        assertEquals(255, PackedU8.multiply(255, 255));
        assertEquals(0, PackedU8.multiply(0, 255));
        assertEquals(64, PackedU8.multiply(128, 128));
        assertEquals(128, PackedU8.blend(255, 0, 128));
        assertEquals(128, PackedU8.ratio(1, 2));
        assertEquals(255, PackedU8.saturatingAdd(240, 30));
        assertEquals(0, PackedU8.saturatingSubtract(10, 30));
        assertEquals(255, PackedU8.ratio(Long.MAX_VALUE - 1L, Long.MAX_VALUE));
    }

    @Test
    void byteRoundTripPreservesAllUnsignedLevels() {
        for (int value = 0; value <= 255; value++) {
            assertEquals(value, PackedU8.unsigned(PackedU8.of(value)));
        }
        assertTrue(PackedU8.atLeast(PackedU8.of(192), 192));
    }
}
