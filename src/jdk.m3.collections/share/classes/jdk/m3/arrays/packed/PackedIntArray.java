// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.arrays.IntWidth;
import jdk.m3.arrays.WorkMonitor;

/**
 * Fixed-length, owned packed storage for byte-aligned integers of every {@link IntWidth} (32 to 64 bits),
 * signed or unsigned; signed 40-, 48- and 56-bit arrays have the typed classes {@link Int40Array},
 * {@link Int48Array} and {@link Int56Array}.
 *
 * <p>Elements use little-endian two's-complement bytes. Operations return values
 * as {@code long}; no per-element object or long array is retained. Input
 * validation completes before mutation. Instances are not thread-safe and
 * multi-byte accesses are not atomic; callers must synchronize concurrent use.
 *
 * <p>PACKED_UNION U9 (2026-11-09): the API of the Synexia common-collections PackedIntArray (IntWidth,
 * signedness, monitored overloads taking a WorkMonitor, staged commits) is part of this owner. Unsigned INT64 values are
 * raw long bit patterns. Mutations from a monitor callback are rejected with IllegalStateException.</p>
 */
public sealed abstract class PackedIntArray permits Int40Array, Int48Array, Int56Array, FixedWidthIntArray {
    private final int bits;
    private final byte[] data;
    private final boolean signed;
    private int callbacks;
    private static final int CHECKPOINT_VALUES = 1024;

    PackedIntArray(int length, int bits) {
        this(length, bits, true);
    }

    PackedIntArray(int length, int bits, boolean signed) {
        int bytes = checkedBytes(bits);
        if (length < 0 || length > Integer.MAX_VALUE / bytes) {
            throw new IllegalArgumentException("length cannot fit packed byte storage: " + length);
        }
        this.bits = bits;
        this.data = new byte[length * bytes];
        this.signed = signed;
    }

    PackedIntArray(byte[] packed, int bits) {
        this(packed, bits, true);
    }

    PackedIntArray(byte[] packed, int bits, boolean signed) {
        Objects.requireNonNull(packed, "packed");
        int bytes = checkedBytes(bits);
        if (packed.length % bytes != 0) {
            throw new IllegalArgumentException("packed length must be divisible by " + bytes);
        }
        this.bits = bits;
        this.data = packed.clone();
        this.signed = signed;
    }

    private static int checkedBytes(int bits) {
        if (bits != 32 && bits != 40 && bits != 48 && bits != 56 && bits != 64) {
            throw new IllegalArgumentException("unsupported width: " + bits);
        }
        return bits / Byte.SIZE;
    }

    /**
     * Returns the fixed element count.
     * @return number of elements
     */
    public final int length() {
        return data.length / bytesPerValue();
    }

    /**
     * Returns the signed width, including its sign bit.
     * @return 40, 48, or 56
     */
    public final int bitsPerValue() {
        return bits;
    }

    /**
     * Returns the exact payload bytes per element.
     * @return 5, 6, or 7
     */
    public final int bytesPerValue() {
        return bits / Byte.SIZE;
    }

    /**
     * Returns the exact backing byte-array length, excluding all object overhead.
     * @return payload bytes
     */
    public final long payloadBytes() {
        return data.length;
    }

    /**
     * Returns the smallest representable value.
     * @return inclusive signed minimum
     */
    public final long minValue() {
        return bits == Long.SIZE ? Long.MIN_VALUE : signed ? -(1L << (bits - 1)) : 0L;
    }

    /**
     * Returns the largest representable value.
     * @return inclusive signed maximum
     */
    public final long maxValue() {
        return bits == Long.SIZE ? Long.MAX_VALUE : signed ? (1L << (bits - 1)) - 1 : (1L << bits) - 1;
    }

    /**
     * Reads and sign-extends an element into a long.
     * @param index zero-based element index
     * @return decoded signed value
     * @throws IndexOutOfBoundsException if the index is outside this array
     */
    public final long get(int index) {
        Objects.checkIndex(index, length());
        int bytes = bytesPerValue();
        return decode(index * bytes, bytes);
    }

    /**
     * Replaces one element after checking its index and numeric range.
     * @param index zero-based element index
     * @param value signed value to store
     * @throws IndexOutOfBoundsException if the index is outside this array
     * @throws IllegalArgumentException if the value does not fit this width
     */
    public final void set(int index, long value) {
        writable();
        Objects.checkIndex(index, length());
        checkValue(value);
        int bytes = bytesPerValue();
        PackedCodec.writeSigned(data, index * bytes, bytes, value);
    }

