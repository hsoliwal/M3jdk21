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
import java.util.function.LongConsumer;

/**
 * Bounded CAS-based MPMC queue backed only by primitive long arrays.
 *
 * <p>Each slot uses one primitive sequence word plus one primitive value word.
 * Producers and consumers reserve monotonically increasing positions with CAS.
 * There is no node, wrapper, entry or allocation per queued element.
 *
 * <p>A reserved slot is not published until its sequence word is released. Poll/peek
 * may report unavailable at an unpublished FIFO head even if a later offer completed;
 * this is not a linearizable empty observation. A stalled producer can obstruct FIFO
 * consumption, and a stalled consumer can obstruct slot reuse. CAS does not make this
 * protocol formally lock-free. {@code pollReserved()} waits for publication and has no
 * cancellation/timeout. Batch calls reserve a contiguous range, then publish/recycle
 * its slots individually; they are not atomic multi-element transactions.
 *
 * <p>Capacity is fixed and must be a power of two. {@link #sizeApprox()} is an
 * observation for telemetry/back-pressure, not a linearizable size contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongMpmcQueue.java}
 * blob {@code 0737671271c904bb7c56ba143710f7e43a325767} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongMpmcQueue implements LongQueue {
    private static final LongConsumer DISCARD = ignored -> { };
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle LONG_ELEMENT =
            MethodHandles.arrayElementVarHandle(long[].class);

    static {
        try {
            var lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(M3PackedLongMpmcQueue.class, "head", long.class);
            TAIL = lookup.findVarHandle(M3PackedLongMpmcQueue.class, "tail", long.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final long[] values;
    private final long[] sequences;
    private final int mask;
    private final int capacity;

    @SuppressWarnings("unused")
    private volatile long head;
    @SuppressWarnings("unused")
    private volatile long tail;

    public M3PackedLongMpmcQueue(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two >= 2");
        }
        if (capacity > (1 << 30)) {
            throw new IllegalArgumentException("capacity too large");
        }
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.values = new long[capacity];
        this.sequences = new long[capacity];
        for (int index = 0; index < capacity; index++) {
            LONG_ELEMENT.set(sequences, index, (long) index);
        }
    }

    public int capacity() {
        return capacity;
    }

    public int remainingCapacityApprox() {
        return Math.max(0, capacity - sizeApprox());
    }

    public boolean offer(long value) {
        for (;;) {
            long position = (long) TAIL.getAcquire(this);
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            long difference = sequence - position;

            if (difference == 0L) {
                if (TAIL.compareAndSet(this, position, position + 1L)) {
                    LONG_ELEMENT.setRelease(values, index, value);
                    LONG_ELEMENT.setRelease(sequences, index, position + 1L);
                    return true;
                }
            } else if (difference < 0L) {
                return false;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Attempts to consume one primitive value without allocating a result object.
     *
     * @param consumer invoked exactly once after a successful reservation
     * @return true when a value was removed; false if the head is not available
     */
    public boolean tryPoll(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (;;) {
            long position = (long) HEAD.getAcquire(this);
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            long difference = sequence - (position + 1L);

            if (difference == 0L) {
                if (HEAD.compareAndSet(this, position, position + 1L)) {
                    long value = (long) LONG_ELEMENT.getAcquire(values, index);
                    LONG_ELEMENT.setRelease(sequences, index, position + capacity);
                    consumer.accept(value);
                    return true;
                }
            } else if (difference < 0L) {
                return false;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Reads the current FIFO head without removing it.
     *
     * <p>The head/slot sequence is rechecked so the returned value was present
     * at one instant during this call even with concurrent consumers.
     */
    @Override public long peek() {
        for (;;) {
            long position = (long) HEAD.getAcquire(this);
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            long difference = sequence - (position + 1L);
            if (difference == 0L) {
                long value = (long) LONG_ELEMENT.getAcquire(values, index);
                if ((long) HEAD.getAcquire(this) == position
                        && (long) LONG_ELEMENT.getAcquire(sequences, index) == position + 1L) {
                    return value;
                }
            } else if (difference < 0L) {
                throw new NoSuchElementException();
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Removes one value or throws when the FIFO head is not available.
     *
     * <p>No sentinel is used; every long bit pattern remains a valid payload.
     */
    @Override public long poll() {
        for (;;) {
            long position = (long) HEAD.getAcquire(this);
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            long difference = sequence - (position + 1L);

            if (difference == 0L) {
                if (HEAD.compareAndSet(this, position, position + 1L)) {
                    long value = (long) LONG_ELEMENT.getAcquire(values, index);
                    LONG_ELEMENT.setRelease(sequences, index, position + capacity);
                    return value;
                }
            } else if (difference < 0L) {
                throw new NoSuchElementException();
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /** Alias matching conventional collection terminology. */
    public long remove() {
        return poll();
    }

    /**
     * Allocation-free variant writing into caller-owned storage.
     */
    @Override
    public boolean tryPoll(long[] target, int targetIndex) {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(targetIndex, target.length);
        for (;;) {
            long position = (long) HEAD.getAcquire(this);
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            long difference = sequence - (position + 1L);

            if (difference == 0L) {
                if (HEAD.compareAndSet(this, position, position + 1L)) {
                    target[targetIndex] = (long) LONG_ELEMENT.getAcquire(values, index);
                    LONG_ELEMENT.setRelease(sequences, index, position + capacity);
                    return true;
                }
            } else if (difference < 0L) {
                return false;
            } else {
                Thread.onSpinWait();
            }
        }
    }

    @Override
    public int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int requested = Math.min(length, capacity);
        if (requested == 0) return 0;
        for (;;) {
            long position = (long) TAIL.getAcquire(this);
            int available = 0;
            boolean stale = false;
            for (int slot = 0; slot < requested; slot++) {
                long p = position + slot;
                long sequence = (long) LONG_ELEMENT.getAcquire(sequences, (int) p & mask);
                long difference = sequence - p;
                if (difference == 0L) available++;
                else if (difference < 0L) break;
                else { stale = true; break; }
            }
            if (stale) { Thread.onSpinWait(); continue; }
            if (available == 0) return 0;
            if (!TAIL.compareAndSet(this, position, position + available)) {
                Thread.onSpinWait();
                continue;
            }
            for (int slot = 0; slot < available; slot++) {
                long p = position + slot;
                int index = (int) p & mask;
                LONG_ELEMENT.setRelease(values, index, source[offset + slot]);
                LONG_ELEMENT.setRelease(sequences, index, p + 1L);
            }
            return available;
        }
    }

    @Override
    public int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int requested = Math.min(length, capacity);
        if (requested == 0) return 0;
        for (;;) {
            long position = (long) HEAD.getAcquire(this);
            int available = 0;
            boolean stale = false;
            for (int slot = 0; slot < requested; slot++) {
                long p = position + slot;
                long sequence = (long) LONG_ELEMENT.getAcquire(sequences, (int) p & mask);
                long difference = sequence - (p + 1L);
                if (difference == 0L) available++;
                else if (difference < 0L) break;
                else { stale = true; break; }
            }
            if (stale) { Thread.onSpinWait(); continue; }
            if (available == 0) return 0;
            if (!HEAD.compareAndSet(this, position, position + available)) {
                Thread.onSpinWait();
                continue;
            }
            for (int slot = 0; slot < available; slot++) {
                long p = position + slot;
                int index = (int) p & mask;
                target[offset + slot] = (long) LONG_ELEMENT.getAcquire(values, index);
                LONG_ELEMENT.setRelease(sequences, index, p + capacity);
            }
            return available;
        }
    }

    @Override
    public long pollReserved() {
        for (;;) {
            long position = (long) HEAD.getAcquire(this);
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            if (sequence - (position + 1L) == 0L) {
                if (HEAD.compareAndSet(this, position, position + 1L)) {
                    long value = (long) LONG_ELEMENT.getAcquire(values, index);
                    LONG_ELEMENT.setRelease(sequences, index, position + capacity);
                    return value;
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Queue size observation clamped to the bounded capacity.
     */
    public int sizeApprox() {
        long observedTail = (long) TAIL.getAcquire(this);
        long observedHead = (long) HEAD.getAcquire(this);
        long difference = observedTail - observedHead;
        if (difference <= 0L) return 0;
        return (int) Math.min(capacity, difference);
    }

    @Override public int size() {
        return sizeApprox();
    }

    @Override public boolean contains(long value) {
        long observedHead = (long) HEAD.getAcquire(this);
        long observedTail = (long) TAIL.getAcquire(this);
        long count = Math.min(capacity, Math.max(0L, observedTail - observedHead));
        for (long offset = 0; offset < count; offset++) {
            long position = observedHead + offset;
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            if (sequence == position + 1L
                    && (long) LONG_ELEMENT.getAcquire(values, index) == value) {
                return true;
            }
        }
        return false;
    }

    @Override public void clear() {
        while (tryPoll(DISCARD)) {
            // drain
        }
    }

    @Override public long[] toArray() {
        long observedHead = (long) HEAD.getAcquire(this);
        long observedTail = (long) TAIL.getAcquire(this);
        int maximum = (int) Math.min(capacity, Math.max(0L, observedTail - observedHead));
        long[] result = new long[maximum];
        int size = 0;
        for (int offset = 0; offset < maximum; offset++) {
            long position = observedHead + offset;
            int index = (int) position & mask;
            long sequence = (long) LONG_ELEMENT.getAcquire(sequences, index);
            if (sequence == position + 1L) {
                result[size++] = (long) LONG_ELEMENT.getAcquire(values, index);
            }
        }
        return size == result.length ? result : java.util.Arrays.copyOf(result, size);
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
