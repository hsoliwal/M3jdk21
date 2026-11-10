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
 * Node-free table keyed by the two primitive words of a context UID.
 *
 * <p>The mutable runtime API and persistent copy-on-write convenience API share
 * one storage owner. Null values are unsupported and mark empty slots.
 *
 * <p>Exactly four instance fields are retained: two primitive UID lanes, one
 * value-reference lane, and size. No ContextUid, UUID, Map.Entry, node or
 * tombstone object is retained per mapping.
 *
 * <p>This class is deliberately not thread-safe. Concurrent publication belongs
 * to the owning context/runtime.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidTable.java}
 * blob {@code 96ab69a631b7d44bc98fb29b8b1f2acdf039868b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidTable<V> {
    @FunctionalInterface
    public interface EntryConsumer<V> {
        void accept(long high, long low, V value);
    }

    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 5;
    private static final int LOAD_DENOMINATOR = 8;

    private long[] high;
    private long[] low;
    private Object[] values;
    private int size;

    public UidTable() {
        this(0);
    }

    public UidTable(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize must be >= 0");
        }
        int capacity = MIN_CAPACITY;
        while ((long) expectedSize * LOAD_DENOMINATOR
                > (long) capacity * LOAD_NUMERATOR) {
            capacity <<= 1;
            if (capacity <= 0) {
                throw new IllegalArgumentException("expectedSize too large");
            }
        }
        high = new long[capacity];
        low = new long[capacity];
        values = new Object[capacity];
    }

    private UidTable(
            long[] high,
            long[] low,
            Object[] values,
            int size) {
        this.high = high;
        this.low = low;
        this.values = values;
        this.size = size;
    }

    /** Persistent-style empty table convenience retained from PR #3283. */
    public static <V> UidTable<V> empty() {
        return new UidTable<>();
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return values.length;
    }

    public boolean contains(long uidHigh, long uidLow) {
        return find(uidHigh, uidLow) >= 0;
    }

    @SuppressWarnings("unchecked")
    public V get(long uidHigh, long uidLow) {
        int slot = find(uidHigh, uidLow);
        return slot < 0 ? null : (V) values[slot];
    }

    /**
     * Mutable runtime update.
     *
     * @return previous value or null when absent
     */
    @SuppressWarnings("unchecked")
    public V put(long uidHigh, long uidLow, V value) {
        Objects.requireNonNull(value, "value");
        int existing = find(uidHigh, uidLow);
        if (existing >= 0) {
            V old = (V) values[existing];
            values[existing] = value;
            return old;
        }
        ensureInsertCapacity();
        insertNew(uidHigh, uidLow, value);
        return null;
    }

    /**
     * Persistent convenience: returns a changed copy without mutating this table.
     * Complete no-ops return this instance.
     */
    public UidTable<V> with(
            long uidHigh,
            long uidLow,
            V value) {
        Objects.requireNonNull(value, "value");
        int existing = find(uidHigh, uidLow);
        if (existing >= 0
                && Objects.equals(values[existing], value)) {
            return this;
        }
        UidTable<V> copy = copy();
        copy.put(uidHigh, uidLow, value);
        return copy;
    }

    @SuppressWarnings("unchecked")
    public V remove(long uidHigh, long uidLow) {
        int slot = find(uidHigh, uidLow);
        if (slot < 0) {
            return null;
        }
        V old = (V) values[slot];
        deleteAt(slot);
        return old;
    }

    /**
     * Persistent convenience: returns a changed copy without mutating this table.
     * Absent removals return this instance.
     */
    public UidTable<V> without(
            long uidHigh,
            long uidLow) {
        if (find(uidHigh, uidLow) < 0) {
            return this;
        }
        UidTable<V> copy = copy();
        copy.remove(uidHigh, uidLow);
        return copy;
    }

    @SuppressWarnings("unchecked")
    public void forEach(EntryConsumer<? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value != null) {
                consumer.accept(high[i], low[i], (V) value);
            }
        }
    }

    public void clear() {
        Arrays.fill(high, 0L);
        Arrays.fill(low, 0L);
        Arrays.fill(values, null);
        size = 0;
        if (values.length != MIN_CAPACITY) {
            high = new long[MIN_CAPACITY];
            low = new long[MIN_CAPACITY];
            values = new Object[MIN_CAPACITY];
        }
    }

    private UidTable<V> copy() {
        return new UidTable<>(
                high.clone(),
                low.clone(),
                values.clone(),
                size);
    }

    private int find(long uidHigh, long uidLow) {
        int mask = values.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while (true) {
            Object value = values[slot];
            if (value == null) {
                return -1;
            }
            if (high[slot] == uidHigh
                    && low[slot] == uidLow) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void deleteAt(int slot) {
        values[slot] = null;
        high[slot] = 0L;
        low[slot] = 0L;
        size--;

        int mask = values.length - 1;
        int cursor = (slot + 1) & mask;
        while (values[cursor] != null) {
            long rehashHigh = high[cursor];
            long rehashLow = low[cursor];
            Object rehashValue = values[cursor];
            values[cursor] = null;
            high[cursor] = 0L;
            low[cursor] = 0L;
            size--;
            insertNew(rehashHigh, rehashLow, rehashValue);
            cursor = (cursor + 1) & mask;
        }
        maybeShrink();
    }

    private void ensureInsertCapacity() {
        if ((long) (size + 1) * LOAD_DENOMINATOR
                > (long) values.length * LOAD_NUMERATOR) {
            resize(values.length << 1);
        }
    }

    private void maybeShrink() {
        int capacity = values.length;
        if (capacity > MIN_CAPACITY
                && (long) size * LOAD_DENOMINATOR
                < (long) (capacity >>> 1) * LOAD_NUMERATOR) {
            resize(capacity >>> 1);
        }
    }

    private void resize(int capacity) {
        long[] oldHigh = high;
        long[] oldLow = low;
        Object[] oldValues = values;
        high = new long[capacity];
        low = new long[capacity];
        values = new Object[capacity];
        int oldSize = size;
        size = 0;
        for (int i = 0; i < oldValues.length; i++) {
            Object value = oldValues[i];
            if (value != null) {
                insertNew(oldHigh[i], oldLow[i], value);
            }
        }
        if (size != oldSize) {
            throw new IllegalStateException(
                    "context UID table resize lost entries");
        }
    }

    private void insertNew(
            long uidHigh,
            long uidLow,
            Object value) {
        int mask = values.length - 1;
        int slot = mix(uidHigh, uidLow) & mask;
        while (values[slot] != null) {
            slot = (slot + 1) & mask;
        }
        high[slot] = uidHigh;
        low[slot] = uidLow;
        values[slot] = value;
        size++;
    }

    private static int mix(long high, long low) {
        long value = high ^ Long.rotateLeft(low, 29);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) (value ^ (value >>> 32));
    }
}
