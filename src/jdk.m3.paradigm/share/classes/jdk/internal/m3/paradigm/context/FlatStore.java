/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Internal exact-width array ownership; no wrapper or node is retained for primitive elements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatStore.java}
 * blob {@code ab913f36630e38e97c478f44a1e0dd4e13b115dd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class FlatStore<T> {
    final StorageType<T> type;
    final int capacity;
    private final Object data;
    private long[] nulls;

    FlatStore(StorageType<T> type, int capacity) {
        this.type = Objects.requireNonNull(type);
        if (capacity < 0) throw new IllegalArgumentException("Negative capacity");
        this.capacity = capacity;
        data = switch (type.kind) {
            case OBJECT -> new Object[capacity];
            case BYTE -> new byte[capacity];
            case SHORT -> new short[capacity];
            case CHAR -> new char[capacity];
            case INT -> new int[capacity];
            case LONG -> new long[capacity];
            case FLOAT -> new float[capacity];
            case DOUBLE -> new double[capacity];
            case BOOLEAN -> new long[(int) (((long) capacity + 63) >>> 6)];
            case PRESENCE -> null;
        };
    }
    boolean isNull(int index) {
        if (type.kind == StorageType.Kind.OBJECT) return ((Object[]) data)[index] == null;
        return nulls != null && (nulls[index >>> 6] & (1L << index)) != 0;
    }
    @SuppressWarnings("unchecked")
    T get(int index) {
        if (type.kind == StorageType.Kind.OBJECT) return (T) ((Object[]) data)[index];
        if (type.kind == StorageType.Kind.PRESENCE) return (T) Boolean.TRUE;
        return isNull(index) ? null : type.box(bits(index));
    }
    void set(int index, T value) {
        type.validate(value);
        if (type.kind == StorageType.Kind.OBJECT) {
            ((Object[]) data)[index] = value;
        } else if (type.kind != StorageType.Kind.PRESENCE) {
            if (value == null) {
                if (nulls == null) nulls = new long[(int) (((long) capacity + 63) >>> 6)];
                raw(index, 0);
                nulls[index >>> 6] |= 1L << index;
            } else {
                setBits(index, type.toBits(value));
            }
        }
    }
    long bits(int index) {
        if (isNull(index)) throw new NoSuchElementException("Null element has no raw bits");
        return switch (type.kind) {
            case BYTE -> ((byte[]) data)[index] & 0xffL;
            case SHORT -> ((short[]) data)[index] & 0xffffL;
            case CHAR -> ((char[]) data)[index];
            case INT -> Integer.toUnsignedLong(((int[]) data)[index]);
            case LONG -> ((long[]) data)[index];
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits(((float[]) data)[index]));
            case DOUBLE -> Double.doubleToRawLongBits(((double[]) data)[index]);
            case BOOLEAN -> (((long[]) data)[index >>> 6] >>> index) & 1;
            default -> throw new UnsupportedOperationException("Not primitive storage");
        };
    }
    void setBits(int index, long bits) {
        type.checkBits(bits);
        raw(index, bits);
        if (nulls != null) nulls[index >>> 6] &= ~(1L << index);
    }
    private void raw(int index, long bits) {
        switch (type.kind) {
            case BYTE -> ((byte[]) data)[index] = (byte) bits;
            case SHORT -> ((short[]) data)[index] = (short) bits;
            case CHAR -> ((char[]) data)[index] = (char) bits;
            case INT -> ((int[]) data)[index] = (int) bits;
            case LONG -> ((long[]) data)[index] = bits;
            case FLOAT -> ((float[]) data)[index] = Float.intBitsToFloat((int) bits);
            case DOUBLE -> ((double[]) data)[index] = Double.longBitsToDouble(bits);
            case BOOLEAN -> {
                long mask = 1L << index;
                long[] words = (long[]) data;
                words[index >>> 6] = (words[index >>> 6] & ~mask) | (bits == 0 ? 0 : mask);
            }
            default -> throw new UnsupportedOperationException("Not primitive storage");
        }
    }
    void clear(int index) {
        if (type.kind == StorageType.Kind.OBJECT) ((Object[]) data)[index] = null;
        else if (type.kind != StorageType.Kind.PRESENCE) {
            raw(index, 0);
            if (nulls != null) nulls[index >>> 6] &= ~(1L << index);
        }
    }
    void copyTo(int from, FlatStore<T> target, int to) {
        if (type != target.type) throw new IllegalArgumentException("Different storage types");
        if (type.kind == StorageType.Kind.OBJECT) {
            ((Object[]) target.data)[to] = ((Object[]) data)[from];
        } else if (type.kind != StorageType.Kind.PRESENCE) {
            if (isNull(from)) target.set(to, null);
            else target.setBits(to, bits(from));
        }
    }
    void move(int from, int to, int length) {
        if (from < to) for (int i = length - 1; i >= 0; i--) copyTo(from + i, this, to + i);
        else for (int i = 0; i < length; i++) copyTo(from + i, this, to + i);
    }
    FlatStore<T> resized(int newCapacity, int length) {
        FlatStore<T> result = new FlatStore<>(type, newCapacity);
        for (int i = 0; i < length; i++) copyTo(i, result, i);
        return result;
    }
    boolean equalsAt(int index, Object value) {
        if (!type.accepts(value)) return false;
        if (type.kind == StorageType.Kind.OBJECT) return Objects.equals(value, ((Object[]) data)[index]);
        if (value == null || isNull(index)) return value == null && isNull(index);
        if (type.kind == StorageType.Kind.PRESENCE) return Boolean.TRUE.equals(value);
        return type.canonical(bits(index)) == type.canonical(type.toBits(value));
    }
    boolean bitsEqual(int index, long bits) {
        return !isNull(index) && type.canonical(bits(index)) == type.canonical(bits);
    }
    int hashAt(int index) {
        if (type.kind == StorageType.Kind.OBJECT) return Objects.hashCode(((Object[]) data)[index]);
        return isNull(index) ? 0 : type.hashBits(bits(index));
    }
    static int grow(int old, int needed) {
        if (needed < 0 || needed > Integer.MAX_VALUE - 8) throw new IllegalStateException("Capacity exhausted");
        return (int) Math.max(needed, Math.min(Integer.MAX_VALUE - 8L, Math.max(8L, old + (old >>> 1) + 1L)));
    }
}
