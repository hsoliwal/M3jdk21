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
 * Immutable 128-bit context UID -> primitive int index/offset map.
 *
 * <p>Designed for page numbers, slots, graph indexes and adjacency offsets.
 * No UUID, ContextUid, Integer, Map.Entry or node object is retained per mapping.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidIntMap.java}
 * blob {@code ea51f64ba6ad235dd333a8e9018463e65febcc3d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidIntMap {
    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 5;
    private static final int LOAD_DENOMINATOR = 8;

    private static final long[] EMPTY_LONGS = new long[0];
    private static final int[] EMPTY_INTS = new int[0];

    private final long[] high;
    private final long[] low;
    private final int[] values;
    private final long[] occupied;
    private final int size;

    private UidIntMap(
            long[] high,
            long[] low,
            int[] values,
            long[] occupied,
            int size) {
        this.high = high;
        this.low = low;
        this.values = values;
        this.occupied = occupied;
        this.size = size;
    }

    public static UidIntMap empty() {
        return new UidIntMap(
                EMPTY_LONGS,
                EMPTY_LONGS,
                EMPTY_INTS,
                EMPTY_LONGS,
                0);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(
            long uidHigh,
            long uidLow) {
        return find(uidHigh, uidLow) >= 0;
    }

    public int getOrDefault(
            long uidHigh,
            long uidLow,
            int fallback) {
        int slot = find(uidHigh, uidLow);
        return slot < 0
                ? fallback
                : values[slot];
    }

    public UidIntMap with(
            long uidHigh,
            long uidLow,
            int value) {
        int existing = find(uidHigh, uidLow);
        if (existing >= 0) {
            if (values[existing] == value) {
                return this;
            }
            int[] nextValues = values.clone();
            nextValues[existing] = value;
            return new UidIntMap(
                    high,
                    low,
                    nextValues,
                    occupied,
                    size);
        }

        int capacity = high.length == 0
                ? MIN_CAPACITY
                : high.length;
        if (size != 0
                && (size + 1) * LOAD_DENOMINATOR
                > capacity * LOAD_NUMERATOR) {
            capacity <<= 1;
        }
        UidIntMap next =
                create(capacity, size + 1);
        rehashInto(
                next.high,
                next.low,
                next.values,
                next.occupied);
        insert(
                next.high,
                next.low,
                next.values,
                next.occupied,
                uidHigh,
                uidLow,
                value);
        return next;
    }

    public UidIntMap without(
            long uidHigh,
            long uidLow) {
        if (find(uidHigh, uidLow) < 0) {
            return this;
        }
        if (size == 1) {
            return empty();
        }
        int capacity = high.length;
        if (capacity > MIN_CAPACITY
                && (size - 1) * LOAD_DENOMINATOR
                < (capacity >>> 1) * LOAD_NUMERATOR) {
            capacity >>>= 1;
        }
        UidIntMap next =
                create(capacity, size - 1);
        forEach((h, l, value) -> {
            if (h != uidHigh || l != uidLow) {
                insert(
                        next.high,
                        next.low,
                        next.values,
                        next.occupied,
                        h,
                        l,
                        value);
            }
        });
        return next;
    }

    public void forEach(UidIntConsumer consumer) {
        UidIntConsumer checked =
                Objects.requireNonNull(consumer, "consumer");
        for (int word = 0; word < occupied.length; word++) {
            long bits = occupied[word];
            while (bits != 0L) {
                int bit = Long.numberOfTrailingZeros(bits);
                int slot = (word << 6) + bit;
                checked.accept(
                        high[slot],
                        low[slot],
                        values[slot]);
                bits &= bits - 1L;
            }
        }
    }

    @FunctionalInterface
    public interface UidIntConsumer {
        void accept(long high, long low, int value);
    }

    private int find(
            long uidHigh,
            long uidLow) {
        if (size == 0) {
            return -1;
        }
        int mask = high.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while (true) {
            if (!occupied(slot)) {
                return -1;
            }
            if (high[slot] == uidHigh
                    && low[slot] == uidLow) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
    }

    private boolean occupied(int slot) {
        return (occupied[slot >>> 6]
                & (1L << (slot & 63))) != 0L;
    }

    private void rehashInto(
            long[] targetHigh,
            long[] targetLow,
            int[] targetValues,
            long[] targetOccupied) {
        forEach((h, l, value) -> insert(
                targetHigh,
                targetLow,
                targetValues,
                targetOccupied,
                h,
                l,
                value));
    }

    private static UidIntMap create(
            int capacity,
            int size) {
        if (capacity < MIN_CAPACITY
                || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException(
                    "capacity must be power of two >= 8");
        }
        return new UidIntMap(
                new long[capacity],
                new long[capacity],
                new int[capacity],
                new long[(capacity + 63) >>> 6],
                size);
    }

    private static void insert(
            long[] high,
            long[] low,
            int[] values,
            long[] occupied,
            long uidHigh,
            long uidLow,
            int value) {
        int mask = high.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while ((occupied[slot >>> 6]
                & (1L << (slot & 63))) != 0L) {
            slot = (slot + 1) & mask;
        }
        high[slot] = uidHigh;
        low[slot] = uidLow;
        values[slot] = value;
        occupied[slot >>> 6] |=
                1L << (slot & 63);
    }

    private static int mix(
            long high,
            long low) {
        long value = high ^ Long.rotateLeft(low, 31);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) value;
    }
}
