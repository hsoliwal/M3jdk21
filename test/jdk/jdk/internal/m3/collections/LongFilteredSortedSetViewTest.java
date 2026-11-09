// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.packed.PackedLongSortedSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class LongFilteredSortedSetViewTest {
    @Test
    void navigationSkipsRejectedValuesAndRemainsLive() {
        PackedLongSortedSet source = new PackedLongSortedSet();
        source.addAll(new long[] {1, 2, 3, 4, 5, 6, 7, 8});
        LongFilteredSortedSetView even =
                new LongFilteredSortedSetView(source, value -> (value & 1L) == 0L);

        assertArrayEquals(new long[] {2, 4, 6, 8}, even.toArray());
        assertEquals(4L, even.floor(5, -1));
        assertEquals(6L, even.ceiling(5, -1));
        assertEquals(4L, even.lower(6, -1));
        assertEquals(8L, even.higher(6, -1));

        source.add(10);
        assertEquals(10L, even.last());
    }

    @Test
    void navigationHandlesExtremeLongValuesWithoutSentinelCollisions() {
        PackedLongSortedSet source = new PackedLongSortedSet();
        source.addAll(new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
        LongFilteredSortedSetView nonNegative =
                new LongFilteredSortedSetView(source, value -> value >= 0);

        assertEquals(0L, nonNegative.ceiling(Long.MIN_VALUE, 77));
        assertEquals(Long.MAX_VALUE, nonNegative.floor(Long.MAX_VALUE, 77));
        assertEquals(77L, nonNegative.higher(Long.MAX_VALUE, 77));
        assertEquals(77L, nonNegative.lower(0, 77));
    }

    @Test
    void emptyFilteredViewUsesDefaultsAndThrowsAtEndpoints() {
        PackedLongSortedSet source = new PackedLongSortedSet();
        source.addAll(new long[] {1, 3, 5});
        LongFilteredSortedSetView even =
                new LongFilteredSortedSetView(source, value -> (value & 1L) == 0L);

        assertEquals(99L, even.floor(9, 99));
        assertEquals(99L, even.ceiling(0, 99));
        assertThrows(NoSuchElementException.class, even::first);
        assertThrows(NoSuchElementException.class, even::last);
    }
}
