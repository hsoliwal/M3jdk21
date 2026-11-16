/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Independent high-coordinate regression for the existing empty-pattern scan. */
final class IntKmpEmptyBoundaryTest {
    @Test
    void terminalMaximumIndexEmitsOnceWithoutSignedWraparound() {
        int maximum = Integer.MAX_VALUE;
        List<Integer> starts = new ArrayList<>();
        IntKmpPattern.emitEmptyBoundaries(maximum - 2, maximum, (from, to) -> {
            assertEquals(from, to);
            starts.add(from);
        });
        assertEquals(List.of(maximum - 2, maximum - 1, maximum), starts);
        starts.clear();
        IntKmpPattern.emitEmptyBoundaries(maximum, maximum, (from, to) -> starts.add(from));
        assertEquals(List.of(maximum), starts);
    }

    @Test
    void originalPublicScanIncludesEveryBoundaryInOrder() {
        IntKmpPattern empty = new IntKmpPattern(new int[0]);
        for (int length = 0; length < 129; length++) {
            int[] text = new int[length];
            List<Integer> actual = new ArrayList<>();
            empty.scan(text, (from, to) -> {
                assertEquals(from, to);
                actual.add(from);
            });
            assertEquals(length + 1, actual.size());
            for (int offset = 0; offset <= length; offset++) {
                assertEquals(offset, actual.get(offset).intValue());
            }
            assertEquals(length + 1, empty.count(text));
            assertEquals(0, empty.indexOf(text));
        }
    }

    @Test
    void rejectsInvalidInternalBoundsAndPreservesCallbackIdentity() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> IntKmpPattern.emitEmptyBoundaries(-1, 0, (from, to) -> {}));
        assertThrows(IndexOutOfBoundsException.class,
                () -> IntKmpPattern.emitEmptyBoundaries(9, 8, (from, to) -> {}));
        assertEquals("consumer", assertThrows(NullPointerException.class,
                () -> IntKmpPattern.emitEmptyBoundaries(1, 1, null)).getMessage());
        IllegalStateException callback = new IllegalStateException("stop");
        assertSame(callback, assertThrows(IllegalStateException.class,
                () -> IntKmpPattern.emitEmptyBoundaries(4, 5, (from, to) -> {
                    throw callback;
                })));
        IntKmpPattern empty = new IntKmpPattern(new int[0]);
        assertEquals("text", assertThrows(NullPointerException.class,
                () -> empty.scan(null, null)).getMessage());
        assertEquals("consumer", assertThrows(NullPointerException.class,
                () -> empty.scan(new int[0], null)).getMessage());
    }
}
