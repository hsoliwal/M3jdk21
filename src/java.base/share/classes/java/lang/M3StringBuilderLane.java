/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import jdk.internal.util.ArraysSupport;

import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;

//FLLDCIM: R5-BUILDER \u2014 composite lane first cut (pieces + flat chunks; flatten on read/edit)
/**
 * Composite lane of an {@link AbstractStringBuilder}: appended M3 Strings are kept as pieces
 * (references to the caller's immutable String plus a packed range) instead of being copied into
 * the flat buffer, and {@code toString()} composes the pieces and the flat chunks between them into
 * one balanced M3 tuple through {@link M3StringPool#concat} with no flat copy of piece content.
 *
 * <p>While a lane is attached, the builder's {@code value}/{@code count} hold only the flat chunks,
 * in order; piece {@code i} sits logically just before flat offset {@code flatAt[i]}. Flat appends
 * (chars, numbers, short Strings) therefore run the stock code unchanged. Indexed code-unit and
 * code-point readers translate positions through {@code logicalEnd} without copying piece text.
 * Other readers and editors call {@link #flatten}, which materializes the logical content into a
 * buffer of the capacity the stock builder would have had ({@link #virtualCapacity}) and detaches
 * the lane, or read a detached copy ({@link #detached}, {@link #copyOut}) when they must not mutate
 * the builder.</p>
 *
 * <p>DOOP: parallel lanes ({@code text}, {@code span}, {@code flatAt}, {@code logicalEnd}) plus one
 * int of state bits; no per-piece objects. Pattern donor: apple/swift-collections
 * {@code Rope.Builder} (Apache-2.0, Swift, pattern only): undersized items coalesce into the pending
 * chunk, committed items stack, {@code finalize} folds the stack into one rope. Indexed reads use
 * the separate occurrence-coordinate and binary-search concepts reviewed in Apache NetBeans
 * {@code OffsetGapList}/{@code FlyOffsetGapList}; no NetBeans source body is copied.</p>
 */
final class M3StringBuilderLane {
    /** Smallest appended range that becomes a piece; shorter ranges are copied as stock. */
    static final int LIFT_UNITS = 64;
    /** A range must cover at least 1/PIN_RATIO of its source String (pool-bounds rule). */
    static final int PIN_RATIO = 8;

    /** Some piece is UTF16-coded (its content may still be Latin-1 when it is a range). */
    private static final int UTF16_PIECE = 1;

    private String[] text;
    private long[] span;
    private int[] flatAt;
    /** Nondecreasing logical end offsets, including forced empty pieces. */
    private int[] logicalEnd;
    private int pieces;
    /** Sum of piece lengths. */
    int units;
    /** Capacity, in chars, that the stock builder would report for the same call sequence. */
    int virtualCapacity;
    private int state;

    private M3String composed;
    private int composedFlat;
    private int composedPieces;

    M3StringBuilderLane(int flatCapacity) {
        this.virtualCapacity = flatCapacity;
        this.text = new String[4];
        this.span = new long[4];
        this.flatAt = new int[4];
        this.logicalEnd = new int[4];
    }

    /** Lift predicate: only admitted M3 Strings, above the threshold, without pinning a huge owner. */
    static boolean liftable(String s, int start, int end) {
        int length = end - start;
        return String.M3_JOINED_STRINGS
                && length >= LIFT_UNITS
                && (long) length * PIN_RATIO >= s.length()
                && s.m3() != null
                && jdk.internal.misc.VM.isBooted()
                && M3String.admissionEnabled();
    }

