// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.packed;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-length, owned packed storage for signed 40-, 48-, and 56-bit integers.
 *
 * <p>Elements use little-endian two's-complement bytes. Operations return values
 * as {@code long}; no per-element object or long array is retained. Input
 * validation completes before mutation. Instances are not thread-safe and
 * multi-byte accesses are not atomic; callers must synchronize concurrent use.
 */
public sealed abstract class PackedIntArray permits Int40Array, Int48Array, Int56Array {
    private final int bits;
    private final byte[] data;

    PackedIntArray(int length, int bits) {
        int bytes = checkedBytes(bits);
        if (length < 0 || length > Integer.MAX_VALUE / bytes) {
            throw new IllegalArgumentException("length cannot fit packed byte storage: " + length);
        }
        this.bits = bits;
        this.data = new byte[length * bytes];
    }

    PackedIntArray(byte[] packed, int bits) {
        Objects.requireNonNull(packed, "packed");
        int bytes = checkedBytes(bits);
        if (packed.length % bytes != 0) {
            throw new IllegalArgumentException("packed length must be divisible by " + bytes);
        }
        this.bits = bits;
        this.data = packed.clone();
    }

    private static int checkedBytes(int bits) {
        if (bits != 40 && bits != 48 && bits != 56) {
            throw new IllegalArgumentException("unsupported signed width: " + bits);
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
        return -(1L << (bits - 1));
    }

    /**
     * Returns the largest representable value.
     * @return inclusive signed maximum
     */
    public final long maxValue() {
        return (1L << (bits - 1)) - 1;
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
        return PackedCodec.readSigned(data, index * bytes, bytes);
    }

    /**
     * Replaces one element after checking its index and numeric range.
     * @param index zero-based element index
     * @param value signed value to store
     * @throws IndexOutOfBoundsException if the index is outside this array
     * @throws IllegalArgumentException if the value does not fit this width
     */
    public final void set(int index, long value) {
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
            destination[offset + i] = PackedCodec.readSigned(data, byteOffset, bytes);
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
        if (bits != destination.bits) {
            throw new IllegalArgumentException("copy requires identical integer widths");
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

    private void checkValue(long value) {
        if (value < minValue() || value > maxValue()) {
            throw new IllegalArgumentException("value outside signed int" + bits + ": " + value);
        }
    }
}
