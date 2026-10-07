// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveKind.java
// Source-Git-blob: 4bc7c258073072e0310d1a50c3885ed55c50816b
package com.m3.util;

import java.util.Arrays;

/**
 * Primitive lane kind used by the exact-width collection family.
 *
 * <p>Values cross the generic low-level API as a 64-bit carrier. Integral kinds use
 * their numeric value, float/double use raw IEEE-754 bits, and boolean uses 0/1.
 * The retained storage is always a primitive array; no wrapper is retained per value.
 * Boolean storage is a {@code byte[]}, not a {@code boolean[]} and not bit-packed.
 *
 * <p>Integral carriers narrow as Java primitive casts do. Floating-point comparisons use
 * {@link Float#compare(float, float)} and {@link Double#compare(double, double)}:
 * negative zero precedes positive zero; NaNs follow positive infinity and compare equal.
 * Equality/hashing canonicalizes NaNs, while physical lane copies preserve stored raw bits.
 * Collection operations do not imply boxing or implementation of {@code Collection<Long>}.
 */
public enum M3PrimitiveKind {
    /** Boolean values, encoded as zero or one in a {@code byte[]}. */
    BOOLEAN(1),
    /** Signed eight-bit values in a {@code byte[]}. */
    BYTE(Byte.BYTES),
    /** Signed sixteen-bit values in a {@code short[]}. */
    SHORT(Short.BYTES),
    /** Unsigned UTF-16 code units in a {@code char[]}. */
    CHAR(Character.BYTES),
    /** Signed thirty-two-bit values in an {@code int[]}. */
    INT(Integer.BYTES),
    /** Signed sixty-four-bit values in a {@code long[]}. */
    LONG(Long.BYTES),
    /** IEEE-754 values in a {@code float[]}, carried as unsigned thirty-two-bit raw bits. */
    FLOAT(Float.BYTES),
    /** IEEE-754 values in a {@code double[]}, carried as sixty-four-bit raw bits. */
    DOUBLE(Double.BYTES);

    private final int widthBytes;

    M3PrimitiveKind(int widthBytes) {
        this.widthBytes = widthBytes;
    }

    /**
     * Returns the bytes reserved for one physical lane, not an object-footprint measurement.
     * @return the lane width in bytes
     */
    public int widthBytes() {
        return widthBytes;
    }

    /**
     * Returns the primitive payload reservation for a capacity.
     * Array/object headers, alignment, temporary buffers and external indexes are excluded.
     * @param capacity number of physical lanes
     * @return {@code capacity * widthBytes()} using overflow-safe long arithmetic
     * @throws IllegalArgumentException if capacity is negative
     */
    public long payloadBytes(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity < 0");
        return (long) widthBytes * capacity;
    }

    Object newArray(int length) {
        if (length < 0) throw new NegativeArraySizeException(Integer.toString(length));
        return switch (this) {
            case BOOLEAN, BYTE -> new byte[length];
            case SHORT -> new short[length];
            case CHAR -> new char[length];
            case INT -> new int[length];
            case LONG -> new long[length];
            case FLOAT -> new float[length];
            case DOUBLE -> new double[length];
        };
    }

    Object copyOf(Object array, int newLength) {
        checkArray(array);
        return switch (this) {
            case BOOLEAN, BYTE -> Arrays.copyOf((byte[]) array, newLength);
            case SHORT -> Arrays.copyOf((short[]) array, newLength);
            case CHAR -> Arrays.copyOf((char[]) array, newLength);
            case INT -> Arrays.copyOf((int[]) array, newLength);
            case LONG -> Arrays.copyOf((long[]) array, newLength);
            case FLOAT -> Arrays.copyOf((float[]) array, newLength);
            case DOUBLE -> Arrays.copyOf((double[]) array, newLength);
        };
    }

    void clear(Object array, int from, int to) {
        checkArray(array);
        switch (this) {
            case BOOLEAN, BYTE -> Arrays.fill((byte[]) array, from, to, (byte) 0);
            case SHORT -> Arrays.fill((short[]) array, from, to, (short) 0);
            case CHAR -> Arrays.fill((char[]) array, from, to, (char) 0);
            case INT -> Arrays.fill((int[]) array, from, to, 0);
            case LONG -> Arrays.fill((long[]) array, from, to, 0L);
            case FLOAT -> Arrays.fill((float[]) array, from, to, 0.0f);
            case DOUBLE -> Arrays.fill((double[]) array, from, to, 0.0d);
        }
    }

