// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/** Half-open immutable coordinate range over a frozen lexical ordering. */
public record LexicalRange(int startInclusive, int endExclusive) {
    public LexicalRange {
        if (startInclusive < 0 || endExclusive < startInclusive) {
            throw new IllegalArgumentException("invalid lexical range");
        }
    }

    public int size() { return endExclusive - startInclusive; }
    public boolean isEmpty() { return startInclusive == endExclusive; }
    public boolean containsCoordinate(int coordinate) {
        return coordinate >= startInclusive && coordinate < endExclusive;
    }
}
