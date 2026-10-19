/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable, long-addressed rope index over existing M3StringPiece owners.
 *
 * <p>Each leaf is an already immutable M3 piece, including a range or joined piece.
 * Only reference directories and primitive node/metric lanes are stored here:
 * no characters are copied, no per-node entries are allocated, and no second
 * canonical text store is introduced. The physical node fanout is sixteen.
 * This is a separately built M3 proof owner; it is not java.lang.String.</p>
 *
 * <p>All positions are exact UTF-16 code-unit offsets (not Unicode scalar or
 * grapheme indexes). In particular, a surrogate pair may cross a leaf boundary.
 * The total length can exceed {@link Integer#MAX_VALUE}. Java regex works on
 * bounded CharSequence windows; matching across a window boundary is explicitly
 * not supported by that API.</p>
 */
public final class M3RopeTextIndex {
    private static final int FANOUT = 16;
    private static final int LEAF_MASK = 1;
    private static final int BRANCH_MASK = 2;
    private static final int MAX_LITERAL_UNITS = 1 << 20;
    private static final long MAX_INDEX_BYTES = 64L << 20;

    private final M3StringPiece[] leaves;
    private final int[] firstChild;
    private final int[] childCount;
    private final int[] stateMask;
    private final long[] subtreeUtf16;
    private final int root;
    private final long utf16Count;

    private M3RopeTextIndex(M3StringPiece[] inputs) {
        Objects.requireNonNull(inputs, "inputs");
        M3StringPiece[] captured = new M3StringPiece[inputs.length];
        int leafCount = 0;
        for (M3StringPiece input : inputs) {
            Objects.requireNonNull(input, "piece");
            if (input.length() > 0) captured[leafCount++] = input;
        }
        int count = leafCount;
        int level = leafCount;
        while (level > 1) {
            level = (level - 1) / FANOUT + 1; // no addition overflow
            count = Math.addExact(count, level);
        }
        long bytes = Math.addExact(Math.multiplyExact((long) leafCount, 8L),
                Math.multiplyExact((long) count, 20L));
        if (bytes > MAX_INDEX_BYTES) {
            throw new IllegalArgumentException("rope index metadata exceeds budget");
        }
        leaves = Arrays.copyOf(captured, leafCount);
        firstChild = new int[count];
        childCount = new int[count];
        stateMask = new int[count];
        subtreeUtf16 = new long[count];
        for (int i = 0; i < leafCount; i++) {
            stateMask[i] = LEAF_MASK;
            subtreeUtf16[i] = leaves[i].length();
        }
        int levelBegin = 0;
        int levelCount = leafCount;
        int next = leafCount;
        while (levelCount > 1) {
            int produced = 0;
            int end = levelBegin + levelCount;
            for (int first = levelBegin; first < end;) {
                int countHere = Math.min(FANOUT, end - first);
                long units = 0;
                for (int i = 0; i < countHere; i++) {
                    units = Math.addExact(units, subtreeUtf16[first + i]);
                }
                firstChild[next] = first;
                childCount[next] = countHere;
                stateMask[next] = BRANCH_MASK;
                subtreeUtf16[next] = units;
                next++;
                produced++;
                first += countHere;
            }
            levelBegin = end;
            levelCount = produced;
        }
        if (next != count) throw new AssertionError("incorrect rope geometry");
        root = count == 0 ? -1 : count - 1;
        utf16Count = root == -1 ? 0 : subtreeUtf16[root];
    }

    /** Snapshot an existing ordered sequence of immutable M3 pieces. */
    public static M3RopeTextIndex of(M3StringPiece... pieces) {
        return new M3RopeTextIndex(pieces);
    }

    public long utf16Length() { return utf16Count; }

    public int segmentCount() { return leaves.length; }

    /** The number of physical metadata nodes, not Java node objects. */
    public int nodeCount() { return firstChild.length; }

    /** Exact UTF-16 code unit lookup over a potentially very large composition. */
    public char charAt(long position) {
        return findChar(position, null);
    }

    private char findChar(long position, Window window) {
        if (position < 0 || position >= utf16Count) {
            throw new IndexOutOfBoundsException("UTF-16 offset out of bounds: " + position);
        }
        if (window != null && position >= window.cachedStart && position < window.cachedEnd) {
            return leaves[window.cachedLeaf].charAt((int) (position - window.cachedStart));
        }
        int node = root;
        long local = position;
        long base = 0;
        while ((stateMask[node] & BRANCH_MASK) != 0) {
            int child = firstChild[node];
            int end = child + childCount[node];
            for (; child < end; child++) {
                long span = subtreeUtf16[child];
                if (local < span) break;
                local -= span;
                base = Math.addExact(base, span);
            }
            if (child == end) throw new AssertionError("corrupt rope prefix geometry");
            node = child;
        }
        if ((stateMask[node] & LEAF_MASK) == 0) {
            throw new AssertionError("invalid rope leaf");
        }
        if (window != null) {
            window.cachedLeaf = node;
            window.cachedStart = base;
            window.cachedEnd = Math.addExact(base, subtreeUtf16[node]);
        }
        return leaves[node].charAt((int) local);
    }

    /**
     * Project a bounded, no-copy UTF-16 view suitable for java.util.regex.Matcher.
     * Its indices (including captures and ^/$ boundaries) are window-relative.
     * A window is a single-threaded execution cursor; request a separate window
     * per matcher. No matching is performed outside the declared window.
     */
    public CharSequence window(long from, long to) {
        checkRange(from, to);
        if (to - from > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Java CharSequence windows require int length");
        }
        return new Window(from, (int) (to - from));
    }

    /** Match within exactly one specified no-copy window. */
    public Matcher matcher(Pattern pattern, long from, long to) {
        return Objects.requireNonNull(pattern, "pattern").matcher(window(from, to));
    }

    /**
     * KMP literal search across all segment boundaries in O(n + m) reads for
     * leaf-backed text (joined-piece leaves retain their own lookup costs).
     * Uses one bounded int[] prefix table, no materialized corpus String.
     */
    public long findLiteral(CharSequence literal, long from) {
        Objects.requireNonNull(literal, "literal");
        if (from < 0 || from > utf16Count) {
            throw new IndexOutOfBoundsException("starting UTF-16 offset: " + from);
        }
        int m = literal.length();
        if (m == 0) return from;
        if (m > MAX_LITERAL_UNITS) {
            throw new IllegalArgumentException("literal exceeds bounded KMP plan");
        }
        if (utf16Count - from < m) return -1;
        int[] failure = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            char current = literal.charAt(i);
            while (j > 0 && current != literal.charAt(j)) j = failure[j - 1];
            if (current == literal.charAt(j)) j++;
            failure[i] = j;
        }
        long base = 0;
        int matched = 0;
        for (M3StringPiece leaf : leaves) {
            long next = base + leaf.length(); // bounded by validated utf16Count
            if (next <= from) {
                base = next;
                continue;
            }
            int start = (int) Math.max(0, from - base);
            for (int i = start; i < leaf.length(); i++) {
                char current = leaf.charAt(i);
                while (matched > 0 && current != literal.charAt(matched)) {
                    matched = failure[matched - 1];
                }
                if (current == literal.charAt(matched)) matched++;
                if (matched == m) return base + i + 1 - m;
            }
            base = next;
        }
        return -1;
    }

    private void checkRange(long from, long to) {
        if (from < 0 || to < from || to > utf16Count) {
            throw new IndexOutOfBoundsException("invalid UTF-16 window: " + from + ".." + to);
        }
    }

    private final class Window implements CharSequence {
        private final long start;
        private final int size;
        private int cachedLeaf = -1;
        private long cachedStart = -1;
        private long cachedEnd = -1;

        private Window(long start, int size) {
            this.start = start;
            this.size = size;
        }

        @Override public int length() { return size; }

        @Override public char charAt(int index) {
            Objects.checkIndex(index, size);
            return findChar(start + index, this);
        }

        @Override public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, size);
            return new Window(start + from, to - from);
        }

        /** Explicit compatibility materialization only. */
        @Override public String toString() {
            char[] copied = new char[size];
            for (int i = 0; i < size; i++) copied[i] = charAt(i);
            return new String(copied);
        }
    }
}
