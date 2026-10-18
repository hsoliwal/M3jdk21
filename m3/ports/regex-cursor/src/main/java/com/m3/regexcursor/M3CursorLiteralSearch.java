// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Literal search over a chunk cursor, needles spanning any number of chunk boundaries included.
 *
 * <p>The donor ({@code regex-cursor src/literal.rs}) carries a {@code needle-1} byte buffer across
 * chunk boundaries. Here the carried state is the KMP prefix length, so nothing is copied: a match
 * that starts in one chunk and ends several chunks later is found by the same automaton step.
 * While the state is 0 the inner loop only looks for the needle's first unit inside the chunk
 * (the memchr analogue). {@link #findLast} runs the reversed needle with {@code backtrack()}.</p>
 *
 * <p>Lanes: {@code needle}, {@code failure}, {@code reversed}, {@code reversedFailure} are
 * {@code int[]} built once; searches allocate nothing.</p>
 */
public final class M3CursorLiteralSearch {
    private final int[] needle;
    private final int[] failure;
    private final int[] reversed;
    private final int[] reversedFailure;

    /** Searcher for a needle of symbols (copied). */
    public M3CursorLiteralSearch(int[] needle) {
        this.needle = needle.clone();
        this.failure = failure(this.needle);
        this.reversed = reverse(this.needle);
        this.reversedFailure = failure(this.reversed);
    }

    /** Searcher for the UTF-16 units of {@code text}. */
    public static M3CursorLiteralSearch of(CharSequence text) {
        return new M3CursorLiteralSearch(text.chars().toArray());
    }

    /** Needle length. */
    public int length() {
        return needle.length;
    }

    /** Needle symbol at {@code index}. */
    public int unit(int index) {
        return needle[index];
    }

    /**
     * First start {@code s >= from} with the needle inside {@code [from, to)}, else {@code -1}.
     */
    public int find(M3ChunkCursor cursor, int from, int to) {
        Objects.checkFromToIndex(from, to, cursor.totalLength());
        if (needle.length == 0) return from;
        if (to - from < needle.length) return -1;
        cursor.seek(from);
        return scanForward(cursor, from, to);
    }

    private int scanForward(M3ChunkCursor cursor, int from, int to) {
        int n = needle.length;
        int first = needle[0];
        int state = 0;
        int index = from - cursor.offset();
        while (true) {
            int base = cursor.offset();
            int limit = Math.min(cursor.chunkLength(), to - base);
            for (; index < limit; index++) {
                int unit = cursor.unit(index);
                if (state == 0 && unit != first) continue;
                state = step(needle, failure, state, unit);
                if (state == n) return base + index + 1 - n;
            }
            if (base + limit >= to || !cursor.advance()) return -1;
            index = 0;
        }
    }

    /**
     * Last start {@code s} with the needle inside {@code [from, to)}, else {@code -1}.
     */
    public int findLast(M3ChunkCursor cursor, int from, int to) {
        Objects.checkFromToIndex(from, to, cursor.totalLength());
        if (needle.length == 0) return to;
        if (to - from < needle.length) return -1;
        cursor.seek(to - 1);
        return scanBackward(cursor, from, to);
    }

    private int scanBackward(M3ChunkCursor cursor, int from, int to) {
        int n = reversed.length;
        int first = reversed[0];
        int state = 0;
        int index = to - 1 - cursor.offset();
        while (true) {
            int base = cursor.offset();
            int floor = Math.max(0, from - base);
            for (; index >= floor; index--) {
                int unit = cursor.unit(index);
                if (state == 0 && unit != first) continue;
                state = step(reversed, reversedFailure, state, unit);
                if (state == n) return base + index;
            }
            if (base <= from || !cursor.backtrack()) return -1;
            index = cursor.chunkLength() - 1;
        }
    }

    private static int step(int[] pattern, int[] fail, int state, int unit) {
        int current = state;
        while (current > 0 && pattern[current] != unit) {
            current = fail[current - 1];
        }
        return pattern[current] == unit ? current + 1 : 0;
    }

    private static int[] failure(int[] pattern) {
        int[] fail = new int[pattern.length];
        int k = 0;
        for (int index = 1; index < pattern.length; index++) {
            while (k > 0 && pattern[index] != pattern[k]) {
                k = fail[k - 1];
            }
            if (pattern[index] == pattern[k]) k++;
            fail[index] = k;
        }
        return fail;
    }

    private static int[] reverse(int[] pattern) {
        int[] out = new int[pattern.length];
        for (int index = 0; index < pattern.length; index++) {
            out[index] = pattern[pattern.length - 1 - index];
        }
        return out;
    }

    @Override
    public String toString() {
        return "M3CursorLiteralSearch" + Arrays.toString(needle);
    }
}