    /**
     * Fills every element. The value is checked even when this array is empty.
     * @param value signed value to store
     * @throws IllegalArgumentException if the value does not fit this width
     */
    public final void fill(long value) {
        fill(0, length(), value);
    }

    /**
     * Fills an element range after validating the entire request.
     * @param fromIndex inclusive start
     * @param toIndex exclusive end
     * @param value signed value to store, checked even for an empty range
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalArgumentException if the value does not fit this width
     */
    public final void fill(int fromIndex, int toIndex, long value) {
        writable();
        Objects.checkFromToIndex(fromIndex, toIndex, length());
        checkValue(value);
        int bytes = bytesPerValue();
        int start = fromIndex * bytes;
        int end = toIndex * bytes;
        if (value == 0) {
            Arrays.fill(data, start, end, (byte) 0);
            return;
        }
        for (int offset = start; offset < end; offset += bytes) {
            PackedCodec.writeSigned(data, offset, bytes, value);
        }
    }

    /**
     * Encodes a source range after validating all bounds and all source values.
     * The source must remain unchanged during this call.
     * @param index destination element start
     * @param source source long array
     * @param offset source element start
     * @param count number of elements
     * @throws NullPointerException if source is null
     * @throws IndexOutOfBoundsException if either range is invalid
     * @throws IllegalArgumentException if any source value does not fit
     */
    public final void setAll(int index, long[] source, int offset, int count) {
        writable();
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(index, count, length());
        Objects.checkFromIndexSize(offset, count, source.length);
        for (int i = 0; i < count; i++) {
            checkValue(source[offset + i]);
        }
        int bytes = bytesPerValue();
        int byteOffset = index * bytes;
        for (int i = 0; i < count; i++) {
            PackedCodec.writeSigned(data, byteOffset, bytes, source[offset + i]);
            byteOffset += bytes;
        }
    }

