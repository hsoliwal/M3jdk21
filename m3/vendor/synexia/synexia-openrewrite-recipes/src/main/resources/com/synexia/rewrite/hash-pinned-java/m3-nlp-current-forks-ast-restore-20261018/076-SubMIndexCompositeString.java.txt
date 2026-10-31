// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Immutable UTF-16 range over one {@link MIndexCompositeString} without joining its segments.
 *
 * <p>Offsets are absolute in the source composite. Every legal UTF-16 boundary is accepted,
 * including one between the halves of a surrogate pair: regex capture coordinates are UTF-16
 * offsets and must remain representable. Unlike the legacy
 * {@link MIndexCompositeString#subSequence(int, int)}, creating a view does not admit another
 * pooled spelling. Only an explicit {@link #toString()} creates a Java String for the range.</p>
 */
public final class SubMIndexCompositeString implements CharSequence {
    private final MIndexCompositeString source;
    private final int beginIndex;
    private final int endIndex;

    SubMIndexCompositeString(MIndexCompositeString source, int beginIndex, int endIndex) {
        this.source = Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(beginIndex, endIndex, source.length());
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public MIndexCompositeString source() { return source; }
    public int beginIndex() { return beginIndex; }
    public int endIndex() { return endIndex; }

    @Override public int length() { return endIndex - beginIndex; }

    @Override
    public char charAt(int index) {
        return source.charAt(beginIndex + Objects.checkIndex(index, length()));
    }

    @Override
    public SubMIndexCompositeString subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length());
        return new SubMIndexCompositeString(source, beginIndex + start, beginIndex + end);
    }

    /** The explicit Java String boundary copies only the selected UTF-16 range. */
    @Override
    public String toString() {
        char[] characters = new char[length()];
        for (int index = 0; index < characters.length; index++) {
            characters[index] = charAt(index);
        }
        return new String(characters);
    }
}
