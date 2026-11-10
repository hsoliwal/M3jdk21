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
import java.util.function.BiConsumer;

/**
 * Immutable primitive 128-bit UID -> reference table.
 *
 * <p>UIDs occupy parallel long lanes. Occupancy is a bitset; no UUID, ContextUid,
 * Map.Entry or hash-node object is retained per mapping.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidMap.java}
 * blob {@code 64c0f1a3d661b45182bb99cfd11576535660382b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidMap<V> {
    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 5;
    private static final int LOAD_DENOMINATOR = 8;

    private final long[] high;
    private final long[] low;
    private final Object[] values;
    private final long[] occupied;
    private final int size;

    private UidMap(long[] high, long[] low, Object[] values, long[] occupied, int size) {
        this.high = high;
        this.low = low;
        this.values = values;
        this.occupied = occupied;
        this.size = size;
    }

    public static <V> UidMap<V> empty() {
        return create(MIN_CAPACITY, 0);
    }

    public int size() {
        return size;
    }

    public boolean contains(long uidHigh, long uidLow) {
        return find(uidHigh, uidLow) >= 0;
    }

    @SuppressWarnings("unchecked")
    public Optional<V> get(long uidHigh, long uidLow) {
        int slot = find(uidHigh, uidLow);
        return slot < 0 ? Optional.empty() : Optional.of((V) values[slot]);
    }

    public UidMap<V> with(long uidHigh, long uidLow, V value) {
        V checked = Objects.requireNonNull(value, "value");
        int existing = find(uidHigh, uidLow);
        if (existing >= 0) {
            if (Objects.equals(values[existing], checked)) return this;
            Object[] nextValues = values.clone();
            nextValues[existing] = checked;
            return new UidMap<>(high, low, nextValues, occupied, size);
        }
        int capacity = high.length;
        if ((size + 1) * LOAD_DENOMINATOR > capacity * LOAD_NUMERATOR) {
            capacity <<= 1;
        }
        UidMap<V> next = create(capacity, size + 1);
        rehashInto(next.high, next.low, next.values, next.occupied);
        insert(next.high, next.low, next.values, next.occupied, uidHigh, uidLow, checked);
        return next;
    }

    public UidMap<V> without(long uidHigh, long uidLow) {
        if (find(uidHigh, uidLow) < 0) return this;
        int capacity = high.length;
        if (capacity > MIN_CAPACITY
                && (size - 1) * LOAD_DENOMINATOR < (capacity >>> 1) * LOAD_NUMERATOR) {
            capacity >>>= 1;
        }
        UidMap<V> next = create(capacity, size - 1);
        forEach((h, l, value) -> {
            if (h != uidHigh || l != uidLow) {
                insert(next.high, next.low, next.values, next.occupied, h, l, value);
            }
        });
        return next;
    }

    public void forEach(UidBiConsumer<? super V> consumer) {
        UidBiConsumer<? super V> checked = Objects.requireNonNull(consumer, "consumer");
        for (int word = 0; word < occupied.length; word++) {
            long bits = occupied[word];
            while (bits != 0L) {
                int bit = Long.numberOfTrailingZeros(bits);
                int slot = (word << 6) + bit;
                @SuppressWarnings("unchecked") V value = (V) values[slot];
                checked.accept(high[slot], low[slot], value);
                bits &= bits - 1;
            }
        }
    }

    @FunctionalInterface
    public interface UidBiConsumer<V> {
        void accept(long high, long low, V value);
    }

    private int find(long uidHigh, long uidLow) {
        int mask = high.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while (true) {
            if (!occupied(slot)) return -1;
            if (high[slot] == uidHigh && low[slot] == uidLow) return slot;
            slot = (slot + 1) & mask;
        }
    }

    private boolean occupied(int slot) {
        return (occupied[slot >>> 6] & (1L << (slot & 63))) != 0L;
    }

    private void rehashInto(long[] targetHigh, long[] targetLow, Object[] targetValues, long[] targetOccupied) {
        forEach((h, l, value) -> insert(targetHigh, targetLow, targetValues, targetOccupied, h, l, value));
    }

    private static <V> UidMap<V> create(int capacity, int size) {
        if (capacity < MIN_CAPACITY || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two >= 8");
        }
        return new UidMap<>(
                new long[capacity],
                new long[capacity],
                new Object[capacity],
                new long[(capacity + 63) >>> 6],
                size);
    }

    private static void insert(
            long[] high,
            long[] low,
            Object[] values,
            long[] occupied,
            long uidHigh,
            long uidLow,
            Object value) {
        int mask = high.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while ((occupied[slot >>> 6] & (1L << (slot & 63))) != 0L) {
            slot = (slot + 1) & mask;
        }
        high[slot] = uidHigh;
        low[slot] = uidLow;
        values[slot] = value;
        occupied[slot >>> 6] |= 1L << (slot & 63);
    }

    private static int mix(long high, long low) {
        long value = high ^ Long.rotateLeft(low, 31);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) value;
    }
}
