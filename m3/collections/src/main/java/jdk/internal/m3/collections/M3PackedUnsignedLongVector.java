// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Resizable unsigned integer vector using exactly 1..64 payload bits per logical value.
 *
 * <p>Values are packed consecutively into a primitive long lane and may straddle word
 * boundaries. No wrapper object or per-entry header exists. Width is immutable.
 *
 * <p>Modified 2026-11-08: exact bulk append, replacement and extraction plus an
 * input-derived minimum-width factory. Bulk sources must remain unchanged during
 * the call. Range and value failures are checked before any destination mutation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/PackedUnsignedLongVector.java}
 * blob {@code 12f3a210f20232d6257c677b4b7250dc45830e95} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedUnsignedLongVector {
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private final int width;
    private final long mask;
    private long[] words;
    private int size;

    public M3PackedUnsignedLongVector(int width) {
        this(width, 0);
    }

    public M3PackedUnsignedLongVector(int width, int expectedSize) {
        if (width < 1 || width > Long.SIZE) {
            throw new IllegalArgumentException("width must be in [1,64]");
        }
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        this.width = width;
        mask = width == Long.SIZE ? -1L : (1L << width) - 1L;
        words = new long[wordCount(expectedSize)];
    }

    /** Copies unsigned values using their minimum exact width; empty input uses one bit. */
    public static M3PackedUnsignedLongVector copyOf(long[] values) {
        Objects.requireNonNull(values, "values");
        return copyOf(values, 0, values.length);
    }

    /**
     * Copies a slice without retaining the source. Any negative long needs all 64 bits.
     * The source must not be modified concurrently with this operation.
     */
    public static M3PackedUnsignedLongVector copyOf(long[] values, int offset, int length) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, length, values.length);
        long occupiedBits = 0L;
        for (int i = 0; i < length; i++) occupiedBits |= values[offset + i];
        int requiredWidth = Math.max(1, Long.SIZE - Long.numberOfLeadingZeros(occupiedBits));
        M3PackedUnsignedLongVector result = new M3PackedUnsignedLongVector(requiredWidth, length);
        result.addAll(values, offset, length);
        return result;
    }

    public int width() {
        return width;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public long payloadBytes() {
        return 8L * words.length;
    }

    public void add(long value) {
        checkValue(value);
        ensureCapacity(Math.addExact(size, 1));
        write(size++, value);
    }

    /** Appends a fully validated batch; invalid input leaves size, capacity and values unchanged. */
    public void addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        addAll(values, 0, values.length);
    }

    /**
     * Appends a slice with one capacity reservation and no intermediate unpacked copy.
     * Range and value failures are detected before mutation. The source must not be
     * modified concurrently with this operation.
     */
    public void addAll(long[] values, int offset, int length) {
        checkValues(values, offset, length);
        int newSize = Math.addExact(size, length);
        ensureCapacity(newSize);
        writeAll(size, values, offset, length);
        size = newSize;
    }

    /** Replaces an existing range after validating every source value, without changing size. */
    public void setAll(int fromIndex, long[] values, int offset, int length) {
        Objects.checkFromIndexSize(fromIndex, length, size);
        checkValues(values, offset, length);
        writeAll(fromIndex, values, offset, length);
    }

    /** Copies a range into caller-owned storage; both ranges are checked before the first write. */
    public void copyInto(int fromIndex, long[] destination, int offset, int length) {
        Objects.checkFromIndexSize(fromIndex, length, size);
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(offset, length, destination.length);
        if (width == Long.SIZE) {
            System.arraycopy(words, fromIndex, destination, offset, length);
        } else {
            for (int i = 0; i < length; i++) destination[offset + i] = read(fromIndex + i);
        }
    }

    public long get(int index) {
        Objects.checkIndex(index, size);
        return read(index);
    }

    public long set(int index, long value) {
        Objects.checkIndex(index, size);
        checkValue(value);
        long previous = read(index);
        write(index, value);
        return previous;
    }

    public void clear() {
        size = 0;
    }

    public void trimToSize() {
        words = Arrays.copyOf(words, wordCount(size));
    }

    public long[] toLongArray() {
        long[] result = new long[size];
        copyInto(0, result, 0, size);
        return result;
    }

    private void checkValues(long[] values, int offset, int length) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, length, values.length);
        if (width < Long.SIZE) {
            for (int i = 0; i < length; i++) checkValue(values[offset + i]);
        }
    }

    private void writeAll(int fromIndex, long[] values, int offset, int length) {
        if (width == Long.SIZE) {
            System.arraycopy(values, offset, words, fromIndex, length);
        } else {
            for (int i = 0; i < length; i++) write(fromIndex + i, values[offset + i]);
        }
    }

    private long read(int index) {
        long bit = (long) index * width;
        int word = (int) (bit >>> 6);
        int offset = (int) bit & 63;
        if (width == Long.SIZE) {
            return words[word];
        }
        if (offset + width <= Long.SIZE) {
            return (words[word] >>> offset) & mask;
        }
        int lowBits = Long.SIZE - offset;
        long low = words[word] >>> offset;
        long high = words[word + 1] & ((1L << (width - lowBits)) - 1L);
        return (high << lowBits) | low;
    }

    private void write(int index, long value) {
        long bit = (long) index * width;
        int word = (int) (bit >>> 6);
        int offset = (int) bit & 63;
        if (width == Long.SIZE) {
            words[word] = value;
            return;
        }
        if (offset + width <= Long.SIZE) {
            long shiftedMask = mask << offset;
            words[word] = (words[word] & ~shiftedMask) | ((value << offset) & shiftedMask);
            return;
        }
        int lowBits = Long.SIZE - offset;
        long lowMask = (-1L) << offset;
        words[word] = (words[word] & ~lowMask) | (value << offset);
        int highBits = width - lowBits;
        long highMask = (1L << highBits) - 1L;
        words[word + 1] = (words[word + 1] & ~highMask) | ((value >>> lowBits) & highMask);
    }

    private void ensureCapacity(int values) {
        int required = wordCount(values);
        if (required <= words.length) {
            return;
        }
        int grown = Math.max(required, Math.max(1, words.length + (words.length >>> 1) + 1));
        if (grown < 0 || grown > MAX_ARRAY_SIZE) {
            grown = required;
        }
        words = Arrays.copyOf(words, grown);
    }

    private int wordCount(int values) {
        long bits = Math.multiplyExact((long) values, width);
        long count = (bits + 63L) >>> 6;
        if (count > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("packed vector exceeds Java array bounds");
        }
        return (int) count;
    }

    private void checkValue(long value) {
        if (width < Long.SIZE && Long.compareUnsigned(value, mask) > 0) {
            throw new IllegalArgumentException(
                    "value does not fit unsigned " + width + "-bit lane: " + value);
        }
    }
}
