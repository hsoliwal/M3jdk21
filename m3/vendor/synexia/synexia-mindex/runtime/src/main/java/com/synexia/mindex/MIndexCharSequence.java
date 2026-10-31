// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable logical UTF-16 sequence backed by already-interned {@link MIndexString} segments.
 *
 * <p>Construction and slicing retain only immutable segment references, cumulative UTF-16 ends,
 * and range coordinates. No character array or Java String is materialized until an explicit
 * escape such as {@link #copy()} or {@link #toString()} is requested.</p>
 *
 * <p>Charset encoding treats all segments as one logical character sequence. In particular, a
 * high surrogate at the end of one interned segment and a low surrogate at the start of the next
 * are presented to the JDK encoder as one pair rather than as two independently encoded values.</p>
 */
public final class MIndexCharSequence implements CharSequence {
    private final MIndexString[] segments;
    private final int[] segmentStarts;
    private final int[] ends;
    private final int start;
    private final int length;

    private MIndexCharSequence(
            MIndexString[] segments, int[] segmentStarts, int[] ends, int start, int length) {
        this.segments = segments;
        this.segmentStarts = segmentStarts;
        this.ends = ends;
        this.start = start;
        this.length = length;
    }

    /** Join interned immutable values without copying their UTF-16 contents. */
    public static MIndexCharSequence of(MIndexString... values) {
        Objects.requireNonNull(values, "values");
        MIndexString[] retained = new MIndexString[values.length];
        int[] starts = new int[values.length];
        int[] boundaries = new int[values.length];
        int count = 0;
        int total = 0;
        for (MIndexString value : values) {
            MIndexString checked = Objects.requireNonNull(value, "value");
            if (checked.length() == 0) continue;
            total = Math.addExact(total, checked.length());
            retained[count] = checked;
            boundaries[count++] = total;
        }
        return new MIndexCharSequence(
                Arrays.copyOf(retained, count),
                Arrays.copyOf(starts, count),
                Arrays.copyOf(boundaries, count),
                0,
                total);
    }

    /** Join the canonical interned segments already retained by a composite value. */
    public static MIndexCharSequence of(MIndexCompositeString value) {
        MIndexCompositeString checked = Objects.requireNonNull(value, "value");
        int count = checked.segmentCount();
        MIndexString[] parts = new MIndexString[count];
        int[] starts = new int[count];
        int[] boundaries = new int[count];
        int total = 0;
        for (int index = 0; index < count; index++) {
            parts[index] = checked.parentSegment(index);
            starts[index] = checked.segmentStart(index);
            total =
                    Math.addExact(
                            total, checked.segmentEnd(index) - checked.segmentStart(index));
            boundaries[index] = total;
        }
        return new MIndexCharSequence(parts, starts, boundaries, 0, total);
    }

    @Override
    public int length() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public int segmentCount() {
        return segments.length;
    }

    @Override
    public char charAt(int index) {
        int absolute = Math.addExact(start, Objects.checkIndex(index, length));
        int segment = segmentAt(absolute);
        int previous = segment == 0 ? 0 : ends[segment - 1];
        return segments[segment].charAt(segmentStarts[segment] + absolute - previous);
    }

    /**
     * Return another zero-copy logical UTF-16 view.
     *
     * <p>Like {@link String#subSequence(int, int)}, boundaries are UTF-16 code-unit coordinates and
     * may split a surrogate pair. The exact units remain available to subsequent whole-view
     * operations.</p>
     */
    @Override
    public MIndexCharSequence subSequence(int from, int to) {
        Objects.checkFromToIndex(from, to, length);
        return from == 0 && to == length
                ? this
                : new MIndexCharSequence(
                        segments, segmentStarts, ends, start + from, to - from);
    }

    public int codePointAt(int index) {
        Objects.checkIndex(index, length);
        return Character.codePointAt(this, index);
    }

    public int codePointCount() {
        return Character.codePointCount(this, 0, length);
    }

    /** Return an explicit caller-owned mutable UTF-16 copy. */
    public char[] copy() {
        char[] result = new char[length];
        copyTo(0, result, 0, length);
        return result;
    }

    /** String-compatible name for the explicit mutable-array escape boundary. */
    public char[] toCharArray() {
        return copy();
    }

    /** Copy a logical range into caller-owned storage without exposing pooled representation. */
    public void copyTo(int sourceStart, char[] target, int targetStart, int count) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceStart, count, length);
        Objects.checkFromIndexSize(targetStart, count, target.length);
        if (count == 0) return;
        int absolute = start + sourceStart;
        int segment = segmentAt(absolute);
        int copied = 0;
        while (copied < count) {
            int previous = segment == 0 ? 0 : ends[segment - 1];
            int chunk = Math.min(4096, Math.min(count - copied, ends[segment] - absolute));
            segments[segment].copyChars(
                    segmentStarts[segment] + absolute - previous,
                    target,
                    targetStart + copied,
                    chunk);
            copied += chunk;
            absolute += chunk;
            if (absolute == ends[segment]) segment++;
        }
    }

    /** Read-only character cursor over the same joined logical view. */
    public CharBuffer asReadOnlyBuffer() {
        return CharBuffer.wrap(this).asReadOnlyBuffer();
    }

    /**
     * Encode the complete logical text with Java String-compatible replacement behavior.
     *
     * <p>The output byte array is the explicit materialization boundary. There is no intermediate
     * joined char array or String, and encoder state carries across MIndexString boundaries.</p>
     */
    public byte[] encode(Charset charset) {
        var encoder = Objects.requireNonNull(charset, "charset")
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            var encoded = encoder.encode(CharBuffer.wrap(this));
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        } catch (CharacterCodingException impossibleWithReplacement) {
            throw new IllegalStateException("replacement encoder failed", impossibleWithReplacement);
        }
    }

    /** String-compatible explicit byte-copy boundary using the supplied charset. */
    public byte[] getBytes(Charset charset) {
        return encode(charset);
    }

    /** String-compatible explicit byte-copy boundary using the process default charset. */
    public byte[] getBytes() {
        return encode(Charset.defaultCharset());
    }

    public boolean contentEquals(CharSequence other) {
        Objects.requireNonNull(other, "other");
        if (length != other.length()) return false;
        for (int index = 0; index < length; index++) {
            if (charAt(index) != other.charAt(index)) return false;
        }
        return true;
    }

    /** Exact {@link String#hashCode()} for this logical UTF-16 sequence. */
    @Override
    public int hashCode() {
        int hash = 0;
        for (int index = 0; index < length; index++) {
            hash = 31 * hash + charAt(index);
        }
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexCharSequence value
                && hashCode() == value.hashCode()
                && contentEquals(value);
    }

    @Override
    public String toString() {
        return new String(copy());
    }

    private int segmentAt(int absolute) {
        int result = Arrays.binarySearch(ends, absolute + 1);
        return result >= 0 ? result : -result - 1;
    }
}
