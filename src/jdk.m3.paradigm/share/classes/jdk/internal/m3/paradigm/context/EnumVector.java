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
import java.util.Optional;

/**
 * Optional enum-valued slots backed by one primitive array chosen from byte, short or int.
 *
 * <p>At most 64 logical slots are supported so presence remains one long. The enum
 * constants themselves are not retained per slot.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumVector.java}
 * blob {@code e1b8d852646932100f8d6a4b15545f82317939b6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumVector<E extends Enum<E>> {
    private final EnumCodec<E> codec;
    private final Object codes;
    private final long present;

    private EnumVector(EnumCodec<E> codec, Object codes, long present) {
        this.codec = codec;
        this.codes = codes;
        this.present = present;
    }

    public static <E extends Enum<E>> EnumVector<E> empty(
            EnumCodec<E> codec,
            int slots) {
        EnumCodec<E> checked = Objects.requireNonNull(codec, "codec");
        if (slots < 0 || slots > Long.SIZE) {
            throw new IllegalArgumentException("slots must be in [0,64]");
        }
        Object array = switch (checked.width()) {
            case U8 -> new byte[slots];
            case U16 -> new short[slots];
            case I32 -> new int[slots];
        };
        return new EnumVector<>(checked, array, 0L);
    }

    public int slots() {
        return switch (codec.width()) {
            case U8 -> ((byte[]) codes).length;
            case U16 -> ((short[]) codes).length;
            case I32 -> ((int[]) codes).length;
        };
    }

    public int size() {
        return Long.bitCount(present);
    }

    public boolean contains(int slot) {
        Objects.checkIndex(slot, slots());
        return (present & (1L << slot)) != 0L;
    }

    public Optional<E> get(int slot) {
        Objects.checkIndex(slot, slots());
        return contains(slot) ? Optional.of(codec.value(code(slot))) : Optional.empty();
    }

    public EnumVector<E> with(int slot, E value) {
        Objects.checkIndex(slot, slots());
        int encoded = codec.code(Objects.requireNonNull(value, "value"));
        long bit = 1L << slot;
        if ((present & bit) != 0L && code(slot) == encoded) {
            return this;
        }
        Object copy = switch (codec.width()) {
            case U8 -> {
                byte[] values = ((byte[]) codes).clone();
                values[slot] = (byte) encoded;
                yield values;
            }
            case U16 -> {
                short[] values = ((short[]) codes).clone();
                values[slot] = (short) encoded;
                yield values;
            }
            case I32 -> {
                int[] values = ((int[]) codes).clone();
                values[slot] = encoded;
                yield values;
            }
        };
        return new EnumVector<>(codec, copy, present | bit);
    }

    public EnumVector<E> without(int slot) {
        Objects.checkIndex(slot, slots());
        long bit = 1L << slot;
        return (present & bit) == 0L
                ? this
                : new EnumVector<>(codec, codes, present & ~bit);
    }

    public EnumCodec<E> codec() {
        return codec;
    }

    private int code(int slot) {
        return switch (codec.width()) {
            case U8 -> Byte.toUnsignedInt(((byte[]) codes)[slot]);
            case U16 -> Short.toUnsignedInt(((short[]) codes)[slot]);
            case I32 -> ((int[]) codes)[slot];
        };
    }
}
