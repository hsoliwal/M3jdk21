// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent source-value oracle; no production decoder in expected results. */
final class PackedRangeQueryTest {
    @Test void exactQueriesCoverSignedExtremaEmptyAndOutsideIntervals() {
        for (int bits : new int[] {40, 48, 56}) {
            long min = -(1L << (bits - 1));
            long max = (1L << (bits - 1)) - 1;
            long[] values = {min, -7, -1, 0, 1, 7, max, min, max};
            PackedIntArray packed = create(bits, values);
            long[] bounds = {Long.MIN_VALUE, min - 1, min, -1, 0, 1, max, max + 1, Long.MAX_VALUE};
            byte[] before = packed.toByteArray();
            for (int from = 0; from <= values.length; from++) {
                for (int to = from; to <= values.length; to++) {
                    for (long low : bounds) for (long high : bounds) if (low <= high)
                        compare(packed, values, from, to, low, high);
                }
            }
            assertArrayEquals(before, packed.toByteArray());
            assertArrayEquals(values, packed.toLongArray());
        }
    }

    @Test void seededWindowsMatchSourceValuesWithoutExpandedTemporaries() {
        Random random = new Random(0x5041434b52414e47L);
        for (int bits : new int[] {40, 48, 56}) {
            for (int length : new int[] {0, 1, 63, 257, 8193}) {
                long[] values = new long[length];
                for (int i = 0; i < length; i++) values[i] = random.nextLong() >> (64 - bits);
                PackedIntArray packed = create(bits, values);
                for (int trial = 0; trial < 150; trial++) {
                    int from = random.nextInt(length + 1);
                    int to = from + random.nextInt(length - from + 1);
                    long a = random.nextLong() >> (64 - bits);
                    long b = random.nextLong() >> (64 - bits);
                    compare(packed, values, from, to, Math.min(a, b), Math.max(a, b));
                }
            }
        }
    }

    @Test void validationIncludesEmptyWindowsAndPrecedesValueChecks() {
        for (int bits : new int[] {40, 48, 56}) {
            PackedIntArray packed = create(bits, new long[] {0, 1});
            for (int[] window : new int[][] {{-1, 0}, {0, 3}, {2, 1}, {Integer.MIN_VALUE, Integer.MAX_VALUE}}) {
                assertThrows(IndexOutOfBoundsException.class,
                        () -> packed.countInRange(window[0], window[1], 1, 0));
                assertThrows(IndexOutOfBoundsException.class,
                        () -> packed.firstInRange(window[0], window[1], 1, 0));
            }
            assertThrows(IllegalArgumentException.class, () -> packed.countInRange(2, 2, 1, 0));
            assertThrows(IllegalArgumentException.class, () -> packed.firstInRange(2, 2, 1, 0));
            assertEquals(0, packed.countInRange(2, 2, Long.MIN_VALUE, Long.MAX_VALUE));
            assertEquals(-1, packed.firstInRange(2, 2, Long.MIN_VALUE, Long.MAX_VALUE));
        }
    }

    private static void compare(PackedIntArray packed, long[] values, int from, int to, long low, long high) {
        int expectedCount = (int) Arrays.stream(values, from, to).filter(v -> v >= low && v <= high).count();
        int expectedFirst = -1;
        for (int i = from; i < to; i++) if (values[i] >= low && values[i] <= high) { expectedFirst = i; break; }
        assertEquals(expectedCount, packed.countInRange(from, to, low, high));
        assertEquals(expectedFirst, packed.firstInRange(from, to, low, high));
    }

    private static PackedIntArray create(int bits, long[] values) {
        PackedIntArray packed = switch (bits) {
            case 40 -> new Int40Array(values.length);
            case 48 -> new Int48Array(values.length);
            case 56 -> new Int56Array(values.length);
            default -> throw new AssertionError(bits);
        };
        packed.setAll(0, values, 0, values.length);
        return packed;
    }
}
