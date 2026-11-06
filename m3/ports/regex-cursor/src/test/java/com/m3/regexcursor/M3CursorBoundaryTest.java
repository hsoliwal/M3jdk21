// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Inclusive coordinate and zero-width search state contracts without flattening the input. */
class M3CursorBoundaryTest {
    @Test
    void maximumLengthBorrowedTokenLanesKeepEveryRepresentablePosition() {
        // Share physical arrays: the logical sequence has MAX_VALUE IDs, while
        // the input and cursor retain less than one MiB of array storage.
        int[] full = new int[1 << 16];
        int[] tail = new int[(1 << 16) - 1];
        full[0] = 7;
        full[full.length - 1] = 8;
        tail[tail.length - 1] = 9;
        int[][] lanes = new int[1 << 15][];
        Arrays.fill(lanes, full);
        lanes[lanes.length - 1] = tail;
        M3TokenCursor cursor = M3TokenCursor.lanes(lanes);
        assertEquals(Integer.MAX_VALUE, cursor.totalLength());

        for (int at : new int[] {0, 65535, 65536, Integer.MAX_VALUE - 2,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 65536, 65535, 0}) {
            cursor.seek(at);
            int expectedLane = at == Integer.MAX_VALUE ? lanes.length - 1 : at >>> 16;
            assertEquals(expectedLane << 16, cursor.offset());
            assertEquals(lanes[expectedLane].length, cursor.chunkLength());
            if (at < Integer.MAX_VALUE) assertEquals(lanes[expectedLane][at & 65535], cursor.unitAt(at));
        }
        cursor.seek(Integer.MAX_VALUE);
        assertFalse(cursor.advance());
        assertEquals(9, cursor.unit(cursor.chunkLength() - 1));
        assertTrue(cursor.backtrack());
        assertEquals((lanes.length - 2) << 16, cursor.offset());
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.seek(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> cursor.seek(Integer.MIN_VALUE));
        // The endpoint repair must not change the public borrowed-array contract.
        tail[tail.length - 1] = 10;
        assertEquals(10, cursor.unitAt(Integer.MAX_VALUE - 1));
    }

    @Test
    void terminalEmptyFindStaysExhaustedForEveryEngineUntilReset() {
        for (String expression : new String[] {"", "a*", "$", "(?=)"}) {
            for (String text : new String[] {"", "b", "ab"}) {
                Matcher stock = Pattern.compile(expression).matcher(text);
                M3CursorMatcher matcher = M3CursorRegex.compile(expression)
                        .matcher(M3CharSequenceCursor.whole(text));
                assertEquals(stock.find(text.length()), matcher.find(text.length()), expression);
                assertEquals(stock.start(), matcher.start(), expression);
                assertEquals(stock.end(), matcher.end(), expression);
                long before = matcher.operations(M3CursorMatcher.ENGINE_NONE);
                for (int repeat = 0; repeat < 4; repeat++) {
                    assertFalse(stock.find(), expression);
                    assertFalse(matcher.find(), expression);
                    assertThrows(IllegalStateException.class, matcher::start);
                    assertThrows(IllegalStateException.class, matcher::end);
                    assertEquals(M3CursorMatcher.ENGINE_NONE, matcher.engine());
                }
                assertEquals(before + 4, matcher.operations(M3CursorMatcher.ENGINE_NONE));
                assertTrue(matcher.find(text.length()), "find(int) restarts " + expression);
                assertFalse(matcher.find());
                assertTrue(matcher.reset().find(text.length()), "reset restarts " + expression);
                assertFalse(matcher.find());
                assertTrue(matcher.reset(M3CharSequenceCursor.whole("")).find(), "rebind restarts " + expression);
                assertFalse(matcher.find());
                assertFalse(matcher.find());
            }
        }
    }

    @Test
    void maximumLengthTerminalEmptyFindDoesNotWrapItsPosition() {
        CharSequence text = new CharSequence() {
            @Override public int length() { return Integer.MAX_VALUE; }
            @Override public char charAt(int index) {
                Objects.checkIndex(index, length());
                return 'a';
            }
            @Override public CharSequence subSequence(int from, int to) {
                throw new AssertionError("matcher must not materialize its source");
            }
        };
        for (String expression : new String[] {"", "a*", "$", "(?=)"}) {
            M3CursorMatcher matcher = M3CursorRegex.compile(expression)
                    .matcher(M3CharSequenceCursor.chunked(text, new int[] {Integer.MAX_VALUE - 1}));
            assertTrue(matcher.find(Integer.MAX_VALUE), expression);
            assertEquals(Integer.MAX_VALUE, matcher.start());
            assertEquals(Integer.MAX_VALUE, matcher.end());
            assertFalse(matcher.find(), expression);
            assertFalse(matcher.find(), expression);
            assertEquals(M3CursorMatcher.ENGINE_NONE, matcher.engine());
            assertThrows(IllegalStateException.class, matcher::start);
            assertThrows(IndexOutOfBoundsException.class, () -> matcher.find(-1));
            assertFalse(matcher.find(), "invalid find(int) must not reset exhaustion");
            assertTrue(matcher.find(Integer.MAX_VALUE), "valid find(int) restarts " + expression);
        }
    }

    @Test
    void nonterminalEmptyFindAdvanceAndExplicitMatchingStillFollowStock() {
        for (String expression : new String[] {"", "a*?", "a*", "(?:a|)", "()", "(?=)"}) {
            String text = "ab";
            Matcher stock = Pattern.compile(expression).matcher(text);
            M3CursorMatcher matcher = M3CursorRegex.compile(expression)
                    .matcher(M3CharSequenceCursor.chunked(text, new int[] {1}));
            for (int attempt = 0; attempt < text.length() + 4; attempt++) {
                boolean expected = stock.find();
                assertEquals(expected, matcher.find(), expression + " attempt " + attempt);
                if (expected) {
                    assertEquals(stock.start(), matcher.start());
                    assertEquals(stock.end(), matcher.end());
                }
            }
            assertEquals(stock.lookingAt(), matcher.lookingAt(), expression);
            assertEquals(stock.find(), matcher.find(), expression);
            assertEquals(stock.matches(), matcher.matches(), expression);
            assertEquals(stock.find(), matcher.find(), expression);
        }
    }
}
