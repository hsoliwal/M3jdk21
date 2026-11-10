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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Immutable enum-keyed U8 table: one presence word plus one byte lane per declared enum constant.
 * No enum key, Integer wrapper, hash node, or missing-value sentinel is retained per mapping.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumByteTable.java}
 * blob {@code 49560fe884709ea7a246848e15a3487dcada8371} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumByteTable<E extends Enum<E>> {
    private final long present;
    private final byte[] values;

    private EnumByteTable(long present, byte[] values) {
        this.present = present;
        this.values = values;
    }

    public static <E extends Enum<E>> EnumByteTable<E> empty(Class<E> type) {
        E[] constants = Objects.requireNonNull(type, "type").getEnumConstants();
        if (constants.length > Long.SIZE) throw new IllegalArgumentException("enum exceeds 64-key table");
        return new EnumByteTable<>(0L, new byte[constants.length]);
    }

    public int capacity() {
        return values.length;
    }

    public int size() {
        return Long.bitCount(present);
    }

    public boolean isEmpty() {
        return present == 0L;
    }

    public long presenceBits() {
        return present;
    }

    public boolean contains(E key) {
        return (present & keyBit(key)) != 0L;
    }

    public int getOrDefault(E key, int fallback) {
        int ordinal = ordinal(key);
        return (present & (1L << ordinal)) == 0L ? fallback : Byte.toUnsignedInt(values[ordinal]);
    }

    public EnumByteTable<E> with(E key, int unsignedValue) {
        if ((unsignedValue & ~0xff) != 0) throw new IllegalArgumentException("value must be in [0,255]");
        int ordinal = ordinal(key);
        long bit = 1L << ordinal;
        byte encoded = (byte) unsignedValue;
        if ((present & bit) != 0L && values[ordinal] == encoded) return this;
        byte[] next = values.clone();
        next[ordinal] = encoded;
        return new EnumByteTable<>(present | bit, next);
    }

    public EnumByteTable<E> without(E key) {
        int ordinal = ordinal(key);
        long bit = 1L << ordinal;
        if ((present & bit) == 0L) return this;
        byte[] next = values.clone();
        next[ordinal] = 0;
        return new EnumByteTable<>(present & ~bit, next);
    }

    /** Allocation-free value sum over present U8 lanes. */
    public int sumUnsigned() {
        int sum = 0;
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            sum += Byte.toUnsignedInt(values[ordinal]);
            remaining &= remaining - 1;
        }
        return sum;
    }

    /** True when any present U8 value satisfies the primitive predicate. */
    public boolean anyValue(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            if (predicate.test(Byte.toUnsignedInt(values[ordinal]))) return true;
            remaining &= remaining - 1;
        }
        return false;
    }

    /** Packed selection mask; bit positions are enum ordinals. */
    public long selectAtLeast(int threshold) {
        if ((threshold & ~0xff) != 0) throw new IllegalArgumentException("threshold must be in [0,255]");
        long selected = 0L;
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            if (Byte.toUnsignedInt(values[ordinal]) >= threshold) selected |= 1L << ordinal;
            remaining &= remaining - 1;
        }
        return selected;
    }

    /** Direct enum/value iteration without materializing EnumMap, Map.Entry, or boxed Integer values. */
    public void forEach(Class<E> type, EntryConsumer<? super E> consumer) {
        E[] constants = Objects.requireNonNull(type, "type").getEnumConstants();
        if (constants.length != values.length) throw new IllegalArgumentException("enum schema mismatch");
        Objects.requireNonNull(consumer, "consumer");
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            consumer.accept(constants[ordinal], Byte.toUnsignedInt(values[ordinal]));
            remaining &= remaining - 1;
        }
    }

    @FunctionalInterface
    public interface EntryConsumer<E> {
        void accept(E key, int unsignedValue);
    }

    public Map<E, Integer> toMap(Class<E> type) {
        E[] constants = Objects.requireNonNull(type, "type").getEnumConstants();
        if (constants.length != values.length) throw new IllegalArgumentException("enum schema mismatch");
        EnumMap<E, Integer> result = new EnumMap<>(type);
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            result.put(constants[ordinal], Byte.toUnsignedInt(values[ordinal]));
            remaining &= remaining - 1;
        }
        return Collections.unmodifiableMap(result);
    }

    private int ordinal(E key) {
        int ordinal = Objects.requireNonNull(key, "key").ordinal();
        if (ordinal >= values.length) throw new IllegalArgumentException("enum schema mismatch");
        return ordinal;
    }

    private long keyBit(E key) {
        return 1L << ordinal(key);
    }
}
