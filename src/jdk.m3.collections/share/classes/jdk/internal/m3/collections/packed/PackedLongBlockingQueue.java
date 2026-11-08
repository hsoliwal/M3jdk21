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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongQueue;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jdk.internal.m3.collections.PrimitiveConcurrentQueues;
import jdk.internal.m3.collections.concurrent.PackedLongMpmcQueue;
import jdk.internal.m3.collections.concurrent.PackedLongSpscQueue;

/**
 * Blocking primitive long queue with queue-level synchronization only.
 * SPSC pays one payload array; MPMC pays payload + sequence arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongBlockingQueue.java}
 * blob {@code 3228a6ef3ecde4c51b892395b3135c7179fa9f2a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongBlockingQueue {
    private final LongQueue queue;
    private final Semaphore availableItems;
    private final Semaphore availableSlots;

    public PackedLongBlockingQueue(int capacity) {
        this(capacity, false, PrimitiveConcurrentQueues.Topology.MPMC);
    }
    public PackedLongBlockingQueue(int capacity, boolean fair) {
        this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
    }
    public PackedLongBlockingQueue(int capacity, PrimitiveConcurrentQueues.Topology topology) {
        this(capacity, false, topology);
    }
    public PackedLongBlockingQueue(
            int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
        Objects.requireNonNull(topology, "topology");
        queue = switch (topology) {
            case SPSC -> new PackedLongSpscQueue(capacity);
            case MPMC -> new PackedLongMpmcQueue(capacity);
        };
        availableItems = new Semaphore(0, fair);
        availableSlots = new Semaphore(capacity, fair);
    }

    public int capacity() { return queue.capacity(); }
    public int sizeApprox() { return queue.size(); }
    public int remainingCapacityApprox() { return availableSlots.availablePermits(); }

    public boolean offer(long value) {
        if (!availableSlots.tryAcquire()) return false;
        publishReserved(value);
        return true;
    }
    public boolean offer(long value, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit, "unit");
        if (!availableSlots.tryAcquire(timeout, unit)) return false;
        publishReserved(value);
        return true;
    }
    public void put(long value) throws InterruptedException {
        availableSlots.acquire();
        publishReserved(value);
    }
    public long poll() {
        if (!availableItems.tryAcquire()) throw new NoSuchElementException();
        return consumeReserved();
    }
    public long poll(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        Objects.requireNonNull(unit, "unit");
        if (!availableItems.tryAcquire(timeout, unit)) {
            throw new TimeoutException("primitive queue poll timed out");
        }
        return consumeReserved();
    }
    public boolean tryPoll(long[] target, int targetIndex, long timeout, TimeUnit unit)
            throws InterruptedException {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(targetIndex, target.length);
        Objects.requireNonNull(unit, "unit");
        if (!availableItems.tryAcquire(timeout, unit)) return false;
        target[targetIndex] = consumeReserved();
        return true;
    }
    public long take() throws InterruptedException {
        availableItems.acquire();
        return consumeReserved();
    }

    public int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int permits = acquireAvailable(availableSlots, length);
        if (permits == 0) return 0;
        int published = 0;
        try {
            while (published < permits) {
                int n = queue.offerBatch(source, offset + published, permits - published);
                if (n == 0) Thread.onSpinWait(); else published += n;
            }
            return published;
        } finally {
            if (published > 0) availableItems.release(published);
            if (published < permits) availableSlots.release(permits - published);
        }
    }

    public int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int permits = acquireAvailable(availableItems, length);
        if (permits == 0) return 0;
        int removed = 0;
        try {
            while (removed < permits) {
                int n = queue.drainTo(target, offset + removed, permits - removed);
                if (n == 0) Thread.onSpinWait(); else removed += n;
            }
            return removed;
        } finally {
            if (removed > 0) availableSlots.release(removed);
            if (removed < permits) availableItems.release(permits - removed);
        }
    }

    private void publishReserved(long value) {
        boolean published = false;
        try {
            while (!(published = queue.offer(value))) Thread.onSpinWait();
        } finally {
            if (published) availableItems.release();
            else availableSlots.release();
        }
    }
    private long consumeReserved() {
        boolean removed = false;
        try {
            long value = queue.pollReserved();
            removed = true;
            return value;
        } finally {
            if (removed) availableSlots.release();
            else availableItems.release();
        }
    }
    private static int acquireAvailable(Semaphore semaphore, int maximum) {
        int wanted = Math.min(maximum, semaphore.availablePermits());
        while (wanted > 0) {
            if (semaphore.tryAcquire(wanted)) return wanted;
            wanted = Math.min(wanted - 1, semaphore.availablePermits());
        }
        return 0;
    }
}
