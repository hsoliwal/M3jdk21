// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Boundary monoid for CAMEL and PASCAL. A boundary at position j is a function of the unit types at
 * j-1, j, j+1 only. A summary decides positions {@code 1..len-2} (whole window inside) and keeps the first
 * two and last two unit types; {@link #combine} decides the two seam positions whose window becomes
 * complete, {@link #finish} decides the last position with an end sentinel. Each position is decided once
 * by the same function of the global unit sequence, so combine is associative and every atom split of a
 * String gives the same bits (REGEX_PRECOMPUTE_DESIGN.md §2d seam rule, window 3).
 */
public final class M3WindowBoundarySummary {
    private static final int NONE = -1;

    private final M3SeparatorKind kind;
    private final int length;
    private final int[] head;
    private final int[] tail;
    private final long[] bits;

    private M3WindowBoundarySummary(M3SeparatorKind kind, int length, int[] head, int[] tail, long[] bits) {
        this.kind = kind;
        this.length = length;
        this.head = head;
        this.tail = tail;
        this.bits = bits;
    }

    /** Summarizes one atom; types are {@code Character.getType(char)} per UTF-16 unit. */
    public static M3WindowBoundarySummary of(M3SeparatorKind kind, CharSequence atom) {
        if (!kind.boundaryBefore()) {
            throw new IllegalArgumentException("window summary needs a BOUNDARY_BEFORE kind");
        }
        int n = atom.length();
        int[] types = new int[n];
        for (int index = 0; index < n; index++) {
            types[index] = Character.getType(atom.charAt(index));
        }
        long[] bits = M3BitLanes.lanes(n);
        for (int j = 1; j <= n - 2; j++) {
            if (boundary(kind, types[j - 1], types[j], types[j + 1])) {
                M3BitLanes.set(bits, j);
            }
        }
        return new M3WindowBoundarySummary(kind, n, edge(types, 0), edge(types, Math.max(0, n - 2)), bits);
    }

    /** Monoid product: {@code L ++ R}. */
    public M3WindowBoundarySummary combine(M3WindowBoundarySummary right) {
        if (right.kind != kind) {
            throw new IllegalArgumentException("separator kind mismatch");
        }
        if (length == 0 || right.length == 0) {
            return length == 0 ? right : this;
        }
        int a = length;
        int n = a + right.length;
        long[] merged = M3BitLanes.copyOf(bits, n);
        M3BitLanes.orShifted(merged, right.bits, right.length, a);
        for (int j = a - 1; j <= a; j++) {
            if (j >= 1 && j <= n - 2 && boundary(kind, seamType(right, j - 1), seamType(right, j),
                    seamType(right, j + 1))) {
                M3BitLanes.set(merged, j);
            }
        }
        return new M3WindowBoundarySummary(kind, n, joinHead(right), joinTail(right), merged);
    }

    /** Root bits: decides the last position (no right neighbour); position 0 is never a boundary. */
    public long[] finish() {
        long[] root = M3BitLanes.copyOf(bits, length);
        if (length >= 2 && boundary(kind, tail[tail.length - 2], tail[tail.length - 1], NONE)) {
            M3BitLanes.set(root, length - 1);
        }
        return root;
    }

    /** @return number of UTF-16 units summarized */
    public int length() {
        return length;
    }

    static boolean boundary(M3SeparatorKind kind, int before, int at, int after) {
        if (kind == M3SeparatorKind.CAMEL) {
            return at == Character.UPPERCASE_LETTER
                    && (before == Character.LOWERCASE_LETTER || before == Character.DECIMAL_DIGIT_NUMBER);
        }
        boolean change = at != before
                && !(at == Character.LOWERCASE_LETTER && before == Character.UPPERCASE_LETTER);
        boolean acronymTail = after == Character.LOWERCASE_LETTER && at == Character.UPPERCASE_LETTER
                && before == Character.UPPERCASE_LETTER;
        return change || acronymTail;
    }

    private int seamType(M3WindowBoundarySummary right, int position) {
        if (position < length) {
            return tail[position - (length - tail.length)];
        }
        return right.head[position - length];
    }

    private int[] joinHead(M3WindowBoundarySummary right) {
        if (head.length >= 2) {
            return head;
        }
        return new int[] {head[0], right.head[0]};
    }

    private int[] joinTail(M3WindowBoundarySummary right) {
        if (right.tail.length >= 2) {
            return right.tail;
        }
        return new int[] {tail[tail.length - 1], right.tail[0]};
    }

    private static int[] edge(int[] types, int from) {
        int[] edge = new int[Math.min(2, types.length - from)];
        System.arraycopy(types, from, edge, 0, edge.length);
        return edge;
    }
}
