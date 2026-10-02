/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.NoSuchElementException;
import java.util.Objects;

/** One initial directory lookup, then O(N + S) forward traversal without flattening. */
final class M3PieceCursor {
    private final JoinedM3StringPiece joined;
    private final int end;
    private LocalM3StringPiece leaf;
    private int leafNumber;
    private int origin;
    private int index;
    private int leafEnd;

    M3PieceCursor(M3StringPiece piece, int start, int end) {
        Objects.checkFromToIndex(start, end, Objects.requireNonNull(piece).length());
        this.end = end;
        this.index = start;
        joined = piece instanceof JoinedM3StringPiece directory ? directory : null;
        if (start == end) return;
        if (joined == null) {
            leaf = (LocalM3StringPiece) piece;
        } else {
            leafNumber = joined.leafIndex(start);
            leaf = joined.leafAt(leafNumber);
            origin = joined.leafOrigin(leafNumber);
        }
        leafEnd = origin + leaf.length();
    }

    boolean hasNext() { return index < end; }

    char next() {
        if (!hasNext()) throw new NoSuchElementException();
        if (index == leafEnd) {
            origin = leafEnd;
            leaf = joined.leafAt(++leafNumber);
            leafEnd = origin + leaf.length();
        }
        return leaf.charAt(index++ - origin);
    }
}
