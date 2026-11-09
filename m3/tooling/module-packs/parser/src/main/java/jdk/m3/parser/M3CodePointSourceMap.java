// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable Unicode scalar/code-point indexes over ONE original M3File.
 * Sparse primitive checkpoints every 128 scalars keep lookup bounded without
 * retaining a second source String, byte array, parser tree or node wrapper.
 * Unpaired UTF-16 surrogate code units refuse rather than silently round-trip.
 * This is a coordinate projection, not a seal of the external parser input.
 */
public final class M3CodePointSourceMap {
    private static final int STRIDE = 128;
    private final M3File source;
    private final int count;
    private final int[] codePointAnchors;
    private final int[] utf16Anchors;

    private M3CodePointSourceMap(M3File source, int count,
                                 int[] codePointAnchors, int[] utf16Anchors) {
        this.source = source;
        this.count = count;
        this.codePointAnchors = codePointAnchors;
        this.utf16Anchors = utf16Anchors;
    }

    /**
     * Explicit source-unit and Unicode-scalar budgets. Refuse malformed source
     * UTF-16 before publishing any coordinate map. Source is borrowed by
     * immutable identity; construction never copies its text.
     */
    public static M3CodePointSourceMap of(M3File source, int maxUtf16Units,
                                           int maxCodePoints) {
        Objects.requireNonNull(source, "original M3File");
        if (maxUtf16Units < 0 || maxCodePoints < 0 || source.length() > maxUtf16Units) {
            throw new IllegalArgumentException("codepoint source admission budget exceeded");
        }
        int capacity = Math.max(2, source.length() / STRIDE + 2);
        int[] points = new int[capacity], chars = new int[capacity];
        int checkpoints = 1, cp = 0, units = 0;
        while (units < source.length()) {
            if (cp >= maxCodePoints) {
                throw new IllegalArgumentException("Unicode scalar budget exceeded");
            }
            if (cp != 0 && cp % STRIDE == 0) {
                if (checkpoints == points.length) {
                    points = Arrays.copyOf(points, points.length + (points.length >>> 1) + 1);
                    chars = Arrays.copyOf(chars, points.length);
                }
                points[checkpoints] = cp;
                chars[checkpoints++] = units;
            }
            units += scalarWidth(source, units);
            cp++;
        }
        if (points[checkpoints - 1] != cp) {
            if (checkpoints == points.length) {
                points = Arrays.copyOf(points, points.length + 1);
                chars = Arrays.copyOf(chars, chars.length + 1);
            }
            points[checkpoints] = cp;
            chars[checkpoints++] = units;
        }
        return new M3CodePointSourceMap(source, cp,
                Arrays.copyOf(points, checkpoints), Arrays.copyOf(chars, checkpoints));
    }

    public M3File source() { return source; }
    public int codePointCount() { return count; }
    public int utf16Length() { return source.length(); }
    public int checkpointCount() { return codePointAnchors.length; }
    public long retainedPrimitiveBytes() { return (long) checkpointCount() * 2 * Integer.BYTES; }

    /** UTF-16 half-open boundary for an exact code-point/scalar index. */
    public int utf16Offset(int codePointOffset) {
        if (codePointOffset < 0 || codePointOffset > count)
            throw new IndexOutOfBoundsException("Unicode codepoint offset");
        int index = atOrBefore(codePointAnchors, codePointOffset);
        int point = codePointAnchors[index], offset = utf16Anchors[index];
        while (point < codePointOffset) {
            offset += scalarWidth(source, offset);
            point++;
        }
        return offset;
    }

    /** Exact inverse; refuses UTF-16 offsets within a surrogate pair. */
    public int codePointOffset(int utf16Offset) {
        if (utf16Offset < 0 || utf16Offset > source.length())
            throw new IndexOutOfBoundsException("UTF-16 source offset");
        int index = atOrBefore(utf16Anchors, utf16Offset);
        int point = codePointAnchors[index], offset = utf16Anchors[index];
        while (offset < utf16Offset) {
            offset += scalarWidth(source, offset);
            if (offset > utf16Offset)
                throw new IllegalArgumentException("UTF-16 offset splits a Unicode scalar");
            point++;
        }
        return point;
    }

    private static int atOrBefore(int[] values, int value) {
        int index = Arrays.binarySearch(values, value);
        return index >= 0 ? index : -index - 2;
    }

    private static int scalarWidth(M3File source, int at) {
        char first = source.charAt(at);
        if (Character.isHighSurrogate(first)) {
            if (at + 1 >= source.length() || !Character.isLowSurrogate(source.charAt(at + 1)))
                throw new IllegalArgumentException("unpaired leading UTF-16 surrogate");
            return 2;
        }
        if (Character.isLowSurrogate(first))
            throw new IllegalArgumentException("unpaired trailing UTF-16 surrogate");
        return 1;
    }
}
