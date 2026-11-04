/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/** Flat leaf directory avoids recursive traversal or rope-depth amplification. */
final class JoinedM3StringPiece implements M3StringPiece {
    private final LocalM3StringPiece[] leaves;
    private final int[] ends;
    private final int length;

    JoinedM3StringPiece(M3StringPiece[] input) {
        Objects.requireNonNull(input);
        // Snapshot references: callers can mutate their directory after joining.
        M3StringPiece[] pieces = input.clone();
        int count = 0;
        for (M3StringPiece piece : pieces) {
            Objects.requireNonNull(piece);
            count = Math.addExact(count, piece instanceof JoinedM3StringPiece joined ? joined.leaves.length : piece.length() == 0 ? 0 : 1);
        }
        leaves = new LocalM3StringPiece[count];
        ends = new int[count];
        int next = 0, units = 0;
        for (M3StringPiece piece : pieces) {
            if (piece instanceof JoinedM3StringPiece joined) {
                for (LocalM3StringPiece leaf : joined.leaves) {
                    leaves[next] = leaf;
                    ends[next++] = units = Math.addExact(units, leaf.length());
                }
            } else if (piece.length() != 0) {
                LocalM3StringPiece leaf = (LocalM3StringPiece)piece;
                leaves[next] = leaf;
                ends[next++] = units = Math.addExact(units, leaf.length());
            }
        }
        length = units;
    }
    @Override public int length() { return length; }
    @Override public char charAt(int index) {
        Objects.checkIndex(index, length);
        int lo = 0, hi = ends.length;
        while (lo < hi) {
            int middle = (lo + hi) >>> 1;
            if (ends[middle] <= index) lo = middle + 1; else hi = middle;
        }
        return leaves[lo].charAt(index - (lo == 0 ? 0 : ends[lo - 1]));
    }
    @Override public M3StringPiece subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        LocalM3StringPiece[] ranges = new LocalM3StringPiece[leaves.length];
        int count = 0;
        for (int i = 0; i < leaves.length; i++) {
            int origin = i == 0 ? 0 : ends[i - 1];
            int a = Math.max(start, origin), b = Math.min(end, ends[i]);
            if (a < b) ranges[count++] = leaves[i].subSequence(a - origin, b - origin);
        }
        return new JoinedM3StringPiece(java.util.Arrays.copyOf(ranges, count));
    }
    @Override public String toString() { return flatten(); }
}
