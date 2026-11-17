// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@fd9a233680b43197eb8c5745a729c342ca56c4e2
// Source: m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveKind.java
// Source-Git-blob: bbfa6200d5e8583cf414e51f48b546fe5496fb7f
/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

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
public enum PrimitiveKind {
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

    PrimitiveKind(int widthBytes) {
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

    /**
     * Allocates the primitive storage array of this kind: {@code byte[]} for BOOLEAN and BYTE,
     * otherwise the array of the matching primitive type.
     *
     * @param length number of lanes
     * @return a new zero-filled storage array
     * @throws NegativeArraySizeException if {@code length} is negative
     */
    public Object newArray(int length) {
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

    /**
     * Copies a storage array of this kind to a new length, as {@link Arrays#copyOf(long[], int)}
     * does.
     *
     * @param array storage array of this kind
     * @param newLength length of the copy
     * @return the copy, truncated or zero-padded
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     */
    public Object copyOf(Object array, int newLength) {
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

    /**
     * Zeroes the lanes {@code [from, to)} of a storage array of this kind.
     *
     * @param array storage array of this kind
     * @param from first lane, inclusive
     * @param to last lane, exclusive
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     */
    public void clear(Object array, int from, int to) {
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

    /**
     * Reads one lane as a 64-bit carrier: integral kinds sign- or zero-extend as Java widening
     * does, BOOLEAN gives zero or one, FLOAT gives its raw IEEE-754 bits unsigned, DOUBLE its raw
     * bits.
     *
     * @param array storage array of this kind
     * @param index lane index
     * @return the carrier bits of the lane
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     */
    public long readBits(Object array, int index) {
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

    /**
     * Writes one lane from a 64-bit carrier: integral kinds narrow as Java casts do, BOOLEAN stores
     * one for any non-zero carrier, FLOAT and DOUBLE store the raw bits.
     *
     * @param array storage array of this kind
     * @param index lane index
     * @param bits carrier bits
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     */
    public void writeBits(Object array, int index, long bits) {
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

    /**
     * Fills the lanes {@code [from, to)} with one carrier value, storing it as {@link #writeBits}
     * does; raw floating-point bits are preserved.
     *
     * @param array storage array of this kind
     * @param from first lane, inclusive
     * @param to last lane, exclusive
     * @param bits carrier bits
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     * @throws IndexOutOfBoundsException if the range is not within the array
     */
    public void fillBits(Object array, int from, int to, long bits) {
        checkArray(array);
        java.util.Objects.checkFromToIndex(from, to, java.lang.reflect.Array.getLength(array));
        switch (this) {
            case BOOLEAN -> Arrays.fill((byte[]) array, from, to, (byte) (bits == 0L ? 0 : 1));
            case BYTE -> Arrays.fill((byte[]) array, from, to, (byte) bits);
            case SHORT -> Arrays.fill((short[]) array, from, to, (short) bits);
            case CHAR -> Arrays.fill((char[]) array, from, to, (char) bits);
            case INT -> Arrays.fill((int[]) array, from, to, (int) bits);
            case LONG -> Arrays.fill((long[]) array, from, to, bits);
            case FLOAT -> Arrays.fill((float[]) array, from, to, Float.intBitsToFloat((int) bits));
            case DOUBLE -> Arrays.fill((double[]) array, from, to, Double.longBitsToDouble(bits));
        }
    }

    /**
     * Returns the first lane in {@code [from, to)} equal to a carrier value: NaNs are equal to each
     * other, {@code -0.0} and {@code 0.0} are distinct.
     *
     * @param array storage array of this kind
     * @param from first lane, inclusive
     * @param to last lane, exclusive
     * @param bits carrier bits
     * @return the lane index, or -1
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     * @throws IndexOutOfBoundsException if the range is not within the array
     */
    public int indexOfBits(Object array, int from, int to, long bits) {
        checkArray(array);
        java.util.Objects.checkFromToIndex(from, to, java.lang.reflect.Array.getLength(array));
        switch (this) {
            case BOOLEAN -> {
                byte[] values = (byte[]) array;
                boolean wanted = bits != 0;
                for (int i = from; i < to; i++) if ((values[i] != 0) == wanted) return i;
            }
            case BYTE -> {
                byte[] values = (byte[]) array; byte wanted = (byte) bits;
                for (int i = from; i < to; i++) if (values[i] == wanted) return i;
            }
            case SHORT -> {
                short[] values = (short[]) array; short wanted = (short) bits;
                for (int i = from; i < to; i++) if (values[i] == wanted) return i;
            }
            case CHAR -> {
                char[] values = (char[]) array; char wanted = (char) bits;
                for (int i = from; i < to; i++) if (values[i] == wanted) return i;
            }
            case INT -> {
                int[] values = (int[]) array; int wanted = (int) bits;
                for (int i = from; i < to; i++) if (values[i] == wanted) return i;
            }
            case LONG -> {
                long[] values = (long[]) array;
                for (int i = from; i < to; i++) if (values[i] == bits) return i;
            }
            case FLOAT -> {
                float[] values = (float[]) array;
                int wanted = Float.floatToIntBits(Float.intBitsToFloat((int) bits));
                for (int i = from; i < to; i++) if (Float.floatToIntBits(values[i]) == wanted) return i;
            }
            case DOUBLE -> {
                double[] values = (double[]) array;
                long wanted = Double.doubleToLongBits(Double.longBitsToDouble(bits));
                for (int i = from; i < to; i++) if (Double.doubleToLongBits(values[i]) == wanted) return i;
            }
        }
        return -1;
    }

    /**
     * Returns the canonical carrier used for equality and hashing: integral kinds narrowed, BOOLEAN
     * as zero or one, floating-point NaNs collapsed to the canonical NaN.
     *
     * @param bits carrier bits
     * @return the canonical carrier
     */
    public long normalizeBits(long bits) {
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
     * Returns the carrier after one write and read through storage of this kind. Unlike {@link
     * #normalizeBits(long)}, floating-point NaN payload bits are preserved.
     *
     * @param bits carrier bits
     * @return the carrier as storage returns it
     */
    public long storageBits(long bits) {
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

    /**
     * Compares two carriers in the natural order of this kind; FLOAT and DOUBLE follow {@link
     * Float#compare(float, float)} and {@link Double#compare(double, double)}.
     *
     * @param left first carrier
     * @param right second carrier
     * @return a negative, zero or positive value
     */
    public int compareBits(long left, long right) {
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

    /**
     * Tells whether two carriers are equal after {@link #normalizeBits(long)}.
     *
     * @param left first carrier
     * @param right second carrier
     * @return true if both canonical carriers are equal
     */
    public boolean equalBits(long left, long right) {
        return normalizeBits(left) == normalizeBits(right);
    }

    /**
     * Returns the hash of a carrier after {@link #normalizeBits(long)}, mixed as the dense hash
     * tables of this package mix their keys.
     *
     * @param bits carrier bits
     * @return the mixed hash
     */
    public int hashBits(long bits) {
        long normalized = normalizeBits(bits);
        return switch (this) {
            case BOOLEAN, BYTE, SHORT, CHAR, INT, FLOAT -> DenseHash.mix((int) normalized);
            case LONG, DOUBLE -> DenseHash.mix(normalized);
        };
    }

    /**
     * Sorts the lanes {@code [from, to)} in ascending order with {@link Arrays}' primitive sort of
     * the storage type.
     *
     * @param array storage array of this kind
     * @param from first lane, inclusive
     * @param to last lane, exclusive
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     */
    public void sort(Object array, int from, int to) {
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

    /**
     * Searches a sorted range for a carrier value with {@link Arrays}' primitive binary search of
     * the storage type.
     *
     * @param array storage array of this kind, sorted in {@code [from, to)}
     * @param from first lane, inclusive
     * @param to last lane, exclusive
     * @param bits carrier bits
     * @return the index of the value, or {@code -(insertion point) - 1}
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     */
    public int binarySearch(Object array, int from, int to, long bits) {
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

    /**
     * Checks that an object is a storage array of this kind.
     *
     * @param array the object to check
     * @throws IllegalArgumentException if it is not storage of this kind
     */
    public void checkArray(Object array) {
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
    public static int checkedSize(int size, int extra) {
        if (size < 0 || extra < 0 || extra > Integer.MAX_VALUE - size) {
            throw new OutOfMemoryError("required primitive array size exceeds int range");
        }
        return size + extra;
    }

    /** Preserves 1.5x-plus-one growth without signed overflow. */
    public static int grownCapacity(int current, int required) {
        if (current < 0 || required < 0) throw new IllegalArgumentException("negative capacity");
        if (required <= current) return current;
        long grown = (long) current + (current >>> 1) + 1;
        int softLimit = Integer.MAX_VALUE - 8;
        return required > softLimit ? required : (int) Math.min(softLimit, Math.max(grown, required));
    }

    /**
     * Maps a logical offset of a circular range to its physical index without overflowing {@code
     * head + logical}.
     *
     * @param head physical index of logical offset zero
     * @param logical logical offset, {@code 0 <= logical < capacity}
     * @param capacity ring capacity
     * @return the physical index
     */
    public static int ringIndex(int head, int logical, int capacity) {
        int remaining = capacity - head;
        return logical < remaining ? head + logical : logical - remaining;
    }

    /**
     * Copies a logical circular range into a distinct linear array, in at most two bulk copies.
     *
     * @param source ring storage
     * @param head physical index of the first lane
     * @param length number of lanes
     * @param capacity ring capacity
     * @param target destination array, distinct from {@code source}
     * @param offset first destination index
     */
    public static void copyRing(Object source, int head, int length, int capacity, Object target,
            int offset) {
        int first = Math.min(length, capacity - head);
        if (first > 0) System.arraycopy(source, head, target, offset, first);
        if (length > first) System.arraycopy(source, 0, target, offset + first, length - first);
    }

    /**
     * Moves a logical range within one ring, as memmove does: overlapping ranges are handled. The
     * caller validates the ranges first.
     *
     * @param array ring storage
     * @param head physical index of logical offset zero
     * @param capacity ring capacity
     * @param source first logical offset to read
     * @param target first logical offset to write
     * @param length number of lanes
     */
    public static void moveRing(Object array, int head, int capacity, int source, int target,
            int length) {
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

    /**
     * Validates a slice of a storage array of this kind before the caller changes retained state.
     *
     * @param array storage array of this kind
     * @param from first lane
     * @param length number of lanes
     * @return {@code length}
     * @throws NullPointerException if {@code array} is null
     * @throws IllegalArgumentException if {@code array} is not storage of this kind
     * @throws IndexOutOfBoundsException if the slice is not within the array
     */
    public int checkRange(Object array, int from, int length) {
        java.util.Objects.requireNonNull(array, "source");
        checkArray(array);
        java.util.Objects.checkFromIndexSize(from, length, java.lang.reflect.Array.getLength(array));
        return length;
    }

    /**
     * Copies lanes between storage arrays of this kind; BOOLEAN source bytes are normalized to zero
     * or one, and an overlapping copy within one BOOLEAN array is handled.
     *
     * @param source source storage
     * @param from first source lane
     * @param target destination storage
     * @param offset first destination lane
     * @param length number of lanes
     */
    public void copyStorage(Object source, int from, Object target, int offset, int length) {
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

    /**
     * Zeroes the retained lanes of a circular range, in at most two fills.
     *
     * @param array ring storage of this kind
     * @param head physical index of the first lane
     * @param length number of lanes
     * @param capacity ring capacity
     */
    public void clearRing(Object array, int head, int length, int capacity) {
        int first = Math.min(length, capacity - head);
        if (first > 0) clear(array, head, head + first);
        if (length > first) clear(array, 0, length - first);
    }
}
