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

import java.util.Arrays;
import java.util.Objects;

/**
 * Sparse immutable object values for a shared 64-slot schema.
 *
 * <p>No keys are retained per instance. The presence word determines the packed
 * value position through a population count. Null values are intentionally not
 * supported so absence remains unambiguous.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotValues.java}
 * blob {@code 457f065cf6afa8432cb33a45e6695336679f7014} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SlotValues<V> {
    @FunctionalInterface
    public interface SlotConsumer<V> {
        void accept(int slot, V value);
    }

    private static final SlotValues<?> EMPTY = new SlotValues<>(0L, new Object[0]);

    private final long present;
    private final Object[] values;

    private SlotValues(long present, Object[] values) {
        this.present = present;
        this.values = values;
    }

    @SuppressWarnings("unchecked")
    public static <V> SlotValues<V> empty() {
        return (SlotValues<V>) EMPTY;
    }

    public long presentMask() {
        return present;
    }

    public int size() {
        return values.length;
    }

    public boolean isEmpty() {
        return present == 0L;
    }

    public boolean contains(int slot) {
        return (present & bit(slot)) != 0L;
    }

    @SuppressWarnings("unchecked")
    public V getOrNull(int slot) {
        long bit = bit(slot);
        return (present & bit) == 0L ? null : (V) values[rank(present, bit)];
    }

    public V require(int slot) {
        V value = getOrNull(slot);
        if (value == null) {
            throw new IllegalArgumentException("context slot is absent: " + slot);
        }
        return value;
    }

    public SlotValues<V> with(int slot, V value) {
        Objects.requireNonNull(value, "value");
        long bit = bit(slot);
        int at = rank(present, bit);
        if ((present & bit) != 0L) {
            if (Objects.equals(values[at], value)) {
                return this;
            }
            Object[] next = values.clone();
            next[at] = value;
            return new SlotValues<>(present, next);
        }
        Object[] next = new Object[values.length + 1];
        System.arraycopy(values, 0, next, 0, at);
        next[at] = value;
        System.arraycopy(values, at, next, at + 1, values.length - at);
        return new SlotValues<>(present | bit, next);
    }

    public SlotValues<V> without(int slot) {
        long bit = bit(slot);
        if ((present & bit) == 0L) {
            return this;
        }
        int at = rank(present, bit);
        if (values.length == 1) {
            return empty();
        }
        Object[] next = new Object[values.length - 1];
        System.arraycopy(values, 0, next, 0, at);
        System.arraycopy(values, at + 1, next, at, values.length - at - 1);
        return new SlotValues<>(present & ~bit, next);
    }

    @SuppressWarnings("unchecked")
    public void forEach(SlotConsumer<? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long remaining = present;
        int index = 0;
        while (remaining != 0L) {
            int slot = Long.numberOfTrailingZeros(remaining);
            consumer.accept(slot, (V) values[index++]);
            remaining &= remaining - 1L;
        }
    }

    public Object[] valuesCopy() {
        return values.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof SlotValues<?> slots
                && present == slots.present
                && Arrays.equals(values, slots.values);
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(present) + Arrays.hashCode(values);
    }

    private static int rank(long mask, long bit) {
        return Long.bitCount(mask & (bit - 1L));
    }

    private static long bit(int slot) {
        if (slot < 0 || slot >= Long.SIZE) {
            throw new IndexOutOfBoundsException("slot must be in [0,63]");
        }
        return 1L << slot;
    }
}
