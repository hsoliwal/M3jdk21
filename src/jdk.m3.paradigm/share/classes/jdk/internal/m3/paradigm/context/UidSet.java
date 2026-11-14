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
 * Immutable primitive set for 128-bit context UIDs.
 *
 * <p>Identity uses parallel high/low long lanes plus occupancy bits. No UUID,
 * ContextUid, Set.Entry or node object is retained per member.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidSet.java}
 * blob {@code b8437b8e5f15894a0f1a5626bcc91ff6c8685398} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidSet {
    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 5;
    private static final int LOAD_DENOMINATOR = 8;

    private final long[] high;
    private final long[] low;
    private final long[] occupied;
    private final int size;

    private UidSet(long[] high, long[] low, long[] occupied, int size) {
        this.high = high;
        this.low = low;
        this.occupied = occupied;
        this.size = size;
    }

    public static UidSet empty() {
        return create(MIN_CAPACITY, 0);
    }

    public int size() {
        return size;
    }

    public boolean contains(long uidHigh, long uidLow) {
        return find(uidHigh, uidLow) >= 0;
    }

    public UidSet with(long uidHigh, long uidLow) {
        if (contains(uidHigh, uidLow)) return this;
        int capacity = high.length;
        if ((size + 1) * LOAD_DENOMINATOR > capacity * LOAD_NUMERATOR) {
            capacity <<= 1;
        }
        UidSet next = create(capacity, size + 1);
        forEach((h, l) -> insert(next.high, next.low, next.occupied, h, l));
        insert(next.high, next.low, next.occupied, uidHigh, uidLow);
        return next;
    }

    public UidSet without(long uidHigh, long uidLow) {
        if (!contains(uidHigh, uidLow)) return this;
        int capacity = high.length;
        if (capacity > MIN_CAPACITY
                && (size - 1) * LOAD_DENOMINATOR < (capacity >>> 1) * LOAD_NUMERATOR) {
            capacity >>>= 1;
        }
        UidSet next = create(capacity, size - 1);
        forEach((h, l) -> {
            if (h != uidHigh || l != uidLow) insert(next.high, next.low, next.occupied, h, l);
        });
        return next;
    }

    public void forEach(UidConsumer consumer) {
        UidConsumer checked = Objects.requireNonNull(consumer, "consumer");
        for (int word = 0; word < occupied.length; word++) {
            long bits = occupied[word];
            while (bits != 0L) {
                int bit = Long.numberOfTrailingZeros(bits);
                int slot = (word << 6) + bit;
                checked.accept(high[slot], low[slot]);
                bits &= bits - 1;
            }
        }
    }

    @FunctionalInterface
    public interface UidConsumer {
        void accept(long high, long low);
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

    private static UidSet create(int capacity, int size) {
        return new UidSet(
                new long[capacity],
                new long[capacity],
                new long[(capacity + 63) >>> 6],
                size);
    }

    private static void insert(
            long[] high,
            long[] low,
            long[] occupied,
            long uidHigh,
            long uidLow) {
        int mask = high.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while ((occupied[slot >>> 6] & (1L << (slot & 63))) != 0L) {
            slot = (slot + 1) & mask;
        }
        high[slot] = uidHigh;
        low[slot] = uidLow;
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
