// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** Source-sealed real JUnit entry; standalone proof also runs without Maven. */
final class M3TokenCursorSeekLocalityTest {
    @Test
    void exhaustiveOracle() {
        M3TokenCursorSeekLocalityProof.main(new String[0]);
    }

    @Test
    void repeatedBoundaryAndAdjacentSeeks() {
        M3TokenCursor cursor = M3TokenCursor.lanes(
                new int[][] {{}, {1, 2}, {}, {3}, {4, 5, 6}});
        int[] positions = {0, 1, 2, 3, 5, 6, 2, 0, 6, 5, 3, 1};
        int[] expectedStarts = {0, 0, 2, 3, 3, 3, 2, 0, 3, 3, 3, 0};
        for (int i = 0; i < positions.length; i++) {
            cursor.seek(positions[i]);
            assertEquals(expectedStarts[i], cursor.offset());
        }
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.seek(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.seek(7));
    }

    @Test
    void emptyCursorAndEndSentinel() {
        M3TokenCursor empty = M3TokenCursor.lanes(new int[][] {{}, {}});
        empty.seek(0);
        assertEquals(0, empty.offset());
        M3TokenCursor single = M3TokenCursor.of(new int[] {17, 19});
        single.seek(2);
        assertEquals(0, single.offset());
    }
}
