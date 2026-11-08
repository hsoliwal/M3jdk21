/*******************************************************************************
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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Read-optimized concurrent long-to-long map using one interleaved primitive
 * array snapshot: {@code [key0,value0,key1,value1,...]} sorted by key.
 *
 * <p>Reads are lock-free binary searches. Writes copy the primitive array and
 * CAS-publish one replacement reference, so key/value state changes atomically
 * without a per-entry object or a compound two-array publication protocol.
 * Write cost is O(n); use packed open-addressed maps for write-heavy workloads.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/ConcurrentPackedLongLongMap.java}
 * blob {@code abce6eb22ce0674632329f039b07d7af1a5abe4f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3ConcurrentPackedLongLongMap {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public M3ConcurrentPackedLongLongMap() {
    }

    private static final long[] EMPTY = new long[0];
    private static final VarHandle ENTRIES;

    static {
        try {
            ENTRIES = MethodHandles.lookup().findVarHandle(
                    M3ConcurrentPackedLongLongMap.class, "entries", long[].class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @SuppressWarnings("unused")
    private volatile long[] entries = EMPTY;

    private long[] current() {
        return (long[]) ENTRIES.getAcquire(this);
    }

    public int size() {
        return current().length >>> 1;
    }

    public boolean isEmpty() {
        return current().length == 0;
    }

    public boolean containsKey(long key) {
        return find(current(), key) >= 0;
    }

    public long getOrDefault(long key, long fallback) {
        long[] snapshot = current();
        int index = find(snapshot, key);
        return index < 0 ? fallback : snapshot[(index << 1) + 1];
    }

    /**
     * Inserts or replaces a mapping.
     *
     * @return true when the observable mapping changed
     */
    public boolean put(long key, long value) {
        for (;;) {
            long[] before = current();
            int found = find(before, key);
            if (found >= 0) {
                int valueIndex = (found << 1) + 1;
                if (before[valueIndex] == value) return false;
                long[] after = before.clone();
                after[valueIndex] = value;
                if (ENTRIES.compareAndSet(this, before, after)) return true;
                continue;
            }

            int insertPair = -found - 1;
            int insert = insertPair << 1;
            long[] after = new long[before.length + 2];
            System.arraycopy(before, 0, after, 0, insert);
            after[insert] = key;
            after[insert + 1] = value;
            System.arraycopy(before, insert, after, insert + 2, before.length - insert);
            if (ENTRIES.compareAndSet(this, before, after)) return true;
        }
    }

    public boolean remove(long key) {
        for (;;) {
            long[] before = current();
            int found = find(before, key);
            if (found < 0) return false;
            if (before.length == 2) {
                if (ENTRIES.compareAndSet(this, before, EMPTY)) return true;
                continue;
            }
            int remove = found << 1;
            long[] after = new long[before.length - 2];
            System.arraycopy(before, 0, after, 0, remove);
            System.arraycopy(before, remove + 2, after, remove, before.length - remove - 2);
            if (ENTRIES.compareAndSet(this, before, after)) return true;
        }
    }

    public void clear() {
        for (;;) {
            long[] before = current();
            if (before.length == 0) return;
            if (ENTRIES.compareAndSet(this, before, EMPTY)) return;
        }
    }

    /** Allocation-free traversal of one captured immutable snapshot. */
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long[] snapshot = current();
        for (int index = 0; index < snapshot.length; index += 2) {
            consumer.accept(snapshot[index], snapshot[index + 1]);
        }
    }

    /** Boundary copy of interleaved sorted key/value pairs. */
    public long[] toInterleavedArray() {
        return current().clone();
    }

    /** Primitive keys from one captured immutable snapshot; no key/entry array is copied. */
    public java.util.stream.LongStream keyStream() {
        long[] snapshot = current();
        return java.util.stream.StreamSupport.longStream(new PairLane(snapshot, 0, snapshot.length >>> 1, 0), false);
    }

    /** Primitive values in sorted-key order; call parallel() for direct pair-index splitting. */
    public java.util.stream.LongStream valueStream() {
        long[] snapshot = current();
        return java.util.stream.StreamSupport.longStream(new PairLane(snapshot, 0, snapshot.length >>> 1, 1), false);
    }

    private static final class PairLane implements java.util.Spliterator.OfLong {
        private final long[] snapshot;
        private int cursor;
        private final int fence;
        private final int lane;
        private PairLane(long[] snapshot, int cursor, int fence, int lane) {
            this.snapshot = snapshot; this.cursor = cursor; this.fence = fence; this.lane = lane;
        }
        @Override public OfLong trySplit() {
            int middle = cursor + ((fence - cursor) >>> 1);
            if (middle == cursor) return null;
            PairLane prefix = new PairLane(snapshot, cursor, middle, lane); cursor = middle; return prefix;
        }
        @Override public boolean tryAdvance(java.util.function.LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (cursor == fence) return false;
            long value = snapshot[(cursor++ << 1) + lane]; action.accept(value); return true;
        }
        @Override public long estimateSize() { return fence - cursor; }
        @Override public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL | IMMUTABLE | (lane == 0 ? DISTINCT : 0);
        }
    }

    private static int find(long[] entries, long key) {
        // LeetCode 35 lower bound, branch-free halving over the key lane (one load + one select per level).
        int size = entries.length >>> 1;
        if (size == 0) return -1;
        int base = 0;
        for (int span = size; span > 1; ) {
            int half = span >>> 1;
            base = entries[(base + half) << 1] < key ? base + half : base;
            span -= half;
        }
        int low = base + (entries[base << 1] < key ? 1 : 0);
        return low < size && entries[low << 1] == key ? low : -(low + 1);
    }
}
