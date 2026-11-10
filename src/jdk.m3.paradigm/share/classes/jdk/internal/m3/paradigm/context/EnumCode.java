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
 * Primitive enum encoding for resident ContextOS state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumCode.java}
 * blob {@code 5143a7134d8807cabfe84403a97039f3d5b57803} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumCode {
    /** Stable-code contract for persisted enums. Declaration ordinal is not implied to be persistence ABI. */
    public interface Stable {
        int contextCode();
    }

    private static final ClassValue<Enum<?>[]> UNIVERSES = new ClassValue<>() {
        @Override
        protected Enum<?>[] computeValue(Class<?> type) {
            Object[] constants = type.getEnumConstants();
            if (constants == null) throw new IllegalArgumentException("not an enum type: " + type.getName());
            return (Enum<?>[]) constants;
        }
    };

    private EnumCode() {}

    /** Smallest Java primitive width capable of representing every ordinal in the enum. */
    public static int storageBytes(Class<? extends Enum<?>> type) {
        int count = universe(type).length;
        if (count <= 0x100) return Byte.BYTES;
        if (count <= 0x1_0000) return Short.BYTES;
        return Integer.BYTES;
    }

    public static <E extends Enum<E>> byte toByte(E value) {
        E checked = Objects.requireNonNull(value, "value");
        requireCapacity(checked.getDeclaringClass(), 0x100);
        return (byte) checked.ordinal();
    }

    public static <E extends Enum<E>> short toShort(E value) {
        E checked = Objects.requireNonNull(value, "value");
        requireCapacity(checked.getDeclaringClass(), 0x1_0000);
        return (short) checked.ordinal();
    }

    public static <E extends Enum<E>> int toInt(E value) {
        return Objects.requireNonNull(value, "value").ordinal();
    }

    public static <E extends Enum<E>> E fromByte(Class<E> type, byte code) {
        return constant(type, Byte.toUnsignedInt(code));
    }

    public static <E extends Enum<E>> E fromShort(Class<E> type, short code) {
        return constant(type, Short.toUnsignedInt(code));
    }

    public static <E extends Enum<E>> E fromInt(Class<E> type, int code) {
        return constant(type, code);
    }

    /** Explicit persisted-code decoding; values must define unique non-negative codes. */
    public static <E extends Enum<E> & Stable> E fromStableCode(Class<E> type, int code) {
        if (code < 0) throw new IllegalArgumentException("stable enum code must be >= 0");
        E match = null;
        for (E value : type.getEnumConstants()) {
            if (value.contextCode() == code) {
                if (match != null) throw new IllegalArgumentException("duplicate stable enum code: " + code);
                match = value;
            }
        }
        if (match == null) throw new IllegalArgumentException("unknown stable enum code: " + code);
        return match;
    }

    public static int stableCode(Stable value) {
        int code = Objects.requireNonNull(value, "value").contextCode();
        if (code < 0) throw new IllegalArgumentException("stable enum code must be >= 0");
        return code;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E constant(Class<E> type, int ordinal) {
        Enum<?>[] values = universe(type);
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("enum code out of range: " + ordinal);
        }
        return (E) values[ordinal];
    }

    private static Enum<?>[] universe(Class<? extends Enum<?>> type) {
        return UNIVERSES.get(Objects.requireNonNull(type, "type"));
    }

    private static void requireCapacity(Class<? extends Enum<?>> type, int maximum) {
        if (universe(type).length > maximum) {
            throw new IllegalArgumentException("enum requires wider primitive storage: " + type.getName());
        }
    }
}
