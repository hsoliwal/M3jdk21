/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;

/**
 * Optional UTF-16 line-coordinate view of one immutable parser M3File snapshot.
 *
 * <p>This is NOT a byte-offset map. It neither guesses a character encoding
 * from the String nor certifies equivalence to a separate raw-byte generation.
 * CRLF counts as one line separator, as do lone CR and lone LF. UTF-16 offsets
 * include any unpaired surrogate code units without normalization.
 */
public final class M3FileLineIndex {
    private final M3File source;
    private final int[] starts;

    private M3FileLineIndex(M3File source, int[] starts) {
        this.source = source;
        this.starts = starts;
    }

    /** One source scan; refuses excess lines before unbounded allocation. */
    public static M3FileLineIndex capture(M3File source, int maxLines) {
        Objects.requireNonNull(source, "source");
        if (maxLines < 1) throw new IllegalArgumentException("invalid line budget");
        int[] offsets = new int[Math.min(maxLines, 32)];
        offsets[0] = 0;
        int count = 1;
        for (int i = 0; i < source.length(); i++) {
            char unit = source.charAt(i);
            if (unit != '\r' && unit != '\n') continue;
            if (unit == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
            if (count == maxLines) throw new IllegalArgumentException("line budget exceeded");
            if (count == offsets.length) {
                int grown = (int) Math.min(maxLines,
                        Math.max((long) count + 1, count + (long) (count >>> 1) + 1));
                offsets = Arrays.copyOf(offsets, grown);
            }
            offsets[count++] = i + 1;
        }
        return new M3FileLineIndex(source, Arrays.copyOf(offsets, count));
    }

    public M3File source() { return source; }
    public int lineCount() { return starts.length; }
    public long primitivePayloadBytes() { return (long) starts.length * Integer.BYTES; }
    /** One-based line containing the UTF-16 offset, including EOF. */
    public int lineAtOffset(int offset) {
        if (offset < 0 || offset > source.length())
            throw new IndexOutOfBoundsException("source offset outside snapshot");
        int match = Arrays.binarySearch(starts, offset);
        return match >= 0 ? match + 1 : -match - 1;
    }
    /** Zero-based UTF-16 column; positions inside line separators are retained. */
    public int columnAtOffset(int offset) {
        return offset - starts[lineAtOffset(offset) - 1];
    }
    public int lineStart(int oneBasedLine) {
        if (oneBasedLine < 1 || oneBasedLine > starts.length)
            throw new IndexOutOfBoundsException("line outside snapshot");
        return starts[oneBasedLine - 1];
    }
    /**
     * Inverse of line/column for source offsets. Nonterminal lines exclude the
     * next line's starting coordinate to keep every valid offset canonical.
     */
    public int offsetAt(int oneBasedLine, int utf16Column) {
        int start = lineStart(oneBasedLine);
        int limit = oneBasedLine < starts.length ? starts[oneBasedLine] : source.length();
        int width = limit - start;
        if (utf16Column < 0 || utf16Column > width ||
                (oneBasedLine < starts.length && utf16Column == width))
            throw new IndexOutOfBoundsException("column outside line");
        return start + utf16Column;
    }
}
