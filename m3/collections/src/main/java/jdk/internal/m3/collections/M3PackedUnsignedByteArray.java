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
import java.util.function.IntPredicate;

/**
 * Resizable unsigned 8-bit sequence backed by one {@code byte[]}.
 *
 * <p>Values are presented as ints in 0..255 while retaining exactly one byte per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedUnsignedByteArray.java}
 * blob {@code 58b459e25c3b0e004d55c89fe433886f6f78ba04} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedUnsignedByteArray implements M3UnsignedByteSequence {
    private byte[] values;
    private int size;

    public M3PackedUnsignedByteArray() {
        this(16);
    }

    public M3PackedUnsignedByteArray(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        values = new byte[Math.max(1, expectedSize)];
    }

    public void add(int unsignedValue) {
        int checked = M3PackedSupport.checkedUnsignedByte(unsignedValue);
        ensureCapacity(size + 1);
        values[size++] = (byte) checked;
    }

    public boolean addIf(int unsignedValue, IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        M3PackedSupport.checkedUnsignedByte(unsignedValue);
        if (!predicate.test(unsignedValue)) {
            return false;
        }
        add(unsignedValue);
        return true;
    }

    public int getUnsigned(int index) {
        M3PackedSupport.checkIndex(index, size);
        return Byte.toUnsignedInt(values[index]);
    }

    public int setUnsigned(int index, int unsignedValue) {
        M3PackedSupport.checkIndex(index, size);
        int checked = M3PackedSupport.checkedUnsignedByte(unsignedValue);
        int previous = Byte.toUnsignedInt(values[index]);
        values[index] = (byte) checked;
        return previous;
    }

    public int addClamped(int index, int delta) {
        M3PackedSupport.checkIndex(index, size);
        long candidate = (long) Byte.toUnsignedInt(values[index]) + delta;
        int next = (int) Math.max(0L, Math.min(255L, candidate));
        values[index] = (byte) next;
        return next;
    }

    public int removeAt(int index) {
        M3PackedSupport.checkIndex(index, size);
        int previous = Byte.toUnsignedInt(values[index]);
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        return previous;
    }

    public boolean containsUnsigned(int unsignedValue) {
        int checked = M3PackedSupport.checkedUnsignedByte(unsignedValue);
        byte target = (byte) checked;
        for (int i = 0; i < size; i++) {
            if (values[i] == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counting sort over the unsigned 0..255 domain.
     */
    public void sortUnsigned() {
        int[] counts = new int[256];
        for (int i = 0; i < size; i++) {
            counts[Byte.toUnsignedInt(values[i])]++;
        }
        int write = 0;
        for (int value = 0; value < counts.length; value++) {
            int count = counts[value];
            while (count-- > 0) {
                values[write++] = (byte) value;
            }
        }
    }

    public int filterInPlace(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int write = 0;
        for (int read = 0; read < size; read++) {
            int value = Byte.toUnsignedInt(values[read]);
            if (predicate.test(value)) {
                values[write++] = values[read];
            }
        }
        int removed = size - write;
        size = write;
        return removed;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return values.length;
    }

    public void clear() {
        size = 0;
    }

    public void trimToSize() {
        values = Arrays.copyOf(values, Math.max(1, size));
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(values, size);
    }

    /** Boundary materialization for APIs that insist on int values. */
    public int[] toUnsignedIntArray() {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = Byte.toUnsignedInt(values[i]);
        }
        return result;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= values.length) {
            return;
        }
        values = Arrays.copyOf(
                values,
                M3PackedSupport.growArrayCapacity(values.length, minimumCapacity));
    }
}
