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

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongQueue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Bounded single-producer/single-consumer primitive long FIFO.
 *
 * <p>Unlike the MPMC queue this topology needs no per-slot sequence array:
 * retained storage is exactly one {@code long[]} payload lane plus primitive
 * head/tail counters. One producer thread owns {@link #offer(long)} and one
 * consumer thread owns {@link #poll()}/{@link #tryPoll(long[], int)}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSpscQueue.java}
 * blob {@code 7184e5e88ecffeb4ec57b6dfc04e9d1cc03d1c8a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongSpscQueue implements LongQueue {
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle LONG_ELEMENT =
            MethodHandles.arrayElementVarHandle(long[].class);

    static {
        try {
            var lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(M3PackedLongSpscQueue.class, "head", long.class);
            TAIL = lookup.findVarHandle(M3PackedLongSpscQueue.class, "tail", long.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final long[] values;
    private final int mask;
    private final int capacity;

    @SuppressWarnings("unused")
    private volatile long head;
    @SuppressWarnings("unused")
    private volatile long tail;

    public M3PackedLongSpscQueue(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two >= 2");
        }
        if (capacity > (1 << 30)) throw new IllegalArgumentException("capacity too large");
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.values = new long[capacity];
    }

    @Override public int capacity() { return capacity; }

    @Override public boolean offer(long value) {
        long producerTail = (long) TAIL.getOpaque(this);
        long observedHead = (long) HEAD.getAcquire(this);
        if (producerTail - observedHead >= capacity) return false;
        int index = (int) producerTail & mask;
        LONG_ELEMENT.setRelease(values, index, value);
        TAIL.setRelease(this, producerTail + 1L);
        return true;
    }

    @Override public long peek() {
        long consumerHead = (long) HEAD.getOpaque(this);
        long observedTail = (long) TAIL.getAcquire(this);
        if (consumerHead == observedTail) throw new NoSuchElementException();
        return (long) LONG_ELEMENT.getAcquire(values, (int) consumerHead & mask);
    }

    @Override public long poll() {
        long consumerHead = (long) HEAD.getOpaque(this);
        long observedTail = (long) TAIL.getAcquire(this);
        if (consumerHead == observedTail) throw new NoSuchElementException();
        int index = (int) consumerHead & mask;
        long value = (long) LONG_ELEMENT.getAcquire(values, index);
        HEAD.setRelease(this, consumerHead + 1L);
        return value;
    }

    @Override
    public boolean tryPoll(long[] target, int targetIndex) {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(targetIndex, target.length);
        long consumerHead = (long) HEAD.getOpaque(this);
        long observedTail = (long) TAIL.getAcquire(this);
        if (consumerHead == observedTail) return false;
        int index = (int) consumerHead & mask;
        target[targetIndex] = (long) LONG_ELEMENT.getAcquire(values, index);
        HEAD.setRelease(this, consumerHead + 1L);
        return true;
    }

    @Override
    public int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length == 0) return 0;
        long producerTail = (long) TAIL.getOpaque(this);
        long observedHead = (long) HEAD.getAcquire(this);
        int accepted = (int) Math.min(length,
                Math.max(0L, capacity - (producerTail - observedHead)));
        if (accepted == 0) return 0;
        int index = (int) producerTail & mask;
        int first = Math.min(accepted, capacity - index);
        System.arraycopy(source, offset, values, index, first);
        if (first < accepted) {
            System.arraycopy(source, offset + first, values, 0, accepted - first);
        }
        TAIL.setRelease(this, producerTail + accepted);
        return accepted;
    }

    @Override
    public int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        if (length == 0) return 0;
        long consumerHead = (long) HEAD.getOpaque(this);
        long observedTail = (long) TAIL.getAcquire(this);
        int removed = (int) Math.min(length, Math.max(0L, observedTail - consumerHead));
        if (removed == 0) return 0;
        int index = (int) consumerHead & mask;
        int first = Math.min(removed, capacity - index);
        System.arraycopy(values, index, target, offset, first);
        if (first < removed) {
            System.arraycopy(values, 0, target, offset + first, removed - first);
        }
        HEAD.setRelease(this, consumerHead + removed);
        return removed;
    }

    @Override
    public long pollReserved() {
        for (;;) {
            long consumerHead = (long) HEAD.getOpaque(this);
            long observedTail = (long) TAIL.getAcquire(this);
            if (consumerHead != observedTail) {
                int index = (int) consumerHead & mask;
                long value = (long) LONG_ELEMENT.getAcquire(values, index);
                HEAD.setRelease(this, consumerHead + 1L);
                return value;
            }
            Thread.onSpinWait();
        }
    }

    @Override public int size() {
        long observedTail = (long) TAIL.getAcquire(this);
        long observedHead = (long) HEAD.getAcquire(this);
        return (int) Math.min(capacity, Math.max(0L, observedTail - observedHead));
    }

    @Override public boolean contains(long value) {
        long observedHead = (long) HEAD.getAcquire(this);
        long observedTail = (long) TAIL.getAcquire(this);
        int count = (int) Math.min(capacity, Math.max(0L, observedTail - observedHead));
        for (int offset = 0; offset < count; offset++) {
            if ((long) LONG_ELEMENT.getAcquire(
                    values, (int) (observedHead + offset) & mask) == value) return true;
        }
        return false;
    }

    @Override public void clear() {
        HEAD.setRelease(this, (long) TAIL.getAcquire(this));
    }

    @Override public long[] toArray() {
        long observedHead = (long) HEAD.getAcquire(this);
        long observedTail = (long) TAIL.getAcquire(this);
        int count = (int) Math.min(capacity, Math.max(0L, observedTail - observedHead));
        long[] result = new long[count];
        for (int offset = 0; offset < count; offset++) {
            result[offset] = (long) LONG_ELEMENT.getAcquire(
                    values, (int) (observedHead + offset) & mask);
        }
        return result;
    }

    @Override public LongIterator iterator() {
        long[] snapshot = toArray();
        return new LongIterator() {
            private int index;
            @Override public boolean hasNext() { return index < snapshot.length; }
            @Override public long nextLong() {
                if (!hasNext()) throw new NoSuchElementException();
                return snapshot[index++];
            }
        };
    }
}
