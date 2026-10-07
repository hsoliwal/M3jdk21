/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Primitive-long FIFO contract with allocation-free hot paths (Synexia {@code LongQueue} lineage).
 *
 * <p>Primitive {@code peek}/{@code poll} have no {@code null} sentinel: both throw
 * {@link NoSuchElementException} on an empty queue, and {@link #tryPoll} is the non-throwing
 * form. Batch defaults are plain loops over the single-element contract; owners may override
 * them with bulk copies.</p>
 */
public interface M3LongQueue extends M3LongCollection {
    boolean offer(long value);

    long peek();

    long poll();

    int capacity();

    default boolean tryPoll(long[] target, int targetIndex) {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(targetIndex, target.length);
        try {
            target[targetIndex] = poll();
            return true;
        } catch (NoSuchElementException empty) {
            return false;
        }
    }

    /**
     * Consumes one externally reserved item. Concurrent packed queues override this to spin on
     * publication without transient exception allocation.
     */
    default long pollReserved() {
        for (;;) {
            try {
                return poll();
            } catch (NoSuchElementException notPublishedYet) {
                Thread.onSpinWait();
            }
        }
    }

    default int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int accepted = 0;
        while (accepted < length && offer(source[offset + accepted])) {
            accepted++;
        }
        return accepted;
    }

    default int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int removed = 0;
        while (removed < length && tryPoll(target, offset + removed)) {
            removed++;
        }
        return removed;
    }
}
