// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Chunk-walk contract of every cursor: order, boundaries, seek, end behaviour, facts. */
class M3ChunkCursorTest {
    @Test
    void randomRopesAndCutPlansWalkExactlyTheText() {
        Random random = new Random(7);
        for (int round = 0; round < 3000; round++) {
            String text = M3RegexCursorFixtures.input(random, 40, true);
            M3ChunkCursor cursor = M3RegexCursorFixtures.cursor(random, text);
            assertEquals(text.length(), cursor.totalLength());
            assertEquals(text, forward(cursor));
            assertEquals(text, backward(cursor));
            for (int probe = 0; probe < 8 && !text.isEmpty(); probe++) {
                int position = random.nextInt(text.length() + 1);
                cursor.seek(position);
                int begin = cursor.offset();
                assertTrue(begin <= position && (position < begin + cursor.chunkLength() || position == text.length()));
                if (position < text.length()) assertEquals(text.charAt(position), cursor.unitAt(position));
            }
        }
    }

    private static String forward(M3ChunkCursor cursor) {
        StringBuilder out = new StringBuilder();
        cursor.seek(0);
        do {
            assertEquals(out.length(), cursor.offset());
            assertTrue(cursor.chunkLength() > 0 || cursor.totalLength() == 0);
            for (int index = 0; index < cursor.chunkLength(); index++) out.append((char) cursor.unit(index));
        } while (cursor.advance());
        int offset = cursor.offset();
        assertFalse(cursor.advance());
        assertEquals(offset, cursor.offset());
        return out.toString();
    }

    private static String backward(M3ChunkCursor cursor) {
        StringBuilder out = new StringBuilder();
        cursor.seek(cursor.totalLength());
        do {
            StringBuilder chunk = new StringBuilder();
            for (int index = 0; index < cursor.chunkLength(); index++) chunk.append((char) cursor.unit(index));
            out.insert(0, chunk);
        } while (cursor.backtrack());
        assertEquals(0, cursor.offset());
        assertFalse(cursor.backtrack());
        return out.toString();
    }

    @Test
    void emptySourcesHaveOneEmptyChunk() {
        for (M3ChunkCursor cursor : new M3ChunkCursor[] {
            new M3StringCursor(M3RopeString.atom("")), M3CharSequenceCursor.whole("")}) {
            assertEquals(0, cursor.totalLength());
            assertEquals(0, cursor.chunkLength());
            assertFalse(cursor.advance());
            assertFalse(cursor.backtrack());
            cursor.seek(0);
            assertThrows(IndexOutOfBoundsException.class, () -> cursor.seek(1));
            assertThrows(IndexOutOfBoundsException.class, () -> cursor.seek(-1));
        }
    }

    @Test
    void cutPlansAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> M3CharSequenceCursor.chunked("abc", new int[] {0}));
        assertThrows(IllegalArgumentException.class, () -> M3CharSequenceCursor.chunked("abc", new int[] {3}));
        assertThrows(IllegalArgumentException.class, () -> M3CharSequenceCursor.chunked("abc", new int[] {2, 1}));
        M3CharSequenceCursor cursor = M3CharSequenceCursor.chunked("abc", new int[] {1, 2});
        assertEquals(1, cursor.chunkLength());
        assertTrue(cursor.advance());
        assertEquals('b', cursor.unit(0));
    }

    @Test
    void ropeModelKeepsCoordinatesAndFacts() {
        M3RopeString latin = M3RopeString.atom("abc");
        M3RopeString wide = M3RopeString.atom("Āxy");
        assertEquals(1, M3RopeAtom.of("abc").storageWidth());
        assertEquals(2, M3RopeAtom.of("Ā").storageWidth());
        M3RopeString joined = latin.concat(wide);
        assertEquals(6, joined.length());
        assertEquals(1, joined.height());
        assertEquals(0, latin.height());
        assertTrue(joined.concat(M3RopeString.atom("")) == joined);
        assertTrue(M3RopeString.atom("").concat(joined) == joined);
        assertTrue(joined.slice(0, 6) == joined);
        assertEquals(0, joined.slice(2, 2).length());
        assertThrows(IndexOutOfBoundsException.class, () -> joined.slice(4, 7));
        M3StringCursor whole = new M3StringCursor(joined);
        long expected = 0;
        for (char c : "abcĀxy".toCharArray()) expected |= M3CursorFacts.bit(c);
        assertEquals(expected, whole.unitPresenceMask());
        assertEquals(M3CursorFacts.UNKNOWN, new M3StringCursor(joined.slice(1, 5)).unitPresenceMask());
        assertEquals("cĀx", new M3CursorCharSequence(new M3StringCursor(joined.slice(2, 5))).toString());
    }

    @Test
    void charSequenceViewSeeksWithoutCopying() {
        M3CursorProbe.reset();
        M3ChunkCursor cursor = M3CharSequenceCursor.chunked("hello world", new int[] {1, 4, 5, 9});
        M3CursorCharSequence view = new M3CursorCharSequence(cursor);
        assertEquals(11, view.length());
        assertEquals('w', view.charAt(6));
        assertEquals('h', view.charAt(0));
        CharSequence sub = view.subSequence(2, 7);
        assertEquals(5, sub.length());
        assertEquals('o', sub.charAt(2));
        assertEquals(0, M3CursorProbe.materializedUnits());
        assertEquals("llo w", sub.toString());
        assertEquals(5, M3CursorProbe.materializedUnits());
        assertThrows(IndexOutOfBoundsException.class, () -> view.charAt(11));
        assertThrows(IndexOutOfBoundsException.class, () -> view.subSequence(3, 2));
        M3CursorProbe.reset();
        assertEquals(0, M3CursorProbe.materializedUnits());
    }
}
