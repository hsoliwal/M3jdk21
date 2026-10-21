// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Literal search vs {@code String.indexOf/lastIndexOf}, needles spanning chunk boundaries. */
class M3CursorLiteralSearchTest {
    @Test
    void forwardAndReverseMatchStringSearchOverRandomChunks() {
        Random random = new Random(11);
        int compared = 0;
        for (int round = 0; round < 4000; round++) {
            String text = randomText(random, random.nextInt(48));
            String needle = random.nextInt(4) == 0 && text.length() > 2
                    ? text.substring(random.nextInt(text.length() / 2), text.length() / 2 + 1)
                    : randomText(random, random.nextInt(6));
            M3CursorLiteralSearch search = M3CursorLiteralSearch.of(needle);
            M3ChunkCursor cursor = M3RegexCursorFixtures.cursor(random, text);
            for (int probe = 0; probe < 6; probe++) {
                int from = random.nextInt(text.length() + 1);
                int to = from + random.nextInt(text.length() - from + 1);
                assertEquals(expectedFirst(text, needle, from, to), search.find(cursor, from, to), needle + " in " + text);
                assertEquals(expectedLast(text, needle, from, to), search.findLast(cursor, from, to), needle + " in " + text);
                compared += 2;
            }
        }
        assertEquals(48_000, compared);
    }

    private static String randomText(Random random, int length) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) out.append("aabĀ".charAt(random.nextInt(4)));
        return out.toString();
    }

    private static int expectedFirst(String text, String needle, int from, int to) {
        int at = text.substring(0, to).indexOf(needle, from);
        return at;
    }

    private static int expectedLast(String text, String needle, int from, int to) {
        int at = text.substring(0, to).lastIndexOf(needle);
        return at >= from ? at : -1;
    }

    @Test
    void accessorsAndBounds() {
        M3CursorLiteralSearch search = new M3CursorLiteralSearch(new int[] {'a', 'b'});
        assertEquals(2, search.length());
        assertEquals('b', search.unit(1));
        assertEquals("M3CursorLiteralSearch[97, 98]", search.toString());
        M3ChunkCursor cursor = M3CharSequenceCursor.whole("xxab");
        assertThrows(IndexOutOfBoundsException.class, () -> search.find(cursor, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> search.findLast(cursor, 0, 5));
        assertEquals(2, search.find(cursor, 0, 4));
        assertEquals(-1, search.find(cursor, 0, 3));
        assertEquals(4, M3CursorLiteralSearch.of("").findLast(cursor, 1, 4));
        assertEquals(1, M3CursorLiteralSearch.of("").find(cursor, 1, 4));
    }
}
