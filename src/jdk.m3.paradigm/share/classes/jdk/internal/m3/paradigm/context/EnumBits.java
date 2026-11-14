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

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * One-long enum-set algebra for enum domains of at most 64 constants.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumBits.java}
 * blob {@code afc57b05b0c48de61b9ef26613c35050af1ecf02} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumBits {
    private EnumBits() {
    }

    public static <E extends Enum<E>> long bit(E value) {
        E checked = Objects.requireNonNull(value, "value");
        E[] constants = checked.getDeclaringClass().getEnumConstants();
        if (constants.length > Long.SIZE) {
            throw new IllegalArgumentException("Enum cardinality exceeds 64: "
                    + checked.getDeclaringClass().getName());
        }
        return 1L << checked.ordinal();
    }

    public static <E extends Enum<E>> long add(long bits, E value) {
        return bits | bit(value);
    }

    public static <E extends Enum<E>> long remove(long bits, E value) {
        return bits & ~bit(value);
    }

    public static <E extends Enum<E>> boolean contains(long bits, E value) {
        return (bits & bit(value)) != 0L;
    }

    public static int size(long bits) {
        return Long.bitCount(bits);
    }

    public static boolean containsAll(long bits, long required) {
        return (bits & required) == required;
    }

    public static boolean containsAny(long bits, long candidates) {
        return (bits & candidates) != 0L;
    }

    public static <E extends Enum<E>> long of(Set<E> values) {
        Objects.requireNonNull(values, "values");
        long bits = 0L;
        for (E value : values) {
            bits |= bit(value);
        }
        return bits;
    }

    /** EnumSet is materialized only at the compatibility/API boundary. */
    public static <E extends Enum<E>> Set<E> toSet(Class<E> type, long bits) {
        E[] constants = constants(type);
        long allowed = constants.length == Long.SIZE ? -1L : (1L << constants.length) - 1L;
        if ((bits & ~allowed) != 0L) {
            throw new IllegalArgumentException("Bits outside enum domain");
        }
        EnumSet<E> result = EnumSet.noneOf(type);
        for (E value : constants) {
            if ((bits & (1L << value.ordinal())) != 0L) {
                result.add(value);
            }
        }
        return Set.copyOf(result);
    }

    /** Boundary traversal in declaration order without retaining an EnumSet. */
    public static <E extends Enum<E>> void forEach(
            Class<E> type,
            long bits,
            Consumer<? super E> consumer) {
        E[] constants = constants(type);
        Objects.requireNonNull(consumer, "consumer");
        long remaining = bits;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            if (ordinal >= constants.length) {
                throw new IllegalArgumentException("Bits outside enum domain");
            }
            consumer.accept(constants[ordinal]);
            remaining &= remaining - 1;
        }
    }

    private static <E extends Enum<E>> E[] constants(Class<E> type) {
        Objects.requireNonNull(type, "type");
        E[] constants = type.getEnumConstants();
        if (constants == null || constants.length > Long.SIZE) {
            throw new IllegalArgumentException("Enum cardinality exceeds 64: " + type.getName());
        }
        return constants;
    }
}
