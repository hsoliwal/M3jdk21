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
import java.util.function.ToIntFunction;

/**
 * Shared enum schema that stores enum values as the smallest unsigned primitive code.
 *
 * <p>The enum object is materialized only at the API boundary. Hot state should retain
 * the numeric code, not a reference to the enum constant.
 *
 * <p>{@link #of(Class)} uses declaration ordinal and is intended for runtime-local layout.
 * Persisted/wire ABIs should use {@link #stable(Class, ToIntFunction)} with explicit codes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumCodec.java}
 * blob {@code a34ad8c2901d992eb9a120e2df0c7dd8d6c3d1dd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumCodec<E extends Enum<E>> {
    private static final ClassValue<EnumCodec<?>> ORDINAL_CODECS = new ClassValue<>() {
        @Override
        protected EnumCodec<?> computeValue(Class<?> type) {
            if (!type.isEnum()) {
                throw new IllegalArgumentException("Not an enum type: " + type.getName());
            }
            @SuppressWarnings({"rawtypes", "unchecked"})
            EnumCodec<?> codec = new EnumCodec(
                    type.asSubclass(Enum.class),
                    (ToIntFunction<Enum>) Enum::ordinal);
            return codec;
        }
    };

    public enum Width {
        U8(1),
        U16(2),
        I32(4);

        private final int bytes;

        Width(int bytes) {
            this.bytes = bytes;
        }

        public int bytes() {
            return bytes;
        }
    }

    private final Class<E> type;
    private final E[] constants;
    private final int[] codes;
    private final Width width;

    private EnumCodec(Class<E> type, ToIntFunction<E> encoder) {
        this.type = Objects.requireNonNull(type, "type");
        this.constants = type.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("Enum type must have at least one constant");
        }
        Objects.requireNonNull(encoder, "encoder");
        this.codes = new int[constants.length];
        int maximum = 0;
        for (int i = 0; i < constants.length; i++) {
            int code = encoder.applyAsInt(constants[i]);
            if (code < 0) {
                throw new IllegalArgumentException("Enum code must be >= 0: " + constants[i]);
            }
            for (int previous = 0; previous < i; previous++) {
                if (codes[previous] == code) {
                    throw new IllegalArgumentException("Duplicate enum code: " + code);
                }
            }
            codes[i] = code;
            maximum = Math.max(maximum, code);
        }
        this.width = maximum <= 255
                ? Width.U8
                : maximum <= 65_535 ? Width.U16 : Width.I32;
    }

    /** Runtime-local ordinal encoding, cached once per enum class. */
    public static <E extends Enum<E>> EnumCodec<E> of(Class<E> type) {
        Objects.requireNonNull(type, "type");
        @SuppressWarnings("unchecked")
        EnumCodec<E> codec = (EnumCodec<E>) ORDINAL_CODECS.get(type);
        return codec;
    }

    /** Explicit stable encoding suitable for versioned persistence when the caller freezes the code ABI. */
    public static <E extends Enum<E>> EnumCodec<E> stable(
            Class<E> type,
            ToIntFunction<E> encoder) {
        return new EnumCodec<>(type, encoder);
    }

    public Class<E> type() {
        return type;
    }

    public int cardinality() {
        return constants.length;
    }

    public Width width() {
        return width;
    }

    public int code(E value) {
        E checked = Objects.requireNonNull(value, "value");
        if (checked.getDeclaringClass() != type) {
            throw new IllegalArgumentException("Wrong enum type: " + checked.getDeclaringClass().getName());
        }
        return codes[checked.ordinal()];
    }

    public byte codeU8(E value) {
        if (width != Width.U8) {
            throw new IllegalStateException("Enum requires " + width);
        }
        return (byte) code(value);
    }

    public short codeU16(E value) {
        if (width == Width.I32) {
            throw new IllegalStateException("Enum requires I32");
        }
        return (short) code(value);
    }

    public E value(int code) {
        if (code < 0) {
            throw new IndexOutOfBoundsException("enum code: " + code);
        }
        for (int i = 0; i < codes.length; i++) {
            if (codes[i] == code) {
                return constants[i];
            }
        }
        throw new IndexOutOfBoundsException("enum code: " + code);
    }

    public E value(byte unsignedCode) {
        return value(Byte.toUnsignedInt(unsignedCode));
    }

    public E value(short unsignedCode) {
        return value(Short.toUnsignedInt(unsignedCode));
    }
}
