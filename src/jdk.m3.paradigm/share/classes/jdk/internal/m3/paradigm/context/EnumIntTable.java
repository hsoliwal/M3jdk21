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
import java.util.function.IntPredicate;

/**
 * Enum-keyed int table: one presence word plus one primitive int lane per ordinal.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumIntTable.java}
 * blob {@code a237c8a82d952e0e7fb742c1a3b33bd364609d3c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumIntTable<E extends Enum<E>> {
    private final long present;
    private final int[] values;

    private EnumIntTable(long present, int[] values) {
        this.present = present;
        this.values = values;
    }

    public static <E extends Enum<E>> EnumIntTable<E> empty(Class<E> type) {
        E[] constants = Objects.requireNonNull(type, "type").getEnumConstants();
        if (constants.length > Long.SIZE) {
            throw new IllegalArgumentException("enum exceeds 64-key table");
        }
        return new EnumIntTable<>(0L, new int[constants.length]);
    }

    public int capacity() { return values.length; }
    public int size() { return Long.bitCount(present); }
    public long presenceBits() { return present; }
    public boolean contains(E key) { return (present & bit(key)) != 0L; }

    public int getOrDefault(E key, int fallback) {
        int ordinal = ordinal(key);
        return (present & (1L << ordinal)) == 0L ? fallback : values[ordinal];
    }

    public EnumIntTable<E> with(E key, int value) {
        int ordinal = ordinal(key);
        long bit = 1L << ordinal;
        if ((present & bit) != 0L && values[ordinal] == value) return this;
        int[] next = values.clone();
        next[ordinal] = value;
        return new EnumIntTable<>(present | bit, next);
    }

    public EnumIntTable<E> without(E key) {
        int ordinal = ordinal(key);
        long bit = 1L << ordinal;
        if ((present & bit) == 0L) return this;
        int[] next = values.clone();
        next[ordinal] = 0;
        return new EnumIntTable<>(present & ~bit, next);
    }

    public long select(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long selected = 0L;
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            if (predicate.test(values[ordinal])) selected |= 1L << ordinal;
            remaining &= remaining - 1L;
        }
        return selected;
    }

    public long sum() {
        long sum = 0L;
        long remaining = present;
        while (remaining != 0L) {
            int ordinal = Long.numberOfTrailingZeros(remaining);
            sum += values[ordinal];
            remaining &= remaining - 1L;
        }
        return sum;
    }

    private int ordinal(E key) {
        int ordinal = Objects.requireNonNull(key, "key").ordinal();
        if (ordinal >= values.length) throw new IllegalArgumentException("enum schema mismatch");
        return ordinal;
    }

    private long bit(E key) { return 1L << ordinal(key); }
}
