/* *****************************************************************************
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
package jdk.m3.collections;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Blocking facade over {@link PrimitiveMpmcRingBuffer}.
 *
 * <p>The queue data path remains lock-free CAS/sequence-slot logic. Two
 * collection-level semaphores are used only to park callers waiting for an
 * available slot or item. There is still no lock, semaphore, node or wrapper
 * retained per queue entry.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSemaphoreMpmcQueue.java}
 * blob {@code e3df63561821d8c0f773426e0515175d18adce89} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveSemaphoreMpmcQueue {
    private final PrimitiveMpmcRingBuffer queue;
    private final Semaphore slots;
    private final Semaphore items;

    public PrimitiveSemaphoreMpmcQueue(PrimitiveKind kind, int capacity) {
        this(kind, capacity, false);
    }

    public PrimitiveSemaphoreMpmcQueue(
            PrimitiveKind kind, int capacity, boolean fairWaiters) {
        queue = new PrimitiveMpmcRingBuffer(
                Objects.requireNonNull(kind, "kind"), capacity);
        slots = new Semaphore(queue.capacity(), fairWaiters);
        items = new Semaphore(0, fairWaiters);
    }

    public PrimitiveKind kind() { return queue.kind(); }
    public int capacity() { return queue.capacity(); }
    public long payloadBytes() { return queue.payloadBytes(); }
    public int size() { return queue.size(); }
    public int availableItems() { return items.availablePermits(); }
    public int availableSlots() { return slots.availablePermits(); }

    public boolean tryPutBits(long bits) {
        if (!slots.tryAcquire()) return false;
        boolean published = false;
        try {
            queue.offerClaimedBits(bits);
            published = true;
            return true;
        } finally {
            if (published) items.release();
            else slots.release();
        }
    }

    public void putBits(long bits) throws InterruptedException {
        slots.acquire();
        boolean published = false;
        try {
            queue.offerClaimedBits(bits);
            published = true;
        } finally {
            if (published) items.release();
            else slots.release();
        }
    }

    public boolean putBits(long bits, long timeout, TimeUnit unit)
            throws InterruptedException {
        Objects.requireNonNull(unit, "unit");
        if (!slots.tryAcquire(timeout, unit)) return false;
        boolean published = false;
        try {
            queue.offerClaimedBits(bits);
            published = true;
            return true;
        } finally {
            if (published) items.release();
            else slots.release();
        }
    }

    public long takeBits() throws InterruptedException {
        items.acquire();
        boolean consumed = false;
        try {
            long value = queue.pollClaimedBits();
            consumed = true;
            return value;
        } finally {
            if (consumed) slots.release();
            else items.release();
        }
    }

    public long pollOrDefaultBits(long defaultValueBits) {
        if (!items.tryAcquire()) return defaultValueBits;
        boolean consumed = false;
        try {
            long value = queue.pollClaimedBits();
            consumed = true;
            return value;
        } finally {
            if (consumed) slots.release();
            else items.release();
        }
    }

    public long pollOrDefaultBits(
            long defaultValueBits, long timeout, TimeUnit unit)
            throws InterruptedException {
        Objects.requireNonNull(unit, "unit");
        if (!items.tryAcquire(timeout, unit)) return defaultValueBits;
        boolean consumed = false;
        try {
            long value = queue.pollClaimedBits();
            consumed = true;
            return value;
        } finally {
            if (consumed) slots.release();
            else items.release();
        }
    }
}