    /** Appends {@code s[start, start + length)} as a piece before flat offset {@code flatCount}. */
    void push(String s, int start, int length, int flatCount, byte flatCoder) {
        long end = (long) flatCount + units + length;
        if (end > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        reserveLogical((int) end, flatCoder);
        if (pieces == text.length) {
            grow();
        }
        text[pieces] = s;
        span[pieces] = ((long) start << 32) | (length & 0xffff_ffffL);
        flatAt[pieces] = flatCount;
        logicalEnd[pieces] = (int) end;
        pieces++;
        units += length;
        if (s.coder() == UTF16) {
            state |= UTF16_PIECE;
        }
    }

    /** Publish replacement lanes only after every allocation and copy succeeds. */
    private void grow() {
        int grown = ArraysSupport.newLength(text.length, 1, text.length);
        String[] newText = java.util.Arrays.copyOf(text, grown);
        long[] newSpan = java.util.Arrays.copyOf(span, grown);
        int[] newFlatAt = java.util.Arrays.copyOf(flatAt, grown);
        int[] newLogicalEnd = java.util.Arrays.copyOf(logicalEnd, grown);
        text = newText;
        span = newSpan;
        flatAt = newFlatAt;
        logicalEnd = newLogicalEnd;
    }

    /**
     * Read one already bounds-checked logical UTF-16 unit without changing the lane or flat buffer.
     * A strict upper bound skips empty pieces and selects either the next piece or its flat prefix.
     */
    char charAt(byte[] flat, byte flatCoder, int index) {
        int low = 0;
        int high = pieces;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (index < logicalEnd[mid]) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        if (low == pieces) {
            return flatCharAt(flat, flatCoder, index - units);
        }
        long packed = span[low];
        int start = logicalEnd[low] - (int) packed;
        if (index >= start) {
            return text[low].charAt((int) (packed >>> 32) + (index - start));
        }
        int precedingUnits = start - flatAt[low];
        return flatCharAt(flat, flatCoder, index - precedingUnits);
    }

    private static char flatCharAt(byte[] flat, byte flatCoder, int index) {
        return flatCoder == LATIN1
                ? (char) (flat[index] & 0xff)
                : StringUTF16.getChar(flat, index);
    }

    /** Stock capacity growth for a flat-buffer minimum, applied to the logical length. */
    void reserve(int flatMinimum, byte flatCoder) {
        long logical = (long) flatMinimum + units;
        if (logical > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        reserveLogical((int) logical, flatCoder);
    }

    /** Public {@code ensureCapacity}: the argument is already a logical length. */
    void reserveLogical(int minimumCapacity, byte flatCoder) {
        int oldCapacity = virtualCapacity;
        if (minimumCapacity - oldCapacity > 0) {
            byte coder = logicalCoder(flatCoder);
            int oldLength = oldCapacity << coder;
            int growth = (minimumCapacity << coder) - oldLength;
            int length = ArraysSupport.newLength(oldLength, growth, oldLength + (2 << coder));
            if (length == Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }
            virtualCapacity = length >> coder;
        }
    }

    byte logicalCoder(byte flatCoder) {
        return (state & UTF16_PIECE) != 0 ? UTF16 : flatCoder;
    }

    /** Copies the logical content (flat chunks and pieces, in order) into {@code dst}. */
    void copyOut(byte[] flat, byte flatCoder, int flatCount, byte[] dst, int dstBegin, byte dstCoder) {
        int from = 0;
        int out = dstBegin;
        for (int i = 0; i < pieces; i++) {
            int at = flatAt[i];
            out = copyFlat(flat, flatCoder, from, at, dst, out, dstCoder);
            from = at;
            long packed = span[i];
            int length = (int) packed;
            text[i].getBytes(dst, (int) (packed >>> 32), out, dstCoder, length);
            out += length;
        }
        copyFlat(flat, flatCoder, from, flatCount, dst, out, dstCoder);
    }

    private static int copyFlat(byte[] src, byte srcCoder, int from, int to,
                                byte[] dst, int out, byte dstCoder) {
        int length = to - from;
        if (length != 0) {
            if (srcCoder == dstCoder) {
                System.arraycopy(src, from << srcCoder, dst, out << dstCoder, length << srcCoder);
            } else { // srcCoder == LATIN1 && dstCoder == UTF16
                StringLatin1.inflate(src, from, dst, out, length);
            }
        }
        return out + length;
    }

    /** Exact-length flat copy for readers that must not mutate the builder (streams, peers). */
    byte[] detached(byte[] flat, byte flatCoder, int flatCount, byte logicalCoder) {
        int length = flatCount + units;
        byte[] copy = logicalCoder == LATIN1 ? new byte[length] : StringUTF16.newBytesFor(length);
        copyOut(flat, flatCoder, flatCount, copy, 0, logicalCoder);
        return copy;
    }

    /** Materializes the builder into a stock-shaped flat buffer and detaches this lane. */
    void flatten(AbstractStringBuilder builder) {
        byte coder = logicalCoder(builder.coder);
        int capacity = virtualCapacity;
        byte[] buffer = coder == LATIN1 ? new byte[capacity] : StringUTF16.newBytesFor(capacity);
        int total = builder.count + units;
        copyOut(builder.value, builder.coder, builder.count, buffer, 0, coder);
        builder.value = buffer;
        builder.coder = coder;
        builder.count = total;
        builder.m3Lane = null;
    }

    /**
     * The balanced composition of flat chunks and pieces, or {@code null} when admission is off,
     * a piece is not M3-backed, or the pool refuses a chunk (the caller then takes the flat path).
     */
    M3String compose(byte[] flat, byte flatCoder, int flatCount) {
        M3String cached = composed;
        if (cached != null && composedFlat == flatCount && composedPieces == pieces) {
            return cached;
        }
        if (!jdk.internal.misc.VM.isBooted() || !M3String.admissionEnabled()) {
            return null;
        }
        M3String[] parts = new M3String[(pieces << 1) + 1];
        int n = 0;
        int from = 0;
        for (int i = 0; i < pieces; i++) {
            int at = flatAt[i];
            if (at > from) {
                M3String chunk = M3String.admitCompactBytes(flat, from, at - from, flatCoder);
                if (chunk == null) return null;
                parts[n++] = chunk;
            }
            from = at;
            M3String storage = text[i].m3();
            if (storage == null) return null;
            long packed = span[i];
            int start = (int) (packed >>> 32);
            parts[n++] = storage.slice(start, start + (int) packed);
        }
        if (flatCount > from) {
            M3String chunk = M3String.admitCompactBytes(flat, from, flatCount - from, flatCoder);
            if (chunk == null) return null;
            parts[n++] = chunk;
        }
        // Rope.Builder.finalize pattern: fold adjacent pairs level by level (balanced).
        while (n > 1) {
            int w = 0;
            for (int r = 0; r < n; r += 2) {
                parts[w++] = r + 1 < n ? M3StringPool.concat(parts[r], parts[r + 1]) : parts[r];
            }
            n = w;
        }
        composed = parts[0];
        composedFlat = flatCount;
        composedPieces = pieces;
        return parts[0];
    }

    int pieceCount() {
        return pieces;
    }

    // M3NotYetReceived: piece-table middle edits (insert/delete/replace/setCharAt/reverse) over the
    //   lanes; donor naotsugu/piecetable PieceTableImpl (Apache-2.0, PORT). First cut: flatten.
    // M3NotYetReceived: indexOf/lastIndexOf over pieces through the composite search gates
    //   (S2 G25). First cut: flatten.
    // M3NotYetReceived: eager admission of the pending flat chunk at a chunk threshold.
    // M3NotYetReceived: append(AbstractStringBuilder) transfers the source pieces instead of copying.
}
