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
 * Type-safe storage descriptor. Primitive methods use unsigned raw bits of the native width.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/StorageType.java}
 * blob {@code 19f594368ab15607de477ccd4cd986b7d8892574} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StorageType<T> {
    enum Kind { OBJECT, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, BOOLEAN, PRESENCE }
    public static final StorageType<Byte> BYTE = new StorageType<>(Kind.BYTE, Byte.class);
    public static final StorageType<Short> SHORT = new StorageType<>(Kind.SHORT, Short.class);
    public static final StorageType<Character> CHAR = new StorageType<>(Kind.CHAR, Character.class);
    public static final StorageType<Integer> INT = new StorageType<>(Kind.INT, Integer.class);
    public static final StorageType<Long> LONG = new StorageType<>(Kind.LONG, Long.class);
    public static final StorageType<Float> FLOAT = new StorageType<>(Kind.FLOAT, Float.class);
    public static final StorageType<Double> DOUBLE = new StorageType<>(Kind.DOUBLE, Double.class);
    public static final StorageType<Boolean> BOOLEAN = new StorageType<>(Kind.BOOLEAN, Boolean.class);
    static final StorageType<Boolean> PRESENCE = new StorageType<>(Kind.PRESENCE, Boolean.class);
    private static final StorageType<Object> OBJECT = new StorageType<>(Kind.OBJECT, Object.class);
    final Kind kind;
    private final Class<?> wrapper;

    private StorageType(Kind kind, Class<?> wrapper) {
        this.kind = kind;
        this.wrapper = wrapper;
    }

    @SuppressWarnings("unchecked")
    public static <T> StorageType<T> objects() { return (StorageType<T>) OBJECT; }
    public boolean isPrimitive() { return kind != Kind.OBJECT && kind != Kind.PRESENCE; }
    @Override public String toString() { return kind.name(); }

    void validate(Object value) {
        if (value != null && !wrapper.isInstance(value)) {
            throw new ClassCastException("Expected " + wrapper.getName());
        }
    }
    boolean accepts(Object value) { return value == null || wrapper.isInstance(value); }
    void checkBits(long bits) {
        switch (kind) {
            case BYTE -> width(bits, 0xffL);
            case SHORT, CHAR -> width(bits, 0xffffL);
            case INT, FLOAT -> width(bits, 0xffff_ffffL);
            case BOOLEAN -> width(bits, 1);
            case LONG, DOUBLE -> { }
            default -> throw new UnsupportedOperationException("Not primitive storage");
        }
    }
    private static void width(long bits, long mask) {
        if ((bits & ~mask) != 0) throw new IllegalArgumentException("Bits exceed storage width");
    }
    long toBits(Object value) {
        Objects.requireNonNull(value, "null has no raw bits");
        return switch (kind) {
            case BYTE -> ((Byte) value).byteValue() & 0xffL;
            case SHORT -> ((Short) value).shortValue() & 0xffffL;
            case CHAR -> ((Character) value).charValue();
            case INT -> Integer.toUnsignedLong((Integer) value);
            case LONG -> (Long) value;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits((Float) value));
            case DOUBLE -> Double.doubleToRawLongBits((Double) value);
            case BOOLEAN -> (Boolean) value ? 1 : 0;
            default -> throw new UnsupportedOperationException("Not primitive storage");
        };
    }
    @SuppressWarnings("unchecked")
    T box(long bits) {
        Object value = switch (kind) {
            case BYTE -> Byte.valueOf((byte) bits);
            case SHORT -> Short.valueOf((short) bits);
            case CHAR -> Character.valueOf((char) bits);
            case INT -> Integer.valueOf((int) bits);
            case LONG -> Long.valueOf(bits);
            case FLOAT -> Float.valueOf(Float.intBitsToFloat((int) bits));
            case DOUBLE -> Double.valueOf(Double.longBitsToDouble(bits));
            case BOOLEAN, PRESENCE -> Boolean.valueOf(bits != 0);
            case OBJECT -> throw new UnsupportedOperationException("Object storage");
        };
        return (T) value;
    }
    long canonical(long bits) {
        return switch (kind) {
            case FLOAT -> Integer.toUnsignedLong(Float.floatToIntBits(Float.intBitsToFloat((int) bits)));
            case DOUBLE -> Double.doubleToLongBits(Double.longBitsToDouble(bits));
            default -> bits;
        };
    }
    int hashBits(long bits) {
        return switch (kind) {
            case BYTE -> (byte) bits;
            case SHORT -> (short) bits;
            case CHAR -> (char) bits;
            case INT -> (int) bits;
            case LONG, DOUBLE -> Long.hashCode(canonical(bits));
            case FLOAT -> (int) canonical(bits);
            case BOOLEAN -> bits == 0 ? 1237 : 1231;
            default -> throw new UnsupportedOperationException("Not primitive storage");
        };
    }
}
