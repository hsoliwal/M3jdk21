/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
package com.m3.indexstring;

import java.nio.CharBuffer;

/** Reader-owned descriptor cursor: one linear traversal, including surrogate seams. */
final class M3Utf16Cursor {
    private final CharBuffer[] segments;
    private int index;
    M3Utf16Cursor(CharBuffer[] segments) { this.segments = segments; }
    boolean hasNext() {
        while (index < segments.length && !segments[index].hasRemaining()) index++;
        return index < segments.length;
    }
    char peek() {
        if (!hasNext()) throw new java.util.NoSuchElementException();
        return segments[index].get(segments[index].position());
    }
    char next() { char result = peek(); segments[index].get(); return result; }
    int nextCodePoint() {
        char first = next();
        return Character.isHighSurrogate(first) && hasNext() && Character.isLowSurrogate(peek())
            ? Character.toCodePoint(first, next()) : first;
    }
}
