/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/** API_UNION U1: the default {@link LongLongSortedMap#filterInPlace(LongLongPredicate)} keeps accepted entries. */
class LongLongSortedMapFilterInPlaceTest {

    private static LongLongSortedMap squares(int n) {
        LongLongSortedMap map = new LongLongTreeMap();
        LongStream.range(0, n).forEach(k -> map.put(k, k * k));
        return map;
    }

    @Test
    void keepsAcceptedEntriesAndReportsRemovedCount() {
        LongLongSortedMap map = squares(10);
        assertEquals(5, map.filterInPlace((key, value) -> key % 2 == 0));
        assertArrayEquals(new long[] {0, 2, 4, 6, 8}, map.keysToArray());
        assertArrayEquals(new long[] {0, 4, 16, 36, 64}, map.valuesToArray());
    }

    @Test
    void acceptingEverythingRemovesNothingAndRejectingEverythingEmpties() {
        LongLongSortedMap map = squares(4);
        assertEquals(0, map.filterInPlace((key, value) -> true));
        assertEquals(4, map.size());
        assertEquals(4, map.filterInPlace((key, value) -> false));
        assertEquals(0, map.size());
    }

    @Test
    void rejectsNullPredicate() {
        assertThrows(NullPointerException.class, () -> squares(1).filterInPlace(null));
    }
}
