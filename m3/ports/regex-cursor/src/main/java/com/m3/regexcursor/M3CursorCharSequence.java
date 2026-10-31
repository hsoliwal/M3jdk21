// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Objects;

/**
 * {@code CharSequence} view over a UTF-16 chunk cursor: {@code charAt} seeks the cursor, nothing
 * is copied. This is how the stock {@code java.util.regex} engine (the oracle and the last-resort
 * engine) reads an M3 string without a {@code char[]} shadow.
 *
 * <p>{@link #toString()} is the only materialization point and is counted by
 * {@link M3CursorProbe#materializedUnits()}.</p>
 */
public final class M3CursorCharSequence implements CharSequence {
    private final M3ChunkCursor cursor;
    private final int start;
    private final int end;

    /** View over the whole cursor. */
    public M3CursorCharSequence(M3ChunkCursor cursor) {
        this(cursor, 0, cursor.totalLength());
    }

    private M3CursorCharSequence(M3ChunkCursor cursor, int start, int end) {
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        this.start = start;
        this.end = end;
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, end - start);
        return (char) cursor.unitAt(start + index);
    }

    @Override
    public CharSequence subSequence(int from, int to) {
        Objects.checkFromToIndex(from, to, end - start);
        return new M3CursorCharSequence(cursor, start + from, start + to);
    }

    @Override
    public String toString() {
        int length = end - start;
        M3CursorProbe.materialized(length);
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            out.append((char) cursor.unitAt(start + index));
        }
        return out.toString();
    }
}
