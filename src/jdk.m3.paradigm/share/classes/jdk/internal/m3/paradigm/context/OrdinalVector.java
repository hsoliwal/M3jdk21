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

import java.util.Objects;

/**
 * Immutable vector of finite ordinals using the narrowest safe primitive storage.
 * Suitable for enum ordinals when the caller owns the enum-order persistence ABI.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalVector.java}
 * blob {@code ff8ff65a5f54dccc49c5c2547723e65406f32bbf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalVector {
    public enum Width { CONSTANT, BIT, U8, U16, I32 }

    private static final long[] EMPTY_BITS = new long[0];
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final short[] EMPTY_SHORTS = new short[0];
    private static final int[] EMPTY_INTS = new int[0];

    private final int cardinality;
    private final int size;
    private final Object storage;

    private OrdinalVector(int cardinality, int size, Object storage) {
        this.cardinality = cardinality;
        this.size = size;
        this.storage = storage;
    }

    public static OrdinalVector filled(int cardinality, int size, int initialOrdinal) {
        requireShape(cardinality, size);
        requireOrdinal(cardinality, initialOrdinal);
        Width width = width(cardinality);
        Object storage = switch (width) {
            case CONSTANT -> EMPTY_BYTES;
            case BIT -> {
                long[] bits = size == 0 ? EMPTY_BITS : new long[(size + 63) >>> 6];
                if (initialOrdinal == 1) {
                    java.util.Arrays.fill(bits, -1L);
                    if ((size & 63) != 0) {
                        bits[bits.length - 1] &= (1L << (size & 63)) - 1L;
                    }
                }
                yield bits;
            }
            case U8 -> {
                byte[] values = size == 0 ? EMPTY_BYTES : new byte[size];
                java.util.Arrays.fill(values, (byte) initialOrdinal);
                yield values;
            }
            case U16 -> {
                short[] values = size == 0 ? EMPTY_SHORTS : new short[size];
                java.util.Arrays.fill(values, (short) initialOrdinal);
                yield values;
            }
            case I32 -> {
                int[] values = size == 0 ? EMPTY_INTS : new int[size];
                java.util.Arrays.fill(values, initialOrdinal);
                yield values;
            }
        };
        return new OrdinalVector(cardinality, size, storage);
    }

    public int cardinality() { return cardinality; }
    public int size() { return size; }
    public Width width() { return width(cardinality); }

    public static Width width(int cardinality) {
        if (cardinality < 1) throw new IllegalArgumentException("cardinality must be positive");
        if (cardinality == 1) return Width.CONSTANT;
        if (cardinality == 2) return Width.BIT;
        if (cardinality <= 256) return Width.U8;
        if (cardinality <= 65_536) return Width.U16;
        return Width.I32;
    }

    public int get(int index) {
        Objects.checkIndex(index, size);
        return switch (width()) {
            case CONSTANT -> 0;
            case BIT -> (int) ((((long[]) storage)[index >>> 6] >>> (index & 63)) & 1L);
            case U8 -> Byte.toUnsignedInt(((byte[]) storage)[index]);
            case U16 -> Short.toUnsignedInt(((short[]) storage)[index]);
            case I32 -> ((int[]) storage)[index];
        };
    }

    public OrdinalVector with(int index, int ordinal) {
        Objects.checkIndex(index, size);
        requireOrdinal(cardinality, ordinal);
        if (get(index) == ordinal) return this;
        Object copy = switch (width()) {
            case CONSTANT -> throw new AssertionError("constant universe cannot change ordinal");
            case BIT -> {
                long[] bits = ((long[]) storage).clone();
                long mask = 1L << (index & 63);
                if (ordinal == 0) bits[index >>> 6] &= ~mask;
                else bits[index >>> 6] |= mask;
                yield bits;
            }
            case U8 -> {
                byte[] values = ((byte[]) storage).clone();
                values[index] = (byte) ordinal;
                yield values;
            }
            case U16 -> {
                short[] values = ((short[]) storage).clone();
                values[index] = (short) ordinal;
                yield values;
            }
            case I32 -> {
                int[] values = ((int[]) storage).clone();
                values[index] = ordinal;
                yield values;
            }
        };
        return new OrdinalVector(cardinality, size, copy);
    }

    public <E extends Enum<E>> E enumValue(Class<E> type, int index) {
        E[] universe = Objects.requireNonNull(type, "type").getEnumConstants();
        if (universe == null || universe.length != cardinality) {
            throw new IllegalArgumentException("enum cardinality differs from vector");
        }
        return universe[get(index)];
    }

    public <E extends Enum<E>> OrdinalVector withEnum(int index, E value) {
        Objects.requireNonNull(value, "value");
        if (value.getDeclaringClass().getEnumConstants().length != cardinality) {
            throw new IllegalArgumentException("enum cardinality differs from vector");
        }
        return with(index, value.ordinal());
    }

    public long logicalPayloadBytes() {
        return switch (width()) {
            case CONSTANT -> 0L;
            case BIT -> ((long[]) storage).length * (long) Long.BYTES;
            case U8 -> ((byte[]) storage).length;
            case U16 -> ((short[]) storage).length * (long) Short.BYTES;
            case I32 -> ((int[]) storage).length * (long) Integer.BYTES;
        };
    }

    private static void requireShape(int cardinality, int size) {
        if (cardinality < 1) throw new IllegalArgumentException("cardinality must be positive");
        if (size < 0) throw new IllegalArgumentException("size must be nonnegative");
    }

    private static void requireOrdinal(int cardinality, int ordinal) {
        if (ordinal < 0 || ordinal >= cardinality) {
            throw new IllegalArgumentException("ordinal outside universe: " + ordinal);
        }
    }
}