    long readBits(Object array, int index) {
        checkArray(array);
        return switch (this) {
            case BOOLEAN -> ((byte[]) array)[index] == 0 ? 0L : 1L;
            case BYTE -> ((byte[]) array)[index];
            case SHORT -> ((short[]) array)[index];
            case CHAR -> ((char[]) array)[index];
            case INT -> ((int[]) array)[index];
            case LONG -> ((long[]) array)[index];
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits(((float[]) array)[index]));
            case DOUBLE -> Double.doubleToRawLongBits(((double[]) array)[index]);
        };
    }

    void writeBits(Object array, int index, long bits) {
        checkArray(array);
        switch (this) {
            case BOOLEAN -> ((byte[]) array)[index] = (byte) (bits == 0L ? 0 : 1);
            case BYTE -> ((byte[]) array)[index] = (byte) bits;
            case SHORT -> ((short[]) array)[index] = (short) bits;
            case CHAR -> ((char[]) array)[index] = (char) bits;
            case INT -> ((int[]) array)[index] = (int) bits;
            case LONG -> ((long[]) array)[index] = bits;
            case FLOAT -> ((float[]) array)[index] = Float.intBitsToFloat((int) bits);
            case DOUBLE -> ((double[]) array)[index] = Double.longBitsToDouble(bits);
        }
    }

    long normalizeBits(long bits) {
        return switch (this) {
            case BOOLEAN -> bits == 0L ? 0L : 1L;
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG -> bits;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToIntBits(Float.intBitsToFloat((int) bits)));
            case DOUBLE -> Double.doubleToLongBits(Double.longBitsToDouble(bits));
        };
    }

    /**
     * Carrier bits after one write/read through this exact-width storage kind.
     *
     * <p>Unlike normalizeBits, floating NaN payload bits are preserved because
     * this models physical lane storage rather than equality/hash canonicalization.
     */
    long storageBits(long bits) {
        return switch (this) {
            case BOOLEAN -> bits == 0L ? 0L : 1L;
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG -> bits;
            case FLOAT -> Integer.toUnsignedLong((int) bits);
            case DOUBLE -> bits;
        };
    }

    int compareBits(long left, long right) {
        return switch (this) {
            case BOOLEAN -> Boolean.compare(left != 0L, right != 0L);
            case BYTE -> Byte.compare((byte) left, (byte) right);
            case SHORT -> Short.compare((short) left, (short) right);
            case CHAR -> Character.compare((char) left, (char) right);
            case INT -> Integer.compare((int) left, (int) right);
            case LONG -> Long.compare(left, right);
            case FLOAT -> Float.compare(Float.intBitsToFloat((int) left), Float.intBitsToFloat((int) right));
            case DOUBLE -> Double.compare(Double.longBitsToDouble(left), Double.longBitsToDouble(right));
        };
    }

    boolean equalBits(long left, long right) {
        return normalizeBits(left) == normalizeBits(right);
    }

    int hashBits(long bits) {
        long normalized = normalizeBits(bits);
        return switch (this) {
            case BOOLEAN, BYTE, SHORT, CHAR, INT, FLOAT -> DenseHash.mix((int) normalized);
            case LONG, DOUBLE -> DenseHash.mix(normalized);
        };
    }

    void sort(Object array, int from, int to) {
        checkArray(array);
        switch (this) {
            case BOOLEAN, BYTE -> Arrays.sort((byte[]) array, from, to);
            case SHORT -> Arrays.sort((short[]) array, from, to);
            case CHAR -> Arrays.sort((char[]) array, from, to);
            case INT -> Arrays.sort((int[]) array, from, to);
            case LONG -> Arrays.sort((long[]) array, from, to);
            case FLOAT -> Arrays.sort((float[]) array, from, to);
            case DOUBLE -> Arrays.sort((double[]) array, from, to);
        }
    }

    int binarySearch(Object array, int from, int to, long bits) {
        checkArray(array);
        return switch (this) {
            case BOOLEAN -> Arrays.binarySearch((byte[]) array, from, to, (byte) (bits == 0L ? 0 : 1));
            case BYTE -> Arrays.binarySearch((byte[]) array, from, to, (byte) bits);
            case SHORT -> Arrays.binarySearch((short[]) array, from, to, (short) bits);
            case CHAR -> Arrays.binarySearch((char[]) array, from, to, (char) bits);
            case INT -> Arrays.binarySearch((int[]) array, from, to, (int) bits);
            case LONG -> Arrays.binarySearch((long[]) array, from, to, bits);
            case FLOAT -> Arrays.binarySearch((float[]) array, from, to, Float.intBitsToFloat((int) bits));
            case DOUBLE -> Arrays.binarySearch((double[]) array, from, to, Double.longBitsToDouble(bits));
        };
    }

    void checkArray(Object array) {
        boolean valid = switch (this) {
            case BOOLEAN, BYTE -> array instanceof byte[];
            case SHORT -> array instanceof short[];
            case CHAR -> array instanceof char[];
            case INT -> array instanceof int[];
            case LONG -> array instanceof long[];
            case FLOAT -> array instanceof float[];
            case DOUBLE -> array instanceof double[];
        };
        if (!valid) throw new IllegalArgumentException("array does not match " + this);
    }

    /** Checked logical size, evaluated before allocation or mutation. */
    static int checkedSize(int size, int extra) {
        if (size < 0 || extra < 0 || extra > Integer.MAX_VALUE - size) {
            throw new OutOfMemoryError("required primitive array size exceeds int range");
        }
        return size + extra;
    }

    /** Preserves 1.5x-plus-one growth without signed overflow. */
    static int grownCapacity(int current, int required) {
        if (current < 0 || required < 0) throw new IllegalArgumentException("negative capacity");
        if (required <= current) return current;
        long grown = (long) current + (current >>> 1) + 1;
        int softLimit = Integer.MAX_VALUE - 8;
        return required > softLimit ? required : (int) Math.min(softLimit, Math.max(grown, required));
    }

    /** Maps a legal circular offset without overflowing head + logical. */
    static int ringIndex(int head, int logical, int capacity) {
        int remaining = capacity - head;
        return logical < remaining ? head + logical : logical - remaining;
    }

    /** Copies a logical circular range to a distinct linear array in at most two bulk copies. */
    static void copyRing(Object source, int head, int length, int capacity, Object target, int offset) {
        int first = Math.min(length, capacity - head);
        if (first > 0) System.arraycopy(source, head, target, offset, first);
        if (length > first) System.arraycopy(source, 0, target, offset + first, length - first);
    }

    /**
     * Logical memmove within one ring. Owners validate ranges before calling this atom.
     * Direction is chosen in logical coordinates; arraycopy handles overlap inside each span.
     */
    static void moveRing(Object array, int head, int capacity, int source, int target, int length) {
        if (length == 0 || source == target) return;
        if (target < source) {
            int read = ringIndex(head, source, capacity);
            int write = ringIndex(head, target, capacity);
            while (length > 0) {
                int chunk = Math.min(length, Math.min(capacity - read, capacity - write));
                System.arraycopy(array, read, array, write, chunk);
                length -= chunk;
                read += chunk;
                write += chunk;
                if (read == capacity) read = 0;
                if (write == capacity) write = 0;
            }
        } else {
            int read = ringIndex(head, source + length - 1, capacity);
            int write = ringIndex(head, target + length - 1, capacity);
            while (length > 0) {
                int chunk = Math.min(length, Math.min(read + 1, write + 1));
                System.arraycopy(array, read - chunk + 1, array, write - chunk + 1, chunk);
                length -= chunk;
                read -= chunk;
                write -= chunk;
                if (read < 0) read = capacity - 1;
                if (write < 0) write = capacity - 1;
            }
        }
    }

    /** Validates a matching array slice before an owner changes retained state. */
    int checkRange(Object array, int from, int length) {
        java.util.Objects.requireNonNull(array, "source");
        checkArray(array);
        java.util.Objects.checkFromIndexSize(from, length, java.lang.reflect.Array.getLength(array));
        return length;
    }

    /** Copies exact-width lanes; BOOLEAN source bytes are normalized to zero or one. */
    void copyStorage(Object source, int from, Object target, int offset, int length) {
        if (this != BOOLEAN) {
            System.arraycopy(source, from, target, offset, length);
        } else {
            byte[] input = (byte[]) source;
            byte[] output = (byte[]) target;
            // Owners snapshot self-aliases before structural edits; also support local overlap.
            if (source == target && offset > from && offset - from < length) {
                for (int i = length - 1; i >= 0; i--) output[offset + i] = (byte) (input[from + i] == 0 ? 0 : 1);
            } else {
                for (int i = 0; i < length; i++) output[offset + i] = (byte) (input[from + i] == 0 ? 0 : 1);
            }
        }
    }

    /** Clears only retained circular lanes, in at most two typed fills. */
    void clearRing(Object array, int head, int length, int capacity) {
        int first = Math.min(length, capacity - head);
        if (first > 0) clear(array, head, head + first);
        if (length > first) clear(array, 0, length - first);
    }
}