    /**
     * Decodes elements into a caller-owned long array after validating all bounds.
     * @param index source element start
     * @param destination destination long array
     * @param offset destination element start
     * @param count number of elements
     * @throws NullPointerException if destination is null
     * @throws IndexOutOfBoundsException if either range is invalid
     */
    public final void getAll(int index, long[] destination, int offset, int count) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(index, count, length());
        Objects.checkFromIndexSize(offset, count, destination.length);
        int bytes = bytesPerValue();
        int byteOffset = index * bytes;
        for (int i = 0; i < count; i++) {
            destination[offset + i] = decode(byteOffset, bytes);
            byteOffset += bytes;
        }
    }

    /**
     * Copies same-width elements, preserving overlapping source data as arraycopy does.
     * Width compatibility is checked even when count is zero.
     * @param sourceIndex source element start
     * @param destination destination packed collection
     * @param destinationIndex destination element start
     * @param count number of elements
     * @throws NullPointerException if destination is null
     * @throws IndexOutOfBoundsException if either range is invalid
     * @throws IllegalArgumentException if the destination width differs
     */
    public final void copyTo(int sourceIndex, PackedIntArray destination,
            int destinationIndex, int count) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(sourceIndex, count, length());
        Objects.checkFromIndexSize(destinationIndex, count, destination.length());
        destination.writable();
        if (bits != destination.bits) {
            throw new IllegalArgumentException("copy requires identical integer widths");
        }
        if (signed != destination.signed) {
            throw new IllegalArgumentException("copy requires identical signedness");
        }
        int bytes = bytesPerValue();
        System.arraycopy(data, sourceIndex * bytes, destination.data,
                destinationIndex * bytes, count * bytes);
    }

    /**
     * Returns an independent copy of the raw little-endian payload, without a header.
     * @return owned byte copy; callers must retain the width separately
     */
    public final byte[] toByteArray() {
        return data.clone();
    }

    /**
     * Materializes all decoded values into an independent long array.
     * @return long array with eight logical payload bytes per element
     */
    public final long[] toLongArray() {
        long[] values = new long[length()];
        getAll(0, values, 0, values.length);
        return values;
    }

    /**
     * Counts values in the inclusive numeric interval without expanding this storage.
     * Numeric endpoints may be outside the represented width. The element window is
     * checked first, then the numeric interval, including for an empty window.
     * @param fromIndex inclusive element start
     * @param toIndex exclusive element end
     * @param lower inclusive signed lower bound
     * @param upper inclusive signed upper bound
     * @return exact matching element count
     * @throws IndexOutOfBoundsException if the element window is invalid
     * @throws IllegalArgumentException if lower exceeds upper
     */
    public final int countInRange(int fromIndex, int toIndex, long lower, long upper) {
        checkQuery(fromIndex, toIndex, lower, upper);
        if (upper < minValue() || lower > maxValue()) return 0;
        if (lower <= minValue() && upper >= maxValue()) return toIndex - fromIndex;
        int bytes = bytesPerValue();
        int end = toIndex * bytes;
        int count = 0;
        for (int offset = fromIndex * bytes; offset < end; offset += bytes) {
            long value = decode(offset, bytes);
            if (value >= lower && value <= upper) count++;
        }
        return count;
    }

    /**
     * Finds the first value in an inclusive numeric interval without materialization.
     * Uses the same validation and interval rules as {@link #countInRange}.
     * @param fromIndex inclusive element start
     * @param toIndex exclusive element end
     * @param lower inclusive signed lower bound
     * @param upper inclusive signed upper bound
     * @return absolute element index of the first match, or -1 if none matches
     * @throws IndexOutOfBoundsException if the element window is invalid
     * @throws IllegalArgumentException if lower exceeds upper
     */
    public final int firstInRange(int fromIndex, int toIndex, long lower, long upper) {
        checkQuery(fromIndex, toIndex, lower, upper);
        if (fromIndex == toIndex || upper < minValue() || lower > maxValue()) return -1;
        if (lower <= minValue() && upper >= maxValue()) return fromIndex;
        int bytes = bytesPerValue();
        int offset = fromIndex * bytes;
        for (int index = fromIndex; index < toIndex; index++, offset += bytes) {
            long value = decode(offset, bytes);
            if (value >= lower && value <= upper) return index;
        }
        return -1;
    }

    // PACKED_UNION U9 (2026-11-09): the Synexia common-collections PackedIntArray API, on this owner's storage.

    /**
     * Returns a zero-filled array of {@code length} elements of {@code width}; signed 40/48/56-bit arrays are the typed
     * {@link Int40Array}, {@link Int48Array} and {@link Int56Array}.
     * @throws IllegalArgumentException if length is negative or its payload overflows int
     */
    public static PackedIntArray of(int length, IntWidth width, boolean signed) {
        Objects.requireNonNull(width, "width");
        if (signed) {
            switch (width) {
                case INT40: return new Int40Array(length);
                case INT48: return new Int48Array(length);
                case INT56: return new Int56Array(length);
                default: break;
            }
        }
        return new FixedWidthIntArray(length, width.bits(), signed);
    }

    /** Returns a zero-filled signed INT48 array (the Synexia "lint" lane). */
    public static PackedIntArray lint(int length) {
        return of(length, IntWidth.INT48, true);
    }

    /** Returns the element width. */
    public final IntWidth width() {
        switch (bits) {
            case 32: return IntWidth.INT32;
            case 40: return IntWidth.INT40;
            case 48: return IntWidth.INT48;
            case 56: return IntWidth.INT56;
            default: return IntWidth.INT64;
        }
    }

    /** Returns whether elements are signed (two's complement); unsigned INT64 elements are raw bit patterns. */
    public final boolean signed() {
        return signed;
    }

    /** Reads one element after a cancellation check of {@code monitor} (may be null). */
    public final long get(int index, WorkMonitor monitor) {
        Objects.checkIndex(index, length());
        checkCanceled(monitor);
        int bytes = bytesPerValue();
        return decode(index * bytes, bytes);
    }

    /** Writes one element after validation and a cancellation check of {@code monitor} (may be null). */
    public final void set(int index, long value, WorkMonitor monitor) {
        writable();
        Objects.checkIndex(index, length());
        checkValue(value);
        checkCanceled(monitor);
        int bytes = bytesPerValue();
        PackedCodec.writeSigned(data, index * bytes, bytes, value);
    }

    /** Fills {@code [from, to)} atomically with respect to validation and cancellation. */
    public final void fill(int from, int to, long value, WorkMonitor monitor) {
        writable();
        Objects.checkFromToIndex(from, to, length());
        checkValue(value);
        checkCanceled(monitor);
        int bytes = bytesPerValue();
        int count = to - from;
        byte[] staged = new byte[count * bytes];
        int pending = 0;
        for (int i = 0; i < count; i++) {
            PackedCodec.writeSigned(staged, i * bytes, bytes, value);
            if (++pending == CHECKPOINT_VALUES) {
                checkpoint(monitor, pending);
                pending = 0;
            }
        }
        finishPreparation(monitor, pending);
        System.arraycopy(staged, 0, data, from * bytes, staged.length);
    }

    /** Copies {@code count} source values in; an invalid later value leaves every destination element unchanged. */
    public final void copyFrom(long[] source, int sourceOffset, int destinationOffset, int count,
            WorkMonitor monitor) {
        writable();
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(sourceOffset, count, source.length);
        Objects.checkFromIndexSize(destinationOffset, count, length());
        checkCanceled(monitor);
        int bytes = bytesPerValue();
        byte[] staged = new byte[count * bytes];
        int pending = 0;
        for (int i = 0; i < count; i++) {
            long value = source[sourceOffset + i];
            checkValue(value);
            PackedCodec.writeSigned(staged, i * bytes, bytes, value);
            if (++pending == CHECKPOINT_VALUES) {
                checkpoint(monitor, pending);
                pending = 0;
            }
        }
        finishPreparation(monitor, pending);
        System.arraycopy(staged, 0, data, destinationOffset * bytes, staged.length);
    }

    /** Decodes {@code count} values out; cancellation leaves the caller-owned destination unchanged. */
    public final void copyTo(int sourceOffset, long[] destination, int destinationOffset, int count,
            WorkMonitor monitor) {
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(sourceOffset, count, length());
        Objects.checkFromIndexSize(destinationOffset, count, destination.length);
        checkCanceled(monitor);
        int bytes = bytesPerValue();
        long[] staged = new long[count];
        int pending = 0;
        for (int i = 0; i < count; i++) {
            staged[i] = decode((sourceOffset + i) * bytes, bytes);
            if (++pending == CHECKPOINT_VALUES) {
                checkpoint(monitor, pending);
                pending = 0;
            }
        }
        finishPreparation(monitor, pending);
        System.arraycopy(staged, 0, destination, destinationOffset, count);
    }

    /** Returns an independent little-endian payload, reporting progress to {@code monitor} (may be null). */
    public final byte[] toByteArray(WorkMonitor monitor) {
        checkCanceled(monitor);
        int bytes = bytesPerValue();
        int length = length();
        byte[] result = new byte[data.length];
        for (int position = 0; position < length;) {
            int count = Math.min(CHECKPOINT_VALUES, length - position);
            int offset = position * bytes;
            System.arraycopy(data, offset, result, offset, count * bytes);
            checkpoint(monitor, count);
            position += count;
        }
        checkCanceled(monitor);
        return result;
    }

    /**
     * Copies a headerless little-endian payload of {@code width}; every byte pattern is a valid value for the chosen
     * signedness.
     * @throws IllegalArgumentException if the payload length is not a multiple of the width
     */
    public static PackedIntArray fromByteArray(byte[] payload, IntWidth width, boolean signed,
            WorkMonitor monitor) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(width, "width");
        if (payload.length % width.bytes() != 0) {
            throw new IllegalArgumentException("payload length is not divisible by the width");
        }
        if (monitor != null) {
            monitor.checkCanceled();
        }
        PackedIntArray result = of(payload.length / width.bytes(), width, signed);
        int bytes = width.bytes();
        int length = result.length();
        for (int position = 0; position < length;) {
            int count = Math.min(CHECKPOINT_VALUES, length - position);
            int offset = position * bytes;
            System.arraycopy(payload, offset, result.data, offset, count * bytes);
            result.checkpoint(monitor, count);
            position += count;
        }
        result.checkCanceled(monitor);
        return result;
    }

    private long decode(int offset, int bytes) {
        return signed ? PackedCodec.readSigned(data, offset, bytes) : PackedCodec.readUnsigned(data, offset, bytes);
    }

    private void writable() {
        if (callbacks != 0) {
            throw new IllegalStateException("mutation from packed-array monitor callback");
        }
    }

    private void checkCanceled(WorkMonitor monitor) {
        if (monitor != null) {
            callbacks++;
            try {
                monitor.checkCanceled();
            } finally {
                callbacks--;
            }
        }
    }

    private void checkpoint(WorkMonitor monitor, int completed) {
        if (monitor != null) {
            callbacks++;
            try {
                monitor.worked(completed);
                monitor.checkCanceled();
            } finally {
                callbacks--;
            }
        }
    }

    private void finishPreparation(WorkMonitor monitor, int pending) {
        if (pending != 0) {
            checkpoint(monitor, pending);
        }
        checkCanceled(monitor);
    }

    private void checkQuery(int fromIndex, int toIndex, long lower, long upper) {
        Objects.checkFromToIndex(fromIndex, toIndex, length());
        if (lower > upper) throw new IllegalArgumentException("lower exceeds upper");
    }

    private void checkValue(long value) {
        if (value < minValue() || value > maxValue()) {
            throw new IllegalArgumentException("value outside " + (signed ? "signed" : "unsigned") + " int" + bits + ": " + value);
        }
    }
}
